package com.example.gymlog

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onDeleteTemplate: (WorkoutTemplate) -> Unit,
    onExportTemplates: () -> String,
    onExportHistory: () -> String // RICEVE ANCHE LO STORICO ORA
) {
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var templateToStartConfirmation by remember { mutableStateOf<WorkoutTemplate?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isNavigating by remember { mutableStateOf(false) }
    var frozenTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    val context = LocalContext.current

    // LANCER ESPORTAZIONE SCHEDE
    val exportTemplatesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(onExportTemplates().toByteArray())
                    Toast.makeText(context, "Schede esportate con successo!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Errore durante l'esportazione.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // LANCER ESPORTAZIONE STORICO DATI
    val exportHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(onExportHistory().toByteArray())
                    Toast.makeText(context, "Storico esportato con successo!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Errore durante l'esportazione.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNavigating = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val displayTemplate = if (isNavigating) frozenTemplate else activeWorkoutTemplate

    // BOX PRINCIPALE PER SOVRAPPORRE IL TASTO FLUTTUANTE
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("GymLog", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // SPAZIO INFERIORE AGGIUNTO PER EVITARE CHE IL TASTO COPRA L'ULTIMA SCHEDA
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(templates) { template ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${template.exercises.size} esercizi", style = MaterialTheme.typography.bodyMedium)
                                }

                                Row {
                                    IconButton(onClick = {
                                        if (activeWorkoutTemplate != null) {
                                            templateToStartConfirmation = template
                                        } else {
                                            frozenTemplate = activeWorkoutTemplate
                                            isNavigating = true
                                            onStartWorkoutClick(template)
                                        }
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

        // ==========================================
        // TASTO FLUTTUANTE ROTONDO IN BASSO A DESTRA
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .systemBarsPadding()
        ) {
            var menuExpanded by remember { mutableStateOf(false) }

            FloatingActionButton(
                onClick = { menuExpanded = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape // Forma perfettamente rotonda
            ) {
                Icon(Icons.Default.Share, contentDescription = "Opzioni Esportazione")
            }

            // MENU A TENDINA CHE COMPARE SOPRA IL TASTO
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Esporta Dati Storico", fontWeight = FontWeight.Medium) },
                    onClick = {
                        menuExpanded = false
                        exportHistoryLauncher.launch("GymLog_Storico.csv")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Esporta Schede", fontWeight = FontWeight.Medium) },
                    onClick = {
                        menuExpanded = false
                        exportTemplatesLauncher.launch("GymLog_Schede.csv")
                    }
                )
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

    if (templateToStartConfirmation != null) {
        AlertDialog(
            onDismissRequest = { templateToStartConfirmation = null },
            title = { Text("Sovrascrivere allenamento?") },
            text = { Text("Hai già un allenamento in corso in pausa. Se avvii una nuova scheda, i dati non salvati dell'allenamento attuale andranno persi per sempre. Vuoi procedere?") },
            confirmButton = {
                Button(
                    onClick = {
                        val templateToStart = templateToStartConfirmation!!
                        templateToStartConfirmation = null
                        frozenTemplate = activeWorkoutTemplate
                        isNavigating = true
                        onStartWorkoutClick(templateToStart)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Procedi") }
            },
            dismissButton = { TextButton(onClick = { templateToStartConfirmation = null }) { Text("Annulla") } }
        )
    }
}