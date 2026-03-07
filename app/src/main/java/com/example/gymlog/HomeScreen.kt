package com.example.gymlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun HomeScreen(
    templates: List<WorkoutTemplate>,
    activeWorkoutTemplate: WorkoutTemplate?,
    onCreateWorkoutClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStartWorkoutClick: (WorkoutTemplate) -> Unit,
    onResumeWorkoutClick: () -> Unit,
    onEditTemplate: (WorkoutTemplate) -> Unit,
    onDeleteTemplate: (WorkoutTemplate) -> Unit
) {
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }

    // ==========================================
    // SISTEMA ANTI-SFARFALLIO (FREEZE DELLA UI)
    // ==========================================
    val lifecycleOwner = LocalLifecycleOwner.current
    var isNavigating by remember { mutableStateOf(false) }
    var frozenTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }

    // Questo pezzo rileva quando torni sulla Home (es. se metti in pausa o abbandoni)
    // e sblocca nuovamente la UI per aggiornare il banner.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNavigating = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Se stiamo cambiando pagina, usa la "fotografia" vecchia. Altrimenti usa i dati in tempo reale.
    val displayTemplate = if (isNavigating) frozenTemplate else activeWorkoutTemplate
    // ==========================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("GymLog", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Usiamo 'displayTemplate' al posto di 'activeWorkoutTemplate'
        if (displayTemplate != null) {
            Card(
                onClick = {
                    frozenTemplate = activeWorkoutTemplate
                    isNavigating = true
                    onResumeWorkoutClick()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Allenamento in corso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(displayTemplate.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Riprendi", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onCreateWorkoutClick, modifier = Modifier.weight(1f).height(80.dp), shape = MaterialTheme.shapes.medium) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, null); Text("Crea Scheda") }
            }
            Button(onClick = onHistoryClick, modifier = Modifier.weight(1f).height(80.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), shape = MaterialTheme.shapes.medium) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.DateRange, null); Text("Storico") }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("I miei Allenamenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (templates.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Nessun allenamento creato.\nPremi 'Crea Scheda' per iniziare!", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { template ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${template.exercises.size} esercizi", style = MaterialTheme.typography.bodyMedium)
                            }

                            Row {
                                IconButton(onClick = {
                                    // LA MAGIA: Prima di avviare, scattiamo la foto e ghiacciamo la UI!
                                    frozenTemplate = activeWorkoutTemplate
                                    isNavigating = true
                                    onStartWorkoutClick(template)
                                }) { Icon(Icons.Default.PlayArrow, "Avvia", tint = MaterialTheme.colorScheme.primary) }

                                IconButton(onClick = { onEditTemplate(template) }) { Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                IconButton(onClick = { templateToDelete = template }) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Elimina Scheda") },
            text = { Text("Sei sicuro di voler eliminare la scheda '${templateToDelete?.name}'?") },
            confirmButton = {
                Button(onClick = { onDeleteTemplate(templateToDelete!!); templateToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { templateToDelete = null }) { Text("Annulla") } }
        )
    }
}