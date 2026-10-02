package com.example.repsgrams.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.repsgrams.MainActivity
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.widget.WidgetFace
import com.example.repsgrams.ui.theme.CanvasDark
import com.example.repsgrams.ui.theme.CanvasLight
import com.example.repsgrams.ui.theme.DoneGreen
import com.example.repsgrams.ui.theme.DoneGreenOnDark
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.SignalRed

class RepsGramsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RepsGramsWidget()
}

internal class RepsGramsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = loadWidgetFace(context)
        runCatching { writeWidgetFace(context, id, loaded) }
        provideContent {
            val face = currentState<Preferences>().widgetFaceOrNull() ?: loaded
            WidgetContent(face)
        }
    }
}

private val widgetCanvas = ColorProvider(day = CanvasLight, night = CanvasDark)
private val widgetType = ColorProvider(day = Ink, night = Color.White)
private val widgetDone = ColorProvider(day = DoneGreen, night = DoneGreenOnDark)
private val widgetLive = ColorProvider(day = SignalRed, night = SignalRed)

private val statusStyle = TextStyle(
    color = widgetType,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = FontFamily.SansSerif,
)
private val doneStatusStyle = statusStyle.copy(color = widgetDone)
private val dueStatusStyle = statusStyle.copy(color = widgetLive)
private val headlineStyle = TextStyle(
    color = widgetType,
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.SansSerif,
)
private val detailStyle = TextStyle(
    color = widgetType,
    fontSize = 13.sp,
    fontWeight = FontWeight.Normal,
    fontFamily = FontFamily.SansSerif,
)

@Composable
private fun WidgetContent(face: WidgetFace) {
    val context = LocalContext.current
    val openApp = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetCanvas)
            .clickable(actionStartActivity(openApp))
            .padding(8.dp),
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        if (face.status.isNotEmpty() || face.live) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (face.status.isNotEmpty()) {
                    val style = when (face.status) {
                        "DONE" -> doneStatusStyle
                        "DUE" -> dueStatusStyle
                        else -> statusStyle
                    }
                    Text(text = face.status, style = style, maxLines = 1)
                }
                if (face.live) {
                    Box(
                        modifier = GlanceModifier
                            .padding(start = if (face.status.isEmpty()) 0.dp else 6.dp)
                            .size(8.dp)
                            .cornerRadius(4.dp)
                            .background(widgetLive),
                    ) {}
                }
            }
        }
        if (face.headline.isNotEmpty()) {
            Text(text = face.headline, style = headlineStyle, maxLines = 1)
        }
        if (face.detail.isNotEmpty()) {
            Text(text = face.detail, style = detailStyle, maxLines = 1)
        }
        if (face.marks.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                face.marks.forEach { kind -> WeekDot(kind) }
            }
        }
    }
}

@Composable
private fun WeekDot(kind: DayMarkKind) {
    // Trained is green. Missed is a red ring. A personal record and the live session are signal red.
    Box(
        modifier = GlanceModifier.padding(horizontal = 2.dp).size(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            DayMarkKind.PR ->
                Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(widgetLive)) {}
            DayMarkKind.TRAINED ->
                Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(widgetDone)) {}
            DayMarkKind.PENDING ->
                Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(widgetType)) {}
            DayMarkKind.MISSED ->
                Box(
                    modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(widgetLive),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = GlanceModifier.size(4.dp).cornerRadius(2.dp).background(widgetCanvas)) {}
                }
            DayMarkKind.UPCOMING ->
                Box(
                    modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(widgetType),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = GlanceModifier.size(4.dp).cornerRadius(2.dp).background(widgetCanvas)) {}
                }
            DayMarkKind.NUMERAL ->
                Box(modifier = GlanceModifier.size(4.dp).cornerRadius(2.dp).background(widgetType)) {}
        }
    }
}
