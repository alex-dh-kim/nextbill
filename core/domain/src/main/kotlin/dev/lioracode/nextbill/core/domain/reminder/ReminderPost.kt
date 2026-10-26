package dev.lioracode.nextbill.core.domain.reminder

import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.SubscriptionStatus
import java.time.LocalDate

/** 화면에 게시할 앱 알림 하나. */
sealed interface ReminderPost {
    data class Single(val subscription: Subscription, val billingDate: LocalDate) : ReminderPost

    /** 묶음 알림. [items]는 결제일 → 이름 순입니다. */
    data class Summary(val items: List<Pair<Subscription, LocalDate>>) : ReminderPost
}

/** 이 건수 이상이면 묶음 알림 하나로 보여 줍니다(설계 문서 ADR-14). */
const val SUMMARY_THRESHOLD = 4

/** 지금 화면에 떠 있는 앱 알림의 항목(묶음이면 항목마다). */
data class ShownReminder(val subscriptionId: SubscriptionId, val billingDate: LocalDate)

private val postingOrder = compareBy<Pair<Subscription, LocalDate>>(
    { it.second },
    { it.first.name },
    { it.first.id.value }
)

/** 3건 이하는 Single 각각, 4건 이상은 Summary 하나. 둘 다 결제일 → 이름 순입니다. */
fun groupForPosting(items: List<Pair<Subscription, LocalDate>>): List<ReminderPost> {
    val sorted = items.sortedWith(postingOrder)
    return if (sorted.size >= SUMMARY_THRESHOLD) {
        listOf(ReminderPost.Summary(sorted))
    } else {
        sorted.map { (sub, date) -> ReminderPost.Single(sub, date) }
    }
}

/**
 * 화면에 남길 앱 알림 전체(설계 문서 ADR-14). [shown] 중 구독이 남아 있고 해지되지 않았고 결제일이
 * [today] 이후(당일 포함)인 항목에 [deliverable]을 합칩니다. 같은 구독이면 [deliverable]이 이깁니다.
 * 사용자가 지워 [shown]에 없는 알림은 되살리지 않습니다. 결과는 Single 3개 이하 또는 Summary 1개입니다.
 */
fun planShown(
    shown: List<ShownReminder>,
    deliverable: List<Pair<Subscription, LocalDate>>,
    subscriptions: Map<SubscriptionId, Subscription>,
    today: LocalDate
): List<ReminderPost> {
    val merged = LinkedHashMap<SubscriptionId, Pair<Subscription, LocalDate>>()
    shown.forEach { item ->
        val sub = subscriptions[item.subscriptionId] ?: return@forEach
        if (sub.status != SubscriptionStatus.CANCELLED && item.billingDate >= today) {
            merged[sub.id] = sub to item.billingDate
        }
    }
    deliverable.forEach { merged[it.first.id] = it }
    return groupForPosting(merged.values.toList())
}

/** 화면에 알림을 남길 수 있는 구독(남아 있고 해지되지 않음). [planShown]·정리가 같은 규칙을 씁니다. */
fun showableIds(subscriptions: Collection<Subscription>): Set<SubscriptionId> = subscriptions
    .filter { it.status != SubscriptionStatus.CANCELLED }
    .mapTo(HashSet()) { it.id }

/**
 * 화면의 앱 알림을 구분하는 tag. Single은 구독마다, Summary는 하나입니다.
 * Stable contract: matches the tag of the posted Android notification; changing it orphans live notifications.
 */
val ReminderPost.tag: String
    get() = when (this) {
        is ReminderPost.Single -> "subscription-${subscription.id.value}"
        is ReminderPost.Summary -> "summary"
    }

/** 이 알림이 담은 항목. */
val ReminderPost.shownItems: List<ShownReminder>
    get() = when (this) {
        is ReminderPost.Single -> listOf(ShownReminder(subscription.id, billingDate))
        is ReminderPost.Summary -> items.map { (s, d) -> ShownReminder(s.id, d) }
    }
