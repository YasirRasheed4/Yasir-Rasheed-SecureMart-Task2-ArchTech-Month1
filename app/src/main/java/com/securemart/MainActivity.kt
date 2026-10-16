package com.securemart

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.securemart.security.SecurityConfig
import com.securemart.ui.SecureMartTheme

class MainActivity : ComponentActivity() {

    private val vm: MartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SecurityConfig.BLOCK_SCREENSHOTS) {
            // Blocks screenshots, screen recording and the recent-apps preview.
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            SecureMartTheme {
                SecureMartApp(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.checkSession()
    }

    override fun onStop() {
        super.onStop()
        vm.markInactive()
    }
}
