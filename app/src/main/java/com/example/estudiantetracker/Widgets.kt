package com.example.estudiantetracker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate

// =====================================================================
// Colores y Temas para Widgets
// =====================================================================

private fun fijo(argb: Long): ColorProvider =
    androidx.glance.color.ColorProvider(day = Color(argb), night = Color(argb))

private fun colorFondoWidget(tema: Int): ColorProvider = when (tema) {
    2 -> fijo(0xFF000000) // Amoled
    1 -> fijo(0xFF16132A) // Oscuro
    else -> fijo(0xFFFFFFFF) // Claro
}

private fun colorTarjetaWidget(tema: Int): ColorProvider = when (tema) {
    2 -> fijo(0xFF121214) // Amoled
    1 -> fijo(0xFF231F3D)
    else -> fijo(0xFFF5F5FA)
}

private fun colorTextoPrincipal(tema: Int): ColorProvider = when (tema) {
    0 -> fijo(0xFF24243A)
    else -> fijo(0xFFF4F2FF)
}

private fun colorTextoSecundario(tema: Int): ColorProvider = when (tema) {
    0 -> fijo(0xFF626279)
    else -> fijo(0xFF9C98B8)
}

private fun colorAcento(tema: Int, colorAcentoIndex: Int = 0): ColorProvider = when (tema) {
    0 -> fijo(listaColoresAcento[colorAcentoIndex.coerceIn(0, 4)])
    else -> fijo(listaColoresAcentoOscuro[colorAcentoIndex.coerceIn(0, 4)])
}

private fun colorTarjetaSuave(tema: Int): ColorProvider = when (tema) {
    0 -> fijo(0xFFE5E5F2)
    2 -> fijo(0xFF1B1B1F)
    else -> fijo(0xFF2E2650)
}

private val Rojo = fijo(0xFFEF4444)
private val RojoSuave = fijo(0xFF3B1F2F)
private val RojoTexto = fijo(0xFFFCA5A5)
private val Ambar = fijo(0xFFF59E0B)
private val Azul = fijo(0xFF3B82F6)
private val AzulSuave = fijo(0xFF1E2A4A)
private val AzulTexto = fijo(0xFF93C5FD)

private fun colorBarra(p: Int): ColorProvider = when (p) {
    2 -> Rojo
    1 -> Ambar
    else -> Azul
}

private fun textoHorario(a: Actividad, idioma: String): String = when (a.tipoHorario) {
    1 -> tx(idioma, "Todo el día", "All day", "Todo o dia")
    else -> formatoHora(a.vence.toLocalTime())
}

// =====================================================================
// Datos y utilidades comunes
// =====================================================================

@Composable
private fun abrirApp(): Action = actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))

private class DatosWidget(
    val hoy: LocalDate,
    val actividades: List<Actividad>,
    val materias: List<Materia>,
    val prefs: Preferencias,
    val idioma: String
) {
    fun pendientesDel(fecha: LocalDate): List<Actividad> =
        actividades.filter { !it.completada && it.vence.toLocalDate() == fecha }.sortedBy { it.vence }

    fun nombreMateria(a: Actividad): String = materias.find { it.id == a.materiaId }?.nombre ?: tx(idioma, "Sin materia", "No subject", "Sem matéria")

    fun inicioSemana(): LocalDate {
        val desplazamiento = if (prefs.semanaEmpiezaDomingo) hoy.dayOfWeek.value % 7 else hoy.dayOfWeek.value - 1
        return hoy.minusDays(desplazamiento.toLong())
    }

    fun diasDeLaSemana(): List<LocalDate> = inicioSemana().let { ini -> (0..6).map { ini.plusDays(it.toLong()) } }
}

private fun cargarDatos(context: Context): DatosWidget {
    val repo = Repository(context.applicationContext)
    return DatosWidget(LocalDate.now(), repo.loadActividades(), repo.loadMaterias(), repo.loadPrefs(), IdiomaApp.leer(context))
}

private fun rangoSemana(inicio: LocalDate, idioma: String): String {
    val fin = inicio.plusDays(6)
    val m1 = mesCorto(inicio.monthValue, idioma)
    val m2 = mesCorto(fin.monthValue, idioma)
    return if (inicio.month == fin.month) "$m1 ${inicio.dayOfMonth}–${fin.dayOfMonth}"
    else "$m1 ${inicio.dayOfMonth} – $m2 ${fin.dayOfMonth}"
}

private fun diaSeleccionado(context: Context, hoy: LocalDate): LocalDate {
    val sp = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
    return if (sp.contains("dia") && sp.getLong("guardado_el", -1L) == hoy.toEpochDay()) {
        LocalDate.ofEpochDay(sp.getLong("dia", hoy.toEpochDay()))
    } else hoy
}

