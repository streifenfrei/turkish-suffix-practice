package com.example.suffixtrainer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.suffixtrainer.audio.AudioPlayer
import com.example.suffixtrainer.audio.Media3AudioPlayer
import com.example.suffixtrainer.data.CardRepository
import com.example.suffixtrainer.data.DataStorePreferencesRepository
import com.example.suffixtrainer.data.FakeCardRepository
import com.example.suffixtrainer.data.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level bindings. Currently wires the **fake** in-memory corpus; swapping to the
 * Room-backed implementation later is a one-line change to the [bindCardRepository] binding.
 * The Room `createFromAsset` path (DatabaseModule) is left in place but unused.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: FakeCardRepository): CardRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: Media3AudioPlayer): AudioPlayer

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile("settings")
            }
    }
}
