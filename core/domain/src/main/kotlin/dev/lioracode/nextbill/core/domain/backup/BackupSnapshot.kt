package dev.lioracode.nextbill.core.domain.backup

import dev.lioracode.nextbill.core.model.Subscription
import dev.lioracode.nextbill.core.model.UserSettings

data class BackupSnapshot(val subscriptions: List<Subscription>, val settings: UserSettings)
