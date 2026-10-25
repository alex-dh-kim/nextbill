package dev.lioracode.nextbill.core.domain.effect

import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.ShownReminder
import java.time.LocalDate

/**
 * [show]·[pruneExpired]는 coordinator 잠금 안에서만 부릅니다(Task 8). 각자 읽고-고치고-게시하므로
 * 따로 부르면 서로의 결과를 덮습니다.
 */
interface ReminderPoster {
    fun canPost(): Boolean

    /** 지금 떠 있는 이 앱 알림의 항목. 사용자가 지운 것은 없습니다. */
    fun shown(): List<ShownReminder>

    /**
     * 화면의 앱 알림을 [posts]와 같게 맞춥니다. [posts]에 없는 tag는 취소하고, 새 항목이 든 알림만
     * 소리와 함께 게시하고, 이미 보였던 항목만 든 알림은 조용히(setSilent) 게시하고, 그대로인 것은
     * 다시 게시하지 않습니다.
     */
    fun show(posts: List<ReminderPost>, today: LocalDate)

    /**
     * 결제일이 [today]보다 이른 Single은 취소하고, Summary는 지난 항목을 빼고 조용히 다시 게시합니다
     * (0건이면 취소).
     */
    fun pruneExpired(today: LocalDate)
}
