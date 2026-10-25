package dev.lioracode.nextbill.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.sub
import org.junit.Test

class ReminderPostTagTest {
    private val a = sub(id = 7, name = "A")
    private val b = sub(id = 8, name = "B")

    @Test
    fun singleTagAndItems() {
        val post = ReminderPost.Single(a, d("2026-11-15"))
        assertThat(post.tag).isEqualTo("subscription-7")
        assertThat(
            post.shownItems
        ).containsExactly(ShownReminder(SubscriptionId(7), d("2026-11-15")))
    }

    @Test
    fun summaryTagAndItems() {
        val post = ReminderPost.Summary(listOf(a to d("2026-11-15"), b to d("2026-11-16")))
        assertThat(post.tag).isEqualTo("summary")
        assertThat(post.shownItems).containsExactly(
            ShownReminder(SubscriptionId(7), d("2026-11-15")),
            ShownReminder(SubscriptionId(8), d("2026-11-16"))
        ).inOrder()
    }
}
