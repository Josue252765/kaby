package com.mofoxx.kaby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mofoxx.kaby.backup.BackupScheduler
import com.mofoxx.kaby.ui.KabyApp
import com.mofoxx.kaby.ui.KabyViewModel
import com.mofoxx.kaby.ui.KabyViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as KabyApplication).repository
        setContent {
            val viewModel: KabyViewModel = viewModel(factory = KabyViewModelFactory(repository))
            KabyApp(viewModel)
        }
    }

    override fun onStop() {
        super.onStop()
        BackupScheduler.scheduleAfterBackground(this)
    }
}
