package com.example.gymlog

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("GymLogPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val templates: StateFlow<List<WorkoutTemplate>> = _templates

    private val _workoutLogs = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val workoutLogs: StateFlow<List<WorkoutLog>> = _workoutLogs

    init {
        loadTemplates()
        loadLogs()
    }

    private fun loadTemplates() {
        val json = prefs.getString("templates", null)
        if (json != null) {
            val type = object : TypeToken<List<WorkoutTemplate>>() {}.type
            _templates.value = gson.fromJson(json, type)
        }
    }

    private fun loadLogs() {
        val json = prefs.getString("logs", null)
        if (json != null) {
            val type = object : TypeToken<List<WorkoutLog>>() {}.type
            _workoutLogs.value = gson.fromJson(json, type)
        }
    }

    fun saveTemplate(template: WorkoutTemplate) {
        val currentList = _templates.value.toMutableList()
        currentList.add(template)
        _templates.value = currentList
        prefs.edit().putString("templates", gson.toJson(currentList)).apply()
    }

    // NUOVA FUNZIONE: Aggiorna una scheda modificata
    fun updateTemplate(updatedTemplate: WorkoutTemplate) {
        val currentList = _templates.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedTemplate.id }
        if (index != -1) {
            currentList[index] = updatedTemplate
            _templates.value = currentList
            prefs.edit().putString("templates", gson.toJson(currentList)).apply()
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) {
        val currentList = _templates.value.toMutableList()
        currentList.remove(template)
        _templates.value = currentList
        prefs.edit().putString("templates", gson.toJson(currentList)).apply()
    }

    fun saveWorkoutLog(log: WorkoutLog) {
        val currentList = _workoutLogs.value.toMutableList()
        currentList.add(log)
        _workoutLogs.value = currentList
        prefs.edit().putString("logs", gson.toJson(currentList)).apply()
    }

    // NUOVA FUNZIONE: Elimina un allenamento dallo storico
    fun deleteWorkoutLog(log: WorkoutLog) {
        val currentList = _workoutLogs.value.toMutableList()
        currentList.remove(log)
        _workoutLogs.value = currentList
        prefs.edit().putString("logs", gson.toJson(currentList)).apply()
    }
}