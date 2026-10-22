package com.example.estudiantetracker

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID

// ---------- Modelos ----------

data class Materia(val id: String, val nombre: String, val color: Long)

data class Adjunto(val uri: String, val nombre: String, val tipo: String)

data class Subtarea(val id: String, val titulo: String, val completada: Boolean = false)

data class Actividad(
    val id: String,
    val titulo: String,
    val esExamen: Boolean,
    val materiaId: String,
    val vence: LocalDateTime,
    val prioridad: Int, // 0 = Baja, 1 = Media, 2 = Alta
    val notas: String,
    val adjuntos: List<Adjunto>,
    val completadaEn: LocalDateTime?,
    val tipoHorario: Int = 0, // 0 = Hora específica, 1 = Todo el día
    val subtareas: List<Subtarea> = emptyList()
) {
    val completada: Boolean get() = completadaEn != null
}

data class Preferencias(
    val nombreUsuario: String = "",
    val oscuro: Boolean = false,
    val semanaEmpiezaDomingo: Boolean = false,
    val horaPredeterminada: String = "08:00",
    val notificaciones: Boolean = false,
    val recordar1Dia: Boolean = true,
    val recordar3Horas: Boolean = false,
    val recordar30Min: Boolean = false,
    val tema: Int = 0, // 0 = Claro, 1 = Oscuro, 2 = Amoled
    val colorAcento: Int = 0, // 0 = Violeta, 1 = Esmeralda, 2 = Azul, 3 = Rosa, 4 = Menta
    val ultimaVersionVista: String = ""
)

// ---------- Almacenamiento local (SharedPreferences + JSON) ----------

class Repository(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("estudiante_tracker", Context.MODE_PRIVATE)

    fun loadMaterias(): List<Materia> {
        val raw = sp.getString("materias", null)
            ?: return materiasIniciales().also { saveMaterias(it) }
        // Si el dato guardado estuviera dañado, la app abre igual en lugar de cerrarse.
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Materia(o.getString("id"), o.getString("nombre"), o.getLong("color"))
            }
        } catch (e: Exception) {
            materiasIniciales()
        }
    }

    fun saveMaterias(lista: List<Materia>) {
        val arr = JSONArray()
        lista.forEach {
            arr.put(JSONObject().put("id", it.id).put("nombre", it.nombre).put("color", it.color))
        }
        sp.edit().putString("materias", arr.toString()).apply()
    }

    fun loadActividades(): List<Actividad> {
        val raw = sp.getString("actividades", null)
            ?: return actividadesIniciales().also { saveActividades(it) }
        return try {
            val arr = JSONArray(raw)
            // Una actividad dañada se omite sin afectar a las demás.
            (0 until arr.length()).mapNotNull { i ->
                try {
                    leerActividad(arr.getJSONObject(i))
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun leerActividad(o: JSONObject): Actividad {
        val adj = o.optJSONArray("adjuntos") ?: JSONArray()
        val subArr = o.optJSONArray("subtareas") ?: JSONArray()
        return Actividad(
            id = o.getString("id"),
            titulo = o.getString("titulo"),
            esExamen = o.getBoolean("esExamen"),
            materiaId = o.getString("materiaId"),
            vence = LocalDateTime.parse(o.getString("vence")),
            prioridad = o.getInt("prioridad"),
            notas = o.optString("notas", ""),
            adjuntos = (0 until adj.length()).map { j ->
                val a = adj.getJSONObject(j)
                Adjunto(a.getString("uri"), a.optString("nombre", ""), a.optString("tipo", ""))
            },
            completadaEn = o.optString("completadaEn", "")
                .takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it) },
            tipoHorario = o.optInt("tipoHorario", 0),
            subtareas = (0 until subArr.length()).map { k ->
                val s = subArr.getJSONObject(k)
                Subtarea(s.getString("id"), s.getString("titulo"), s.optBoolean("completada", false))
            }
        )
    }

    fun saveActividades(lista: List<Actividad>) {
        val arr = JSONArray()
        lista.forEach { a ->
            val adj = JSONArray()
            a.adjuntos.forEach {
                adj.put(JSONObject().put("uri", it.uri).put("nombre", it.nombre).put("tipo", it.tipo))
            }
            val sub = JSONArray()
            a.subtareas.forEach { s ->
                sub.put(JSONObject().put("id", s.id).put("titulo", s.titulo).put("completada", s.completada))
            }
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("titulo", a.titulo)
                    .put("esExamen", a.esExamen)
                    .put("materiaId", a.materiaId)
                    .put("vence", a.vence.toString())
                    .put("prioridad", a.prioridad)
                    .put("notas", a.notas)
                    .put("adjuntos", adj)
                    .put("completadaEn", a.completadaEn?.toString() ?: "")
                    .put("tipoHorario", a.tipoHorario)
                    .put("subtareas", sub)
            )
        }
        sp.edit().putString("actividades", arr.toString()).apply()
    }

    fun loadPrefs(): Preferencias {
        val d = Preferencias()
        val oscuroAntiguo = sp.getBoolean("oscuro", d.oscuro)
        val temaGuardado = sp.getInt("tema", if (oscuroAntiguo) 1 else d.tema)
        return Preferencias(
            nombreUsuario = sp.getString("nombreUsuario", d.nombreUsuario) ?: d.nombreUsuario,
            oscuro = temaGuardado != 0,
            semanaEmpiezaDomingo = sp.getBoolean("semanaDomingo", d.semanaEmpiezaDomingo),
            horaPredeterminada = sp.getString("horaPredeterminada", d.horaPredeterminada) ?: d.horaPredeterminada,
            notificaciones = sp.getBoolean("notificaciones", d.notificaciones),
            recordar1Dia = sp.getBoolean("recordar1Dia", d.recordar1Dia),
            recordar3Horas = sp.getBoolean("recordar3Horas", d.recordar3Horas),
            recordar30Min = sp.getBoolean("recordar30Min", d.recordar30Min),
            tema = temaGuardado,
            colorAcento = sp.getInt("colorAcento", d.colorAcento),
            ultimaVersionVista = sp.getString("ultimaVersionVista", d.ultimaVersionVista) ?: d.ultimaVersionVista
        )
    }

    fun savePrefs(p: Preferencias) {
        sp.edit()
            .putString("nombreUsuario", p.nombreUsuario)
            .putBoolean("oscuro", p.tema != 0)
            .putBoolean("semanaDomingo", p.semanaEmpiezaDomingo)
            .putString("horaPredeterminada", p.horaPredeterminada)
            .putBoolean("notificaciones", p.notificaciones)
            .putBoolean("recordar1Dia", p.recordar1Dia)
            .putBoolean("recordar3Horas", p.recordar3Horas)
            .putBoolean("recordar30Min", p.recordar30Min)
            .putInt("tema", p.tema)
            .putInt("colorAcento", p.colorAcento)
            .putString("ultimaVersionVista", p.ultimaVersionVista)
            .apply()
    }

    // La app comienza vacía: sin materias ni actividades de ejemplo.
    private fun materiasIniciales(): List<Materia> = emptyList()

    private fun actividadesIniciales(): List<Actividad> = emptyList()
}

