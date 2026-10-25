package dev.lioracode.nextbill.core.database

import androidx.room.Room
import org.junit.After
import org.junit.Before
import org.robolectric.RuntimeEnvironment

abstract class DatabaseTest {
    protected lateinit var db: NextBillDatabase

    @Before
    fun openDb() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NextBillDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    protected fun sub(name: String = "Netflix", id: Long = 0) = SubscriptionEntity(
        id = id, name = name, priceMinor = 1_000, currency = "KRW", cycleUnit = "MONTH",
        cycleInterval = 1, anchorEpochDay = 19_000, category = "OTHER", colorKey = "blue",
        memo = "", reminderEnabled = true, reminderDaysBefore = 1, reminderMinuteOfDay = 540
    )

    protected fun period(start: Long, end: Long? = null) =
        ActivePeriodEntity(subscriptionId = 0, startEpochDay = start, endEpochDay = end)
}
