package com.elendheim.vocalrange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.elendheim.vocalrange.audio.PitchEngine
import com.elendheim.vocalrange.ui.HistoryScreen
import com.elendheim.vocalrange.ui.SingScreen
import com.elendheim.vocalrange.ui.theme.ElendheimTheme

class MainActivity : ComponentActivity() {
    private val engine = PitchEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElendheimTheme {
                AppRoot(engine)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.stop()
    }
}

private enum class Screen { Sing, History }

@Composable
private fun AppRoot(engine: PitchEngine) {
    var screen by remember { mutableStateOf(Screen.Sing) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.Sing,
                    onClick = { screen = Screen.Sing },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text("Sing") },
                )
                NavigationBarItem(
                    selected = screen == Screen.History,
                    onClick = { screen = Screen.History },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("History") },
                )
            }
        }
    ) { padding ->
        when (screen) {
            Screen.Sing -> SingScreen(engine, Modifier.padding(padding))
            Screen.History -> HistoryScreen(Modifier.padding(padding))
        }
    }
}
