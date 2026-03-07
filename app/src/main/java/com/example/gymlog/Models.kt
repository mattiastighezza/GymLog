package com.example.gymlog

import java.util.UUID

data class Exercise(
    val name: String,
    val muscleGroup: String = ""
)

data class ExerciseConfig(
    val exerciseName: String,
    var sets: Int,
    var reps: Int,
    var restSeconds: Int,
    var note: String = "",
    var isTimeBased: Boolean = false,
    var timeSeconds: Int = 0
)

data class LoggedSet(
    var weight: Double = 0.0,
    var reps: Int = 0,
    var timeSeconds: Int = 0,
    var completed: Boolean = false
)

data class LoggedExercise(
    val exerciseName: String,
    var note: String = "",
    var isTimeBased: Boolean = false,
    var sets: List<LoggedSet>,
    var lastWeight: Double? = null // NUOVO: Ricorda l'ultimo peso usato!
)

data class WorkoutLog(
    val id: String = UUID.randomUUID().toString(),
    val templateName: String,
    val date: String,
    val exercises: List<LoggedExercise>
)

data class WorkoutTemplate(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var exercises: List<ExerciseConfig>
)