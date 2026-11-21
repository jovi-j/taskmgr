package com.jovij.taskmgr

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jovij.taskmgr.ui.theme.TaskmgrTheme
import com.taskmgr.ui.MainScreen
import com.taskmgr.ui.viewmodel.TaskMgrViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                MainScreen(viewModel = vm)
            }
        }
    }
}
