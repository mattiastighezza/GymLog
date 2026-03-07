package com.example.gymlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymlog.ui.theme.GymLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // 1. Variabile per dire ad Android di aspettare prima di chiudere la Splash Screen
        var keepSplash = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)

        // 2. Facciamo durare la Splash Screen per 1.5 secondi
        lifecycleScope.launch {
            delay(1500L) // 1500 millisecondi
            keepSplash = false // Rilascia la Splash Screen!
        }

        setContent {
            GymLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: GymViewModel = viewModel()

                    val templates by viewModel.templates.collectAsState()
                    val workoutLogs by viewModel.workoutLogs.collectAsState()
                    val availableExercises by viewModel.exercises.collectAsState()

                    val activeTemplate by viewModel.activeTemplate.collectAsState()
                    val activeExercises by viewModel.activeExercises.collectAsState()

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(
                                templates = templates,
                                activeWorkoutTemplate = activeTemplate,
                                onCreateWorkoutClick = { navController.navigate("create_workout") },
                                onHistoryClick = { navController.navigate("history") },
                                onStartWorkoutClick = { template ->
                                    viewModel.startWorkout(template)
                                    navController.navigate("active_workout")
                                },
                                onResumeWorkoutClick = { navController.navigate("active_workout") },
                                onEditTemplate = { template -> navController.navigate("create_workout?templateId=${template.id}") },
                                onDeleteTemplate = { template -> viewModel.deleteTemplate(template) }
                            )
                        }

                        composable(
                            route = "create_workout?templateId={templateId}",
                            arguments = listOf(navArgument("templateId") { nullable = true })
                        ) { backStackEntry ->
                            val templateId = backStackEntry.arguments?.getString("templateId")
                            val templateToEdit = templates.find { it.id == templateId }

                            CreateWorkoutScreen(
                                initialTemplate = templateToEdit,
                                availableExercises = availableExercises,
                                onAddExerciseToDb = { nomeEs -> viewModel.addExerciseToLibrary(nomeEs) },
                                onDeleteExerciseFromDb = { es -> viewModel.deleteExerciseFromLibrary(es) },
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { workout ->
                                    if (templateToEdit != null) viewModel.updateTemplate(workout)
                                    else viewModel.saveTemplate(workout)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("active_workout") {
                            if (activeTemplate != null && activeExercises != null) {
                                ActiveWorkoutScreen(
                                    template = activeTemplate!!,
                                    exercises = activeExercises!!,
                                    onUpdateSet = { exIdx, setIdx, newSet -> viewModel.updateActiveSet(exIdx, setIdx, newSet) },
                                    onAddSet = { exIdx -> viewModel.addSetToActiveExercise(exIdx) },
                                    onMoveUp = { exIdx -> viewModel.moveActiveExerciseUp(exIdx) },
                                    onMoveDown = { exIdx -> viewModel.moveActiveExerciseDown(exIdx) },
                                    onPauseWorkout = { navController.popBackStack() },
                                    onAbandonWorkout = {
                                        viewModel.clearActiveWorkout()
                                        navController.popBackStack()
                                    },
                                    onFinishWorkout = { workoutLogCompletato ->
                                        viewModel.saveWorkoutLog(workoutLogCompletato)
                                        viewModel.clearActiveWorkout()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        composable("history") {
                            HistoryScreen(
                                logs = workoutLogs,
                                onBackClick = { navController.popBackStack() },
                                onDeleteLog = { logToDelete -> viewModel.deleteWorkoutLog(logToDelete) }
                            )
                        }
                    }
                }
            }
        }
    }
}