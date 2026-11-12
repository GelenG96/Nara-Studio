package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.TtsPlaybackStatus
import com.example.ai.TtsUiState
import com.example.data.model.BuiltinWallpaper
import com.example.data.model.WallpaperAlignment
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperScaleMode
import com.example.data.model.WallpaperSourceType
import com.example.ui.theme.LocalNaraPanelStyle
import java.io.File

@Composable
fun WallpaperBackdrop(
    config: WallpaperConfig,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        bgColor,
                        bgColor.copy(alpha = 0.96f),
                        primary.copy(alpha = 0.04f).compositeOver(bgColor)
                    )
                )
            )
    ) {
        val contentScale = when (config.scaleMode) {
            WallpaperScaleMode.CROP -> ContentScale.Crop
            WallpaperScaleMode.FIT -> ContentScale.Fit
            WallpaperScaleMode.FILL -> ContentScale.FillBounds
        }
        val alignment = when (config.alignment) {
            WallpaperAlignment.CENTER -> Alignment.Center
            WallpaperAlignment.TOP -> Alignment.TopCenter
            WallpaperAlignment.BOTTOM -> Alignment.BottomCenter
        }
        val blurModifier = if (config.blurRadiusDp > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(config.blurRadiusDp.dp)
        } else {
            Modifier
        }

        val imageModifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .then(blurModifier)
            .graphicsLayer {
                scaleX = config.zoomScale
                scaleY = config.zoomScale
                translationX = size.width * config.offsetX
                translationY = size.height * config.offsetY
            }
            .alpha(config.opacity.coerceIn(0.05f, 1f))

        when (config.sourceType) {
            WallpaperSourceType.GALLERY -> {
                val file = File(config.galleryPath)
                if (config.galleryPath.isNotBlank() && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Wallpaper kustom aplikasi",
                        modifier = imageModifier,
                        contentScale = contentScale,
                        alignment = alignment
                    )
                }
            }
            WallpaperSourceType.BUILTIN -> {
                val resId = when (BuiltinWallpaper.fromId(config.builtinId)) {
                    BuiltinWallpaper.PAPER_DAWN -> R.drawable.img_wallpaper_paper_dawn
                    BuiltinWallpaper.MIDNIGHT_STUDY -> R.drawable.img_wallpaper_midnight_study
                    BuiltinWallpaper.BOTANICAL_INK -> R.drawable.img_wallpaper_botanical_ink
                }
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Wallpaper bawaan aplikasi",
                    modifier = imageModifier,
                    contentScale = contentScale,
                    alignment = alignment
                )
            }
            WallpaperSourceType.NONE -> {
                // Subtle ambient literary gradient mesh
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.08f),
                                    secondary.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Adaptive readability scrim so text, icons, and controls always stay legible
        val effectiveScrimAlpha = if (config.sourceType == WallpaperSourceType.NONE) {
            0f
        } else {
            val blurBoost = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && config.blurRadiusDp > 1f) {
                (config.blurRadiusDp / 48f).coerceAtMost(0.25f)
            } else 0f
            (config.overlayScrimAlpha + blurBoost).coerceIn(0f, 0.88f)
        }
        if (effectiveScrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = effectiveScrimAlpha))
            )
        }

        content()
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha
    return Color(
        red = this.red * a + background.red * (1f - a),
        green = this.green * a + background.green * (1f - a),
        blue = this.blue * a + background.blue * (1f - a),
        alpha = 1f
    )
}

@Composable
fun NaraPanelCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    tonalElevation: Dp = 2.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val panelStyle = LocalNaraPanelStyle.current
    val resolvedColor = containerColor.copy(alpha = panelStyle.panelOpacity.coerceIn(0.55f, 0.98f))
    val border = BorderStroke(
        width = 0.8.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = resolvedColor),
            elevation = CardDefaults.cardElevation(defaultElevation = tonalElevation),
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = resolvedColor),
            elevation = CardDefaults.cardElevation(defaultElevation = tonalElevation),
            border = border,
            content = content
        )
    }
}

@Composable
fun NaraTtsFloatingBar(
    ttsState: TtsUiState,
    onStop: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = ttsState.status != TtsPlaybackStatus.IDLE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (ttsState.status == TtsPlaybackStatus.ERROR) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
            },
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    when (ttsState.status) {
                        TtsPlaybackStatus.LOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        TtsPlaybackStatus.PLAYING -> {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "NARA membacakan",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        else -> Unit
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (ttsState.status) {
                            TtsPlaybackStatus.LOADING -> "Menyiapkan suara NARA..."
                            TtsPlaybackStatus.PLAYING -> "NARA membacakan: ${ttsState.activeLabel}"
                            TtsPlaybackStatus.ERROR -> ttsState.errorMessage ?: "Gagal memutar suara"
                            TtsPlaybackStatus.IDLE -> ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (ttsState.status == TtsPlaybackStatus.ERROR) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        if (ttsState.status == TtsPlaybackStatus.ERROR) onDismissError() else onStop()
                    },
                    modifier = Modifier.testTag("tts_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Hentikan suara",
                        tint = if (ttsState.status == TtsPlaybackStatus.ERROR) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }
    }
}
