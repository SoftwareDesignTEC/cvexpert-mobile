package com.cvpro.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput


private val BrandBlue = Color(0xFF1565F5)
private val BrandBlueLight = Color(0xFF4DA3FF)
private val BrandGreen = Color(0xFF00D9A0)

// Curvas: "Smooth" frena suave al final; "Travel" acelera y frena.
private val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val Travel = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/**
 * Splash de CVExpert.
 *
 * 1. El icono (documento + check/flecha) se dibuja trazo a trazo.
 * 2. Aparece "CVEx".
 * 3. "Ex" da una voltereta en arco hasta la segunda línea.
 * 4. De "Ex" crece "pert" -> CV / Expert.
 * 5. Fade out y onFinished().
 *
 * Duración total aprox. 4 s.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    canSkip: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Icono
    val outline = remember { Animatable(0f) }
    val lines = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.85f) }

    // Texto
    val wordAlpha = remember { Animatable(0f) }
    val exTravel = remember { Animatable(0f) }
    val pertReveal = remember { Animatable(0f) }

    // Salida
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(200)

        // 1) Icono: todo en paralelo pero escalonado
        coroutineScope {
            launch {
                iconScale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
            launch { outline.animateTo(1f, tween(700, easing = Smooth)) }
            launch {
                delay(350)
                lines.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
            }
            launch {
                delay(800)
                check.animateTo(1f, tween(700, easing = Smooth))
            }
        }

        // 2) Aparece "CVEx"
        wordAlpha.animateTo(1f, tween(500, easing = Smooth))
        delay(450)

        // 3) "Ex" viaja hasta la segunda línea
        exTravel.animateTo(1f, tween(900, easing = Travel))

        // 4) "pert" nace desde "Ex"
        pertReveal.animateTo(1f, tween(600, easing = Smooth))

        // 5) Pausa para ver el logo completo y salida
        delay(1000)
        splashAlpha.animateTo(0f, tween(350))
        currentOnFinished()
    }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val wordStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.ExtraBold,
    )

    // Medimos en vez de usar números "mágicos": así funciona en cualquier pantalla/fuente.
    val cvWidthPx = remember(wordStyle, density) {
        textMeasurer.measure("CV", wordStyle).size.width.toFloat()
    }
    val lineHeightPx = remember(wordStyle, density) {
        textMeasurer.measure("CV", wordStyle).size.height.toFloat()
    }
    val exWidthDp = remember(wordStyle, density) {
        with(density) { textMeasurer.measure("Ex", wordStyle).size.width.toDp() }
    }
    val slideUpPx = with(density) { 20.dp.toPx() }
    val pertSlidePx = with(density) { 14.dp.toPx() }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canSkip) {
                    detectTapGestures {
                        if (canSkip) {
                            currentOnFinished()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = splashAlpha.value },
            ) {
                CvExLogoIcon(
                    outline = { outline.value },
                    lines = { lines.value },
                    check = { check.value },
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        },
                )

                Spacer(Modifier.height(24.dp))

                // Wordmark
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = wordAlpha.value
                        translationY = (1f - wordAlpha.value) * slideUpPx
                    },
                ) {
                    Row {
                        Text(
                            text = "CV",
                            style = wordStyle,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Ex",
                            style = wordStyle,
                            maxLines = 1,
                            softWrap = false,
                            color = BrandGreen,
                            modifier = Modifier.graphicsLayer {
                                val p = exTravel.value
                                val arc = sin(PI * p).toFloat() // 0 -> 1 -> 0

                                // Baja y a la izquierda, levantándose un poco en el camino (arco)
                                translationX = -cvWidthPx * p
                                translationY = lineHeightPx * p - arc * lineHeightPx * 0.35f

                                // Voltereta completa que termina derecha
                                rotationZ = -360f * p

                                // Pequeño "pop" a mitad de camino
                                val s = 1f + 0.18f * arc
                                scaleX = s
                                scaleY = s
                            },
                        )
                    }

                    // Segunda línea: espacio para "Ex" (que aterriza ahí) + "pert"
                    Row {
                        Spacer(Modifier.width(exWidthDp))
                        Text(
                            text = "pert",
                            style = wordStyle,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = pertReveal.value
                                    translationX = -pertSlidePx * (1f - pertReveal.value)
                                }
                                .drawWithContent {
                                    // Revelado de izquierda a derecha
                                    clipRect(right = size.width * pertReveal.value) {
                                        this@drawWithContent.drawContent()
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icono dibujado con Canvas (documento + check que termina en flecha).
 * Trabaja en un espacio de 100x100 y se escala al tamaño del Composable.
 * Los parámetros son lambdas para leer el estado solo al dibujar
 * (no provoca recomposición en cada frame).
 */
