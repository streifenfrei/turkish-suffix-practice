package com.example.suffixtrainer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.suffixtrainer.data.CardRepository
import com.example.suffixtrainer.data.DataStorePreferencesRepository
import com.example.suffixtrainer.data.PreferencesRepository
import com.example.suffixtrainer.data.RoomCardRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level bindings. The corpus comes from the prepackaged Room database
 * ([RoomCardRepository] over DatabaseModule's `createFromAsset`). [FakeCardRepository] is kept
 * for fake-based UI dev and tests; flip this binding back to it to run without the asset db.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: RoomCardRepository): CardRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile("settings")
            }
    }
}
