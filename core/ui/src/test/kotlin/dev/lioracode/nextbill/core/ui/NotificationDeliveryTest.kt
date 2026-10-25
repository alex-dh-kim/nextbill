package dev.lioracode.nextbill.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationDeliveryTest {
    private fun d(
        sdk: Int = 34,
        granted: Boolean = true,
        appEnabled: Boolean = true,
        channelBlocked: Boolean = false,
        requestedBefore: Boolean = false,
        rationale: Boolean = false
    ) = notificationDelivery(sdk, granted, appEnabled, channelBlocked, requestedBefore, rationale)

    @Test fun api26AppNotificationsOff() = assertThat(
        d(sdk = 26, appEnabled = false)
    ).isEqualTo(NotificationDelivery.BlockedInSettings)

    @Test fun api33First() = assertThat(
        d(sdk = 33, granted = false)
    ).isEqualTo(NotificationDelivery.NeedsPermission(true))

    @Test fun deniedOnceWithRationale() = assertThat(
        d(sdk = 33, granted = false, requestedBefore = true, rationale = true)
    ).isEqualTo(NotificationDelivery.NeedsPermission(true))

    @Test fun requestedWithoutRationale() = assertThat(
        d(sdk = 33, granted = false, requestedBefore = true)
    ).isEqualTo(NotificationDelivery.NeedsPermission(false))

    @Test fun grantedChannelBlocked() =
        assertThat(d(channelBlocked = true)).isEqualTo(NotificationDelivery.BlockedInSettings)

    @Test fun granted() = assertThat(d()).isEqualTo(NotificationDelivery.Available)

    @Test fun preApi33IgnoresPermissionFlag() =
        assertThat(d(sdk = 32, granted = false)).isEqualTo(NotificationDelivery.Available)

    @Test fun api33GrantedAppNotificationsOff() = assertThat(
        d(sdk = 33, appEnabled = false)
    ).isEqualTo(NotificationDelivery.BlockedInSettings)

    @Test fun api33DeniedChannelBlockedStillNeedsPermission() = assertThat(
        d(sdk = 33, granted = false, channelBlocked = true)
    ).isEqualTo(NotificationDelivery.NeedsPermission(true))
}
