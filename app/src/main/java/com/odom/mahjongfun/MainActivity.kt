package com.odom.mahjongfun

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.odom.mahjongfun.ui.components.FeltBackground
import com.odom.mahjongfun.ui.navigation.AppNavHost
import com.odom.mahjongfun.ui.theme.MahjongFunTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 배경이 항상 어두운 초록이므로 시스템 바 아이콘은 항상 밝게.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            MahjongFunTheme {
                FeltBackground { AppNavHost() }
            }
        }
    }
}
