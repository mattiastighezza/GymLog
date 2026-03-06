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

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(
                                templates = templates,
                                onCreateWorkoutClick = { navController.navigate("create_workout") },
                                onHistoryClick = { navController.navigate("history") },
                                onStartWorkoutClick = { template -> navController.navigate("active_workout/${template.id}") },
                                onEditTemplate = { template -> navController.navigate("create_workout?templateId=${template.id}") },
                                onDeleteTemplate = { template -> viewModel.deleteTemplate(template) }
                            )
                        }

                        // ROTTA MODIFICATA: Accetta un ID opzionale per editare
                        composable(
                            route = "create_workout?templateId={templateId}",
                            arguments = listOf(navArgument("templateId") { nullable = true })
                        ) { backStackEntry ->
                            val templateId = backStackEntry.arguments?.getString("templateId")
                            val templateToEdit = templates.find { it.id == templateId }

                            CreateWorkoutScreen(
                                initialTemplate = templateToEdit,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { workout ->
                                    if (templateToEdit != null) {
                                        viewModel.updateTemplate(workout) // Se modificato
                                    } else {
                                        viewModel.saveTemplate(workout) // Se nuovo
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("active_workout/{templateId}") { backStackEntry ->
                            val templateId = backStackEntry.arguments?.getString("templateId")
                            val templateDaAvviare = templates.find { it.id == templateId }

                            if (templateDaAvviare != null) {
                                ActiveWorkoutScreen(
                                    template = templateDaAvviare,
                                    onBackClick = { navController.popBackStack() },
                                    onFinishWorkout = { workoutLogCompletato ->
                                        viewModel.saveWorkoutLog(workoutLogCompletato)
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