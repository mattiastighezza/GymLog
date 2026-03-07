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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gymlog.ui.theme.GymLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                    // Raccogliamo lo stato dell'allenamento attivo!
                    val activeTemplate by viewModel.activeTemplate.collectAsState()
                    val activeExercises by viewModel.activeExercises.collectAsState()

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(
                                templates = templates,
                                activeWorkoutTemplate = activeTemplate, // Passiamo la scheda in corso
                                onCreateWorkoutClick = { navController.navigate("create_workout") },
                                onHistoryClick = { navController.navigate("history") },
                                onStartWorkoutClick = { template ->
                                    // Avvia nel ViewModel e poi vai alla pagina
                                    viewModel.startWorkout(template)
                                    navController.navigate("active_workout")
                                },
                                onResumeWorkoutClick = {
                                    // Riprendi semplicemente tornando alla pagina
                                    navController.navigate("active_workout")
                                },
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

                        // L'URL ora è generico, non serve più passargli l'ID perché ci pensa il ViewModel
                        composable("active_workout") {
                            if (activeTemplate != null && activeExercises != null) {
                                ActiveWorkoutScreen(
                                    template = activeTemplate!!,
                                    exercises = activeExercises!!,
                                    onUpdateSet = { exIdx, setIdx, newSet ->
                                        viewModel.updateActiveSet(exIdx, setIdx, newSet)
                                    },
                                    onPauseWorkout = {
                                        navController.popBackStack() // Torna alla home, i dati restano nel ViewModel!
                                    },
                                    onAbandonWorkout = {
                                        viewModel.clearActiveWorkout() // Cancella i dati e torna alla home
                                        navController.popBackStack()
                                    },
                                    onFinishWorkout = { workoutLogCompletato ->
                                        viewModel.saveWorkoutLog(workoutLogCompletato)
                                        viewModel.clearActiveWorkout() // Pulizia
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