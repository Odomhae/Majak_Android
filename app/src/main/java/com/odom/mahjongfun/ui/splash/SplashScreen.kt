package com.odom.mahjongfun.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.mahjongfun.R
import com.odom.mahjongfun.ui.components.PlaceholderScreen

@Composable
fun SplashScreen(onReady: () -> Unit, viewModel: SplashViewModel = hiltViewModel()) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    LaunchedEffect(ready) { if (ready) onReady() }
    PlaceholderScreen(title = stringResource(R.string.app_name))
}
