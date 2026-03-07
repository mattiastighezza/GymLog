package com.example.gymlog

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    template: WorkoutTemplate,
    exercises: List<LoggedExercise>, // Riceviamo la lista in tempo reale dal ViewModel
    onUpdateSet: (Int, Int, LoggedSet) -> Unit, // Diciamo al ViewModel di aggiornarsi
    onPauseWorkout: () -> Unit,
    onAbandonWorkout: () -> Unit,
    onFinishWorkout: (WorkoutLog) -> Unit
) {
    // Variabili per i due Popup
    var showPauseDialog by remember { mutableStateOf(false) }
    var showAbandonDialog by remember { mutableStateOf(false) }

    // MAGIA: Intercetta il tasto fisico indietro di Android!
    BackHandler {
        showPauseDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template.name) },
                navigationIcon = {
                    // Anche la freccia in alto a sinistra apre il popup
                    IconButton(onClick = { showPauseDialog = true }) { Icon(Icons.Default.ArrowBack, "Indietro") }
                },
                actions = {
                    Button(
                        onClick = {
                            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            val currentDateTime = LocalDateTime.now().format(formatter)

                            val finalLog = WorkoutLog(
                                templateName = template.name,
                                date = currentDateTime,
                                exercises = exercises
                            )
                            onFinishWorkout(finalLog)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Termina")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(exercises) { exIndex, exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (exercise.note.isNotBlank()) {
                            Text(
                                text = "📝 ${exercise.note}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Set", modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Text("kg", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)

                            if (exercise.isTimeBased) {
                                Text("Tempo (s)", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Reps", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            }

                            Text("Fatto", modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        exercise.sets.forEachIndexed { setIndex, loggedSet ->
                            WorkoutSetRow(
                                setIndex = setIndex,
                                exercise = exercise,
                                loggedSet = loggedSet,
                                onUpdate = { newSet -> onUpdateSet(exIndex, setIndex, newSet) } // Salviamo nel ViewModel
                            )
                        }
                    }
                }
            }

            // TASTO ROSSO: ABBANDONA ALLENAMENTO
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAbandonDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abbandona Allenamento", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // POPUP 1: TASTO INDIETRO (PAUSA)
        if (showPauseDialog) {
            AlertDialog(
                onDismissRequest = { showPauseDialog = false },
                title = { Text("Metti in pausa?") },
                text = { Text("Vuoi uscire dalla schermata? L'allenamento rimarrà in memoria e potrai riprenderlo dalla Home.") },
                confirmButton = {
                    Button(onClick = {
                        showPauseDialog = false
                        onPauseWorkout()
                    }) { Text("Metti in Pausa") }
                },
                dismissButton = {
                    TextButton(onClick = { showPauseDialog = false }) { Text("Rimani qui") }
                }
            )
        }

        // POPUP 2: TASTO ROSSO (ABBANDONA)
        if (showAbandonDialog) {
            AlertDialog(
                onDismissRequest = { showAbandonDialog = false },
                title = { Text("Abbandona Allenamento") },
                text = { Text("Sei sicuro? Tutti i dati non salvati andranno persi e l'allenamento non finirà nello storico.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showAbandonDialog = false
                            onAbandonWorkout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Abbandona") }
                },
                dismissButton = {
                    TextButton(onClick = { showAbandonDialog = false }) { Text("Annulla") }
                }
            )
        }
    }
}

@Composable
fun WorkoutSetRow(
    setIndex: Int,
    exercise: LoggedExercise,
    loggedSet: LoggedSet,
    onUpdate: (LoggedSet) -> Unit
) {
    val context = LocalContext.current
    var isTimerRunning by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(loggedSet.timeSeconds) }

    LaunchedEffect(loggedSet.timeSeconds) {
        if (!isTimerRunning) {
            timeLeft = loggedSet.timeSeconds
        }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            if (timeLeft == 0) {
                isTimerRunning = false
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onUpdate(loggedSet.copy(completed = true))
                timeLeft = loggedSet.timeSeconds
            }
        }
    }

    val bgColor = if (loggedSet.completed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, shape = MaterialTheme.shapes.small)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${setIndex + 1}", modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)

        OutlinedTextField(
            value = if (loggedSet.weight == 0.0) "" else loggedSet.weight.toString(),
            onValueChange = { newValue ->
                val weight = newValue.replace(",", ".").toDoubleOrNull() ?: 0.0
                onUpdate(loggedSet.copy(weight = weight))
            },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(50.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            singleLine = true
        )

        if (exercise.isTimeBased) {
            Row(modifier = Modifier.weight(1.2f).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = if (isTimerRunning) timeLeft.toString() else if (loggedSet.timeSeconds == 0) "" else loggedSet.timeSeconds.toString(),
                    onValueChange = { newValue ->
                        if (!isTimerRunning) {
                            val sec = newValue.toIntOrNull() ?: 0
                            onUpdate(loggedSet.copy(timeSeconds = sec))
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    readOnly = isTimerRunning
                )
                IconButton(
                    onClick = {
                        if (isTimerRunning) {
                            isTimerRunning = false
                            timeLeft = loggedSet.timeSeconds
                        } else {
                            if (loggedSet.timeSeconds > 0) {
                                timeLeft = loggedSet.timeSeconds
                                isTimerRunning = true
                            }
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Timer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = if (loggedSet.reps == 0) "" else loggedSet.reps.toString(),
                onValueChange = { newValue ->
                    val reps = newValue.toIntOrNull() ?: 0
                    onUpdate(loggedSet.copy(reps = reps))
                },
                modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp).height(50.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )
        }

        Box(modifier = Modifier.weight(0.4f), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = {
                    onUpdate(loggedSet.copy(completed = !loggedSet.completed))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completato",
                    tint = if (loggedSet.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}