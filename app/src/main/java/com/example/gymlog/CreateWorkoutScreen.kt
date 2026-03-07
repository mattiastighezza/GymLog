package com.example.gymlog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.abs

enum class DialogState { HIDDEN, SELECT_EXERCISE, NEW_EXERCISE, CONFIG_EXERCISE }

// NUOVO COMPONENTE: Il Selettore Rotante per il Timer!
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePicker(
    options: List<Int>,
    initialValue: Int,
    onValueChange: (Int) -> Unit
) {
    // Cerchiamo l'indice iniziale. Se il valore non esiste, parte dal primo (0s)
    val initialIndex = options.indexOf(initialValue).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialIndex) { options.size }

    // Ogni volta che il pager si ferma su una pagina, comunichiamo il nuovo valore
    LaunchedEffect(pagerState.currentPage) {
        onValueChange(options[pagerState.currentPage])
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sfondo colorato per evidenziare la selezione centrale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        // Il vero e proprio rullo scorrevole
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 43.dp), // Spazio per mostrare l'elemento sopra e sotto
            horizontalAlignment = Alignment.CenterHorizontally
        ) { page ->
            val isSelected = page == pagerState.currentPage
            val optionSeconds = options[page]

            // Formattazione furba del testo (es. "1m 15s")
            val text = if (optionSeconds == 0) "Nessun riposo" else {
                val m = optionSeconds / 60
                val s = optionSeconds % 60
                if (m == 0) "${s}s" else if (s == 0) "${m}m" else "${m}m ${s}s"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    // Sfuma il colore degli elementi non selezionati
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}


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

    // Generiamo la lista dinamica per il timer (Scatti di 5s fino a 60, poi scatti di 15s fino a 10 min)
    val restTimerOptions = remember {
        val opts = mutableListOf<Int>()
        for (i in 0..60 step 5) opts.add(i)
        for (i in 75..600 step 15) opts.add(i)
        opts
    }

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. ${exercise.exerciseName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f)
                                )

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

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = "Serie", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${exercise.sets} Serie", style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Ripetizioni", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${exercise.reps} Reps", style = MaterialTheme.typography.bodyLarge)
                                }
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

        if (dialogState == DialogState.CONFIG_EXERCISE) {
            val initialConfig = if (editingIndex != null) exercises[editingIndex!!] else null

            var sets by remember { mutableStateOf(if (initialConfig?.sets != 0 && initialConfig != null) initialConfig.sets.toString() else "") }
            var reps by remember { mutableStateOf(if (initialConfig?.reps != 0 && initialConfig != null) initialConfig.reps.toString() else "") }

            // Per il timer: cerchiamo il valore più vicino nella lista (nel caso si modifichi una vecchia scheda con valori non standard)
            val initialRestTarget = initialConfig?.restSeconds ?: 90 // Default 1m 30s
            val closestValidRest = restTimerOptions.minByOrNull { abs(it - initialRestTarget) } ?: 90
            var finalRestSeconds by remember { mutableStateOf(closestValidRest) }

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

                        Spacer(Modifier.height(24.dp))
                        Text("Tempo di recupero:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        // IL NOSTRO NUOVO SELETTORE A RULLO!
                        WheelTimePicker(
                            options = restTimerOptions,
                            initialValue = finalRestSeconds,
                            onValueChange = { newValue -> finalRestSeconds = newValue }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val newConfig = ExerciseConfig(selectedExerciseName, sets.toIntOrNull() ?: 0, reps.toIntOrNull() ?: 0, finalRestSeconds)

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