package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.reminder.PlannedReminder
import dev.lioracode.nextbill.core.domain.validation.SubscriptionDraft
import dev.lioracode.nextbill.core.domain.validation.toDraft
import dev.lioracode.nextbill.core.model.Outcome
import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.SubscriptionId
import dev.lioracode.nextbill.core.model.UserSettings
import dev.lioracode.nextbill.core.testing.FakeBackupCodec
import dev.lioracode.nextbill.core.testing.FakeCurrentDateSource
import dev.lioracode.nextbill.core.testing.FakeDayChangeScheduler
import dev.lioracode.nextbill.core.testing.FakeImportStaging
import dev.lioracode.nextbill.core.testing.FakeReminderAlarm
import dev.lioracode.nextbill.core.testing.FakeReminderPoster
import dev.lioracode.nextbill.core.testing.FakeScheduledReminderRepository
import dev.lioracode.nextbill.core.testing.FakeSettingsRepository
import dev.lioracode.nextbill.core.testing.FakeSubscriptionRepository
import dev.lioracode.nextbill.core.testing.FakeWidgetUpdater
import dev.lioracode.nextbill.core.testing.MutableClock
import dev.lioracode.nextbill.core.testing.SEOUL
import dev.lioracode.nextbill.core.testing.at
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/** 기본 현재 시각: 2026-11-10 10:00 Asia/Seoul. */
val NOW: Instant = at("2026-11-10T10:00+09:00")

/**
 * 가짜들로 조립한 use case 묶음. 저장소·OS 쪽 가짜(알람·알림)는 [recreate] 뒤에도 남고,
 * 메모리 객체(coordinator·sync·refresher·startup)만 새로 만듭니다(프로세스 재생성).
 *
 * [scope]는 refresher의 앱 범위 scope입니다. 실제 스레드를 쓰는 테스트는 [idleScope]를 넘겨
 * request()가 실행되지 않게 하고 refreshNow()를 직접 부릅니다.
 */
class TestEnv(
    private val scope: CoroutineScope,
    subscriptions: List<Subscription> = emptyList(),
    records: List<PlannedReminder> = emptyList(),
    now: Instant = NOW,
    initialSettings: UserSettings = UserSettings.DEFAULT
) {
    val clock = MutableClock(now, SEOUL)
    val dateSource = FakeCurrentDateSource(LocalDate.ofInstant(now, SEOUL))
    val repo = FakeSubscriptionRepository(subscriptions)
    val settings = FakeSettingsRepository(initialSettings)
    val scheduled = FakeScheduledReminderRepository(records)
    val alarm = FakeReminderAlarm()
    val poster = FakeReminderPoster()
    val widget = FakeWidgetUpdater()
    val dayChange = FakeDayChangeScheduler()
    val codec = FakeBackupCodec()
    val staging = FakeImportStaging()

    lateinit var coordinator: DataCoordinator
        private set
    lateinit var sync: SyncRemindersUseCase
        private set
    lateinit var refresher: DerivedStateRefresher
        private set
    lateinit var startup: AppStartup
        private set

    init {
        recreate()
    }

    /** 프로세스 재생성. */
    fun recreate() {
        coordinator = DataCoordinator()
        sync = SyncRemindersUseCase(repo, scheduled, alarm, clock)
        refresher = DerivedStateRefresher(
            coordinator,
            sync,
            poster,
            widget,
            dayChange,
            dateSource,
            scope
        )
        startup = AppStartup(coordinator, repo, settings, codec, staging, sync, refresher)
    }

    val today: LocalDate get() = LocalDate.ofInstant(clock.instant(), SEOUL)

    fun setNow(instant: Instant) {
        clock.instant = instant
        dateSource.set(LocalDate.ofInstant(instant, SEOUL))
    }

    val deliver get() = DeliverDueRemindersUseCase(
        repo,
        scheduled,
        poster,
        coordinator,
        sync,
        refresher,
        clock
    )
    val save get() = SaveSubscriptionUseCase(repo, coordinator, refresher)
    val cancel get() = CancelSubscriptionUseCase(repo, coordinator, refresher, dateSource)
    val restore get() = RestoreSubscriptionUseCase(repo, coordinator, refresher, dateSource)
    val delete get() = DeleteSubscriptionUseCase(repo, coordinator, refresher)
    val updateSettings get() = UpdateSettingsUseCase(settings, coordinator)
    val export get() = ExportBackupUseCase(repo, settings, codec, coordinator)
    val import
        get() = ImportBackupUseCase(repo, settings, codec, staging, coordinator, sync, refresher)

    /** 수신기 순서: 재시도 알람을 먼저 걸고 처리합니다. */
    suspend fun fire(): Admission<Int> {
        sync.onAlarmFired()
        return deliver()
    }

    /** sync 없이 Ready로만 바꿉니다(기록을 직접 심은 처리 테스트용). */
    suspend fun markReady() {
        coordinator.recovery { setState(StartupState.Ready) }
    }

    /** 이 스냅샷을 가져오기 대상으로 둡니다. */
    fun prepare(snapshot: BackupSnapshot) {
        codec.decodeResult = Outcome.Ok(snapshot)
    }

    /** 새 env에서: staging에 [snapshot]이 남았고 적용이 두 번 실패해 RecoveryFailed가 됩니다. */
    suspend fun startIntoRecoveryFailed(snapshot: BackupSnapshot) {
        prepare(snapshot)
        staging.content = "fake-backup:${snapshot.subscriptions.size}"
        repo.failNextReplaceAll = IOException("disk")
        repo.failNextReplaceAllTimes = 2
        startup.start()
        check(coordinator.state.value == StartupState.RecoveryFailed)
    }
}

/** 실제 스레드 테스트용: 아무 것도 실행하지 않는 scope(request()가 쌓이기만 함). */
fun idleScope(): CoroutineScope = CoroutineScope(Job() + StandardTestDispatcher())

/** 새 구독 입력(id 0). */
fun Subscription.newDraft(): SubscriptionDraft = copy(id = SubscriptionId(0)).toDraft()

/** 가져오기가 id를 새로 매기므로 id를 빼고 비교합니다. */
fun List<Subscription>.withoutIds(): List<Subscription> = map { it.copy(id = SubscriptionId(0)) }

fun reminderOf(id: Long, billing: String, trigger: String) =
    PlannedReminder(SubscriptionId(id), LocalDate.parse(billing), at(trigger))

/**
 * 앱 범위 scope 대용. backgroundScope의 작업은 advanceUntilIdle이 기다리지 않으므로, 같은 스케줄러의
 * 일반(foreground) 디스패처에 테스트 Job과 분리된 Job을 씁니다.
 */
fun TestScope.appScope(): CoroutineScope =
    CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
