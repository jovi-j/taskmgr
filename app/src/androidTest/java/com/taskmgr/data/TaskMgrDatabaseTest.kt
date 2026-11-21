package com.taskmgr.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskmgr.data.local.HabitEntity
import com.taskmgr.data.local.HabitGroupEntity
import com.taskmgr.data.local.HabitLogEntity
import com.taskmgr.data.local.MomentEntity
import com.taskmgr.data.local.TaskEntity
import com.taskmgr.data.local.TaskMgrDatabase
import com.taskmgr.data.repository.HabitGroupRepositoryImpl
import com.taskmgr.data.repository.HabitRepositoryImpl
import com.taskmgr.domain.contract.FullScreenAlertDispatcher
import com.taskmgr.domain.contract.ReminderScheduler
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.FullScreenReminderPayload
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.ReminderItemType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskMgrDatabaseTest {

    private lateinit var db: TaskMgrDatabase

    private val noOpScheduler = object : ReminderScheduler {
        override suspend fun scheduleNextForTask(taskId: Long) {}
        override suspend fun scheduleNextForHabit(habitId: Long, epochDay: Long) {}
        override suspend fun cancelTaskReminders(taskId: Long) {}
        override suspend fun cancelHabitRemindersForDay(habitId: Long, epochDay: Long) {}
        override suspend fun reconcileAll(nowEpochMillis: Long) {}
    }

    private val noOpDispatcher = object : FullScreenAlertDispatcher {
        override fun canUseFullScreenIntent(): Boolean = true
        override fun triggerFullScreenReminder(payload: FullScreenReminderPayload) {}
        override fun dismissReminder(itemType: ReminderItemType, itemId: Long) {}
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskMgrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deleteMoment_setsNullOnTaskAndHabit_andDeleteHabit_cascadesLogs() = runBlocking {
        val momentId = db.momentDao().upsert(
            MomentEntity(
                name = "Manhã",
                startMinutesOfDay = 480,
                endMinutesOfDay = 720,
                daysOfWeekMask = 127,
                reminderMode = "ONCE_AT_START"
            )
        )

        val taskId = db.taskDao().upsert(
            TaskEntity(
                title = "Relatório",
                startEpochDay = 20_000L,
                endEpochDay = 20_002L,
                momentId = momentId
            )
        )

        val habitId = db.habitDao().upsert(
            HabitEntity(
                title = "Água",
                startEpochDay = 20_000L,
                momentId = momentId,
                completionType = "CUSTOM_UNIT",
                incrementAmount = 250.0,
                incrementUnitLabel = "+250 ml",
                targetAmount = 2000.0,
                targetUnitLabel = "2000 ml"
            )
        )

        db.habitLogDao().upsert(
            HabitLogEntity(
                habitId = habitId,
                epochDay = 20_000L,
                accumulatedAmount = 500.0,
                isCompleted = false,
                dismissedForDay = false,
                updatedAtEpochMillis = 1234L
            )
        )

        db.momentDao().deleteById(momentId)
        val fetchedTask = db.taskDao().getById(taskId)
        val fetchedHabit = db.habitDao().getById(habitId)
        assertNotNull(fetchedTask)
        assertNotNull(fetchedHabit)
        assertNull(fetchedTask!!.momentId)
        assertNull(fetchedHabit!!.momentId)

        db.habitDao().deleteById(habitId)
        assertNull(db.habitLogDao().getLog(habitId, 20_000L))
    }

    @Test
    fun ac06_retroactiveHabitLogEdit_persistsAndAppearsInRangeQuery() = runBlocking {
        val repo = HabitRepositoryImpl(
            habitDao = db.habitDao(),
            habitLogDao = db.habitLogDao(),
            schedulerProvider = { noOpScheduler },
            alertDispatcherProvider = { noOpDispatcher }
        )

        val habitId = repo.upsertHabit(
            Habit(
                title = "Beber água",
                startEpochDay = 19_990L,
                activeDaysMask = 127,
                completionType = CompletionType.CUSTOM_UNIT,
                incrementAmount = 250.0,
                incrementUnitLabel = "+250 ml",
                targetAmount = 2000.0,
                targetUnitLabel = "2000 ml"
            )
        )

        val pastDay = 19_998L
        val updated = repo.updateHabitLogManual(
            habitId = habitId,
            epochDay = pastDay,
            accumulatedAmount = 2000.0,
            dismissedForDay = false,
            nowEpochMillis = 99999L
        )
        assertTrue(updated.isCompleted)
        assertEquals(99999L, updated.updatedAtEpochMillis)

        val rangeLogs = repo.observeHabitLogsForRange(19_990L, 20_005L).first()
        assertEquals(1, rangeLogs.size)
        assertEquals(pastDay, rangeLogs.first().epochDay)
        assertTrue(rangeLogs.first().isCompleted)
    }

    @Test
    fun habitGroup_persistsColorAndIcon_andDeleteSetsNullOnHabit() = runBlocking {
        val groupRepo = HabitGroupRepositoryImpl(db.habitGroupDao())
        val groupId = db.habitGroupDao().upsert(
            HabitGroupEntity(
                name = "Casa",
                colorArgb = 0xFFEF4444,
                iconGlyph = "\uF02DC"
            )
        )

        val habitId = db.habitDao().upsert(
            HabitEntity(
                title = "Arrumar quarto",
                startEpochDay = 20_000L,
                groupId = groupId,
                iconGlyph = "\uF00E2",
                completionType = "BOOLEAN"
            )
        )

        val groups = groupRepo.observeGroups().first()
        assertEquals(1, groups.size)
        assertEquals("Casa", groups.first().name)
        assertEquals(0xFFEF4444, groups.first().colorArgb)

        val fetchedHabit = db.habitDao().getById(habitId)
        assertNotNull(fetchedHabit)
        assertEquals(groupId, fetchedHabit!!.groupId)
        assertEquals("\uF00E2", fetchedHabit.iconGlyph)

        groupRepo.delete(groupId)
        val afterDelete = db.habitDao().getById(habitId)
        assertNotNull(afterDelete)
        assertNull(afterDelete!!.groupId)
    }
}
