package com.example.repsgrams.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.example.repsgrams.ui.theme.HairlineDark
import com.example.repsgrams.ui.theme.HairlineLight
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.InsetDark
import com.example.repsgrams.ui.theme.InsetLight
import com.example.repsgrams.ui.theme.LabelDark
import com.example.repsgrams.ui.theme.LabelLight
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.PaperDark
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed
import com.example.repsgrams.ui.theme.doneGreen
import com.example.repsgrams.ui.theme.onDoneGreen
import java.util.Locale

enum class TileTone { Ink, Paper }

enum class TileShape { Squircle, Circle }

private val PillShape = RoundedCornerShape(50)
private val SquircleShape = RoundedCornerShape(24.dp)

val LocalTileMuted = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun tileMuted(): Color {
    val local = LocalTileMuted.current
    return if (local == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else local
}

@Composable
fun BackChevron(onClick: () -> Unit, contentDescription: String = "Back") {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = contentDescription)
    }
}

@Composable
fun BoardTile(
    modifier: Modifier = Modifier,
    tone: TileTone = TileTone.Paper,
    shape: TileShape = TileShape.Squircle,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalDarkTheme.current
    val fill = when (tone) {
        TileTone.Ink -> Ink
        TileTone.Paper -> if (dark) PaperDark else PaperLight
    }
    val contentColor = if (tone == TileTone.Ink || dark) PaperLight else Ink
    val muted = when {
        tone == TileTone.Ink -> PaperLight.copy(alpha = 0.72f)
        dark -> LabelDark
        else -> LabelLight
    }
    val tileShape = if (shape == TileShape.Circle) CircleShape else SquircleShape
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shell = if (dark) {
        Modifier.border(1.dp, HairlineDark, tileShape)
    } else {
        Modifier.shadow(
            elevation = 2.dp,
            shape = tileShape,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.10f),
        )
    }
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTileMuted provides muted,
    ) {
        Column(
            modifier = modifier
                .alpha(if (onClick != null && pressed) 0.88f else 1f)
                .then(shell)
                .clip(tileShape)
                .background(fill)
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    },
                ),
            content = content,
        )
    }
}

@Composable
fun InkPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onInk: Boolean = false,
) {
    val inverted = LocalDarkTheme.current || onInk
    val fill = if (inverted) PaperLight else Ink
    val content = if (inverted) Ink else PaperLight
    PillButton(text, onClick, modifier, enabled, fill, content, hairline = null)
}

@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = LocalDarkTheme.current
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fill = if (dark) PaperDark else PaperLight,
        content = if (dark) PaperLight else Ink,
        hairline = if (dark) HairlineDark else HairlineLight,
    )
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fill: Color,
    content: Color,
    hairline: Color?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .height(48.dp)
            .alpha(if (!enabled) 0.38f else if (pressed) 0.82f else 1f)
            .then(if (hairline == null) Modifier else Modifier.border(1.dp, hairline, PillShape))
            .clip(PillShape)
            .background(fill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    color: Color? = null,
    contentDescription: String? = null,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Text(
            text,
            modifier = if (contentDescription == null) {
                Modifier
            } else {
                Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
            },
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                destructive -> SignalRed
                color != null -> color
                else -> LocalContentColor.current
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = "Cancel",
    message: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        BoardTile(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = tileMuted())
                }
                content?.invoke(this)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextAction(dismissText, onDismiss)
                    TextAction(confirmText, onConfirm, enabled = confirmEnabled, destructive = destructive)
                }
            }
        }
    }
}

@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(Locale.US),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: tileMuted(),
    )
}

@Composable
fun MonoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val fill = when {
        selected && dark -> PaperLight
        selected -> Ink
        dark -> PaperDark
        else -> PaperLight
    }
    val content = when {
        selected && dark -> Ink
        selected -> PaperLight
        dark -> PaperLight
        else -> Ink
    }
    val hairline = if (selected) null else if (dark) HairlineDark else HairlineLight
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (pressed) 0.82f else 1f)
            .then(if (hairline == null) Modifier else Modifier.border(1.dp, hairline, PillShape))
            .clip(PillShape)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = content, maxLines = 1)
    }
}

@Composable
fun StatusDot(live: Boolean, modifier: Modifier = Modifier) {
    val color = if (live) SignalRed else if (LocalDarkTheme.current) HairlineDark else HairlineLight
    Box(modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
fun SetCompleteCircle(
    complete: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val green = doneGreen()
    val hairline = if (LocalDarkTheme.current) HairlineDark else HairlineLight
    Box(
        modifier
            .size(48.dp)
            .border(1.dp, if (complete) green else hairline, CircleShape)
            .clip(CircleShape)
            .background(if (complete) green else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (complete) {
            Icon(Icons.Outlined.Check, contentDescription = "Complete", tint = onDoneGreen())
        }
    }
}

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTravel",
    )
    val dark = LocalDarkTheme.current
    val ground = if (dark) InsetDark else InsetLight
    val band = if (dark) PaperDarkBand else PaperLight
    return this.drawBehind {
        drawRect(ground)
        val width = size.width * 0.28f
        val x = (size.width + width) * travel - width
        drawRect(color = band.copy(alpha = 0.55f), topLeft = Offset(x, 0f), size = Size(width, size.height))
    }
}

private val PaperDarkBand = Color(0xFF2A2A2A)
