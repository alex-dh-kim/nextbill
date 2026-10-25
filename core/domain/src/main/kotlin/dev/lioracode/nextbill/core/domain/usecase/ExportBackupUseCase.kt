package dev.lioracode.nextbill.core.domain.usecase

import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.domain.repository.SettingsRepository
import dev.lioracode.nextbill.core.domain.repository.SubscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** 구독(사용 구간 포함)과 설정 전체를 백업 문자열로 만듭니다(ADR-7). */
class ExportBackupUseCase @Inject constructor(
    private val repo: SubscriptionRepository,
    private val settings: SettingsRepository,
    private val codec: BackupCodec
) {
    suspend operator fun invoke(): String =
        codec.encode(BackupSnapshot(repo.getAll(), settings.settings.first()))
}
