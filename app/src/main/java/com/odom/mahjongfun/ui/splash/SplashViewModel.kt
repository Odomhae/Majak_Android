package com.odom.mahjongfun.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 이후 마일스톤에서 로그인 시도, UMP·광고 초기화, 레벨 데이터 프리로드를 여기서 수행한다. */
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            delay(MIN_SPLASH_MILLIS)
            _ready.value = true
        }
    }

    private companion object {
        const val MIN_SPLASH_MILLIS = 800L
    }
}
