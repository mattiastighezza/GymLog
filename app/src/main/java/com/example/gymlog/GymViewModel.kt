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

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _activeTemplate = MutableStateFlow<WorkoutTemplate?>(null)
    val activeTemplate: StateFlow<WorkoutTemplate?> = _activeTemplate

    private val _activeExercises = MutableStateFlow<List<LoggedExercise>?>(null)
    val activeExercises: StateFlow<List<LoggedExercise>?> = _activeExercises

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

    fun addExerciseToLibrary(name: String) {
        val currentList = _exercises.value.toMutableList()
        if (currentList.none { it.name.equals(name, ignoreCase = true) }) {
            currentList.add(Exercise(name = name))
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

    // ==========================================================
    // FUNZIONI PER L'ALLENAMENTO ATTIVO
    // ==========================================================

    fun startWorkout(template: WorkoutTemplate) {
        if (_activeTemplate.value?.id == template.id && _activeExercises.value != null) return

        _activeTemplate.value = template
        _activeExercises.value = template.exercises.map { config ->

            // MAGIA: Cerca l'ultimo peso usato scorrendo lo storico al contrario (dal più recente)
            var foundLastWeight: Double? = null
            for (log in _workoutLogs.value.reversed()) {
                val pastEx = log.exercises.find { it.exerciseName == config.exerciseName }
                if (pastEx != null) {
                    val maxW = pastEx.sets.filter { it.completed }.maxOfOrNull { it.weight }
                    if (maxW != null && maxW > 0) {
                        foundLastWeight = maxW
                        break // Trovato! Smettiamo di cercare.
                    }
                }
            }

            LoggedExercise(
                exerciseName = config.exerciseName,
                note = config.note,
                isTimeBased = config.isTimeBased,
                lastWeight = foundLastWeight, // Impostiamo il peso trovato
                sets = List(config.sets) {
                    LoggedSet(weight = 0.0, reps = config.reps, timeSeconds = config.timeSeconds, completed = false)
                }
            )
        }
    }

    fun updateActiveSet(exIndex: Int, setIndex: Int, newSet: LoggedSet) {
        val currentList = _activeExercises.value?.toMutableList() ?: return
        val newSets = currentList[exIndex].sets.toMutableList()
        newSets[setIndex] = newSet
        currentList[exIndex] = currentList[exIndex].copy(sets = newSets)
        _activeExercises.value = currentList
    }

    // NUOVO: Aggiunge una serie extra
    fun addSetToActiveExercise(exIndex: Int) {
        val currentList = _activeExercises.value?.toMutableList() ?: return
        val exercise = currentList[exIndex]
        val newSets = exercise.sets.toMutableList()

        // Copiamo i target dall'ultima serie (se esiste) per comodità
        val lastSet = newSets.lastOrNull()
        newSets.add(
            LoggedSet(
                weight = 0.0,
                reps = lastSet?.reps ?: 0,
                timeSeconds = lastSet?.timeSeconds ?: 0,
                completed = false
            )
        )

        currentList[exIndex] = exercise.copy(sets = newSets)
        _activeExercises.value = currentList
    }

    // NUOVO: Sposta in alto
    fun moveActiveExerciseUp(index: Int) {
        val currentList = _activeExercises.value?.toMutableList() ?: return
        if (index <= 0) return
        val temp = currentList[index]
        currentList[index] = currentList[index - 1]
        currentList[index - 1] = temp
        _activeExercises.value = currentList
    }

    // NUOVO: Sposta in basso
    fun moveActiveExerciseDown(index: Int) {
        val currentList = _activeExercises.value?.toMutableList() ?: return
        if (index >= currentList.size - 1) return
        val temp = currentList[index]
        currentList[index] = currentList[index + 1]
        currentList[index + 1] = temp
        _activeExercises.value = currentList
    }

    fun clearActiveWorkout() {
        _activeTemplate.value = null
        _activeExercises.value = null
    }
}