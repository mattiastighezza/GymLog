package com.example.gymlog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    template: WorkoutTemplate,
    onBackClick: () -> Unit,
    onFinishWorkout: (WorkoutLog) -> Unit
) {
    // Prepariamo la struttura dati: traduciamo le impostazioni della scheda in esercizi da compilare
    var loggedExercises by remember {
        mutableStateOf(template.exercises.map { config ->
            LoggedExercise(
                exerciseName = config.exerciseName,
                // Creiamo tante righe vuote quante sono le serie previste
                sets = List(config.sets) { LoggedSet(weight = 0.0, reps = config.reps, completed = false) }
            )
        })
    }

    // Funzione per aggiornare una singola cella (peso, reps o spunta)
    fun updateSet(exIndex: Int, setIndex: Int, newSet: LoggedSet) {
        val newList = loggedExercises.toMutableList()
        val newSets = newList[exIndex].sets.toMutableList()
        newSets[setIndex] = newSet
        newList[exIndex] = newList[exIndex].copy(sets = newSets)
        loggedExercises = newList
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Indietro") }
                },
                actions = {
                    Button(
                        onClick = {
                            // Quando l'utente preme "Termina", creiamo il Log e lo passiamo a chi lo salverà
                            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            val currentDateTime = LocalDateTime.now().format(formatter)

                            val finalLog = WorkoutLog(
                                templateName = template.name,
                                date = currentDateTime,
                                exercises = loggedExercises
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
            // Per ogni esercizio creiamo un riquadro visivo
            itemsIndexed(loggedExercises) { exIndex, exercise ->
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
                        Spacer(modifier = Modifier.height(12.dp))

                        // Intestazione tabella: Serie | kg | Reps | Fatto
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Set", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Text("kg", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Text("Reps", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Text("Fatto", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Le righe vere e proprie per ogni serie
                        exercise.sets.forEachIndexed { setIndex, loggedSet ->
                            // Lo sfondo diventa verdino se la serie è completata
                            val bgColor = if (loggedSet.completed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor, shape = MaterialTheme.shapes.small)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Numero Serie
                                Text("${setIndex + 1}", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)

                                // 2. Input per i kg
                                OutlinedTextField(
                                    value = if (loggedSet.weight == 0.0) "" else loggedSet.weight.toString(),
                                    onValueChange = { newValue ->
                                        val weight = newValue.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        updateSet(exIndex, setIndex, loggedSet.copy(weight = weight))
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(50.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )

                                // 3. Input per le Ripetizioni (precompilato col target della scheda)
                                OutlinedTextField(
                                    value = loggedSet.reps.toString(),
                                    onValueChange = { newValue ->
                                        val reps = newValue.toIntOrNull() ?: 0
                                        updateSet(exIndex, setIndex, loggedSet.copy(reps = reps))
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(50.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )

                                // 4. Checkbox Spunta completato
                                Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = {
                                            updateSet(exIndex, setIndex, loggedSet.copy(completed = !loggedSet.completed))
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
                    }
                }
            }
        }
    }
}