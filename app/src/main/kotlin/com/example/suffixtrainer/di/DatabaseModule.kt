package com.example.suffixtrainer.di

import android.content.Context
import androidx.room.Room
import com.example.suffixtrainer.data.AppDatabase
import com.example.suffixtrainer.data.SentenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "trainer.db")
            // The corpus ships prebuilt in assets; the app never writes to it.
            .createFromAsset("trainer.db")
            .build()

    @Provides
    fun provideSentenceDao(database: AppDatabase): SentenceDao = database.sentenceDao()
}
