package com.example.estudiantetracker

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

// ---------- Funciones auxiliares ----------

private fun aviso(context: Context, mensaje: String) =
    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()

private fun nombreArchivo(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        ?: tx("Archivo adjunto", "Attached file", "Arquivo anexado")

private fun abrirAdjunto(context: Context, adjunto: Adjunto) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(adjunto.uri), adjunto.tipo.ifEmpty { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        aviso(
            context,
            tx(
                "No hay una aplicación disponible para abrir este archivo.",
                "There is no app available to open this file.",
                "Não há um aplicativo disponível para abrir este arquivo."
            )
        )
    } catch (e: SecurityException) {
        aviso(
            context,
            tx(
                "No se pudo acceder al archivo. Puede haber sido movido o eliminado.",
                "The file could not be accessed. It may have been moved or deleted.",
                "Não foi possível acessar o arquivo. Ele pode ter sido movido ou excluído."
            )
        )
    }
}

// =====================================================================
// PANTALLA 1: MIS DEBERES
// =====================================================================

@Composable
fun MisDeberesScreen(
    state: AppState,
    onNueva: () -> Unit,
    onEditar: (String) -> Unit,
    onAjustes: () -> Unit
) {
    var tipo by rememberSaveable { mutableIntStateOf(0) }       // 0 Todos, 1 Exámenes, 2 Tareas
    var prioridad by rememberSaveable { mutableIntStateOf(-1) } // -1 Todas, 0 Baja, 1 Media, 2 Alta
    var materiaFiltro by rememberSaveable { mutableStateOf<String?>(null) }
    var aEliminar by remember { mutableStateOf<Actividad?>(null) }

    // Los cálculos se memorizan: solo se rehacen si cambian los datos o los filtros.
    // Conteo de pendientes de la semana actual, según el inicio de semana elegido en Ajustes.
    val hoy = LocalDate.now()
    val domingo = state.prefs.semanaEmpiezaDomingo
    val pendientesSemana = remember(state.actividades, domingo, hoy) {
        val diasDesdeInicio = if (domingo) hoy.dayOfWeek.value % 7 else hoy.dayOfWeek.value - 1
        val inicioSemana = hoy.minusDays(diasDesdeInicio.toLong())
        val finSemana = inicioSemana.plusDays(7)
        state.actividades.count {
            val dia = it.vence.toLocalDate()
            !it.completada && !dia.isBefore(inicioSemana) && dia.isBefore(finSemana)
        }
    }
    val materiasPorId = remember(state.materias) { state.materias.associateBy { it.id } }
    // Si se elimina la materia que estaba filtrada, vuelve a mostrarse "Todas".
    LaunchedEffect(state.materias) {
        if (materiaFiltro != null && state.materias.none { it.id == materiaFiltro }) materiaFiltro = null
    }
    val lista = remember(state.actividades, tipo, prioridad, materiaFiltro) {
        state.actividades
            .sortedBy { it.vence }
            .filter { a ->
                (tipo == 0 || (tipo == 1) == a.esExamen) &&
                        (prioridad == -1 || a.prioridad == prioridad) &&
                        (materiaFiltro == null || a.materiaId == materiaFiltro)
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNueva,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, contentDescription = tx("Añadir actividad", "Add activity", "Adicionar atividade")) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 96.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            saludoSegunHora(state.prefs.nombreUsuario),
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (pendientesSemana == 1) {
                                tx("1 pendiente esta semana", "1 pending this week", "1 pendente esta semana")
                            } else {
                                tx(
                                    "$pendientesSemana pendientes esta semana",
                                    "$pendientesSemana pending this week",
                                    "$pendientesSemana pendentes esta semana"
                                )
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAjustes) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = tx("Ajustes", "Settings", "Configurações"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                Column(Modifier.padding(bottom = 16.dp)) {
                    Segmentado(
                        listOf(tx("Todos", "All", "Todos"), tx("Exámenes", "Exams", "Provas"), tx("Tareas", "Tasks", "Tarefas")),
                        tipo
                    ) { tipo = it }
                    Text(
                        tx("PRIORIDAD", "PRIORITY", "PRIORIDADE"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    val indicePrioridad = when (prioridad) { -1 -> 0; 2 -> 1; 1 -> 2; else -> 3 }
                    Segmentado(
                        listOf(tx("Todas", "All", "Todas"), nombrePrioridad(2), nombrePrioridad(1), nombrePrioridad(0)),
                        indicePrioridad
                    ) { i ->
                        prioridad = when (i) { 0 -> -1; 1 -> 2; 2 -> 1; else -> 0 }
                    }
                    Text(
                        tx("MATERIA", "SUBJECT", "MATÉRIA"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { ChipMateria(tx("Todas", "All", "Todas"), materiaFiltro == null) { materiaFiltro = null } }
                        items(state.materias, key = { it.id }) { m ->
                            ChipMateria(m.nombre, materiaFiltro == m.id) { materiaFiltro = m.id }
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        tx("Próximas actividades", "Upcoming activities", "Próximas atividades"),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (lista.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                tx("Todo al día", "All caught up", "Tudo em dia"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Text(
                                tx(
                                    "No hay actividades en este filtro. Añade una con el botón +.",
                                    "No activities match this filter. Add one with the + button.",
                                    "Não há atividades neste filtro. Adicione uma com o botão +."
                                ),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                items(lista, key = { it.id }) { a ->
                    TarjetaActividad(
                        a = a,
                        materia = materiasPorId[a.materiaId],
                        onClick = { onEditar(a.id) },
                        onAlternar = { state.alternarCompletada(a) },
                        onAlternarSubtarea = { subId -> state.alternarSubtarea(a.id, subId) },
                        onEliminar = { aEliminar = a }
                    )
                }
            }
        }
    }

    aEliminar?.let { a ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text(tx("Eliminar actividad", "Delete activity", "Excluir atividade")) },
            text = {
                Text(
                    tx(
                        "¿Quieres eliminar \"${a.titulo}\"?",
                        "Do you want to delete \"${a.titulo}\"?",
                        "Deseja excluir \"${a.titulo}\"?"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { state.eliminarActividad(a.id); aEliminar = null }) {
                    Text(tx("Eliminar", "Delete", "Excluir"))
                }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text(tx("Cancelar", "Cancel", "Cancelar")) } }
        )
    }
}

@Composable
private fun ChipMateria(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun TarjetaActividad(
    a: Actividad,
    materia: Materia?,
    onClick: () -> Unit,
    onAlternar: () -> Unit,
    onAlternarSubtarea: (String) -> Unit,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current
    val colorMateria = materia?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).alpha(if (a.completada) 0.62f else 1f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Pastilla(
                    materia?.nombre ?: tx("Sin materia", "No subject", "Sem matéria"),
                    colorMateria,
                    conPunto = true,
                    colorTexto = MaterialTheme.colorScheme.onSurface
                )
                Pastilla(nombrePrioridad(a.prioridad), colorPrioridad(a.prioridad))
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = a.completada, onCheckedChange = { onAlternar() })
                Column(Modifier.weight(1f)) {
                    Text(
                        a.titulo,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (a.completada) TextDecoration.LineThrough else null
                    )
                    Text(
                        fechaTexto(a.vence.toLocalDate()) + " · " + (if (a.tipoHorario == 1) tx("Todo el día", "All day", "Todo o dia") else formatoHora(a.vence.toLocalTime())),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (a.subtareas.isNotEmpty()) {
                val completadasCount = a.subtareas.count { it.completada }
                Column(Modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp)) {
                    Text(
                        "$completadasCount/${a.subtareas.size} " + tx("subtareas completadas", "subtasks completed", "subtarefas concluídas"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    a.subtareas.forEach { sub ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onAlternarSubtarea(sub.id) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sub.completada,
                                onCheckedChange = { onAlternarSubtarea(sub.id) },
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                sub.titulo,
                                fontSize = 13.sp,
                                textDecoration = if (sub.completada) TextDecoration.LineThrough else null,
                                color = if (sub.completada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(start = 48.dp)) {
                a.adjuntos.forEach { adj ->
                    TextButton(onClick = { abrirAdjunto(context, adj) }, contentPadding = PaddingValues(0.dp)) {
                        Text(adj.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (a.esExamen) tx("Examen", "Exam", "Prova") else tx("Tarea", "Task", "Tarefa")) +
                                (a.completadaEn?.let {
                                    "  ·  " + tx("Completado el ", "Completed on ", "Concluído em ") + fechaTexto(it.toLocalDate())
                                } ?: ""),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEliminar) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = tx("Eliminar", "Delete", "Excluir"),
                            tint = Color(0xFFA83D50)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// PANTALLA 2: NUEVA ACTIVIDAD (también sirve para editar)
// =====================================================================

@Composable
fun NuevaActividadScreen(state: AppState, actividadId: String?, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val existente = remember(actividadId) { state.actividades.find { it.id == actividadId } }

    var titulo by rememberSaveable { mutableStateOf(existente?.titulo ?: "") }
    var esExamen by rememberSaveable { mutableStateOf(existente?.esExamen ?: true) }
    var materiaId by rememberSaveable { mutableStateOf(existente?.materiaId ?: "") }
    var tipoHorario by rememberSaveable { mutableIntStateOf(existente?.tipoHorario ?: 0) }
    var fechaDia by rememberSaveable { mutableStateOf(existente?.vence?.toLocalDate()?.toEpochDay()) }
    var hora by rememberSaveable { mutableStateOf(existente?.let { formatoHora(it.vence.toLocalTime()) } ?: "") }
    var prioridad by rememberSaveable { mutableIntStateOf(existente?.prioridad ?: 1) }
    var notas by rememberSaveable { mutableStateOf(existente?.notas ?: "") }
    var adjuntos by remember { mutableStateOf(existente?.adjuntos ?: emptyList()) }
    var subtareas by remember { mutableStateOf(existente?.subtareas ?: emptyList()) }
    var nuevaSubtareaTexto by rememberSaveable { mutableStateOf("") }
    var menuMateria by remember { mutableStateOf(false) }
    var dialogoMateria by remember { mutableStateOf(false) }
    val fecha = fechaDia?.let { LocalDate.ofEpochDay(it) }

    val selectorArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {}
            adjuntos = adjuntos + Adjunto(
                uri = uri.toString(),
                nombre = nombreArchivo(context, uri),
                tipo = context.contentResolver.getType(uri) ?: ""
            )
        }
    }

    fun guardar() {
        val f = fecha
        val h = hora.trim()
        if (titulo.isBlank() || materiaId.isEmpty() || f == null) {
            aviso(
                context,
                tx(
                    "Completa el nombre, la materia y la fecha.",
                    "Fill in the name, subject and date.",
                    "Preencha o nome, a matéria e a data."
                )
            )
            return
        }
        if (tipoHorario == 0 && h.isNotEmpty() && !regexHora.matches(h)) {
            aviso(
                context,
                tx("Escribe la hora en formato HH:MM.", "Enter the time as HH:MM.", "Digite a hora no formato HH:MM.")
            )
            return
        }
        val horaFinal = if (tipoHorario == 0) {
            runCatching { LocalTime.parse(h.ifEmpty { state.prefs.horaPredeterminada }) }
                .getOrDefault(LocalTime.of(8, 0))
        } else {
            LocalTime.of(0, 0)
        }
        state.guardarActividad(
            Actividad(
                id = existente?.id ?: UUID.randomUUID().toString(),
                titulo = titulo.trim(),
                esExamen = esExamen,
                materiaId = materiaId,
                vence = LocalDateTime.of(f, horaFinal),
                prioridad = prioridad,
                notas = notas.trim(),
                adjuntos = adjuntos,
                completadaEn = existente?.completadaEn,
                tipoHorario = tipoHorario,
                subtareas = subtareas
            )
        )
        onCerrar()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CabeceraPantalla(
                if (existente != null) tx("Editar Actividad", "Edit Activity", "Editar Atividade")
                else tx("Nueva Actividad", "New Activity", "Nova Atividade"),
                Icons.Default.Close, tx("Cerrar", "Close", "Fechar"), onCerrar
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Segmentado(
                listOf(tx("Examen", "Exam", "Prova"), tx("Tarea", "Task", "Tarefa")),
                if (esExamen) 0 else 1
            ) { esExamen = it == 0 }

            EtiquetaCampo(tx("Modalidad de horario", "Schedule type", "Modalidade de horário"))
            Segmentado(
                listOf(
                    tx("Hora específica", "Specific time", "Horário específico"),
                    tx("Todo el día", "All day", "Todo o dia")
                ),
                tipoHorario
            ) { tipoHorario = it }

            EtiquetaCampo(tx("Nombre de la actividad *", "Activity name *", "Nome da atividade *"))
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                placeholder = { Text(tx("Ej. Examen de biología", "E.g. Biology exam", "Ex.: Prova de biologia")) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            EtiquetaCampo(tx("Materia *", "Subject *", "Matéria *"))
            Box {
                val seleccionada = state.materias.find { it.id == materiaId }
                CampoBox(onClick = { menuMateria = true }) {
                    Text(
                        seleccionada?.nombre ?: tx("Seleccionar materia", "Select subject", "Selecionar matéria"),
                        fontSize = 15.sp,
                        color = if (seleccionada != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuMateria, onDismissRequest = { menuMateria = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "+ " + tx("Crear nueva materia", "Create new subject", "Criar nova matéria"),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            menuMateria = false
                            dialogoMateria = true
                        }
                    )
                    state.materias.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.nombre) },
                            leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(m.color))) },
                            onClick = { materiaId = m.id; menuMateria = false }
                        )
                    }
                }
            }

            if (tipoHorario == 0) {
                val timePickerDialog = remember {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            hora = String.format("%02d:%02d", h, m)
                        },
                        hora.takeIf { regexHora.matches(it) }?.split(":")?.get(0)?.toIntOrNull() ?: 8,
                        hora.takeIf { regexHora.matches(it) }?.split(":")?.get(1)?.toIntOrNull() ?: 0,
                        true
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        EtiquetaCampo(tx("Fecha *", "Date *", "Data *"))
                        CampoFecha(fecha) { fechaDia = it.toEpochDay() }
                    }
                    Column(Modifier.width(130.dp)) {
                        EtiquetaCampo(tx("Hora", "Time", "Hora"))
                        OutlinedTextField(
                            value = hora,
                            onValueChange = { v -> hora = v.filter { it.isDigit() || it == ':' }.take(5) },
                            placeholder = { Text("HH:MM") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { timePickerDialog.show() }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Elegir hora")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                EtiquetaCampo(tx("Fecha *", "Date *", "Data *"))
                CampoFecha(fecha) { fechaDia = it.toEpochDay() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 2).forEach { valor ->
                    val activo = prioridad == valor
                    val c = colorPrioridad(valor)
                    val forma = RoundedCornerShape(12.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(forma)
                            .background(if (activo) c.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (activo) c else MaterialTheme.colorScheme.outline, forma)
                            .clickable { prioridad = valor },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            nombrePrioridad(valor),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activo) c else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            EtiquetaCampo(tx("Descripción o notas", "Description or notes", "Descrição ou notas"))
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                placeholder = {
                    Text(
                        tx(
                            "Anota temas, entregables o detalles...",
                            "Write topics, deliverables or details...",
                            "Anote temas, entregas ou detalhes..."
                        )
                    )
                },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            EtiquetaCampo(tx("Subtareas / Checklist (máx. 6)", "Subtasks / Checklist (max 6)", "Subtarefas / Lista de verificação (máx 6)"))
            subtareas.forEachIndexed { idx, sub ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(sub.titulo, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    IconButton(onClick = { subtareas = subtareas.filterIndexed { i, _ -> i != idx } }) {
                        Icon(Icons.Default.Close, contentDescription = "Eliminar subtarea", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (subtareas.size < 6) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nuevaSubtareaTexto,
                        onValueChange = { nuevaSubtareaTexto = it },
                        placeholder = { Text(tx("Añadir paso o subtarea...", "Add step or subtask...", "Adicionar passo ou subtarefa...")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (nuevaSubtareaTexto.isNotBlank()) {
                                subtareas = subtareas + Subtarea(UUID.randomUUID().toString(), nuevaSubtareaTexto.trim(), false)
                                nuevaSubtareaTexto = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir subtarea", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            EtiquetaCampo(tx("Imágenes y archivos PDF", "Images and PDF files", "Imagens e arquivos PDF"))
            CampoBox(onClick = { selectorArchivo.launch(arrayOf("image/*", "application/pdf")) }) {
                Text(
                    tx("Añadir imagen o PDF", "Add image or PDF", "Adicionar imagem ou PDF"),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            adjuntos.forEachIndexed { indice, adj ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(adj.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { adjuntos = adjuntos.filterIndexed { i, _ -> i != indice } }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = tx("Quitar archivo", "Remove file", "Remover arquivo"),
                            tint = Color(0xFFA83D50)
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(descripcionAvisos(state.prefs), fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp))
            }

            Button(
                onClick = { guardar() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    tx("Guardar Actividad", "Save Activity", "Salvar Atividade"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (dialogoMateria) {
        DialogoNuevaMateria(
            existentes = state.materias.map { it.nombre },
            onCerrar = { dialogoMateria = false },
            onGuardar = { nombre, color ->
                state.agregarMateria(nombre, color)
                val nueva = state.materias.find { it.nombre.equals(nombre, ignoreCase = true) }
                if (nueva != null) {
                    materiaId = nueva.id
                }
                dialogoMateria = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFecha(fecha: LocalDate?, onCambio: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CampoBox(onClick = { abierto = true }) {
        Text(
            if (fecha != null) fechaTexto(fecha) else tx("Seleccionar", "Select", "Selecionar"),
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (fecha != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
    if (abierto) {
        val estadoPicker = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoPicker.selectedDateMillis?.let {
                        onCambio(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text(tx("Aceptar", "OK", "OK")) }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text(tx("Cancelar", "Cancel", "Cancelar")) } }
        ) { DatePicker(state = estadoPicker) }
    }
}

// =====================================================================
// PANTALLA 3: AJUSTES
// =====================================================================

@Composable
fun AjustesScreen(state: AppState, onVolver: () -> Unit, onVerTour: () -> Unit) {
    val context = LocalContext.current
    val prefs = state.prefs
    var dialogoMateria by remember { mutableStateOf(false) }
    var materiaAEliminar by remember { mutableStateOf<Materia?>(null) }
    var horaTexto by remember { mutableStateOf(prefs.horaPredeterminada) }
    var mostrarHistorial by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CabeceraPantalla(
                tx("Ajustes", "Settings", "Configurações"),
                Icons.AutoMirrored.Filled.ArrowBack,
                tx("Volver", "Back", "Voltar"),
                onVolver
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            EncabezadoSeccion(tx("MATERIAS", "SUBJECTS", "MATÉRIAS"))
            TarjetaAjustes {
                if (state.materias.isEmpty()) {
                    Text(
                        tx("Aún no hay materias. Añade la primera.", "No subjects yet. Add the first one.", "Ainda não há matérias. Adicione a primeira."),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                state.materias.forEach { m ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(m.color)))
                        Text(m.nombre, fontSize = 15.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                        IconButton(onClick = { materiaAEliminar = m }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = tx("Eliminar materia", "Delete subject", "Excluir matéria"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(onClick = { dialogoMateria = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        tx("Añadir materia", "Add subject", "Adicionar matéria"),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            EncabezadoSeccion(tx("RECORDATORIOS", "REMINDERS", "LEMBRETES"))
            TarjetaAjustes {
                FilaSwitch(
                    tx("Activar notificaciones", "Enable notifications", "Ativar notificações"),
                    prefs.notificaciones
                ) { v -> state.actualizarPrefs { it.copy(notificaciones = v) } }
                FilaSwitch(
                    tx("Recordar 1 día antes", "Remind 1 day before", "Lembrar 1 dia antes"),
                    prefs.recordar1Dia
                ) { v -> state.actualizarPrefs { it.copy(recordar1Dia = v) } }
                FilaSwitch(
                    tx("Recordar 3 horas antes", "Remind 3 hours before", "Lembrar 3 horas antes"),
                    prefs.recordar3Horas
                ) { v -> state.actualizarPrefs { it.copy(recordar3Horas = v) } }
                FilaSwitch(
                    tx("Recordar 30 minutos antes", "Remind 30 minutes before", "Lembrar 30 minutos antes"),
                    prefs.recordar30Min
                ) { v -> state.actualizarPrefs { it.copy(recordar30Min = v) } }
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tx("Hora predeterminada", "Default time", "Hora padrão"), fontSize = 15.sp, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = horaTexto,
                        onValueChange = { v ->
                            val limpio = v.filter { it.isDigit() || it == ':' }.take(5)
                            horaTexto = limpio
                            if (regexHora.matches(limpio)) state.actualizarPrefs { it.copy(horaPredeterminada = limpio) }
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo(),
                        modifier = Modifier.width(104.dp)
                    )
                }
                Text(
                    tx(
                        "Los avisos se programan al guardar una actividad y pueden demorar unos minutos.",
                        "Reminders are scheduled when you save an activity and may be delayed a few minutes.",
                        "Os lembretes são programados ao salvar uma atividade e podem demorar alguns minutos."
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            EncabezadoSeccion(tx("GENERAL", "GENERAL", "GERAL"))
            TarjetaAjustes {
                Text(tx("Tu nombre", "Your name", "Seu nome"), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                var nombreEdit by remember(prefs.nombreUsuario) { mutableStateOf(prefs.nombreUsuario) }
                OutlinedTextField(
                    value = nombreEdit,
                    onValueChange = { n ->
                        nombreEdit = n
                        state.actualizarPrefs { it.copy(nombreUsuario = n) }
                    },
                    placeholder = { Text(tx("Tu nombre o apodo", "Your name or nickname", "Seu nome ou apelido")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Text(tx("Tema visual", "Visual theme", "Tema visual"), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Segmentado(
                    listOf(
                        tx("Claro", "Light", "Claro"),
                        tx("Oscuro", "Dark", "Escuro"),
                        tx("Amoled", "Amoled", "Amoled")
                    ),
                    prefs.tema.coerceIn(0, 2)
                ) { i -> state.actualizarPrefs { it.copy(tema = i) } }
                Spacer(Modifier.height(20.dp))
                Text(tx("Estilo de color (Acento)", "Color accent", "Estilo de cor"), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listaColoresAcento.forEachIndexed { i, argb ->
                        val activo = prefs.colorAcento == i
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .then(
                                    if (activo) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { state.actualizarPrefs { it.copy(colorAcento = i) } }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(tx("Inicio de semana", "Week starts on", "Início da semana"), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Segmentado(
                    listOf(tx("Lunes", "Monday", "Segunda"), tx("Domingo", "Sunday", "Domingo")),
                    if (prefs.semanaEmpiezaDomingo) 1 else 0
                ) { i -> state.actualizarPrefs { it.copy(semanaEmpiezaDomingo = i == 1) } }
                Spacer(Modifier.height(20.dp))
                Text(tx("Idioma", "Language", "Idioma"), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                val codigos = IdiomaApp.soportados.map { it.first }
                Segmentado(
                    IdiomaApp.soportados.map { it.second },
                    codigos.indexOf(IdiomaApp.codigo).coerceAtLeast(0)
                ) { i -> IdiomaApp.cambiar(context, codigos[i]) }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onVerTour,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        tx("Ver tour de bienvenida", "View welcome tour", "Ver tour de boas-vindas"),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { mostrarHistorial = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        tx("Historial de versiones", "Version history", "Histórico de versões"),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                tx("Estudi – Versión 1.0.2", "Estudi – Version 1.0.2", "Estudi – Versão 1.0.2"),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 4.dp)
            )
            Text(
                tx(
                    "En desarrollo por Nahuel Thomas",
                    "In development by Nahuel Thomas",
                    "Em desenvolvimento por Nahuel Thomas"
                ),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }

    if (mostrarHistorial) {
        DialogoHistorialVersiones(onCerrar = { mostrarHistorial = false })
    }

    materiaAEliminar?.let { m ->
        val cantidad = state.actividades.count { it.materiaId == m.id }
        var pasoConfirmacion by remember { mutableStateOf(1) }

        AlertDialog(
            onDismissRequest = { materiaAEliminar = null },
            title = {
                Text(
                    if (pasoConfirmacion == 1) tx("Eliminar materia", "Delete subject", "Excluir matéria")
                    else tx("⚠️ Confirmación final", "⚠️ Final confirmation", "⚠️ Confirmação final")
                )
            },
            text = {
                Text(
                    if (pasoConfirmacion == 1) {
                        if (cantidad == 0) {
                            tx("¿Eliminar \"${m.nombre}\"?", "Delete \"${m.nombre}\"?", "Excluir \"${m.nombre}\"?")
                        } else {
                            tx(
                                "⚠️ ATENCIÓN: Al eliminar \"${m.nombre}\", **se eliminarán permanentemente sus $cantidad actividades asociadas**.",
                                "⚠️ WARNING: Deleting \"${m.nombre}\" will **permanently delete its $cantidad associated activities**.",
                                "⚠️ ATENÇÃO: Ao excluir \"${m.nombre}\", **serão excluídas permanentemente suas $cantidad atividades associadas**."
                            )
                        }
                    } else {
                        tx(
                            "¿Estás 100% seguro? Esta acción no se puede deshacer y perderás toda la información y archivos de esta materia.",
                            "Are you 100% sure? This action cannot be undone and you will lose all info and files for this subject.",
                            "Você tem 100% de certeza? Esta ação não pode ser desfeita e você perderá todas as informações."
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (cantidad > 0 && pasoConfirmacion == 1) {
                        pasoConfirmacion = 2
                    } else {
                        state.eliminarMateria(m.id)
                        materiaAEliminar = null
                    }
                }) {
                    Text(
                        if (pasoConfirmacion == 1 && cantidad > 0) tx("Continuar", "Continue", "Continuar")
                        else tx("Sí, borrar todo", "Yes, delete all", "Sim, excluir tudo"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { materiaAEliminar = null }) {
                    Text(tx("Cancelar", "Cancel", "Cancelar"))
                }
            }
        )
    }

    if (dialogoMateria) {
        DialogoNuevaMateria(
            existentes = state.materias.map { it.nombre },
            onCerrar = { dialogoMateria = false },
            onGuardar = { nombre, color -> state.agregarMateria(nombre, color); dialogoMateria = false }
        )
    }
}

@Composable
private fun FilaSwitch(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}

@Composable
private fun DialogoNuevaMateria(existentes: List<String>, onCerrar: () -> Unit, onGuardar: (String, Long) -> Unit) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(coloresMateria[0]) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(tx("Nueva materia", "New subject", "Nova matéria")) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(tx("Nombre", "Name", "Nome")) },
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    coloresMateria.forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(
                                    if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val limpio = nombre.trim()
                when {
                    limpio.isEmpty() -> aviso(
                        context,
                        tx("Escribe el nombre de la materia.", "Enter the subject name.", "Digite o nome da matéria.")
                    )
                    existentes.any { it.equals(limpio, ignoreCase = true) } -> aviso(
                        context,
                        tx(
                            "Ya hay una materia con ese nombre.",
                            "A subject with that name already exists.",
                            "Já existe uma matéria com esse nome."
                        )
                    )
                    else -> onGuardar(limpio, color)
                }
            }) { Text(tx("Añadir", "Add", "Adicionar")) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(tx("Cancelar", "Cancel", "Cancelar")) } }
    )
}

@Composable
fun DialogoNotasParche(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎉 ", fontSize = 20.sp)
                Text(
                    tx("¡Novedades en Estudi v1.0.2!", "What's new in Estudi v1.0.2!", "O que há de novo no Estudi v1.0.2!"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    tx(
                        "¡Gracias por actualizar! Esto es lo nuevo que traemos para ti:",
                        "Thanks for updating! Here is what's new for you:",
                        "Obrigado por atualizar! Aqui está o que há de novo para você:"
                    ),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                NovedadItem("✨", tx("Rebranding a Estudi", "Rebranding to Estudi", "Rebranding para Estudi"), tx("Cambiamos oficialmente el nombre a Estudi para hacerlo más corto, moderno y accesible.", "We officially changed the name to Estudi to make it shorter, modern and clean.", "Mudamos oficialmente o nome para Estudi para torná-lo mais curto e moderno."))
                NovedadItem("🌊", tx("Nuevo Logo Minimalista", "New Minimalist Logo", "Novo Logo Minimalista"), tx("Nuevo ícono e imagen de marca minimalista con diseño de olas fluido.", "New minimalist icon and brand identity with clean wave design.", "Novo ícone e identidade de marca minimalista com design de ondas."))
                NovedadItem("⚡", tx("Optimización de Código", "Code Optimization", "Otimização de Código"), tx("Mejoras internas de velocidad, respuesta en widgets y fluidez en la interfaz.", "Internal performance improvements, faster widget updates and smooth UI.", "Melhorias internas de velocidade, widgets mais rápidos e interface fluida."))
            }
        },
        confirmButton = {
            Button(onClick = onCerrar, shape = RoundedCornerShape(12.dp)) {
                Text(tx("¡Genial!", "Awesome!", "Legal!"))
            }
        }
    )
}

@Composable
private fun NovedadItem(icono: String, titulo: String, descripcion: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(icono, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Column {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DialogoHistorialVersiones(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(tx("Historial de versiones", "Version history", "Histórico de versões"), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("v1.0.2 (Actual)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    tx(
                        "• Rebranding oficial a Estudi.\n• Nuevo ícono adaptativo y logo minimalista de olas.\n• Optimizaciones generales de rendimiento y respuesta de widgets.",
                        "• Official rebranding to Estudi.\n• New adaptive icon and minimalist wave logo.\n• General performance optimizations and widget responsiveness.",
                        "• Rebranding oficial para Estudi.\n• Novo ícone adaptativo e logo minimalista de ondas.\n• Otimizações gerais de desempenho e resposta dos widgets."
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("v1.0.1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    tx(
                        "• Horarios flexibles (Hora específica y Todo el día).\n• Subtareas con checklists (máx. 6) con completado automático.\n• Selector de hora con reloj nativo.\n• Creación rápida de materias desde el formulario.\n• Confirmación clara al eliminar asignaturas.\n• Temas visuales (Claro, Oscuro y Amoled) + Paleta de colores de acento independientes.\n• Personalización interactiva directa desde el Tour.",
                        "• Flexible schedules (Specific time and All day).\n• Subtasks with checklists (max 6) with auto-completion.\n• Time picker with native clock.\n• Quick subject creation from form.\n• Clear deletion confirmation for subjects.\n• Visual themes (Light, Dark and Amoled) + Independent accent color palettes.\n• Interactive customization inside the Tour.",
                        "• Horários flexíveis (Horário específico e Todo o dia).\n• Subtarefas com checklists (máx 6) com conclusão automática.\n• Seletor de hora com relógio nativo.\n• Criação rápida de matérias no formulário.\n• Confirmação clara ao excluir matérias.\n• Temas visuais (Claro, Escuro e Amoled) + Paleta de cores independente.\n• Personalização interativa direto no Tour."
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("v1.0.0", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    tx(
                        "• Lanzamiento inicial.\n• Gestión de materias, tareas y exámenes.\n• Widgets de Hoy, Semana, Lista semanal y Mensual.\n• Notificaciones y recordatorios inteligentes.",
                        "• Initial release.\n• Subject, task and exam management.\n• Today, Week, Weekly list and Monthly widgets.\n• Smart notifications and reminders.",
                        "• Lançamento inicial.\n• Gestão de matérias, tarefas e exames.\n• Widgets de Hoje, Semana, Lista semanal e Mensal.\n• Notificações e lembretes inteligentes."
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onCerrar, shape = RoundedCornerShape(12.dp)) {
                Text(tx("Cerrar", "Close", "Fechar"))
            }
        }
    )
}