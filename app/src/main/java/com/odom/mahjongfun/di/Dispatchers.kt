package com.odom.mahjongfun.di

import javax.inject.Qualifier

/** CPU를 쓰는 작업(판 생성 등)용 디스패처. 테스트에서는 테스트 디스패처로 바꿔 끼운다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
