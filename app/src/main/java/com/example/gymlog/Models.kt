package com.example.gymlog

import java.util.UUID

// Rappresenta un singolo esercizio nel database
data class Exercise(
    val name: String,
    val muscleGroup: String = ""
)

// Rappresenta come deve essere fatto un esercizio in una scheda (es. 3 serie x 10 rep, 90s riposo)
data class ExerciseConfig(
    val exerciseName: String,
    var sets: Int,
    var reps: Int,
    var restSeconds: Int
)

// Rappresenta la scheda di allenamento (es. "Giorno 1")
data class WorkoutTemplate(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var exercises: List<ExerciseConfig>
)

// Rappresenta una singola serie svolta durante un allenamento (es. 50kg, 10 rep, completata)
data class LoggedSet(
    var weight: Double = 0.0,
    var reps: Int = 0,
    var completed: Boolean = false
)

// Rappresenta un esercizio effettivamente svolto nello storico
data class LoggedExercise(
    val exerciseName: String,
    var sets: List<LoggedSet>
)

// Rappresenta l'allenamento salvato nello storico alla fine della sessione
data class WorkoutLog(
    val id: String = UUID.randomUUID().toString(),
    val templateName: String,
    val date: String,
    val exercises: List<LoggedExercise>
)