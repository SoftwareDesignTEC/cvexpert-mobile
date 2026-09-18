package com.cvpro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Una capa del gráfico: un conjunto de valores con su color y su nombre.
 *
 * @property values un valor de 0 a 100 por cada eje, en el mismo orden que las etiquetas.
 */
data class RadarSeries(
    val label: String,
    val values: List<Int>,
    val color: Color,
)

/**
 * Gráfico de radar, el que parece una telaraña.
 *
 * Cada punta es una habilidad y cada capa es un conjunto de valores: por ejemplo, tu nivel
 * contra el que piden las ofertas. Mientras más lejos del centro, más alto el valor.
 *
 * Está dibujado con Canvas, o sea a mano, para no agregar una librería de gráficos: se
 * calcula la posición de cada punta con seno y coseno y se unen con líneas.
 */
@Composable
fun RadarChart(
    labels: List<String>,
    series: List<RadarSeries>,
    modifier: Modifier = Modifier,
    rings: Int = 4,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            if (labels.isEmpty()) return@Canvas

            // Se deja margen para que las etiquetas no se salgan del dibujo.
            val radius = min(size.width, size.height) / 2f * 0.62f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawRadarGrid(center, radius, labels.size, rings, gridColor)
            series.forEach { serie -> drawRadarSeries(center, radius, serie) }
            drawRadarLabels(center, radius, labels, textMeasurer, labelStyle)
        }
    }
}

/** La telaraña: los anillos y las líneas que salen del centro a cada punta. */
private fun DrawScope.drawRadarGrid(
    center: Offset,
    radius: Float,
    axes: Int,
    rings: Int,
    color: Color,
) {
    repeat(rings) { ring ->
        val ringRadius = radius * (ring + 1) / rings
        val path = Path()
        repeat(axes) { index ->
            val point = radarPoint(center, ringRadius, index, axes)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path = path, color = color, style = Stroke(width = 1.dp.toPx()))
    }

    repeat(axes) { index ->
        drawLine(
            color = color,
            start = center,
            end = radarPoint(center, radius, index, axes),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/** Una capa de datos: el polígono relleno y su borde. */
private fun DrawScope.drawRadarSeries(center: Offset, radius: Float, series: RadarSeries) {
    if (series.values.isEmpty()) return

    val path = Path()
    series.values.forEachIndexed { index, value ->
        val distance = radius * (value.coerceIn(0, 100) / 100f)
        val point = radarPoint(center, distance, index, series.values.size)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()

    drawPath(path = path, color = series.color.copy(alpha = 0.25f))
    drawPath(path = path, color = series.color, style = Stroke(width = 2.dp.toPx()))
}

/** El nombre de cada habilidad, por fuera de la punta que le corresponde. */
private fun DrawScope.drawRadarLabels(
    center: Offset,
    radius: Float,
    labels: List<String>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    labels.forEachIndexed { index, label ->
        val angle = radarAngle(index, labels.size)
        val anchor = radarPoint(center, radius * 1.15f, index, labels.size)
        val measured = textMeasurer.measure(label, style)

        // La etiqueta se alinea según dónde está su punta: las de la derecha arrancan en la
        // punta, las de la izquierda terminan en ella y las de arriba y abajo van centradas.
        // Sin esto, las dos puntas de abajo se encimaban.
        val horizontalFactor = 0.5f - 0.5f * cos(angle).toFloat()
        val verticalFactor = 0.5f - 0.5f * sin(angle).toFloat()

        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = anchor.x - measured.size.width * horizontalFactor,
                y = anchor.y - measured.size.height * verticalFactor,
            ),
        )
    }
}

/**
 * Posición de una punta.
 *
 * Se empieza arriba (por eso el -90°) y se reparten las puntas en partes iguales alrededor
 * del círculo.
 */
private fun radarPoint(center: Offset, radius: Float, index: Int, total: Int): Offset {
    val angle = radarAngle(index, total)
    return Offset(
        x = center.x + (radius * cos(angle)).toFloat(),
        y = center.y + (radius * sin(angle)).toFloat(),
    )
}

/** Ángulo de una punta, empezando arriba y girando en sentido horario. */
private fun radarAngle(index: Int, total: Int): Double = (-PI / 2) + (2 * PI * index / total)

/** Leyenda: un punto de color por capa, con su nombre. */
@Composable
fun RadarLegend(series: List<RadarSeries>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        series.forEachIndexed { index, serie ->
            if (index > 0) Spacer(modifier = Modifier.width(16.dp))
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = serie.color)
            }
            Text(
                text = serie.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