@Composable
private fun CvExLogoIcon(
    outline: () -> Float,
    lines: () -> Float,
    check: () -> Float,
    modifier: Modifier = Modifier,
) {
    // Cuerpo del documento: forma de "C" con esquina superior derecha cortada
    val body = remember {
        Path().apply {
            moveTo(40f, 84f)
            lineTo(30f, 84f)
            quadraticBezierTo(18f, 84f, 18f, 72f)
            lineTo(18f, 22f)
            quadraticBezierTo(18f, 10f, 30f, 10f)
            lineTo(60f, 10f)
            lineTo(78f, 28f)
            lineTo(78f, 42f)
        }
    }
    // Dobladillo de la esquina
    val flap = remember {
        Path().apply {
            moveTo(60f, 10f)
            lineTo(60f, 28f)
            lineTo(78f, 28f)
            close()
        }
    }
    // Check
    val checkPath = remember {
        Path().apply {
            moveTo(38f, 66f)
            lineTo(54f, 82f)
            lineTo(84f, 44f)
        }
    }
    // Punta de flecha
    val arrowHead = remember {
        Path().apply {
            moveTo(92f, 34f)
            lineTo(89f, 56f)
            lineTo(71f, 42f)
            close()
        }
    }

    val measure = remember { PathMeasure() }
    val scratch = remember { Path() }
    val blueBrush = remember {
        Brush.linearGradient(
            colors = listOf(BrandBlue, BrandBlueLight),
            start = Offset(0f, 0f),
            end = Offset(100f, 100f),
        )
    }
    val greenBrush = remember { SolidColor(BrandGreen) }

    // Líneas de texto del documento: (inicio, fin)
    val textLines = remember {
        listOf(
            Offset(30f, 30f) to Offset(54f, 30f),
            Offset(30f, 44f) to Offset(60f, 44f),
            Offset(30f, 58f) to Offset(46f, 58f),
        )
    }

    Canvas(modifier = modifier) {
        scale(
            scaleX = size.width / 100f,
            scaleY = size.height / 100f,
            pivot = Offset.Zero,
        ) {
            val o = outline()
            val l = lines()
            val c = check()

            // Documento
            drawTrimmed(body, measure, scratch, o, blueBrush, 7f)
            val flapAlpha = ((o - 0.8f) / 0.2f).coerceIn(0f, 1f)
            if (flapAlpha > 0f) {
                drawPath(flap, BrandBlueLight, alpha = flapAlpha)
            }

            // Líneas (cada una arranca un poco después de la anterior)
            textLines.forEachIndexed { i, (from, to) ->
                val p = (l * textLines.size - i).coerceIn(0f, 1f)
                if (p > 0f) {
                    drawLine(
                        color = BrandBlueLight,
                        start = from,
                        end = lerp(from, to, p),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Check y punta de flecha
            drawTrimmed(checkPath, measure, scratch, c, greenBrush, 9f)
            val headScale = ((c - 0.8f) / 0.2f).coerceIn(0f, 1f)
            if (headScale > 0f) {
                scale(headScale, pivot = Offset(82f, 46f)) {
                    drawPath(arrowHead, BrandGreen)
                    drawPath(
                        arrowHead,
                        BrandGreen,
                        style = Stroke(width = 3f, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

/** Dibuja solo la fracción [progress] del trazo, como si se estuviera dibujando a mano. */
private fun DrawScope.drawTrimmed(
    path: Path,
    measure: PathMeasure,
    scratch: Path,
    progress: Float,
    brush: Brush,
    strokeWidth: Float,
) {
    if (progress <= 0f) return
    measure.setPath(path, false)
    scratch.rewind()
    measure.getSegment(0f, measure.length * progress, scratch, true)
    drawPath(
        path = scratch,
        brush = brush,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}