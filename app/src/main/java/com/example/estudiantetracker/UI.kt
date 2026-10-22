package com.example.estudiantetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

// ---------- Tema (claro / oscuro) ----------

val listaColoresAcento = listOf(
    0xFF5B5FEF, // Violeta
    0xFF10B981, // Esmeralda
    0xFF3B82F6, // Azul Océano
    0xFFEC4899, // Rosa
    0xFF14B8A6  // Menta
)

val listaColoresAcentoOscuro = listOf(
    0xFF9294FF, // Violeta claro
    0xFF34D399, // Esmeralda suave
    0xFF60A5FA, // Azul claro
    0xFFF472B6, // Rosa pastel
    0xFF2DD4BF  // Menta claro
)

@Composable
fun AppTheme(tema: Int, colorAcentoIndex: Int = 0, content: @Composable () -> Unit) {
    val idx = colorAcentoIndex.coerceIn(0, listaColoresAcento.size - 1)
    val colorAcentoClaro = Color(listaColoresAcento[idx])
    val colorAcentoOscuro = Color(listaColoresAcentoOscuro[idx])

    val esquema = when (tema) {
        1 -> darkColorScheme(
            primary = colorAcentoOscuro,
            onPrimary = Color(0xFF151622),
            background = Color(0xFF151622),
            surface = Color(0xFF232437),
            onBackground = Color(0xFFF6F5FF),
            onSurface = Color(0xFFF6F5FF),
            onSurfaceVariant = Color(0xFFC2C1D4),
            surfaceVariant = Color(0xFF303145),
            outline = Color(0xFF424359)
        )
        2 -> darkColorScheme( // Amoled (Negro puro #000000)
            primary = colorAcentoOscuro,
            onPrimary = Color(0xFF000000),
            background = Color(0xFF000000),
            surface = Color(0xFF121214),
            onBackground = Color(0xFFF4F4F6),
            onSurface = Color(0xFFF4F4F6),
            onSurfaceVariant = Color(0xFFA0A0AB),
            surfaceVariant = Color(0xFF1B1B1F),
            outline = Color(0xFF2C2C33)
        )
        else -> lightColorScheme( // 0: Claro
            primary = colorAcentoClaro,
            onPrimary = Color.White,
            background = Color(0xFFF5F5FA),
            surface = Color.White,
            onBackground = Color(0xFF24243A),
            onSurface = Color(0xFF24243A),
            onSurfaceVariant = Color(0xFF626279),
            surfaceVariant = Color(0xFFEBEBF5),
            outline = Color(0xFFD9D9E5)
        )
    }
    MaterialTheme(colorScheme = esquema, content = content)
}

// ---------- Utilidades ----------

val regexHora = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

val coloresMateria = listOf(0xFF5B5FEF, 0xFF247B72, 0xFFB36945, 0xFF855D9E, 0xFFA15B73, 0xFF536F9A)

fun formatoHora(t: LocalTime): String =
    t.hour.toString().padStart(2, '0') + ":" + t.minute.toString().padStart(2, '0')

fun colorPrioridad(p: Int): Color = when (p) {
    2 -> Color(0xFFA83D50)
    1 -> Color(0xFF805C12)
    else -> Color(0xFF256E55)
}

// ---------- Componentes ----------

@Composable
fun Segmentado(opciones: List<String>, seleccion: Int, onSeleccion: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        opciones.forEachIndexed { i, texto ->
            val activo = i == seleccion
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activo) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSeleccion(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    texto,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Pastilla(texto: String, color: Color, conPunto: Boolean = false, colorTexto: Color = color) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (conPunto) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, color = colorTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CampoBox(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)

@Composable
fun EtiquetaCampo(texto: String) {
    Text(
        texto,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun EncabezadoSeccion(texto: String) {
    Row(Modifier.padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(12.dp))
        Text(
            texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TarjetaAjustes(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun CabeceraPantalla(titulo: String, icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClick) { Icon(icono, contentDescription = descripcion) }
        Text(
            titulo,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(48.dp))
    }
}