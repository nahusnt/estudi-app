package com.example.estudiantetracker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.Duration
import java.time.ZoneId

/**
 * Programa una alarma del sistema por cada actividad y por cada aviso activado en Ajustes.
 * Tipos de aviso: 0 = 1 día antes, 1 = 3 horas antes, 2 = 30 minutos antes.
 */
object Recordatorios {
    private const val CANAL = "recordatorios"
    private const val ARCHIVO_ALARMAS = "alarmas"
    private const val CLAVE_ACTIVAS = "activas"
    const val EXTRA_ID = "actividad_id"
    const val EXTRA_TIPO = "tipo"

    private val anticipaciones = listOf(Duration.ofDays(1), Duration.ofHours(3), Duration.ofMinutes(30))

    /** Con `crear = false` solo devuelve la alarma si ya existe (sirve para cancelarla). */
    private fun pendingIntent(context: Context, actividadId: String, tipo: Int, crear: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_ID, actividadId)
            .putExtra(EXTRA_TIPO, tipo)
        val flags = PendingIntent.FLAG_IMMUTABLE or
                (if (crear) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, "$actividadId#$tipo".hashCode(), intent, flags)
    }

    /**
     * Recalcula las alarmas. Se llama cada vez que cambian las actividades o los ajustes.
     * Guarda cuáles quedaron programadas para poder cancelar después las que ya no corresponden
     * (actividad eliminada, completada o aviso desactivado), sin dejar alarmas sueltas.
     */
    @Synchronized
    fun reprogramar(context: Context, actividades: List<Actividad>, prefs: Preferencias) {
        val app = context.applicationContext
        val alarmas = app.getSystemService(AlarmManager::class.java)
        val sp = app.getSharedPreferences(ARCHIVO_ALARMAS, Context.MODE_PRIVATE)
        val anteriores: Set<String> = sp.getStringSet(CLAVE_ACTIVAS, emptySet()) ?: emptySet()
        val ahora = System.currentTimeMillis()
        val activos = booleanArrayOf(prefs.recordar1Dia, prefs.recordar3Horas, prefs.recordar30Min)
        val nuevas = HashSet<String>()

        if (prefs.notificaciones) {
            actividades.filter { !it.completada }.forEach { a ->
                val vence = a.vence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                anticipaciones.forEachIndexed { tipo, anticipacion ->
                    val disparo = vence - anticipacion.toMillis()
                    if (activos[tipo] && disparo > ahora) {
                        val pi = pendingIntent(app, a.id, tipo, crear = true)
                        if (pi != null) {
                            // Alarma aproximada: no requiere permisos especiales y puede demorar unos minutos.
                            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pi)
                            nuevas.add("${a.id}#$tipo")
                        }
                    }
                }
            }
        }

        (anteriores - nuevas).forEach { clave ->
            val id = clave.substringBeforeLast('#')
            val tipo = clave.substringAfterLast('#').toIntOrNull() ?: return@forEach
            pendingIntent(app, id, tipo, crear = false)?.let {
                alarmas.cancel(it)
                it.cancel()
            }
        }
        sp.edit().putStringSet(CLAVE_ACTIVAS, nuevas).apply()
    }

    fun mostrar(context: Context, a: Actividad, materia: String, tipo: Int) {
        val idioma = IdiomaApp.leer(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CANAL,
                    tx(idioma, "Recordatorios", "Reminders", "Lembretes"),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val hora = formatoHora(a.vence.toLocalTime())
        val cuando = when (tipo) {
            0 -> tx(idioma, "Mañana a las $hora", "Tomorrow at $hora", "Amanhã às $hora")
            1 -> tx(idioma, "En 3 horas ($hora)", "In 3 hours ($hora)", "Em 3 horas ($hora)")
            else -> tx(idioma, "En 30 minutos ($hora)", "In 30 minutes ($hora)", "Em 30 minutos ($hora)")
        }
        val clase = if (a.esExamen) tx(idioma, "Examen", "Exam", "Prova") else tx(idioma, "Tarea", "Task", "Tarefa")
        val abrirApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$clase: ${a.titulo}")
            .setContentText("$materia · $cuando")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        nm.notify("${a.id}#$tipo".hashCode(), notificacion)
    }
}

/** Se ejecuta cuando suena una alarma: verifica que el aviso siga vigente y lo muestra. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(Recordatorios.EXTRA_ID) ?: return
        val tipo = intent.getIntExtra(Recordatorios.EXTRA_TIPO, 0)

        val repo = Repository(context.applicationContext)
        val prefs = repo.loadPrefs()
        val actividad = repo.loadActividades().find { it.id == id } ?: return // fue eliminada
        if (!prefs.notificaciones || actividad.completada) return

        val vigente = when (tipo) {
            0 -> prefs.recordar1Dia
            1 -> prefs.recordar3Horas
            else -> prefs.recordar30Min
        }
        if (!vigente) return

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val materia = repo.loadMaterias().find { it.id == actividad.materiaId }?.nombre
            ?: tx(IdiomaApp.leer(context), "Sin materia", "No subject", "Sem matéria")
        Recordatorios.mostrar(context, actividad, materia, tipo)
    }
}

/** Android borra las alarmas al apagar el teléfono; este receptor las vuelve a programar al encenderlo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = Repository(context.applicationContext)
        Recordatorios.reprogramar(context, repo.loadActividades(), repo.loadPrefs())
    }
}