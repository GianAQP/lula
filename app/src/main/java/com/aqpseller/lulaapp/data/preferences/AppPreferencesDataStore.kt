package com.aqpseller.lulaapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.privacidadDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacidad_prefs")

val Context.ajustesDataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes_prefs")
