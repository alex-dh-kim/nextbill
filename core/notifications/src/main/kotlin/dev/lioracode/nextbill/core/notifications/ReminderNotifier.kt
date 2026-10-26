package dev.lioracode.nextbill.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lioracode.nextbill.core.domain.effect.ReminderPoster
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.ShownReminder
import dev.lioracode.nextbill.core.domain.reminder.dueLabel
import dev.lioracode.nextbill.core.domain.reminder.tag
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.format
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** 이 앱 알림은 모두 같은 id를 쓰고 tag로 구분합니다. `Long.hashCode()`는 충돌하므로 id로 쓰지 않습니다. */
private const val NOTIFICATION_ID = 1

private const val EXTRA_BILLING_EPOCH_DAY = "billing_epoch_day"
private const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
private const val EXTRA_ITEM_IDS = "item_ids"
private const val EXTRA_ITEM_BILLING_DAYS = "item_billing_days"
private const val EXTRA_ITEM_NAMES = "item_names"

/** 내용(제목·본문) 지문. 이름·금액이 바뀌었거나 남은 날 문구가 달라졌을 때 다시 게시하기 위한 것. */
private const val EXTRA_CONTENT = "content"

private data class Entry(val id: Long, val name: String, val epochDay: Long) {
    val shown get() = ShownReminder(SubscriptionId(id), LocalDate.ofEpochDay(epochDay))
}

/** 게시할 알림 하나. [items]는 [extras]에 담긴 항목과 같습니다. */
private class Rendered(
    val tag: String,
    val title: String,
    val text: String,
    val extras: Bundle,
    val entries: List<Entry>,
    val single: Boolean
) {
    val items get() = entries.map { it.shown }
    val content get() = title + "\u0000" + text
}

/** 지금 떠 있는 이 앱 알림. */
private class Existing(
    val tag: String,
    val single: Boolean,
    val entries: List<Entry>,
    val content: String?
) {
    val items get() = entries.map { it.shown }
}

/** 알림 서비스가 보여 주는 지금의 활성 알림. 테스트에서 뒤처진 상태를 흉내 내려고 분리했습니다. */
internal fun interface ActiveNotificationSource {
    fun active(): List<StatusBarNotification>
}

/** 단조 증가 시각(ms). 테스트에서 바꿀 수 있게 분리했습니다. */
internal fun interface ElapsedClock {
    fun now(): Long
}

/** 마지막으로 쓴 상태를 OS가 반영했다고 보기 전까지(또는 사용자의 삭제를 다시 볼 때까지) 기억하는 시간. */
internal const val OVERLAY_TTL_MS = 5_000L

/** [state]가 null이면 우리가 취소한 것. */
private class Written(val state: Existing?, val at: Long)

/**
 * [ReminderPoster]의 Android 구현. 앱에서 하나여야 합니다(@Singleton): 알림 처리(show)와 정리(prune)가 같은
 * overlay와 잠금을 공유합니다. 지금 떠 있는 상태는 `getActiveNotifications()`의 tag·extras에서 읽으므로
 * 프로세스가 죽어도 이어집니다. 호출자가 coordinator 잠금 안에서만 부릅니다.
 *
 * NotificationManager의 게시·취소는 비동기라 방금 쓴 것이 `getActiveNotifications()`에 아직 없을 수
 * 있습니다. 그래서 마지막으로 쓴 상태를 [OVERLAY_TTL_MS] 동안 기억해 OS 목록 위에 덮어 읽습니다. OS 목록이
 * 같은 상태를 보이면 바로 버리고, 시간이 지나면 버려 그 뒤의 사용자 삭제는 그대로 보입니다.
 */
