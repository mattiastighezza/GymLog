package com.example.gymlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseConfigDialog(
    initialConfig: ExerciseConfig?,
    onDismiss: () -> Unit,
    onConfirm: (ExerciseConfig) -> Unit
) {
    var name by remember { mutableStateOf(initialConfig?.exerciseName ?: "") }
    var sets by remember { mutableStateOf(if (initialConfig?.sets != 0) initialConfig?.sets?.toString() ?: "" else "") }
    var reps by remember { mutableStateOf(if (initialConfig?.reps != 0) initialConfig?.reps?.toString() ?: "" else "") }

    val initialMin = (initialConfig?.restSeconds ?: 0) / 60
    val initialSec = (initialConfig?.restSeconds ?: 0) % 60
    var restMin by remember { mutableStateOf(if(initialConfig != null && initialMin > 0) initialMin.toString() else "") }
    var restSec by remember { mutableStateOf(if(initialConfig != null && initialSec > 0) initialSec.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialConfig == null || initialConfig.exerciseName.isBlank()) "Nuovo Esercizio" else "Modifica Esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nome (es. Panca Piana)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Serie") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Ripetizioni") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Spacer(Modifier.height(12.dp))
                Text("Riposo tra le serie", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = restMin, onValueChange = { restMin = it }, label = { Text("Minuti") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = restSec, onValueChange = { restSec = it }, label = { Text("Secondi") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val totalRestSeconds = (restMin.toIntOrNull() ?: 0) * 60 + (restSec.toIntOrNull() ?: 0)
                onConfirm(ExerciseConfig(name.ifBlank { "Esercizio senza nome" }, sets.toIntOrNull() ?: 0, reps.toIntOrNull() ?: 0, totalRestSeconds))
            }) { Text("Conferma") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    initialTemplate: WorkoutTemplate?, // NUOVO: La scheda da modificare (se esiste)
    onBackClick: () -> Unit,
    onSaveClick: (WorkoutTemplate) -> Unit
) {
    // Se c'è una scheda iniziale, pre-compiliamo i campi!
    var workoutName by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var exercises by remember { mutableStateOf(initialTemplate?.exercises ?: listOf()) }

    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialTemplate != null) "Modifica Scheda" else "Crea Scheda") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Indietro") } },
                actions = {
                    TextButton(onClick = {
                        // Manteniamo lo stesso ID se stiamo modificando, altrimenti ne genera uno nuovo automatico
                        val workoutId = initialTemplate?.id ?: java.util.UUID.randomUUID().toString()
                        onSaveClick(WorkoutTemplate(id = workoutId, name = workoutName.ifBlank { "Nuova Scheda" }, exercises = exercises))
                    }) { Text("Salva", style = MaterialTheme.typography.titleMedium) }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = workoutName, onValueChange = { workoutName = it },
                label = { Text("Nome Scheda") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(exercises) { index, exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editingIndex = index; showDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${exercise.sets} Serie x ${exercise.reps} Reps", style = MaterialTheme.typography.bodyLarge)
                                val min = exercise.restSeconds / 60
                                val sec = exercise.restSeconds % 60
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text("⏱ ${String.format("%02d:%02d", min, sec)}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { editingIndex = null; showDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, "Aggiungi")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aggiungi Esercizio")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showDialog) {
            val configToEdit = if (editingIndex != null) exercises[editingIndex!!] else null
            ExerciseConfigDialog(
                initialConfig = configToEdit,
                onDismiss = { showDialog = false },
                onConfirm = { updatedConfig ->
                    val newList = exercises.toMutableList()
                    if (editingIndex != null) newList[editingIndex!!] = updatedConfig
                    else newList.add(updatedConfig)
                    exercises = newList
                    showDialog = false
                }
            )
        }
    }
}