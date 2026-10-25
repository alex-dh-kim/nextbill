package dev.lioracode.nextbill.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dev.lioracode.nextbill.core.domain.billing.d
import dev.lioracode.nextbill.core.domain.billing.sub
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.LocalDate
import org.junit.Test

class ReminderPostTest {
    private val today = d("2026-11-15")
    private val a = sub(id = 1, name = "Apple")
    private val b = sub(id = 2, name = "Bugs")
    private val c = sub(id = 3, name = "Coupang")
    private val dd = sub(id = 4, name = "Disney")
    private val e = sub(id = 5, name = "Eleven")
    private val f = sub(id = 6, name = "Flo")
    private val all = listOf(a, b, c, dd, e, f).associateBy { it.id }

    private fun shown(s: Subscription, date: LocalDate = today) = ShownReminder(s.id, date)

    private fun ReminderPost.entries(): List<Pair<Subscription, LocalDate>> = when (this) {
        is ReminderPost.Single -> listOf(subscription to billingDate)
        is ReminderPost.Summary -> items
    }

    private fun List<ReminderPost>.asShown() =
        flatMap { post -> post.entries().map { (s, date) -> ShownReminder(s.id, date) } }

    @Test fun threeOrFewerAreSingles() {
        val posts = groupForPosting(listOf(a to today, b to today, c to today))

        assertThat(posts).containsExactly(
            ReminderPost.Single(a, today),
            ReminderPost.Single(b, today),
            ReminderPost.Single(c, today)
        ).inOrder()
    }

    @Test fun fourOrMoreBecomeOneSummary() {
        val later = d("2026-11-16")
        val posts = groupForPosting(listOf(dd to today, a to later, c to today, b to today))

        assertThat(posts).containsExactly(
            ReminderPost.Summary(listOf(b to today, c to today, dd to today, a to later))
        )
        assertThat(SUMMARY_THRESHOLD).isEqualTo(4)
    }

    @Test fun shownSinglesMergeWithNewIntoSummary() {
        val posts = planShown(
            shown = listOf(shown(a), shown(b), shown(c)),
            deliverable = listOf(dd to today),
            subscriptions = all,
            today = today
        )

        assertThat(posts).containsExactly(
            ReminderPost.Summary(listOf(a to today, b to today, c to today, dd to today))
        )
    }

    @Test fun shownSummaryAbsorbsNew() {
        val posts = planShown(
            shown = listOf(shown(a), shown(b), shown(c), shown(dd), shown(e)),
            deliverable = listOf(f to today),
            subscriptions = all,
            today = today
        )

        assertThat(posts).hasSize(1)
        val summary = posts.single() as ReminderPost.Summary
        assertThat(summary.items.map { it.first.id }).containsExactly(
            a.id,
            b.id,
            c.id,
            dd.id,
            e.id,
            f.id
        ).inOrder()
    }

    @Test fun dismissedByUserIsNotRevived() {
        // a, b, c가 떠 있었고 사용자가 c를 지웠습니다.
        val posts = planShown(
            shown = listOf(shown(a), shown(b)),
            deliverable = listOf(dd to today),
            subscriptions = all,
            today = today
        )

        assertThat(posts).containsExactly(
            ReminderPost.Single(a, today),
            ReminderPost.Single(b, today),
            ReminderPost.Single(dd, today)
        ).inOrder()
        assertThat(posts.flatMap { it.entries() }.map { it.first.id }).doesNotContain(c.id)
    }

    @Test fun expiredOrDeletedOrCancelledShownAreDropped() {
        val cancelledC = c.copy(periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-11-15"))))
        val subscriptions = mapOf(a.id to a, cancelledC.id to cancelledC, dd.id to dd)

        val posts = planShown(
            shown = listOf(
                shown(a, d("2026-11-14")), // 결제일이 지남
                ShownReminder(SubscriptionId(99), today), // 삭제됨
                shown(cancelledC), // 해지됨
                shown(dd) // 결제일 == 오늘: 남음
            ),
            deliverable = emptyList(),
            subscriptions = subscriptions,
            today = today
        )

        assertThat(posts).containsExactly(ReminderPost.Single(dd, today))
    }

    @Test fun shownUsesCurrentSubscription() {
        val renamed = a.copy(name = "Apple Music")

        val posts = planShown(listOf(shown(a)), emptyList(), mapOf(a.id to renamed), today)

        assertThat(posts).containsExactly(ReminderPost.Single(renamed, today))
    }

    @Test fun shownKeptAfterReminderDisabled() {
        // 알림을 꺼도 이미 화면에 떠 있는 알림은 결제일이 지날 때까지 남습니다(controller 결정).
        val disabled = a.copy(reminder = a.reminder.copy(enabled = false))

        val posts = planShown(listOf(shown(a)), emptyList(), mapOf(a.id to disabled), today)

        assertThat(posts).containsExactly(ReminderPost.Single(disabled, today))
    }

    @Test fun newReplacesShownOfSameSubscription() {
        val next = d("2026-12-15")

        val posts = planShown(
            shown = listOf(shown(a, today)),
            deliverable = listOf(a to next),
            subscriptions = all,
            today = today
        )

        assertThat(posts).containsExactly(ReminderPost.Single(a, next))
    }

    @Test fun oneAtATimeNeverExceedsCap() {
        val subs = (1..51).map { sub(id = it.toLong(), name = "S%02d".format(it)) }
        val byId = subs.associateBy { it.id }
        var onScreen = emptyList<ReminderPost>()

        subs.forEachIndexed { index, s ->
            val step = index + 1
            onScreen = planShown(onScreen.asShown(), listOf(s to today), byId, today)

            val singles = onScreen.filterIsInstance<ReminderPost.Single>()
            val summaries = onScreen.filterIsInstance<ReminderPost.Summary>()
            val withinCap = (summaries.isEmpty() && singles.size <= 3) ||
                (singles.isEmpty() && summaries.size == 1)
            assertWithMessage("step %s: %s", step, onScreen).that(withinCap).isTrue()
            assertWithMessage("step %s: %s", step, onScreen)
                .that(onScreen.sumOf { it.entries().size })
                .isEqualTo(step)
        }
        assertThat((onScreen.single() as ReminderPost.Summary).items).hasSize(51)
    }
}
