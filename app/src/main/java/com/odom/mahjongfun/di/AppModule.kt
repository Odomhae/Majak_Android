package com.odom.mahjongfun.di

import com.odom.mahjongfun.core.util.Clock
import com.odom.mahjongfun.core.util.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock
}
