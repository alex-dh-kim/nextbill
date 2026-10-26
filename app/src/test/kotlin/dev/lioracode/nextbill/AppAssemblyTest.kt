package dev.lioracode.nextbill

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.core.notifications.REMINDER_CHANNEL_ID
import dev.lioracode.nextbill.core.notifications.ReminderReceiver
import dev.lioracode.nextbill.core.notifications.RescheduleReceiver
import dev.lioracode.nextbill.widget.MidnightRefreshReceiver
import dev.lioracode.nextbill.widget.NextBillWidgetReceiver
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 합쳐진 manifest와 Hilt 그래프를 실제 NextBillApplication으로 확인합니다. */
@RunWith(RobolectricTestRunner::class)
class AppAssemblyTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun applicationIsHiltApp() {
        assertThat(app).isInstanceOf(NextBillApplication::class.java)
    }

    @Test
    fun mergedManifestRegistersReceivers() {
        listOf(
            ReminderReceiver::class.java,
            RescheduleReceiver::class.java,
            MidnightRefreshReceiver::class.java,
            NextBillWidgetReceiver::class.java
        ).forEach { receiver ->
            // 등록되지 않았으면 NameNotFoundException을 던집니다.
            val info = app.packageManager.getReceiverInfo(ComponentName(app, receiver), 0)
            assertThat(info.name).isEqualTo(receiver.name)
        }
    }

    @Test
    fun receiversAreInjected() {
        // 처리하지 않는 액션: 주입만 하고 바로 돌아옵니다.
        val ignored = Intent("dev.lioracode.nextbill.test.IGNORED")

        val reminder = ReminderReceiver().apply { onReceive(app, ignored) }
        assertThat(listOf(reminder.sync, reminder.deliver, reminder.refresher, reminder.scope))
            .doesNotContain(null)

        val reschedule = RescheduleReceiver().apply { onReceive(app, ignored) }
        assertThat(listOf(reschedule.refresher, reschedule.scope)).doesNotContain(null)

        val midnight = MidnightRefreshReceiver().apply { onReceive(app, ignored) }
        assertThat(
            listOf(midnight.refresher, midnight.dayChange, midnight.dateSource, midnight.scope)
        ).doesNotContain(null)
    }

    @Test
    fun applicationCreatesNotificationChannel() {
        val manager = app.getSystemService(NotificationManager::class.java)
        assertThat(manager.getNotificationChannel(REMINDER_CHANNEL_ID)).isNotNull()
    }
}
