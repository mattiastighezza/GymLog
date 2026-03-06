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

    // NUOVO: La lista di tutti gli esercizi mai creati
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    init {
        loadTemplates()
        loadLogs()
        loadExercises()
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

    private fun loadExercises() {
        val json = prefs.getString("exercises", null)
        if (json != null) {
            val type = object : TypeToken<List<Exercise>>() {}.type
            _exercises.value = gson.fromJson(json, type)
        } else {
            // Se è la prima volta che apre l'app, inseriamo due esercizi di base
            val defaultExercises = listOf(Exercise("Panca Piana"), Exercise("Squat"))
            _exercises.value = defaultExercises
            prefs.edit().putString("exercises", gson.toJson(defaultExercises)).apply()
        }
    }

    fun saveTemplate(template: WorkoutTemplate) {
        val currentList = _templates.value.toMutableList()
        currentList.add(template)
        _templates.value = currentList
        prefs.edit().putString("templates", gson.toJson(currentList)).apply()
    }

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

    fun deleteWorkoutLog(log: WorkoutLog) {
        val currentList = _workoutLogs.value.toMutableList()
        currentList.remove(log)
        _workoutLogs.value = currentList
        prefs.edit().putString("logs", gson.toJson(currentList)).apply()
    }

    // NUOVE FUNZIONI: Aggiungi e Rimuovi Esercizio dalla libreria
    fun addExerciseToLibrary(name: String) {
        val currentList = _exercises.value.toMutableList()
        // Controlliamo che non esista già (ignorando maiuscole/minuscole)
        if (currentList.none { it.name.equals(name, ignoreCase = true) }) {
            currentList.add(Exercise(name = name))
            // Ordiniamo la lista alfabeticamente
            val sortedList = currentList.sortedBy { it.name }
            _exercises.value = sortedList
            prefs.edit().putString("exercises", gson.toJson(sortedList)).apply()
        }
    }

    fun deleteExerciseFromLibrary(exercise: Exercise) {
        val currentList = _exercises.value.toMutableList()
        currentList.remove(exercise)
        _exercises.value = currentList
        prefs.edit().putString("exercises", gson.toJson(currentList)).apply()
    }
}