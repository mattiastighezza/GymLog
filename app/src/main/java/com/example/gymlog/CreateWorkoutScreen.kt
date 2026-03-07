package com.example.gymlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

enum class DialogState { HIDDEN, SELECT_EXERCISE, NEW_EXERCISE, CONFIG_EXERCISE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    initialTemplate: WorkoutTemplate?,
    availableExercises: List<Exercise>,
    onAddExerciseToDb: (String) -> Unit,
    onDeleteExerciseFromDb: (Exercise) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: (WorkoutTemplate) -> Unit
) {
    var workoutName by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var exercises by remember { mutableStateOf(initialTemplate?.exercises ?: listOf()) }

    var dialogState by remember { mutableStateOf(DialogState.HIDDEN) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseName by remember { mutableStateOf("") }

    var exerciseToDeleteFromDb by remember { mutableStateOf<Exercise?>(null) }
    var indexToRemoveFromTemplate by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialTemplate != null) "Modifica Scheda" else "Crea Scheda") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Indietro") } },
                actions = {
                    TextButton(onClick = {
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
                        modifier = Modifier.fillMaxWidth().clickable {
                            editingIndex = index
                            selectedExerciseName = exercise.exerciseName
                            dialogState = DialogState.CONFIG_EXERCISE
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // 1. RIGA INTESTAZIONE: Numero, Nome e Pulsanti d'Azione
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // NUMERO PROGRESSIVO E NOME
                                Text(
                                    text = "${index + 1}. ${exercise.exerciseName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f) // Evita che il testo spinga fuori i bottoni
                                )

                                // BOTTONI SPOSTA SU / GIU / ELIMINA
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val newList = exercises.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index - 1]
                                                newList[index - 1] = temp
                                                exercises = newList
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) { Icon(Icons.Default.KeyboardArrowUp, "Sposta Su", tint = MaterialTheme.colorScheme.primary) }
                                    }

                                    if (index < exercises.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val newList = exercises.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index + 1]
                                                newList[index + 1] = temp
                                                exercises = newList
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) { Icon(Icons.Default.KeyboardArrowDown, "Sposta Giù", tint = MaterialTheme.colorScheme.primary) }
                                    }

                                    IconButton(
                                        onClick = { indexToRemoveFromTemplate = index },
                                        modifier = Modifier.size(32.dp).padding(start = 4.dp)
                                    ) { Icon(Icons.Default.Delete, "Rimuovi dalla scheda", tint = MaterialTheme.colorScheme.error) }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // 2. RIGA INFORMAZIONI: Serie, Reps, Timer con ICONE
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                // ICONA E TESTO SERIE
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = "Serie", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${exercise.sets} Serie", style = MaterialTheme.typography.bodyLarge)
                                }

                                // ICONA E TESTO REPETIZIONI
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Ripetizioni", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${exercise.reps} Reps", style = MaterialTheme.typography.bodyLarge)
                                }

                                // TIMER
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
                        onClick = {
                            editingIndex = null
                            dialogState = DialogState.SELECT_EXERCISE
                        },
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

        // POPUP CONFERMA RIMOZIONE ESERCIZIO DALLA SCHEDA
        if (indexToRemoveFromTemplate != null) {
            val exName = exercises[indexToRemoveFromTemplate!!].exerciseName
            AlertDialog(
                onDismissRequest = { indexToRemoveFromTemplate = null },
                title = { Text("Rimuovi Esercizio") },
                text = { Text("Vuoi rimuovere '$exName' da questa scheda?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val newList = exercises.toMutableList()
                            newList.removeAt(indexToRemoveFromTemplate!!)
                            exercises = newList
                            indexToRemoveFromTemplate = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Rimuovi") }
                },
                dismissButton = { TextButton(onClick = { indexToRemoveFromTemplate = null }) { Text("Annulla") } }
            )
        }

        // POPUP SELEZIONA ESERCIZIO DALLA LISTA
        if (dialogState == DialogState.SELECT_EXERCISE) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredExercises = availableExercises.filter { it.name.contains(searchQuery, ignoreCase = true) }

            AlertDialog(
                onDismissRequest = { dialogState = DialogState.HIDDEN },
                title = { Text("Scegli un esercizio") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Cerca...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(filteredExercises) { ex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedExerciseName = ex.name
                                        dialogState = DialogState.CONFIG_EXERCISE
                                    }.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ex.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    IconButton(
                                        onClick = { exerciseToDeleteFromDb = ex },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error) }
                                }
                                HorizontalDivider()
                            }
                            if (filteredExercises.isEmpty()) {
                                item { Text("Nessun esercizio trovato.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { dialogState = DialogState.NEW_EXERCISE }) { Text("Nuovo Esercizio") } },
                dismissButton = { TextButton(onClick = { dialogState = DialogState.HIDDEN }) { Text("Annulla") } }
            )
        }

        // POPUP CONFERMA ELIMINAZIONE ESERCIZIO DAL DATABASE
        if (exerciseToDeleteFromDb != null) {
            AlertDialog(
                onDismissRequest = { exerciseToDeleteFromDb = null },
                title = { Text("Elimina Esercizio") },
                text = { Text("Sei sicuro di voler eliminare '${exerciseToDeleteFromDb?.name}' dalla tua lista? (Non verrà eliminato dalle schede già create)") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteExerciseFromDb(exerciseToDeleteFromDb!!)
                            exerciseToDeleteFromDb = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Elimina") }
                },
                dismissButton = { TextButton(onClick = { exerciseToDeleteFromDb = null }) { Text("Annulla") } }
            )
        }

        // POPUP CREA NUOVO ESERCIZIO
        if (dialogState == DialogState.NEW_EXERCISE) {
            var newExerciseName by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { dialogState = DialogState.SELECT_EXERCISE },
                title = { Text("Nuovo Esercizio") },
                text = {
                    OutlinedTextField(
                        value = newExerciseName, onValueChange = { newExerciseName = it },
                        label = { Text("Nome Esercizio") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newExerciseName.isNotBlank()) {
                            onAddExerciseToDb(newExerciseName)
                            selectedExerciseName = newExerciseName
                            dialogState = DialogState.CONFIG_EXERCISE
                        }
                    }) { Text("Aggiungi") }
                },
                dismissButton = { TextButton(onClick = { dialogState = DialogState.SELECT_EXERCISE }) { Text("Indietro") } }
            )
        }

        // POPUP CONFIGURA ESERCIZIO
        if (dialogState == DialogState.CONFIG_EXERCISE) {
            val initialConfig = if (editingIndex != null) exercises[editingIndex!!] else null

            var sets by remember { mutableStateOf(if (initialConfig?.sets != 0 && initialConfig != null) initialConfig.sets.toString() else "") }
            var reps by remember { mutableStateOf(if (initialConfig?.reps != 0 && initialConfig != null) initialConfig.reps.toString() else "") }

            val initialMin = (initialConfig?.restSeconds ?: 0) / 60
            val initialSec = (initialConfig?.restSeconds ?: 0) % 60
            var restMin by remember { mutableStateOf(if(initialConfig != null && initialMin > 0) initialMin.toString() else "") }
            var restSec by remember { mutableStateOf(if(initialConfig != null && initialSec > 0) initialSec.toString() else "") }

            AlertDialog(
                onDismissRequest = { dialogState = DialogState.HIDDEN },
                title = {
                    Column {
                        Text("Configura Esercizio", style = MaterialTheme.typography.labelMedium)
                        Text(selectedExerciseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cambia esercizio",
                            style = MaterialTheme.typography.labelLarge,
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { dialogState = DialogState.SELECT_EXERCISE }
                        )
                    }
                },
                text = {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Serie") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Ripetizioni") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Spacer(Modifier.height(16.dp))
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
                        val newConfig = ExerciseConfig(selectedExerciseName, sets.toIntOrNull() ?: 0, reps.toIntOrNull() ?: 0, totalRestSeconds)

                        val newList = exercises.toMutableList()
                        if (editingIndex != null) newList[editingIndex!!] = newConfig else newList.add(newConfig)
                        exercises = newList

                        dialogState = DialogState.HIDDEN
                    }) { Text("Ok") }
                },
                dismissButton = { TextButton(onClick = { dialogState = DialogState.HIDDEN }) { Text("Annulla") } }
            )
        }
    }
}