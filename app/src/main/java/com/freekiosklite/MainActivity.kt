package com.freekiosklite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.freekiosklite.ui.KioskApp
import com.freekiosklite.ui.KioskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: KioskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KioskApp(viewModel) }
    }
}