// ---------- Estado de la app (lo que las pantallas observan) ----------

class AppState(private val context: Context, private val repo: Repository) {
    var materias by mutableStateOf(repo.loadMaterias()); private set
    var actividades by mutableStateOf(repo.loadActividades()); private set
    var prefs by mutableStateOf(repo.loadPrefs()); private set

    private fun refrescarWidgets() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                actualizarWidgets(context)
            } catch (e: Exception) {}
        }
    }

    fun guardarActividad(a: Actividad) {
        actividades = actividades.filter { it.id != a.id } + a
        repo.saveActividades(actividades)
        refrescarWidgets()
    }

    fun eliminarActividad(id: String) {
        actividades = actividades.filter { it.id != id }
        repo.saveActividades(actividades)
        refrescarWidgets()
    }

    fun alternarCompletada(a: Actividad) {
        val todasSubCompletadas = a.subtareas.map { it.copy(completada = !a.completada) }
        guardarActividad(
            a.copy(
                completadaEn = if (a.completada) null else LocalDateTime.now(),
                subtareas = if (a.subtareas.isNotEmpty()) todasSubCompletadas else a.subtareas
            )
        )
    }

    fun alternarSubtarea(actividadId: String, subtareaId: String) {
        val act = actividades.find { it.id == actividadId } ?: return
        val nuevasSub = act.subtareas.map { s ->
            if (s.id == subtareaId) s.copy(completada = !s.completada) else s
        }
        val todasListas = nuevasSub.isNotEmpty() && nuevasSub.all { it.completada }
        val nuevoCompletadaEn = if (todasListas) (act.completadaEn ?: LocalDateTime.now())
        else if (nuevasSub.any { !it.completada } && act.completada) null
        else act.completadaEn

        guardarActividad(act.copy(subtareas = nuevasSub, completadaEn = nuevoCompletadaEn))
    }

    /** Elimina la materia junto con sus actividades (una actividad no puede quedar sin materia). */
    fun eliminarMateria(id: String) {
        materias = materias.filter { it.id != id }
        repo.saveMaterias(materias)
        actividades = actividades.filter { it.materiaId != id }
        repo.saveActividades(actividades)
        refrescarWidgets()
    }

    fun agregarMateria(nombre: String, color: Long) {
        materias = materias + Materia(UUID.randomUUID().toString(), nombre, color)
        repo.saveMaterias(materias)
        refrescarWidgets()
    }

    fun actualizarPrefs(cambio: (Preferencias) -> Preferencias) {
        prefs = cambio(prefs)
        repo.savePrefs(prefs)
        refrescarWidgets()
    }
}