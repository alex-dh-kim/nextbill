package dev.lioracode.nextbill.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.ShownReminder
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.TestSubscriptions
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** 정리 테스트에서 모든 구독이 남아 있고 해지되지 않았다고 볼 때. */
private val ALL_IDS = (1L..20L).mapTo(HashSet()) { SubscriptionId(it) }

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class ReminderNotifierTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notifier = ReminderNotifier(context)
    private val today = d("2026-11-16")

    @Before
    fun grant() {
        shadowOf(
            RuntimeEnvironment.getApplication()
        ).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun active(): List<StatusBarNotification> = nm.activeNotifications.toList()

    private fun single(id: Long, name: String = "구독$id", billing: String = "2026-11-17") =
        ReminderPost.Single(sub(id, name), d(billing))

    private fun summary(vararg ids: Long, billing: String = "2026-11-20") =
        ReminderPost.Summary(ids.map { sub(it, "구독$it") to d(billing) })

    private fun Notification.title() = extras.getString(Notification.EXTRA_TITLE)

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    /** NotificationCompat.Builder.setSilent(true)는 group을 "silent"로 두고 그룹 알림음을 끕니다. */
    private fun Notification.isSilent() = group == "silent" &&
        groupAlertBehavior == Notification.GROUP_ALERT_SUMMARY

    @Test
    fun singleNotificationText() {
        notifier.show(listOf(ReminderPost.Single(sub(7, "운동"), d("2026-11-23"))), today)
        val n = active().single()
        assertThat(n.tag).isEqualTo("subscription-7")
        assertThat(n.id).isEqualTo(1)
        assertThat(n.notification.title()).isEqualTo("운동 7일 후 결제")
        assertThat(n.notification.text()).contains("11월 23일")
        assertThat(n.notification.text()).contains("₩10,000")
        assertThat(n.notification.contentIntent).isNull() // 테스트 앱엔 launcher activity가 없음
    }

    @Test
    fun dueLabelsTodayTomorrow() {
        notifier.show(
            listOf(single(1, billing = "2026-11-16"), single(2, billing = "2026-11-17")),
            today
        )
        val titles = active().associate { it.tag to it.notification.title() }
        assertThat(titles["subscription-1"]).isEqualTo("구독1 오늘 결제")
        assertThat(titles["subscription-2"]).isEqualTo("구독2 내일 결제")
    }

    @Test
    fun summaryNotificationText() {
        val items = listOf(
            TestSubscriptions.netflix,
            TestSubscriptions.phoneBill,
            TestSubscriptions.youtube,
            TestSubscriptions.icloud,
            TestSubscriptions.gym
        ).map { it to d("2026-11-20") }
        notifier.show(listOf(ReminderPost.Summary(items)), today)
        val n = active().single()
        assertThat(n.tag).isEqualTo("summary")
        assertThat(n.notification.title()).isEqualTo("결제 예정 5건")
        assertThat(n.notification.text()).isEqualTo("넷플릭스, 통신비 외 3건")
    }

    @Test
    @Config(qualifiers = "en")
    fun englishText() {
        notifier.show(listOf(ReminderPost.Single(sub(7, "Gym"), d("2026-11-23"))), today)
        val n = active().single().notification
        assertThat(n.title()).isEqualTo("Gym payment in 7 days")
        assertThat(n.text()).isEqualTo("₩10,000 · Nov 23")
    }

    @Test
    fun distinctSubscriptionsGetDistinctNotifications() {
        notifier.show(listOf(single(1), single(4_294_967_297)), today)
        assertThat(active()).hasSize(2)
    }

    @Test
    fun sameSubscriptionReplaces() {
        notifier.show(listOf(single(1)), today)
        notifier.show(listOf(single(1, name = "바뀐 이름")), today)
        assertThat(active()).hasSize(1)
        assertThat(active().single().notification.title()).startsWith("바뀐 이름")
    }

    @Test
    fun pruneExpiredRemovesPastBillingNotifications() {
        notifier.show(
            listOf(single(1, billing = "2026-11-15"), single(2, billing = "2026-11-16")),
            d("2026-11-15")
        )
        notifier.pruneExpired(today, ALL_IDS)
        assertThat(active().map { it.tag }).containsExactly("subscription-2")
    }

    @Test
    fun pruneRemovesItemsOfDeletedOrCancelledSubscriptions() {
        notifier.show(listOf(single(1), single(2)), today)
        notifier.pruneExpired(today, setOf(SubscriptionId(2)))
        assertThat(active().map { it.tag }).containsExactly("subscription-2")

        notifier.show(listOf(summary(5, 6, 7, 8)), today)
        notifier.pruneExpired(today, setOf(SubscriptionId(5), SubscriptionId(6)))
        val n = active().single()
        assertThat(n.tag).isEqualTo("summary")
        assertThat(notifier.shown().map { it.subscriptionId.value }).containsExactly(5L, 6L)
        assertThat(n.notification.isSilent()).isTrue()
        assertThat(n.notification.text()).isEqualTo("구독5, 구독6")

        notifier.pruneExpired(today, emptySet())
        assertThat(active()).isEmpty()
    }

    @Test
    fun showOnlyRepostsChanged() {
        val posts = listOf(single(1), single(2))
        notifier.show(posts, today)
        // 다시 게시했다면 notify가 같은 key를 덮어써 새 Notification 객체가 됩니다. 객체 동일성으로 확인합니다.
        val objs1 = active().associate { it.tag to System.identityHashCode(it.notification) }
        notifier.show(posts, today)
        val objs2 = active().associate { it.tag to System.identityHashCode(it.notification) }
        assertThat(objs2).isEqualTo(objs1)
        assertThat(objs2.keys).containsExactly("subscription-1", "subscription-2")
    }

    @Test
    fun summaryReplacesSingles() {
        notifier.show(listOf(single(1), single(2), single(3)), today)
        notifier.show(listOf(summary(1, 2, 3, 4)), today)
        assertThat(active().map { it.tag }).containsExactly("summary")
    }

    @Test
    fun shownReadsSummaryItems() {
        notifier.show(listOf(summary(1, 2, 3, 4)), today)
        assertThat(notifier.shown()).containsExactly(
            ShownReminder(SubscriptionId(1), d("2026-11-20")),
            ShownReminder(SubscriptionId(2), d("2026-11-20")),
            ShownReminder(SubscriptionId(3), d("2026-11-20")),
            ShownReminder(SubscriptionId(4), d("2026-11-20"))
        )
    }

    @Test
    fun shownReadsSingles() {
        notifier.show(listOf(single(5, billing = "2026-11-18")), today)
        assertThat(notifier.shown())
            .containsExactly(ShownReminder(SubscriptionId(5), d("2026-11-18")))
    }

    @Test
    fun splitSummaryRepostsOldItemsSilently() {
        notifier.show(listOf(summary(2, 3)), today)
        val b = ReminderPost.Single(sub(2, "구독2"), d("2026-11-20"))
        val c = ReminderPost.Single(sub(3, "구독3"), d("2026-11-20"))
        val e = ReminderPost.Single(sub(5, "구독5"), d("2026-11-20"))
        notifier.show(listOf(b, c, e), today)
        val byTag = active().associateBy { it.tag }
        assertThat(byTag.keys).containsExactly("subscription-2", "subscription-3", "subscription-5")
        assertThat(byTag.getValue("subscription-2").notification.isSilent()).isTrue()
        assertThat(byTag.getValue("subscription-3").notification.isSilent()).isTrue()
        assertThat(byTag.getValue("subscription-5").notification.isSilent()).isFalse()
    }

    @Test
    fun summaryThatGainedItemsIsReplacedWithSound() {
        notifier.show(listOf(summary(1, 2, 3, 4)), today)
        notifier.show(listOf(summary(1, 2, 3, 4, 5)), today)
        val n = active().single()
        assertThat(n.notification.title()).isEqualTo("결제 예정 5건")
        assertThat(n.notification.isSilent()).isFalse()
    }

    @Test
    fun pruneShrinksSummaryQuietly() {
        val items = listOf(
            sub(1, "구독1") to d("2026-11-15"),
            sub(2, "구독2") to d("2026-11-15"),
            sub(3, "구독3") to d("2026-11-20"),
            sub(4, "구독4") to d("2026-11-21")
        )
        notifier.show(listOf(ReminderPost.Summary(items)), d("2026-11-15"))
        notifier.pruneExpired(today, ALL_IDS)
        val n = active().single()
        assertThat(n.tag).isEqualTo("summary")
        assertThat(notifier.shown().map { it.subscriptionId.value }).containsExactly(3L, 4L)
        assertThat(n.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE).isNotEqualTo(0)
        assertThat(n.notification.title()).isEqualTo("결제 예정 2건")
        assertThat(n.notification.text()).isEqualTo("구독3, 구독4")
    }

    @Test
    fun pruneCancelsSummaryWhenAllPast() {
        notifier.show(listOf(summary(1, 2, 3, 4, billing = "2026-11-15")), d("2026-11-15"))
        notifier.pruneExpired(today, ALL_IDS)
        assertThat(active()).isEmpty()
    }

    @Test
    fun pruneLeavesFutureNotificationsUntouched() {
        notifier.show(listOf(single(1), summary(5, 6, 7, 8)), today)
        notifier.pruneExpired(today, ALL_IDS)
        assertThat(active()).hasSize(2)
    }

    @Test
    fun showCancelsTagsNotWanted() {
        notifier.show(listOf(single(1), single(2)), today)
        notifier.show(listOf(single(2)), today)
        assertThat(active().map { it.tag }).containsExactly("subscription-2")
    }

    @Test
    fun ignoresForeignNotifications() {
        nm.notify(
            "other",
            1,
            android.app.Notification.Builder(context, REMINDER_CHANNEL_ID).build()
        )
        notifier.show(emptyList(), today)
        assertThat(active().map { it.tag }).containsExactly("other")
        assertThat(notifier.shown()).isEmpty()
    }

    @Test
    fun showDoesNothingWhenCannotPost() {
        shadowOf(
            RuntimeEnvironment.getApplication()
        ).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier.show(listOf(single(1)), today)
        assertThat(active()).isEmpty()
    }

    @Test
    @Config(sdk = [33])
    fun canPostFalseWithoutPermission() {
        shadowOf(
            RuntimeEnvironment.getApplication()
        ).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(notifier.canPost()).isFalse()
    }

    @Test
    @Config(sdk = [33])
    fun canPostTrueWithPermission() {
        assertThat(notifier.canPost()).isTrue()
    }

    @Test
    @Config(sdk = [34])
    fun canPostFalseWhenNotificationsDisabled() {
        shadowOf(nm).setNotificationsEnabled(false)
        assertThat(notifier.canPost()).isFalse()
    }

    @Test
    @Config(sdk = [34])
    fun canPostFalseWhenChannelBlocked() {
        ensureChannels(context)
        val channel = nm.getNotificationChannel(REMINDER_CHANNEL_ID)
        channel.importance = NotificationManager.IMPORTANCE_NONE
        nm.createNotificationChannel(channel)
        assertThat(notifier.canPost()).isFalse()
    }

    @Test
    fun ensureChannelsCreatesLocalizedChannelOnce() {
        ensureChannels(context)
        ensureChannels(context)
        val channels = nm.notificationChannels.filter { it.id == REMINDER_CHANNEL_ID }
        assertThat(channels).hasSize(1)
        assertThat(channels.single().name.toString()).isEqualTo("결제 알림")
    }

    // --- 비동기 게시를 흉내 낸 overlay 테스트 ---

    private class Lagging(private val nm: NotificationManager) : ActiveNotificationSource {
        var frozen: List<StatusBarNotification>? = null

        fun freeze() {
            frozen = nm.activeNotifications.toList()
        }

        override fun active() = frozen ?: nm.activeNotifications.toList()
    }

    private var now = 1_000L
    private val lagging = Lagging(nm)
    private val lagNotifier = ReminderNotifier(context, { now }, lagging)

    private fun entryAt(id: Long, day: String) = sub(id, "구독$id") to d(day)

    @Test
    fun overlayKeepsJustPostedItemsWhenOsListLags() {
        val a = entryAt(1, "2026-11-15")
        val rest = listOf(2L, 3L, 4L).map { entryAt(it, "2026-11-20") }
        lagNotifier.show(listOf(ReminderPost.Summary(listOf(a) + rest)), d("2026-11-15"))
        lagging.freeze() // 이후 OS 목록은 [A,B,C,D]에 머뭅니다.
        val withE = rest + entryAt(5, "2026-11-20")
        lagNotifier.show(listOf(ReminderPost.Summary(withE)), d("2026-11-15"))
        lagNotifier.pruneExpired(today, ALL_IDS)

        val real = notifier.shown().map { it.subscriptionId.value }
        assertThat(real).containsExactly(2L, 3L, 4L, 5L)
    }

    @Test
    fun immediateSecondShowWithSamePostsIsNotAudible() {
        lagging.freeze() // 비어 있는 채로 뒤처짐
        val posts = listOf(summary(1, 2, 3, 4))
        lagNotifier.show(posts, today)
        val first = active().single().notification
        lagNotifier.show(posts, today)
        assertThat(active().single().notification).isSameInstanceAs(first)
    }

    @Test
    fun osDismissalIsHonouredAfterTtl() {
        lagNotifier.show(listOf(single(1)), today)
        nm.cancelAll() // 사용자가 지움
        lagging.frozen = null
        assertThat(lagNotifier.shown()).hasSize(1) // 아직 덮어쓴 상태를 기억합니다.
        now += OVERLAY_TTL_MS + 1
        assertThat(lagNotifier.shown()).isEmpty()
    }

    @Test
    fun overlayHidesJustCancelledWhileOsLags() {
        lagNotifier.show(listOf(single(1)), today)
        lagging.freeze()
        lagNotifier.show(emptyList(), today)
        assertThat(lagNotifier.shown()).isEmpty()
    }

    @Test
    fun notifierIsSingletonScopedForSharedOverlayAndLock() {
        // 처리(show)와 정리(prune)가 같은 인스턴스를 써야 overlay·잠금이 이어집니다. Hilt 그래프 자체는
        // app 모듈 조립(Task 17)에서 확인합니다.
        assertThat(
            ReminderNotifier::class.java.isAnnotationPresent(javax.inject.Singleton::class.java)
        )
            .isTrue()
    }

    @Test
    fun sharedInstanceServesShowThenPruneConsistently() {
        val poster: dev.lioracode.nextbill.core.domain.effect.ReminderPoster = lagNotifier
        val deliver = poster
        val refresher = poster
        deliver.show(listOf(summary(1, 2, 3, 4)), today)
        lagging.freeze()
        deliver.show(listOf(summary(1, 2, 3, 4, 5)), today)
        refresher.pruneExpired(today, ALL_IDS)
        assertThat(
            notifier.shown().map {
                it.subscriptionId.value
            }
        ).containsExactly(1L, 2L, 3L, 4L, 5L)
    }
}
