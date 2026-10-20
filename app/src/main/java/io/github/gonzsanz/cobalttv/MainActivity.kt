package io.github.gonzsanz.cobalttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.gonzsanz.cobalttv.core.designsystem.CobaltTvTheme
import io.github.gonzsanz.cobalttv.ui.CobaltTvApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CobaltTvTheme {
                CobaltTvApp()
            }
        }
    }
}
