package com.example.gymlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    logs: List<WorkoutLog>,
    onBackClick: () -> Unit,
    onDeleteLog: (WorkoutLog) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var logToDelete by remember { mutableStateOf<WorkoutLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storico Progressi") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Indietro") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Allenamenti", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Grafici", fontWeight = FontWeight.Bold) })
            }

            if (selectedTab == 0) {
                WorkoutLogsTab(logs, onDeleteClick = { logToDelete = it })
            } else {
                ExerciseChartTab(logs)
            }
        }
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Elimina Allenamento") },
            text = { Text("Vuoi eliminare questo allenamento del ${logToDelete?.date}? Verrà rimosso anche dalle statistiche dei grafici.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteLog(logToDelete!!); logToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { logToDelete = null }) { Text("Annulla") } }
        )
    }
}

@Composable
fun WorkoutLogsTab(logs: List<WorkoutLog>, onDeleteClick: (WorkoutLog) -> Unit) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessun allenamento completato.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }

    val sortedLogs = logs.reversed()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(sortedLogs) { log ->
            var expanded by remember { mutableStateOf(false) }

            Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.templateName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(log.date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteClick(log) }) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error) }
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Espandi")
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                            log.exercises.forEach { exercise ->
                                Text(exercise.exerciseName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                exercise.sets.forEachIndexed { i, set ->
                                    if (set.completed) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Serie ${i + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            // MOSTRIAMO IL TESTO CORRETTO NELLO STORICO ESPANSO
                                            if (exercise.isTimeBased) {
                                                Text("${set.weight} kg  x  ${set.timeSeconds} sec", fontWeight = FontWeight.Medium)
                                            } else {
                                                Text("${set.weight} kg  x  ${set.reps} reps", fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseChartTab(logs: List<WorkoutLog>) {
    val allExercises = logs.flatMap { it.exercises }.map { it.exerciseName }.distinct()

    if (allExercises.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Esegui un allenamento per vedere i grafici.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }

    var selectedExercise by remember { mutableStateOf(allExercises.first()) }
    var showVolume by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ExposedDropdownMenuBox(expanded = expandedDropdown, onExpandedChange = { expandedDropdown = !expandedDropdown }) {
            OutlinedTextField(
                value = selectedExercise, onValueChange = {}, readOnly = true, label = { Text("Seleziona Esercizio") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                allExercises.forEach { exerciseName ->
                    DropdownMenuItem(text = { Text(exerciseName) }, onClick = { selectedExercise = exerciseName; expandedDropdown = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showVolume = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = if (!showVolume) MaterialTheme.colorScheme.primary else Color.Gray)) {
                Text("Peso (kg)", fontWeight = if (!showVolume) FontWeight.Bold else FontWeight.Normal)
            }
            TextButton(onClick = { showVolume = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = if (showVolume) MaterialTheme.colorScheme.primary else Color.Gray)) {
                Text("Volume", fontWeight = if (showVolume) FontWeight.Bold else FontWeight.Normal)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val chartData = logs.mapNotNull { log ->
            val exercise = log.exercises.find { it.exerciseName == selectedExercise }
            if (exercise != null) {
                val value = if (showVolume) {
                    // CALCOLO DEL VOLUME: se è a tempo moltiplica per i secondi (es. plank con sovraccarico), altrimenti per le reps
                    val vol = exercise.sets.filter { it.completed }.sumOf {
                        it.weight * if (exercise.isTimeBased) it.timeSeconds else it.reps
                    }.toFloat()
                    if (vol > 0) vol else null
                } else {
                    val maxW = exercise.sets.filter { it.completed }.maxOfOrNull { it.weight }?.toFloat()
                    if (maxW != null && maxW > 0) maxW else null
                }
                if (value != null) Pair(log.date, value) else null
            } else null
        }

        if (chartData.isNotEmpty()) {
            ProgressChart(data = chartData, modifier = Modifier.fillMaxWidth().height(300.dp), color = MaterialTheme.colorScheme.primary)
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) { Text("Nessun dato.", color = Color.Gray) }
        }
    }
}

@Composable
fun ProgressChart(data: List<Pair<String, Float>>, modifier: Modifier = Modifier, color: Color) {
    if (data.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Valore: ${data.first().second}", style = MaterialTheme.typography.titleLarge, color = color)
            Text("\n\n(Fai un altro allenamento per creare la curva)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Canvas(modifier = modifier) {
        val paddingLeft = 100f
        val paddingBottom = 80f
        val paddingTop = 40f
        val paddingRight = 60f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        drawLine(Color.Gray.copy(alpha=0.5f), Offset(paddingLeft, paddingTop), Offset(paddingLeft, size.height - paddingBottom), strokeWidth = 2f)
        drawLine(Color.Gray.copy(alpha=0.5f), Offset(paddingLeft, size.height - paddingBottom), Offset(size.width - paddingRight, size.height - paddingBottom), strokeWidth = 2f)

        val rawMax = data.maxOf { it.second }
        val rawMin = data.minOf { it.second }

        val minRounded = (Math.floor((rawMin / 10).toDouble()) * 10).toFloat()
        var maxRounded = (Math.ceil((rawMax / 10).toDouble()) * 10).toFloat()
        if (maxRounded == minRounded) maxRounded += 10f

        val rangeY = maxRounded - minRounded
        val stepY = rangeY / 5f

        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        for (i in 0..5) {
            val currentYVal = minRounded + (stepY * i)
            val y = paddingTop + chartHeight - ((currentYVal - minRounded) / rangeY * chartHeight)

            if (i > 0) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 2f,
                    pathEffect = dashedEffect
                )
            }

            val text = if (currentYVal % 1f == 0f) String.format("%.0f", currentYVal) else String.format("%.1f", currentYVal)
            val measuredText = textMeasurer.measure(text, labelStyle)
            drawText(textMeasurer, text, Offset(paddingLeft - measuredText.size.width - 16f, y - measuredText.size.height / 2), style = labelStyle)
        }

        val stepX = chartWidth / (data.size - 1)
        val path = Path()

        val xIndicesToShow = mutableSetOf<Int>()
        xIndicesToShow.add(0)
        if (data.size <= 6) {
            xIndicesToShow.addAll(data.indices)
        } else {
            for (i in 1..5) {
                xIndicesToShow.add(((data.size - 1) * i) / 5)
            }
        }

        data.forEachIndexed { index, pair ->
            val x = paddingLeft + (index * stepX)
            val y = paddingTop + chartHeight - ((pair.second - minRounded) / rangeY * chartHeight)

            if (xIndicesToShow.contains(index)) {
                if (index > 0) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(x, paddingTop),
                        end = Offset(x, size.height - paddingBottom),
                        strokeWidth = 2f,
                        pathEffect = dashedEffect
                    )
                }

                val dateLabel = pair.first.take(5)
                val measuredDate = textMeasurer.measure(dateLabel, labelStyle)
                drawText(textMeasurer, dateLabel, Offset(x - measuredDate.size.width / 2, size.height - paddingBottom + 16f), style = labelStyle)
            }

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

            drawCircle(color, radius = 8f, center = Offset(x, y))
        }

        drawPath(path, color, style = Stroke(width = 6f))
    }
}