suspend fun actualizarWidgets(context: Context) {
    try {
        WidgetHoy().updateAll(context)
        WidgetSemana().updateAll(context)
        WidgetLista().updateAll(context)
        WidgetMensual().updateAll(context)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val receivers = listOf(
            WidgetHoyReceiver::class.java,
            WidgetSemanaReceiver::class.java,
            WidgetListaReceiver::class.java,
            WidgetMensualReceiver::class.java
        )
        receivers.forEach { receiverClass ->
            val intent = Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, receiverClass))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    } catch (e: Exception) {}
}

@Composable
private fun Etiqueta(texto: String, fondo: ColorProvider, color: ColorProvider) {
    Text(
        texto,
        modifier = GlanceModifier.background(fondo).cornerRadius(6.dp).padding(horizontal = 6.dp, vertical = 1.dp),
        maxLines = 1,
        style = TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    )
}

// =====================================================================
// WIDGET 1: HOY (pequeño, 2x2)
// =====================================================================

class WidgetHoy : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = cargarDatos(context)
        provideContent { ContenidoHoy(datos) }
    }
}

class WidgetHoyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetHoy()
}

@Composable
private fun ContenidoHoy(d: DatosWidget) {
    val pendientes = d.pendientesDel(d.hoy)
    val critico = pendientes.any { it.prioridad == 2 }
    val tema = d.prefs.tema

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFondoWidget(tema))
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(abrirApp())
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tx(d.idioma, "HOY", "TODAY", "HOJE"),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            if (critico) {
                Text(tx(d.idioma, "● CRÍTICO", "● CRITICAL", "● CRÍTICO"), style = TextStyle(color = Rojo, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(diaLargo(d.hoy.dayOfWeek, d.idioma), style = TextStyle(color = colorTextoSecundario(tema), fontSize = 12.sp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(d.hoy.dayOfMonth.toString(), style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 34.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(4.dp))
            Text(mesCorto(d.hoy.monthValue, d.idioma), style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 15.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            when (pendientes.size) {
                0 -> tx(d.idioma, "Todo al día", "All caught up", "Tudo em dia")
                1 -> tx(d.idioma, "1 pendiente", "1 pending", "1 pendente")
                else -> "${pendientes.size} " + tx(d.idioma, "pendientes", "pending", "pendentes")
            },
            modifier = GlanceModifier
                .background(if (critico) RojoSuave else colorTarjetaSuave(tema))
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            style = TextStyle(
                color = if (critico) RojoTexto else colorAcento(tema),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// =====================================================================
// WIDGET 2: SEMANA (mediano, 4x2)
// =====================================================================

private val CLAVE_DIA = ActionParameters.Key<Long>("dia")

class SeleccionarDiaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val dia = parameters[CLAVE_DIA] ?: return
        context.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit()
            .putLong("dia", dia)
            .putLong("guardado_el", LocalDate.now().toEpochDay())
            .apply()
        WidgetSemana().updateAll(context)
    }
}

class WidgetSemana : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = cargarDatos(context)
        val elegido = diaSeleccionado(context, datos.hoy)
        provideContent { ContenidoSemana(datos, elegido) }
    }
}

class WidgetSemanaReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetSemana()
}

@Composable
private fun ContenidoSemana(d: DatosWidget, elegido: LocalDate) {
    val dias = d.diasDeLaSemana()
    val seleccion = if (elegido in dias) elegido else d.hoy
    val lista = d.pendientesDel(seleccion)
    val tema = d.prefs.tema

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFondoWidget(tema))
            .cornerRadius(24.dp)
            .padding(10.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            dias.forEach { dia ->
                val activo = dia == seleccion
                val base = GlanceModifier
                    .defaultWeight()
                    .cornerRadius(10.dp)
                    .clickable(actionRunCallback<SeleccionarDiaCallback>(actionParametersOf(CLAVE_DIA to dia.toEpochDay())))
                Column(
                    modifier = if (activo) base.background(colorAcento(tema, d.prefs.colorAcento)) else base,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        diaCorto(dia.dayOfWeek, d.idioma),
                        style = TextStyle(color = if (activo) colorFondoWidget(tema) else colorTextoSecundario(tema), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        dia.dayOfMonth.toString(),
                        style = TextStyle(color = if (activo) colorFondoWidget(tema) else colorTextoPrincipal(tema), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(3.dp))
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Column(modifier = GlanceModifier.fillMaxWidth().clickable(abrirApp())) {
            if (lista.isEmpty()) {
                Text(tx(d.idioma, "Sin actividades este día", "No activities this day", "Sem atividades neste dia"), style = TextStyle(color = colorTextoSecundario(tema), fontSize = 11.sp))
            }
            lista.take(2).forEach { a ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("●", style = TextStyle(color = colorBarra(a.prioridad), fontSize = 9.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        a.titulo,
                        modifier = GlanceModifier.defaultWeight(),
                        maxLines = 1,
                        style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(textoHorario(a, d.idioma), style = TextStyle(color = colorTextoSecundario(tema), fontSize = 11.sp))
                }
            }
            if (lista.size > 2) {
                Text("+${lista.size - 2} " + tx(d.idioma, "más", "more", "mais"), style = TextStyle(color = colorAcento(tema), fontSize = 10.sp))
            }
        }
    }
}

// =====================================================================
// WIDGET 3: LISTA SEMANAL (grande, 4x4) - Redimensionable
// =====================================================================

class WidgetLista : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = cargarDatos(context)
        provideContent { ContenidoLista(datos) }
    }
}

class WidgetListaReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetLista()
}

private sealed class Fila {
    class Encabezado(val fecha: LocalDate) : Fila()
    class Tarjeta(val actividad: Actividad) : Fila()
}

@Composable
private fun ContenidoLista(d: DatosWidget) {
    val inicio = d.inicioSemana()
    val tema = d.prefs.tema
    val filas = buildList {
        d.diasDeLaSemana().forEach { dia ->
            val pendientes = d.pendientesDel(dia)
            if (pendientes.isNotEmpty()) {
                add(Fila.Encabezado(dia))
                pendientes.forEach { add(Fila.Tarjeta(it)) }
            }
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFondoWidget(tema))
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(tx(d.idioma, "SEMANA ACTUAL", "THIS WEEK", "SEMANA ATUAL"), style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                Text(
                    "${nombreMes(d.hoy.monthValue, d.idioma)} ${d.hoy.year}",
                    style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
            }
            Text(
                rangoSemana(inicio, d.idioma),
                modifier = GlanceModifier.background(colorTarjetaSuave(tema)).cornerRadius(10.dp).padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            )
        }
        Spacer(GlanceModifier.height(6.dp))

        if (filas.isEmpty()) {
            Text(
                tx(d.idioma, "No hay actividades pendientes esta semana.", "No pending activities this week.", "Não há atividades pendentes esta semana."),
                modifier = GlanceModifier.clickable(abrirApp()),
                style = TextStyle(color = colorTextoSecundario(tema), fontSize = 12.sp)
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(filas) { fila ->
                    when (fila) {
                        is Fila.Encabezado -> Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            val dia = diaLargo(fila.fecha.dayOfWeek, d.idioma)
                            val mes = nombreMes(fila.fecha.monthValue, d.idioma).uppercase()
                            val etiqueta = when (d.idioma) {
                                "en" -> "$dia, $mes ${fila.fecha.dayOfMonth}"
                                else -> "$dia ${fila.fecha.dayOfMonth} DE $mes"
                            }
                            Text(etiqueta, style = TextStyle(color = colorTextoSecundario(tema), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            if (fila.fecha == d.hoy) {
                                Text("  • " + tx(d.idioma, "Hoy", "Today", "Hoje"), style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            }
                        }

                        is Fila.Tarjeta -> {
                            val a = fila.actividad
                            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .background(colorTarjetaWidget(tema))
                                        .cornerRadius(10.dp)
                                        .clickable(abrirApp()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = GlanceModifier.width(4.dp).height(46.dp).background(colorBarra(a.prioridad))) {}
                                    Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 8.dp)) {
                                        Text(
                                            a.titulo,
                                            maxLines = 1,
                                            style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(GlanceModifier.height(2.dp))
                                        Row {
                                            if (a.esExamen) Etiqueta(tx(d.idioma, "Examen", "Exam", "Prova"), RojoSuave, RojoTexto)
                                            else Etiqueta(tx(d.idioma, "Tarea", "Task", "Tarefa"), AzulSuave, AzulTexto)
                                            Spacer(GlanceModifier.width(4.dp))
                                            Etiqueta(d.nombreMateria(a), colorTarjetaSuave(tema), colorTextoSecundario(tema))
                                        }
                                    }
                                    Text(
                                        textoHorario(a, d.idioma),
                                        modifier = GlanceModifier.padding(end = 8.dp),
                                        style = TextStyle(color = colorTextoSecundario(tema), fontSize = 11.sp)
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

// =====================================================================
// WIDGET 4: MENSUAL (vista de calendario mensual) - Redimensionable
// =====================================================================

class WidgetMensual : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = cargarDatos(context)
        provideContent { ContenidoMensual(datos) }
    }
}

class WidgetMensualReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetMensual()
}

@Composable
private fun ContenidoMensual(d: DatosWidget) {
    val hoy = d.hoy
    val tema = d.prefs.tema
    val actividadesMes = d.actividades.filter { !it.completada }

    val primerDiaMes = LocalDate.of(hoy.year, hoy.monthValue, 1)
    val diasEnMes = primerDiaMes.lengthOfMonth()
    val semanaEmpiezaDomingo = d.prefs.semanaEmpiezaDomingo

    val primerDiaSemanaValue = primerDiaMes.dayOfWeek.value
    val desplazamiento = if (semanaEmpiezaDomingo) (primerDiaSemanaValue % 7) else (primerDiaSemanaValue - 1)
    val inicioGrid = primerDiaMes.minusDays(desplazamiento.toLong())

    val totalCeldas = if (desplazamiento + diasEnMes > 35) 42 else 35
    val numFilas = totalCeldas / 7

    val letrasDias = if (semanaEmpiezaDomingo) {
        listOf("D", "L", "M", "M", "J", "V", "S")
    } else {
        listOf("L", "M", "M", "J", "V", "S", "D")
    }

    val proximas = actividadesMes.sortedBy { it.vence }

    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorFondoWidget(tema))
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        item {
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(abrirApp()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        tx(d.idioma, "MES ACTUAL", "THIS MONTH", "MÊS ATUAL"),
                        style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${nombreMes(hoy.monthValue, d.idioma)} ${hoy.year}",
                        style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    "${actividadesMes.count { it.vence.year == hoy.year && it.vence.monthValue == hoy.monthValue }} " + tx(d.idioma, "pendientes", "pending", "pendentes"),
                    modifier = GlanceModifier.background(colorTarjetaSuave(tema)).cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 3.dp),
                    style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        item {
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                letrasDias.forEach { letra ->
                    Text(
                        letra,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(color = colorTextoSecundario(tema), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    )
                }
            }
            Spacer(GlanceModifier.height(2.dp))
        }

        repeat(numFilas) { r ->
            item {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    repeat(7) { c ->
                        val fechaCell = inicioGrid.plusDays((r * 7 + c).toLong())
                        val esMismoMes = fechaCell.monthValue == hoy.monthValue
                        val esHoy = fechaCell == hoy
                        val pendientesDia = d.pendientesDel(fechaCell)
                        val tienePendientes = pendientesDia.isNotEmpty()
                        val tieneCritico = pendientesDia.any { it.prioridad == 2 || it.esExamen }

                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(vertical = 1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = if (esHoy) {
                                    GlanceModifier.background(colorAcento(tema, d.prefs.colorAcento)).cornerRadius(4.dp).padding(horizontal = 4.dp, vertical = 1.dp)
                                } else {
                                    GlanceModifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    fechaCell.dayOfMonth.toString(),
                                    style = TextStyle(
                                        color = if (esHoy) colorFondoWidget(tema) else if (esMismoMes) colorTextoPrincipal(tema) else colorTextoSecundario(tema),
                                        fontSize = 10.sp,
                                        fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                            if (tienePendientes && esMismoMes) {
                                Text(
                                    "●",
                                    style = TextStyle(
                                        color = if (tieneCritico) Rojo else colorAcento(tema, d.prefs.colorAcento),
                                        fontSize = 6.sp
                                    )
                                )
                            } else {
                                Spacer(GlanceModifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                tx(d.idioma, "PRÓXIMAS ENTREGAS", "UPCOMING DELIVERIES", "PRÓXIMAS ENTREGAS"),
                style = TextStyle(color = colorAcento(tema, d.prefs.colorAcento), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(4.dp))
        }

        if (proximas.isEmpty()) {
            item {
                Text(
                    tx(d.idioma, "Sin entregas pendientes este mes", "No pending activities this month", "Sem entregas pendentes este mês"),
                    modifier = GlanceModifier.clickable(abrirApp()),
                    style = TextStyle(color = colorTextoSecundario(tema), fontSize = 12.sp)
                )
            }
        } else {
            items(proximas) { a ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(bottom = 4.dp)
                        .background(colorTarjetaWidget(tema))
                        .cornerRadius(10.dp)
                        .clickable(abrirApp()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = GlanceModifier.width(4.dp).height(42.dp).background(colorBarra(a.prioridad))) {}
                    Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 8.dp)) {
                        Text(
                            a.titulo,
                            maxLines = 1,
                            style = TextStyle(color = colorTextoPrincipal(tema), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            d.nombreMateria(a) + if (a.subtareas.isNotEmpty()) " · ${a.subtareas.count { it.completada }}/${a.subtareas.size}" else "",
                            maxLines = 1,
                            style = TextStyle(color = colorTextoSecundario(tema), fontSize = 10.sp)
                        )
                    }
                    Text(
                        "${a.vence.dayOfMonth} ${mesCorto(a.vence.monthValue, d.idioma)}" + if (a.tipoHorario == 0) " ${formatoHora(a.vence.toLocalTime())}" else "",
                        modifier = GlanceModifier.padding(end = 8.dp),
                        style = TextStyle(color = colorTextoSecundario(tema), fontSize = 11.sp)
                    )
                }
            }
        }
    }
}
