package com.example.repsgrams.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.animation.core.EaseOutCubic
import com.example.repsgrams.ui.theme.HairlineDark
import com.example.repsgrams.ui.theme.HairlineLight
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.InkTile
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.PaperDark
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed

enum class TileTone { Ink, Paper }

enum class TileShape { Squircle, Circle }

private val PillShape = RoundedCornerShape(50)
private val SquircleShape = RoundedCornerShape(24.dp)
private val PressSpec = tween<Float>(durationMillis = 180, easing = EaseOutCubic)

@Composable
fun BackChevron(onClick: () -> Unit, contentDescription: String = "Back") {
    IconButton(onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardTile(
    modifier: Modifier = Modifier,
    tone: TileTone = TileTone.Paper,
    shape: TileShape = TileShape.Squircle,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalDarkTheme.current
    val tileShape = if (shape == TileShape.Circle) CircleShape else SquircleShape
    val background = when (tone) {
        TileTone.Ink -> InkTile
        TileTone.Paper -> if (dark) PaperDark else PaperLight
    }
    val hairline = if (tone == TileTone.Ink || dark) HairlineDark else HairlineLight
    val contentColor = if (tone == TileTone.Ink || dark) PaperLight else Ink
    val border = BorderStroke(1.dp, hairline)
    if (onClick == null) {
        Surface(modifier = modifier, shape = tileShape, color = background, contentColor = contentColor, border = border) {
            Column(content = content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = tileShape,
            color = background,
            contentColor = contentColor,
            border = border,
        ) {
            Column(content = content)
        }
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
    val label = if (inverted) Ink else PaperLight
    PillButton(text, onClick, modifier, enabled, fill, label, border = null)
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
        fill = Color.Transparent,
        label = if (dark) PaperLight else Ink,
        border = BorderStroke(1.dp, if (dark) HairlineDark else HairlineLight),
    )
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fill: Color,
    label: Color,
    border: BorderStroke?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (pressed && enabled) 0.55f else 1f, PressSpec, label = "pill")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = PillShape,
        border = border,
        contentPadding = PaddingValues(horizontal = 24.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = fill,
            contentColor = label,
            disabledContainerColor = fill.copy(alpha = 0.38f),
            disabledContentColor = label.copy(alpha = 0.38f),
        ),
        modifier = modifier.height(48.dp).alpha(alpha),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
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
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                destructive -> SignalRed
                else -> color ?: MaterialTheme.colorScheme.onSurface
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
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun MonoLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.US),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val fill = when {
        !selected -> Color.Transparent
        dark -> PaperLight
        else -> Ink
    }
    val label = when {
        !selected -> if (dark) PaperLight else Ink
        dark -> Ink
        else -> PaperLight
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = PillShape,
        color = fill,
        contentColor = label,
        border = if (selected) null else BorderStroke(1.dp, if (dark) HairlineDark else HairlineLight),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
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
    val dark = LocalDarkTheme.current
    val fill = if (complete) (if (dark) PaperLight else Ink) else Color.Transparent
    val mark = if (complete) (if (dark) Ink else PaperLight) else if (dark) PaperLight else Ink
    val hairline = if (dark) HairlineDark else HairlineLight
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, if (complete) fill else hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (complete) {
            Icon(Icons.Outlined.Check, contentDescription = "Complete", tint = mark)
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
    val band = if (LocalDarkTheme.current) HairlineDark else HairlineLight
    return this.drawBehind {
        val width = size.width * 0.28f
        val x = (size.width + width) * travel - width
        drawRect(color = band, topLeft = Offset(x, 0f), size = Size(width, size.height))
    }
}
