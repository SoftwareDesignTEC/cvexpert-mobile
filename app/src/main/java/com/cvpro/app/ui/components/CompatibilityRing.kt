package com.cvpro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cvpro.app.R

/**
 * Anillo de compatibilidad de una oferta.
 *
 * Es el círculo que en el diseño rodea el porcentaje. Está dibujado con Canvas, o sea a
 * mano, para no agregar una librería de gráficos: es un arco sobre otro arco.
 *
 * @param percentage qué tanto encaja la oferta con el perfil, de 0 a 100.
 */
@Composable
fun CompatibilityRing(
    percentage: Int,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val safePercentage = percentage.coerceIn(0, 100)

    Box(
        modifier = modifier.size(RING_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(RING_SIZE)) {
            val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
            // El arco se dibuja dentro del borde: si no, el trazo se corta por la mitad.
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            // -90 grados arranca arriba, como en el diseño, y no a la derecha.
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * safePercentage / 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        // En una línea y sin ajuste: con 100% el texto no debe partirse dentro del anillo.
        Text(
            text = stringResource(R.string.jobs_match_percentage, safePercentage),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private val RING_SIZE = 64.dp
private val STROKE_WIDTH = 5.dp
