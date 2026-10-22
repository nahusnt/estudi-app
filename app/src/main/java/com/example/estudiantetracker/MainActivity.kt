package com.example.estudiantetracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El idioma se carga antes de dibujar la primera pantalla.
        IdiomaApp.cargar(applicationContext)

        setContent {
            val contexto = LocalContext.current
            val app = contexto.applicationContext
            val estado = remember { AppState(app, Repository(app)) }
            val tema = estado.prefs.tema

            // Ajusta los íconos de la barra de estado según el tema elegido en la app.
            DisposableEffect(tema) {
                val transparente = Color.TRANSPARENT
                val esOscuro = tema != 0
                val estilo = if (esOscuro) SystemBarStyle.dark(transparente)
                else SystemBarStyle.light(transparente, transparente)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }

            // Si el usuario rechaza el permiso, el interruptor de notificaciones vuelve a apagarse.
            val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
                if (!concedido) {
                    estado.actualizarPrefs { it.copy(notificaciones = false) }
                    Toast.makeText(
                        contexto,
                        tx(
                            "Sin el permiso de notificaciones, la app no puede enviar recordatorios.",
                            "Without the notification permission, the app cannot send reminders.",
                            "Sem a permissão de notificações, o app não pode enviar lembretes."
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Al activar las notificaciones en Ajustes, pide el permiso (necesario desde Android 13).
            LaunchedEffect(estado.prefs.notificaciones) {
                if (estado.prefs.notificaciones && Build.VERSION.SDK_INT >= 33 &&
                    contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Cada vez que cambian las actividades o los ajustes se recalculan las alarmas.
            // El trabajo se hace fuera del hilo principal para no frenar la pantalla.
            LaunchedEffect(estado.actividades, estado.prefs) {
                val actividades = estado.actividades
                val prefs = estado.prefs
                withContext(Dispatchers.Default) {
                    Recordatorios.reprogramar(app, actividades, prefs)
                }
            }

            // Los widgets se actualizan con los datos y también al cambiar el idioma.
            LaunchedEffect(estado.actividades, estado.prefs, IdiomaApp.codigo) {
                actualizarWidgets(app)
            }

            AppTheme(tema = estado.prefs.tema, colorAcentoIndex = estado.prefs.colorAcento) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var pantalla by rememberSaveable {
                        mutableStateOf(if (Tour.visto(contexto)) "inicio" else "tour")
                    }
                    var editandoId by rememberSaveable { mutableStateOf<String?>(null) }
                    var mostrarNovedades by rememberSaveable {
                        mutableStateOf(estado.prefs.ultimaVersionVista != "1.0.2" && Tour.visto(contexto))
                    }

                    if (mostrarNovedades) {
                        DialogoNotasParche(
                            onCerrar = {
                                mostrarNovedades = false
                                estado.actualizarPrefs { it.copy(ultimaVersionVista = "1.0.2") }
                            }
                        )
                    }

                    BackHandler(enabled = pantalla != "inicio" && pantalla != "tour") { pantalla = "inicio" }

                    when (pantalla) {
                        "tour" -> TourScreen(
                            state = estado,
                            onTerminar = {
                                Tour.marcarVisto(app)
                                estado.actualizarPrefs { it.copy(ultimaVersionVista = "1.0.2") }
                                pantalla = "inicio"
                            }
                        )
                        "formulario" -> NuevaActividadScreen(
                            state = estado,
                            actividadId = editandoId,
                            onCerrar = { pantalla = "inicio" }
                        )
                        "ajustes" -> AjustesScreen(
                            state = estado,
                            onVolver = { pantalla = "inicio" },
                            onVerTour = { pantalla = "tour" }
                        )
                        else -> MisDeberesScreen(
                            state = estado,
                            onNueva = { editandoId = null; pantalla = "formulario" },
                            onEditar = { id -> editandoId = id; pantalla = "formulario" },
                            onAjustes = { pantalla = "ajustes" }
                        )
                    }
                }
            }
        }
    }
}