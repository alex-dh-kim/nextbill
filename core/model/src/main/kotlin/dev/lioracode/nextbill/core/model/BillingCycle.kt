package dev.lioracode.nextbill.core.model

/** 결제 주기. [interval]은 1..365입니다. */
sealed interface BillingCycle {
    val interval: Int

    data class Days(override val interval: Int) : BillingCycle {
        init {
            require(interval in INTERVAL_RANGE) { "interval must be in $INTERVAL_RANGE" }
        }
    }

    data class Weeks(override val interval: Int) : BillingCycle {
        init {
            require(interval in INTERVAL_RANGE) { "interval must be in $INTERVAL_RANGE" }
        }
    }

    data class Months(override val interval: Int) : BillingCycle {
        init {
            require(interval in INTERVAL_RANGE) { "interval must be in $INTERVAL_RANGE" }
        }
    }

    data class Years(override val interval: Int) : BillingCycle {
        init {
            require(interval in INTERVAL_RANGE) { "interval must be in $INTERVAL_RANGE" }
        }
    }

    companion object {
        val INTERVAL_RANGE = 1..365
    }
}
