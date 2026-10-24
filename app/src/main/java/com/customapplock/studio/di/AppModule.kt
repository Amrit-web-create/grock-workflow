package com.customapplock.studio.di

import android.content.Context
import androidx.room.Room
import com.customapplock.studio.data.AppDatabase
import com.customapplock.studio.data.AppLockDao
import com.customapplock.studio.data.IntruderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "applock.db").build()

    @Provides fun provideLockDao(db: AppDatabase): AppLockDao = db.appLockDao()
    @Provides fun provideIntruderDao(db: AppDatabase): IntruderDao = db.intruderDao()
}
