package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.backup.BackupCodec
import dev.lioracode.nextbill.core.domain.backup.BackupError
import dev.lioracode.nextbill.core.domain.backup.BackupSnapshot
import dev.lioracode.nextbill.core.model.Outcome

/** [decode]는 [decodeResult]를 그대로 돌려줍니다. [encode]는 마지막 입력을 [lastEncoded]에 남깁니다. */
class FakeBackupCodec(
    var decodeResult: Outcome<BackupError, BackupSnapshot> = Outcome.Err(BackupError.Malformed),
    var lastEncoded: BackupSnapshot? = null
) : BackupCodec {
    var lastDecodedInput: String? = null

    override fun encode(snapshot: BackupSnapshot): String {
        lastEncoded = snapshot
        return "fake-backup:${snapshot.subscriptions.size}"
    }

    override fun decode(json: String): Outcome<BackupError, BackupSnapshot> {
        lastDecodedInput = json
        return decodeResult
    }
}