@Singleton
class ReminderNotifier internal constructor(
    private val context: Context,
    private val elapsed: ElapsedClock,
    private val source: ActiveNotificationSource?
) : ReminderPoster {
    @Inject constructor(
        @ApplicationContext context: Context
    ) : this(context, ElapsedClock { SystemClock.elapsedRealtime() }, null)

    private val overlay = LinkedHashMap<String, Written>()

    private val manager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private fun osActive(): List<StatusBarNotification> =
        source?.active() ?: manager.activeNotifications.toList()

    override fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        ensureChannels(context)
        val channel = manager.getNotificationChannel(REMINDER_CHANNEL_ID)
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    @Synchronized
    override fun shown(): List<ShownReminder> = readExisting().flatMap { it.items }

    @Synchronized
    override fun show(posts: List<ReminderPost>, today: LocalDate) {
        if (!canPost()) return
        val existing = readExisting().associateBy { it.tag }
        val alreadyShown = existing.values.flatMap { it.items }.toSet()
        val wanted = posts.map { render(it, today) }
        val wantedTags = wanted.map { it.tag }.toSet()
        existing.keys.filter { it !in wantedTags }.forEach { cancel(it) }
        wanted.forEach { rendered ->
            val old = existing[rendered.tag]
            if (old != null && old.items == rendered.items && old.content == rendered.content) {
                return@forEach
            }
            // 이미 보였던 항목만 든 알림은 조용히, 새 항목이 든 알림은 소리와 함께 게시합니다.
            val silent = rendered.items.all { it in alreadyShown }
            // 바뀐 알림은 같은 key를 덮어쓰면 다시 울리지 않을 수 있어 먼저 취소합니다.
            if (old != null && !silent) cancel(rendered.tag)
            post(rendered, silent = silent, onlyAlertOnce = false)
        }
    }

    @Synchronized
    override fun pruneExpired(today: LocalDate, showable: Set<SubscriptionId>) {
        val todayDay = today.toEpochDay()

        // 결제일이 지났거나 구독이 삭제·해지된 항목은 남기지 않습니다(planShown과 같은 규칙).
        fun Entry.keep() = epochDay >= todayDay && SubscriptionId(id) in showable
        readExisting().forEach { existing ->
            if (existing.single) {
                if (existing.entries.none { it.keep() }) cancel(existing.tag)
                return@forEach
            }
            val kept = existing.entries.filter { it.keep() }
            when {
                kept.isEmpty() -> cancel(existing.tag)

                kept.size != existing.entries.size ->
                    post(renderSummary(existing.tag, kept), silent = true, onlyAlertOnce = true)
            }
        }
    }

    private fun cancel(tag: String) {
        manager.cancel(tag, NOTIFICATION_ID)
        overlay[tag] = Written(null, elapsed.now())
    }

    private fun post(rendered: Rendered, silent: Boolean, onlyAlertOnce: Boolean) {
        ensureChannels(context)
        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(rendered.title)
            .setContentText(rendered.text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addExtras(rendered.extras)
            .setSilent(silent)
            .setOnlyAlertOnce(onlyAlertOnce)
        launchIntent()?.let { builder.setContentIntent(it) }
        manager.notify(rendered.tag, NOTIFICATION_ID, builder.build())
        overlay[rendered.tag] = Written(
            Existing(rendered.tag, rendered.single, rendered.entries, rendered.content),
            elapsed.now()
        )
    }

    /** 탭하면 앱을 엽니다. 실행 가능한 activity가 없으면 null. */
    private fun launchIntent(): PendingIntent? {
        val intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** OS 목록에 [overlay]를 덮어 읽습니다. */
    private fun readExisting(): List<Existing> {
        val result = LinkedHashMap<String, Existing>()
        osActive().forEach { sbn -> parse(sbn)?.let { result[it.tag] = it } }
        val now = elapsed.now()
        val iterator = overlay.iterator()
        while (iterator.hasNext()) {
            val (tag, written) = iterator.next()
            val os = result[tag]
            val state = written.state
            val agrees = if (state == null) {
                os == null
            } else {
                os != null && os.items == state.items && os.content == state.content
            }
            if (agrees || now - written.at > OVERLAY_TTL_MS) {
                iterator.remove()
            } else if (state == null) {
                result.remove(tag)
            } else {
                result[tag] = state
            }
        }
        return result.values.toList()
    }

    private fun parse(sbn: StatusBarNotification): Existing? {
        val tag = sbn.tag ?: return null
        if (sbn.id != NOTIFICATION_ID) return null
        val extras = sbn.notification.extras ?: return null
        val content = extras.getString(EXTRA_CONTENT)
        if (extras.containsKey(EXTRA_BILLING_EPOCH_DAY)) {
            val entry = Entry(
                extras.getLong(EXTRA_SUBSCRIPTION_ID),
                "",
                extras.getLong(EXTRA_BILLING_EPOCH_DAY)
            )
            return Existing(tag, single = true, entries = listOf(entry), content = content)
        }
        val ids = extras.getLongArray(EXTRA_ITEM_IDS) ?: return null
        val days = extras.getLongArray(EXTRA_ITEM_BILLING_DAYS) ?: return null
        val names = extras.getStringArray(EXTRA_ITEM_NAMES) ?: return null
        if (ids.size != days.size || ids.size != names.size) return null
        val entries = ids.indices.map { Entry(ids[it], names[it], days[it]) }
        return Existing(tag, single = false, entries = entries, content = content)
    }

    private fun render(post: ReminderPost, today: LocalDate): Rendered = when (post) {
        is ReminderPost.Single -> renderSingle(post, today)

        is ReminderPost.Summary -> renderSummary(
            post.tag,
            post.items.map { (sub, date) -> Entry(sub.id.value, sub.name, date.toEpochDay()) }
        )
    }

    private fun renderSingle(post: ReminderPost.Single, today: LocalDate): Rendered {
        // 결제일이 지난 항목은 도메인이 걸러 보내지만, 방어적으로 "오늘"로 표시합니다.
        val label = when (val due = dueLabel(maxOf(post.billingDate, today), today)) {
            DueLabel.Today -> context.getString(R.string.reminder_due_today)
            DueLabel.Tomorrow -> context.getString(R.string.reminder_due_tomorrow)
            is DueLabel.InDays -> context.getString(R.string.reminder_due_in_days, due.days)
        }
        val title = context.getString(R.string.reminder_single_title, post.subscription.name, label)
        val text = context.getString(
            R.string.reminder_single_text,
            post.subscription.price.format(),
            formatDate(post.billingDate)
        )
        val extras = Bundle().apply {
            putLong(EXTRA_SUBSCRIPTION_ID, post.subscription.id.value)
            putLong(EXTRA_BILLING_EPOCH_DAY, post.billingDate.toEpochDay())
        }
        val rendered = Rendered(
            post.tag,
            title,
            text,
            extras,
            listOf(
                Entry(post.subscription.id.value, "", post.billingDate.toEpochDay())
            ),
            single = true
        )
        extras.putString(EXTRA_CONTENT, rendered.content)
        return rendered
    }

    private fun renderSummary(tag: String, entries: List<Entry>): Rendered {
        val title = context.resources.getQuantityString(
            R.plurals.reminder_summary_title,
            entries.size,
            entries.size
        )
        val text = if (entries.size >= 3) {
            context.getString(
                R.string.reminder_summary_text_many,
                entries[0].name,
                entries[1].name,
                entries.size - 2
            )
        } else {
            entries.joinToString(", ") { it.name }
        }
        val extras = Bundle().apply {
            putLongArray(EXTRA_ITEM_IDS, entries.map { it.id }.toLongArray())
            putLongArray(EXTRA_ITEM_BILLING_DAYS, entries.map { it.epochDay }.toLongArray())
            putStringArray(EXTRA_ITEM_NAMES, entries.map { it.name }.toTypedArray())
        }
        val rendered = Rendered(tag, title, text, extras, entries, single = false)
        extras.putString(EXTRA_CONTENT, rendered.content)
        return rendered
    }

    private fun formatDate(date: LocalDate): String {
        val locale = context.resources.configuration.locales[0]
        return DateTimeFormatter.ofPattern(
            context.getString(R.string.reminder_date_pattern),
            locale
        )
            .format(date)
    }
}
