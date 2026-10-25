@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.lioracode.nextbill.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.domain.validation.FieldError
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.ActivePeriod
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.ThemeMode
import dev.lioracode.nextbill.core.testing.TestSubscriptions.netflix
import dev.lioracode.nextbill.core.testing.at
import dev.lioracode.nextbill.core.testing.d
import dev.lioracode.nextbill.core.testing.krw
import dev.lioracode.nextbill.core.testing.sub
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubscriptionWriteUseCasesTest {
    @Test fun saveInvalidDraftDoesNotTouchRepoOrEffects() = runTest {
        val env = TestEnv(appScope())
        env.startup.start()
        advanceUntilIdle()
        val dayChanges = env.dayChange.count
        val widgets = env.widget.updateCount

        val result = env.save(netflix.newDraft().copy(name = "  "), 0)
        advanceUntilIdle()

        assertThat(result).isEqualTo(Outcome.Err(SaveError.Invalid(setOf(FieldError.NAME_BLANK))))
        assertThat(env.repo.upsertCount).isEqualTo(0)
        assertThat(env.scheduled.snapshot).isEmpty()
        assertThat(env.alarm.setCount).isEqualTo(0)
        assertThat(env.dayChange.count).isEqualTo(dayChanges)
        assertThat(env.widget.updateCount).isEqualTo(widgets)
    }

    @Test fun saveEditPreservesPeriods() = runTest {
        val s = sub(
            1,
            periods = listOf(
                ActivePeriod(d("2026-01-15"), d("2026-03-31")),
                ActivePeriod(d("2026-05-01"), null)
            )
        )
        val env = TestEnv(appScope(), listOf(s))
        env.startup.start()

        val result = env.save(s.toDraft().copy(amountText = "12000"), 0)

        assertThat(result).isEqualTo(Outcome.Ok(s.id))
        val saved = env.repo.get(s.id)!!
        assertThat(saved.periods).isEqualTo(s.periods)
        assertThat(saved.price).isEqualTo(krw(12_000))
    }

    @Test fun cancelUsesCurrentDate() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()

        assertThat(env.cancel(netflix.id, 0)).isEqualTo(Outcome.Ok(Unit))

        assertThat(env.repo.get(netflix.id)!!.cancelledAt).isEqualTo(d("2026-11-10"))
    }

    @Test fun restoreOpensNewPeriod() = runTest {
        val cancelled = netflix.copy(
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-10-01")))
        )
        val env = TestEnv(appScope(), listOf(cancelled))
        env.startup.start()

        assertThat(env.restore(netflix.id, 0)).isEqualTo(Outcome.Ok(Unit))

        assertThat(env.repo.get(netflix.id)!!.periods).containsExactly(
            ActivePeriod(d("2026-01-15"), d("2026-10-01")),
            ActivePeriod(d("2026-11-10"), null)
        ).inOrder()
    }

    @Test fun deleteRemovesAndRequestsRefresh() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        advanceUntilIdle()
        val dayChanges = env.dayChange.count
        assertThat(env.scheduled.snapshot).isNotEmpty()

        assertThat(env.delete(netflix.id, 0)).isEqualTo(Outcome.Ok(Unit))
        advanceUntilIdle()

        assertThat(env.repo.get(netflix.id)).isNull()
        assertThat(env.dayChange.count).isEqualTo(dayChanges + 1)
        assertThat(env.scheduled.snapshot).isEmpty()
        assertThat(env.alarm.nextAt).isNull()
    }

    @Test fun saveRejectsAtLimit() = runTest {
        val subs = (1L..1000L).map { sub(id = it, name = "S$it") }
        val env = TestEnv(appScope(), subs)
        env.startup.start()

        assertThat(env.save(netflix.newDraft(), 0)).isEqualTo(Outcome.Err(SaveError.LimitReached))
        assertThat(env.save(subs.first().toDraft().copy(name = "Edited"), 0))
            .isEqualTo(Outcome.Ok(SubscriptionId(1)))
        assertThat(env.repo.count()).isEqualTo(1000)
    }

    @Test fun saveOfVanishedIdIsMissing() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()

        val result = env.save(netflix.copy(id = SubscriptionId(7)).toDraft(), 0)

        assertThat(result).isEqualTo(Outcome.Err(SaveError.Missing))
        assertThat(env.repo.getAll()).containsExactly(netflix)
        assertThat(env.repo.upsertCount).isEqualTo(0)
    }

    /** P10: 편집 중 다른 곳에서 해지된 구독은 저장하지 않습니다(withAnchor의 require가 새지 않음). */
    @Test fun saveOfCancelledSubscriptionIsMissing() = runTest {
        val cancelled = netflix.copy(
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-10-01")))
        )
        val env = TestEnv(appScope(), listOf(cancelled))
        env.startup.start()

        val result = env.save(netflix.toDraft().copy(name = "Edited"), 0)

        assertThat(result).isEqualTo(Outcome.Err(SaveError.Missing))
        assertThat(env.repo.get(netflix.id)).isEqualTo(cancelled)
    }

    @Test fun saveStorageFailure() = runTest {
        val env = TestEnv(appScope())
        env.startup.start()
        env.repo.failNextUpsert = IOException("disk full")

        assertThat(env.save(netflix.newDraft(), 0)).isEqualTo(Outcome.Err(SaveError.StorageFailed))
    }

    @Test fun cancelBeforeFirstBilling() = runTest {
        val future = sub(1, anchor = "2026-12-01")
        val env = TestEnv(appScope(), listOf(future), now = at("2026-10-24T10:00+09:00"))
        env.startup.start()

        assertThat(env.cancel(future.id, 0)).isEqualTo(Outcome.Err(ActionError.NOT_STARTED))
        assertThat(env.repo.get(future.id)).isEqualTo(future)
    }

    @Test fun restoreAtPeriodLimit() = runTest {
        val start = d("2020-01-01")
        val periods = (0L until 50L).map {
            ActivePeriod(start.plusDays(it * 20), start.plusDays(it * 20 + 10))
        }
        val full = sub(1, anchor = "2020-01-01", periods = periods)
        val env = TestEnv(appScope(), listOf(full))
        env.startup.start()

        assertThat(env.restore(full.id, 0)).isEqualTo(Outcome.Err(ActionError.PERIOD_LIMIT))
        assertThat(env.repo.get(full.id)).isEqualTo(full)
    }

    @Test fun cancelOrRestoreInWrongStatusIsMissing() = runTest {
        val cancelled = netflix.copy(
            periods = listOf(ActivePeriod(d("2026-01-15"), d("2026-10-01")))
        )
        val active = sub(2, name = "Active")
        val env = TestEnv(appScope(), listOf(cancelled, active))
        env.startup.start()

        assertThat(env.cancel(cancelled.id, 0)).isEqualTo(Outcome.Err(ActionError.MISSING))
        assertThat(env.restore(active.id, 0)).isEqualTo(Outcome.Err(ActionError.MISSING))
        assertThat(env.delete(SubscriptionId(99), 0)).isEqualTo(Outcome.Err(ActionError.MISSING))
    }

    @Test fun writesBeforeReadyAreNotReady() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))

        assertThat(env.save(netflix.newDraft(), 0)).isEqualTo(Outcome.Err(SaveError.NotReady))
        assertThat(env.cancel(netflix.id, 0)).isEqualTo(Outcome.Err(ActionError.NOT_READY))
        assertThat(env.delete(netflix.id, 0)).isEqualTo(Outcome.Err(ActionError.NOT_READY))
        assertThat(env.updateSettings { it.copy(themeMode = ThemeMode.DARK) })
            .isEqualTo(Outcome.Err(ActionError.NOT_READY))
        assertThat(env.repo.getAll()).containsExactly(netflix)
    }

    @Test fun actionStorageFailureIsReported() = runTest {
        val env = TestEnv(appScope(), listOf(netflix))
        env.startup.start()
        env.repo.failNextDelete = IOException("disk")

        assertThat(env.delete(netflix.id, 0)).isEqualTo(Outcome.Err(ActionError.STORAGE_FAILED))
    }

    @Test fun updateSettingsWritesWhenReady() = runTest {
        val env = TestEnv(appScope())
        env.startup.start()

        assertThat(
            env.updateSettings {
                it.copy(themeMode = ThemeMode.DARK)
            }
        ).isEqualTo(Outcome.Ok(Unit))
        assertThat(env.settings.current.themeMode).isEqualTo(ThemeMode.DARK)
    }
}
