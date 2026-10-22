package com.example.estudiantetracker

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek

/** Recuerda si el usuario ya vio el tour de bienvenida. */
object Tour {
    private const val ARCHIVO = "tour"
    private const val CLAVE = "visto"

    fun visto(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean(CLAVE, false)

    fun marcarVisto(context: Context) {
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit().putBoolean(CLAVE, true).apply()
    }
}

private class PasoTour(
    val etiqueta: String,
    val titulo: String,
    val descripcion: String,
    val consejo: String,
    val boton: String
)

private const val TOTAL_PASOS = 7

@Composable
private fun pasosDelTour(): List<PasoTour> = listOf(
    PasoTour(
        tx("TU ESPACIO ACADÉMICO", "YOUR ACADEMIC SPACE", "SEU ESPAÇO ACADÊMICO"),
        tx("Todo tu semestre, en un solo lugar", "Your whole semester, in one place", "Todo o seu semestre, em um só lugar"),
        tx(
            "En este tour aprenderás a organizar materias, priorizar actividades y guardar tus archivos importantes.",
            "In this tour you will learn how to organize subjects, prioritize activities and save your important files.",
            "Neste tour você aprenderá a organizar matérias, priorizar atividades e guardar seus arquivos importantes."
        ),
        tx("Solo tomará 2 minutos", "It will only take 2 minutes", "Levará apenas 2 minutos"),
        tx("Comenzar tour", "Start tour", "Começar tour")
    ),
    PasoTour(
        tx("MATERIAS", "SUBJECTS", "MATÉRIAS"),
        tx("Organiza cada materia a tu manera", "Organize each subject your way", "Organize cada matéria do seu jeito"),
        tx(
            "Crea tus materias desde Ajustes, elige un color para cada una y asígnalas a tus tareas y exámenes. Luego podrás filtrar la lista por materia.",
            "Create your subjects from Settings, pick a color for each one and assign them to your tasks and exams. Then you can filter the list by subject.",
            "Crie suas matérias em Configurações, escolha uma cor para cada uma e atribua-as às suas tarefas e provas. Depois você poderá filtrar a lista por matéria."
        ),
        tx(
            "Puedes eliminar una materia cuando quieras desde Ajustes",
            "You can delete a subject at any time from Settings",
            "Você pode excluir uma matéria quando quiser em Configurações"
        ),
        tx("Siguiente", "Next", "Próximo")
    ),
    PasoTour(
        tx("AJUSTES GENERALES", "GENERAL SETTINGS", "CONFIGURAÇÕES GERAIS"),
        tx("Haz que tu espacio se sienta tuyo", "Make your space feel like yours", "Faça o seu espaço ser a sua cara"),
        tx(
            "Cambia el tema entre claro y oscuro, el inicio de semana, el idioma y los recordatorios desde Ajustes.",
            "Switch between light and dark theme, the start of the week, the language and the reminders from Settings.",
            "Alterne entre o tema claro e escuro, o início da semana, o idioma e os lembretes em Configurações."
        ),
        tx("Los cambios se aplican al instante", "Changes apply instantly", "As mudanças se aplicam na hora"),
        tx("Siguiente", "Next", "Próximo")
    ),
    PasoTour(
        tx("WIDGETS", "WIDGETS", "WIDGETS"),
        tx("Lo importante, siempre a la vista", "What matters, always in sight", "O que importa, sempre à vista"),
        tx(
            "Añade widgets a tu pantalla de inicio: Hoy, Semana, Lista semanal y Mensual muestran tus pendientes sin abrir la app.",
            "Add widgets to your home screen: Today, Week, Weekly list and Monthly show your pending items without opening the app.",
            "Adicione widgets à tela inicial: Hoje, Semana, Lista semanal e Mensal mostram seus pendentes sem abrir o app."
        ),
        tx(
            "Mantén presionada la pantalla de inicio y elige Widgets",
            "Long-press the home screen and choose Widgets",
            "Pressione e segure a tela inicial e escolha Widgets"
        ),
        tx("Siguiente", "Next", "Próximo")
    ),
    PasoTour(
        tx("PRIORIDADES", "PRIORITIES", "PRIORIDADES"),
        tx("Decide qué hacer primero", "Decide what to do first", "Decida o que fazer primeiro"),
        tx(
            "Cada actividad tiene tres niveles de prioridad: alta, media y baja. Úsalos para ver tu semana de un vistazo y enfocarte sin sentirte abrumado.",
            "Each activity has three priority levels: high, medium and low. Use them to see your week at a glance and focus without feeling overwhelmed.",
            "Cada atividade tem três níveis de prioridade: alta, média e baixa. Use-os para ver sua semana de relance e focar sem se sentir sobrecarregado."
        ),
        tx(
            "La prioridad se puede cambiar en cualquier momento",
            "Priority can be changed at any time",
            "A prioridade pode ser alterada a qualquer momento"
        ),
        tx("Siguiente", "Next", "Próximo")
    ),
    PasoTour(
        tx("DOCUMENTOS", "DOCUMENTS", "DOCUMENTOS"),
        tx("Tus archivos, justo donde los necesitas", "Your files, right where you need them", "Seus arquivos, bem onde você precisa"),
        tx(
            "Adjunta apuntes, guías o entregas dentro de cada actividad. Quedarán guardados para consultarlos después.",
            "Attach notes, guides or assignments inside each activity. They stay saved so you can check them later.",
            "Anexe anotações, guias ou entregas dentro de cada atividade. Elas ficarão salvas para consultar depois."
        ),
        tx("Listo: ya conoces lo esencial", "Done: you now know the essentials", "Pronto: você já conhece o essencial"),
        tx("Empezar ahora", "Start now", "Começar agora")
    ),
    PasoTour(
        tx("ESTE PROYECTO", "THIS PROJECT", "ESTE PROJETO"),
        tx("Estudi sigue en desarrollo", "Estudi is still in development", "O Estudi ainda está em desenvolvimento"),
        tx(
            "Se agradece tu instalación y cualquier comentario que quieras enviar directamente al creador. Es un proyecto sin fines de lucro, gratuito y sin anuncios.",
            "Thank you for installing it. Any feedback sent directly to the creator is appreciated. It is a non-profit project, free and ad-free.",
            "Agradecemos a sua instalação e qualquer comentário enviado diretamente ao criador. É um projeto sem fins lucrativos, gratuito e sem anúncios."
        ),
        tx("Hecho por y para estudiantes ♥", "Made by and for students ♥", "Feito por e para estudantes ♥"),
        tx("Empezar a usar la app", "Start using the app", "Começar a usar o app")
    )
)

@Composable
fun TourScreen(state: AppState, onTerminar: () -> Unit) {
    val context = LocalContext.current
    var paso by rememberSaveable { mutableStateOf(0) }
    val pasos = pasosDelTour()
    val actual = pasos[paso]
    val esUltimo = paso == TOTAL_PASOS - 1

    BackHandler(enabled = paso > 0) { paso-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${paso + 1} " + tx("de", "of", "de") + " $TOTAL_PASOS",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (esUltimo) {
                Text(
                    tx("Último paso", "Last step", "Último passo"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    tx("Omitir", "Skip", "Pular"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onTerminar() }.padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(TOTAL_PASOS) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i <= paso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (paso) {
                    0 -> IlustracionBienvenida()
                    1 -> IlustracionMaterias()
                    2 -> IlustracionTema()
                    3 -> IlustracionWidgets()
                    4 -> IlustracionPrioridades()
                    5 -> IlustracionDocumentos()
                    else -> IlustracionGracias()
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                actual.etiqueta,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(actual.titulo, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                actual.descripcion,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (paso == 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    tx("Idioma de preferencia", "Preferred language", "Idioma de preferência"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                val codigos = IdiomaApp.soportados.map { it.first }
                Segmentado(
                    opciones = IdiomaApp.soportados.map { it.second },
                    seleccion = codigos.indexOf(IdiomaApp.codigo).coerceAtLeast(0),
                    onSeleccion = { i -> IdiomaApp.cambiar(context, codigos[i]) }
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    tx("¿Cómo quieres que te llamemos?", "What should we call you?", "Como quer ser chamado?"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                var nombreInput by rememberSaveable { mutableStateOf(state.prefs.nombreUsuario) }
                OutlinedTextField(
                    value = nombreInput,
                    onValueChange = { n ->
                        nombreInput = n
                        state.actualizarPrefs { it.copy(nombreUsuario = n) }
                    },
                    placeholder = { Text(tx("Tu nombre o apodo", "Your name or nickname", "Seu nome ou apelido")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (paso == 2) {
                Spacer(Modifier.height(16.dp))
                Text(
                    tx("Elige tu modo de apariencia", "Choose your theme mode", "Escolha seu modo de aparência"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Segmentado(
                    opciones = listOf(
                        tx("Claro", "Light", "Claro"),
                        tx("Oscuro", "Dark", "Escuro"),
                        tx("Amoled", "Amoled", "Amoled")
                    ),
                    seleccion = state.prefs.tema.coerceIn(0, 2),
                    onSeleccion = { i -> state.actualizarPrefs { it.copy(tema = i) } }
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    tx("Elige tu color de énfasis", "Choose your accent color", "Escolha sua cor de destaque"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listaColoresAcento.forEachIndexed { i, argb ->
                        val activo = state.prefs.colorAcento == i
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
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    actual.consejo,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { if (paso > 0) paso-- },
                enabled = paso > 0,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text(tx("Atrás", "Back", "Voltar")) }
            Button(
                onClick = { if (esUltimo) onTerminar() else paso++ },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(2f).height(52.dp)
            ) {
                Text(actual.boton, fontWeight = FontWeight.SemiBold)
                if (!esUltimo) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp).size(18.dp)
                    )
                }
            }
        }
    }
}

// ---------- Ilustraciones (dibujadas con componentes, sin imágenes) ----------

private val VerdeOk = Color(0xFF2E9E6B)

@Composable
private fun TarjetaBlanca(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun IlustracionBienvenida() {
    TarjetaBlanca {
        Text(
            tx("Tu semestre, más claro", "Your semester, clearer", "Seu semestre, mais claro"),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        listOf(
            tx("Organiza tus materias", "Organize your subjects", "Organize suas matérias"),
            tx("Prioriza tus actividades", "Prioritize your activities", "Priorize suas atividades"),
            tx("Adjunta tus archivos", "Attach your files", "Anexe seus arquivos")
        ).forEach { texto ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Text(texto, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                Icon(Icons.Default.Check, null, tint = VerdeOk, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun IlustracionMaterias() {
    val ejemplos = listOf(
        tx("Cálculo II", "Calculus II", "Cálculo II"),
        tx("Biología", "Biology", "Biologia"),
        tx("Historia moderna", "Modern history", "História moderna")
    )
    TarjetaBlanca {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tx("Mis materias", "My subjects", "Minhas matérias"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        ejemplos.forEachIndexed { i, nombre ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(coloresMateria[i % coloresMateria.size])))
                Text(nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun MiniTema(oscuro: Boolean, nombre: String, seleccionado: Boolean, modifier: Modifier) {
    val fondo = if (oscuro) Color(0xFF151622) else Color(0xFFF5F5FA)
    val tarjeta = if (oscuro) Color(0xFF303145) else Color.White
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                2.dp,
                if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .padding(10.dp)
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(fondo).padding(8.dp)) {
            Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(Color(0xFF5B5FEF)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(tarjeta))
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(tarjeta))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (seleccionado) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun IlustracionTema() {
    Column(Modifier.fillMaxWidth()) {
        Text(
            tx("Apariencia", "Appearance", "Aparência"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniTema(false, tx("Claro", "Light", "Claro"), true, Modifier.weight(1f))
            MiniTema(true, tx("Oscuro", "Dark", "Escuro"), false, Modifier.weight(1f))
        }
    }
}

@Composable
private fun IlustracionWidgets() {
    val idioma = IdiomaApp.codigo
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(14.dp)
            ) {
                Text(
                    tx("HOY", "TODAY", "HOJE"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text("3", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    tx("pendientes", "pending", "pendentes"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Text(
                    tx("SEMANA", "WEEK", "SEMANA"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.values().forEachIndexed { i, d ->
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (i == 2) MaterialTheme.colorScheme.primary else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                diaCorto(d, idioma).take(1),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (i == 2) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    tx("Añadir widget", "Add widget", "Adicionar widget"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    tx("Hoy · Semana · Lista semanal · Mensual", "Today · Week · Weekly list · Monthly", "Hoje · Semana · Lista semanal · Mensal"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IlustracionPrioridades() {
    val filas = listOf(
        2 to tx("Entregar ensayo final", "Submit final essay", "Entregar ensaio final"),
        1 to tx("Repasar integrales", "Review integrals", "Revisar integrais"),
        0 to tx("Ordenar apuntes", "Organize notes", "Organizar anotações")
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tx("Hoy", "Today", "Hoje"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                tx("3 actividades", "3 activities", "3 atividades"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        filas.forEach { (prioridad, titulo) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(4.dp).height(28.dp).clip(CircleShape).background(colorPrioridad(prioridad)))
                Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp).weight(1f))
                Pastilla(nombrePrioridad(prioridad), colorPrioridad(prioridad))
            }
        }
    }
}

@Composable
private fun IlustracionDocumentos() {
    val colorBorde = MaterialTheme.colorScheme.primary
    TarjetaBlanca {
        Text(
            tx("Informe de laboratorio", "Lab report", "Relatório de laboratório"),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = colorBorde,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    )
                }
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Add, null, tint = colorBorde, modifier = Modifier.size(22.dp))
            Text(
                tx("Subir un documento", "Upload a document", "Enviar um documento"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorBorde
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("guia-laboratorio.pdf", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Check, null, tint = VerdeOk, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = VerdeOk, modifier = Modifier.size(12.dp))
            Text(
                tx(
                    "Disponible cuando vuelvas a esta actividad",
                    "Available when you come back to this activity",
                    "Disponível quando você voltar a esta atividade"
                ),
                fontSize = 11.sp,
                color = VerdeOk,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun IlustracionGracias() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            tx("¡Gracias por instalar!", "Thanks for installing!", "Obrigado por instalar!"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Icons.Default.Star to tx("Siempre gratis", "Always free", "Sempre grátis"),
                Icons.Default.Favorite to tx("Sin fines de lucro", "Non-profit", "Sem fins lucrativos"),
                Icons.Default.Close to tx("Sin anuncios", "No ads", "Sem anúncios")
            ).forEach { (icono, texto) ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(texto, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}