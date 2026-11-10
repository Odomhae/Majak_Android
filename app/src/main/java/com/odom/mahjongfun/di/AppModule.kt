package com.odom.mahjongfun.di

import com.odom.mahjongfun.core.util.Clock
import com.odom.mahjongfun.core.util.SystemClock
import com.odom.mahjongfun.data.level.AssetLevelRepository
import com.odom.mahjongfun.data.level.LevelSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindLevelSource(impl: AssetLevelRepository): LevelSource
}
