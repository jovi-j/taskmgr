package com.jovij.taskmgr

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jovij.taskmgr.ui.theme.TaskmgrTheme
import com.taskmgr.ui.MainScreen
import com.taskmgr.ui.viewmodel.TaskMgrViewModel

class MainActivity : ComponentActivity() {

    private var pendingOpenTaskId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val app = application as TaskMgrApplication
        setContent {
            TaskmgrTheme {
                val vm: TaskMgrViewModel = viewModel(
                    factory = TaskMgrViewModel.Factory(app.container)
                )
                MainScreen(
                    viewModel = vm,
                    pendingOpenTaskId = pendingOpenTaskId,
                    onConsumedOpenTaskId = { pendingOpenTaskId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val taskId = intent?.getLongExtra(EXTRA_OPEN_TASK_ID, -1L) ?: -1L
        if (taskId > 0L) {
            pendingOpenTaskId = taskId
            intent?.removeExtra(EXTRA_OPEN_TASK_ID)
        }
    }

    companion object {
        const val EXTRA_OPEN_TASK_ID = "com.jovij.taskmgr.EXTRA_OPEN_TASK_ID"
        const val ACTION_OPEN_TASK_PREFIX = "com.jovij.taskmgr.OPEN_TASK_"
    }
}
