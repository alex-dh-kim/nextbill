package dev.lioracode.nextbill.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.model.format
import dev.lioracode.nextbill.core.ui.R as UiR

/** 4×2 이상으로 보는 최소 너비. 이보다 좁으면 2×2 "다음 결제" 레이아웃입니다. */
internal val WIDE_MIN_WIDTH = 250.dp

/** 이 높이 이상이면 행 3개와 푸터를 그립니다. */
internal val TALL_MIN_HEIGHT = 180.dp

internal fun rowsFor(size: DpSize): Int = if (size.height >= TALL_MIN_HEIGHT) 3 else 2

// 목록 레이아웃 치수(dp). 250x110에서 넘치지 않도록 합이 높이 안에 들어와야 합니다.
internal val LIST_PADDING_V = 8.dp
internal val HEADER_HEIGHT = 30.dp
internal val ROW_HEIGHT = 28.dp
internal val DIVIDER_HEIGHT = 1.dp
internal val FOOTER_GAP = 2.dp
internal val FOOTER_HEIGHT = 14.dp

/** [ListContent]가 [rows]행(과 푸터)을 그릴 때 차지하는 세로 길이. */
internal fun listContentHeight(rows: Int, footer: Boolean): Dp =
    LIST_PADDING_V * 2 + HEADER_HEIGHT + (ROW_HEIGHT + DIVIDER_HEIGHT) * rows +
        if (footer) FOOTER_GAP + FOOTER_HEIGHT else 0.dp

/** 순수 표시용. 크기 판단은 셀 수가 아니라 [size](dp)로만 합니다. 전체를 탭하면 앱을 엽니다. */
@Composable
fun WidgetContent(state: WidgetState, size: DpSize) {
    val context = LocalContext.current
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    GlanceTheme(colors = WidgetColorProviders) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(WidgetColors.card)
                .cornerRadius(20.dp)
                .let { if (launch != null) it.clickable(actionStartActivity(launch)) else it }
        ) {
            when {
                state.rows.isEmpty() -> EmptyContent(context)
                size.width < WIDE_MIN_WIDTH -> NextOnlyContent(context, state.rows.first())
                else -> ListContent(context, state, rowsFor(size))
            }
        }
    }
}

@Composable
private fun EmptyContent(context: Context) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = context.getString(R.string.widget_name),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.brand
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.widget_empty),
            maxLines = 2,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.textPrimary
            )
        )
    }
}

@Composable
private fun ListContent(context: Context, state: WidgetState, maxRows: Int) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(
            horizontal = 14.dp,
            vertical = LIST_PADDING_V
        )
    ) {
        Header(context, state)
        val rows = state.rows.take(maxRows)
        rows.forEach { row ->
            Divider()
            ListRow(context, row)
        }
        if (maxRows == 3) {
            Spacer(GlanceModifier.height(FOOTER_GAP))
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.widget_upcoming_count,
                    state.upcomingCount,
                    state.upcomingCount
                ),
                maxLines = 1,
                style = TextStyle(fontSize = 10.sp, color = WidgetColors.textSecondary),
                modifier = GlanceModifier.height(FOOTER_HEIGHT)
            )
        }
    }
}

@Composable
private fun Header(context: Context, state: WidgetState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(22.dp).background(WidgetColors.brand).cornerRadius(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WidgetColors.onBrand
                )
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = context.getString(R.string.widget_name),
            maxLines = 1,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.textPrimary
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = context.getString(R.string.widget_this_month),
                maxLines = 1,
                style = TextStyle(fontSize = 10.sp, color = WidgetColors.textSecondary)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                val others = state.monthTotal.others.size
                if (others > 0) {
                    Text(
                        text = context.getString(R.string.widget_other_currencies, others),
                        maxLines = 1,
                        style = TextStyle(fontSize = 10.sp, color = WidgetColors.textSecondary)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                }
                Text(
                    text = state.monthTotal.primary.format(),
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WidgetColors.textPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = GlanceModifier.fillMaxWidth().height(
            DIVIDER_HEIGHT
        ).background(WidgetColors.border)
    ) {
    }
}

@Composable
private fun ListRow(context: Context, row: WidgetRow) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(row, 22.dp)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.name,
            maxLines = 1,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = WidgetColors.textPrimary
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = row.price.format(),
            maxLines = 1,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.textPrimary
            )
        )
        Spacer(GlanceModifier.width(6.dp))
        DuePill(context, row.label)
    }
}

@Composable
private fun NextOnlyContent(context: Context, row: WidgetRow) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            text = context.getString(R.string.widget_next),
            maxLines = 1,
            style = TextStyle(fontSize = 11.sp, color = WidgetColors.textSecondary)
        )
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(row, 20.dp)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = row.name,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = WidgetColors.textPrimary
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = row.price.format(),
            maxLines = 1,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.textPrimary
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        DuePill(context, row.label)
    }
}

@Composable
private fun Avatar(row: WidgetRow, diameter: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = GlanceModifier
            .size(diameter)
            .background(WidgetColors.avatarBackground(row.colorKey))
            .cornerRadius(diameter / 2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = widgetAvatarLetter(row.name),
            style = TextStyle(
                fontSize = (diameter.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.avatarLetter(row.colorKey)
            )
        )
    }
}

internal fun widgetAvatarLetter(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val first = String(Character.toChars(trimmed.codePointAt(0)))
    return if (Character.isLetter(trimmed.codePointAt(0))) first.uppercase() else first
}

/** 오늘/내일은 Urgent, D-n은 Neutral. 색이 아니라 글자로도 구분됩니다. */
@Composable
private fun DuePill(context: Context, label: DueLabel) {
    val (text, urgent) = when (label) {
        DueLabel.Today -> context.getString(UiR.string.due_today) to true
        DueLabel.Tomorrow -> context.getString(UiR.string.due_tomorrow) to true
        is DueLabel.InDays -> context.getString(UiR.string.due_in_days, label.days) to false
    }
    val bg: ColorProvider = if (urgent) WidgetColors.urgentSurface else WidgetColors.neutralSurface
    val fg: ColorProvider = if (urgent) WidgetColors.urgentText else WidgetColors.neutralText
    Box(
        modifier = GlanceModifier
            .background(bg)
            .cornerRadius(9.dp)
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
        )
    }
}
