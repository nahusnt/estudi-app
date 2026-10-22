package com.example.estudiantetracker

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Idioma de la app: "es", "en" o "pt".
 * `codigo` es un estado observable: al cambiarlo, todas las pantallas se redibujan solas.
 * Los widgets y las notificaciones (que no son pantallas) leen el idioma con `leer(context)`.
 */
object IdiomaApp {
    private const val ARCHIVO = "idioma"
    private const val CLAVE = "codigo"

    val soportados = listOf("es" to "Español", "en" to "English", "pt" to "Português")

    var codigo by mutableStateOf("es")
        private set

    /** Idioma guardado; si aún no se eligió ninguno, usa el del teléfono cuando es inglés o portugués. */
    fun leer(context: Context): String {
        val guardado = context.applicationContext
            .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .getString(CLAVE, null)
        if (guardado != null) return guardado
        val sistema = Locale.getDefault().language
        return if (sistema == "en" || sistema == "pt") sistema else "es"
    }

    fun cargar(context: Context) {
        codigo = leer(context)
    }

    fun cambiar(context: Context, nuevo: String) {
        context.applicationContext
            .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit().putString(CLAVE, nuevo).apply()
        codigo = nuevo
    }
}

// ---------- Traducción de textos ----------

fun tx(idioma: String, es: String, en: String, pt: String): String = when (idioma) {
    "en" -> en
    "pt" -> pt
    else -> es
}

/** Igual que la anterior, usando el idioma actual de la app (para las pantallas). */
fun tx(es: String, en: String, pt: String): String = tx(IdiomaApp.codigo, es, en, pt)

fun nombrePrioridad(p: Int, idioma: String = IdiomaApp.codigo): String = when (p) {
    2 -> tx(idioma, "Alta", "High", "Alta")
    1 -> tx(idioma, "Media", "Medium", "Média")
    else -> tx(idioma, "Baja", "Low", "Baixa")
}

fun saludoSegunHora(nombreUsuario: String, idioma: String = IdiomaApp.codigo): String {
    val hora = LocalTime.now().hour
    val nombre = nombreUsuario.trim()
    return when (hora) {
        in 0..5 -> {
            if (nombre.isNotEmpty()) {
                tx(idioma, "Buena madrugada, noctámbulo, $nombre 🦉", "Early morning, night owl, $nombre 🦉", "Boa madrugada, coruja, $nombre 🦉")
            } else {
                tx(idioma, "Buena madrugada, noctámbulo 🦉", "Early morning, night owl 🦉", "Boa madrugada, coruja 🦉")
            }
        }
        in 6..11 -> {
            if (nombre.isNotEmpty()) {
                tx(idioma, "Buenos días, $nombre ☀️", "Good morning, $nombre ☀️", "Bom dia, $nombre ☀️")
            } else {
                tx(idioma, "Buenos días ☀️", "Good morning ☀️", "Bom dia ☀️")
            }
        }
        in 12..18 -> {
            if (nombre.isNotEmpty()) {
                tx(idioma, "Buenas tardes, $nombre 🌤️", "Good afternoon, $nombre 🌤️", "Boa tarde, $nombre 🌤️")
            } else {
                tx(idioma, "Buenas tardes 🌤️", "Good afternoon 🌤️", "Boa tarde 🌤️")
            }
        }
        else -> {
            if (nombre.isNotEmpty()) {
                tx(idioma, "Buenas noches, $nombre 🌙", "Good evening, $nombre 🌙", "Boa noite, $nombre 🌙")
            } else {
                tx(idioma, "Buenas noches 🌙", "Good evening 🌙", "Boa noite 🌙")
            }
        }
    }
}

// ---------- Fechas (los formateadores se crean una sola vez) ----------

private val FORMATO_ES = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("es-ES"))
private val FORMATO_EN = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("en-US"))
private val FORMATO_PT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("pt-BR"))

fun fechaTexto(d: LocalDate, idioma: String = IdiomaApp.codigo): String =
    d.format(
        when (idioma) {
            "en" -> FORMATO_EN
            "pt" -> FORMATO_PT
            else -> FORMATO_ES
        }
    )

// Nombres para los widgets (en mayúsculas, como en el diseño).
private val DIAS_CORTOS = mapOf(
    "es" to listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"),
    "en" to listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    "pt" to listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM")
)
private val DIAS_LARGOS = mapOf(
    "es" to listOf("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO"),
    "en" to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
    "pt" to listOf("SEGUNDA", "TERÇA", "QUARTA", "QUINTA", "SEXTA", "SÁBADO", "DOMINGO")
)
private val MESES = mapOf(
    "es" to listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    ),
    "en" to listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ),
    "pt" to listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )
)

fun diaCorto(dia: DayOfWeek, idioma: String): String = (DIAS_CORTOS[idioma] ?: DIAS_CORTOS.getValue("es"))[dia.value - 1]

fun diaLargo(dia: DayOfWeek, idioma: String): String = (DIAS_LARGOS[idioma] ?: DIAS_LARGOS.getValue("es"))[dia.value - 1]

fun nombreMes(mes: Int, idioma: String): String = (MESES[idioma] ?: MESES.getValue("es"))[mes - 1]

fun mesCorto(mes: Int, idioma: String): String = nombreMes(mes, idioma).take(3)

// ---------- Resumen de avisos activados (formulario) ----------

fun descripcionAvisos(p: Preferencias, idioma: String = IdiomaApp.codigo): String {
    if (!p.notificaciones) {
        return tx(
            idioma,
            "Los recordatorios están desactivados en Ajustes.",
            "Reminders are turned off in Settings.",
            "Os lembretes estão desativados em Configurações."
        )
    }
    val partes = buildList {
        if (p.recordar1Dia) add(tx(idioma, "1 día antes", "1 day before", "1 dia antes"))
        if (p.recordar3Horas) add(tx(idioma, "3 horas antes", "3 hours before", "3 horas antes"))
        if (p.recordar30Min) add(tx(idioma, "30 minutos antes", "30 minutes before", "30 minutos antes"))
    }
    return if (partes.isEmpty()) {
        tx(
            idioma,
            "No hay avisos anticipados seleccionados.",
            "No advance reminders selected.",
            "Nenhum lembrete antecipado selecionado."
        )
    } else {
        tx(idioma, "Recordatorios seleccionados: ", "Selected reminders: ", "Lembretes selecionados: ") +
                partes.joinToString(", ") + "."
    }
}