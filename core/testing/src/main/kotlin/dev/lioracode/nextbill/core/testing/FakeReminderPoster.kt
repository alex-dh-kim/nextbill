package dev.lioracode.nextbill.core.testing

import dev.lioracode.nextbill.core.domain.effect.ReminderPoster
import dev.lioracode.nextbill.core.domain.reminder.DueLabel
import dev.lioracode.nextbill.core.domain.reminder.ReminderPost
import dev.lioracode.nextbill.core.domain.reminder.ShownReminder
import dev.lioracode.nextbill.core.domain.reminder.dueLabel
import dev.lioracode.nextbill.core.domain.reminder.shownItems
import dev.lioracode.nextbill.core.domain.reminder.tag
import dev.lioracode.nextbill.core.model.SubscriptionId
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * 화면의 앱 알림을 tag([ReminderPost.tag])로 구분해 [active]에 두는 가짜. 상태 읽기·쓰기는 한 번에
 * 하나씩이지만, 읽고-고치고-게시하는 전체 묶음은 원자적이지 않습니다(진짜와 같음, 호출자가 잠금).
 *
 * - [posted]: 성공한 모든 게시(조용한 것 포함). [silentPosts]: 그중 조용히 게시한 것.
 * - 진짜처럼 보이는 내용으로 비교합니다: 같은 post라도 Single의 남은 날 문구(오늘·내일·D-n)가 [show]의
 *   today로 달라졌으면 다시 게시합니다(항목이 같으니 조용히). Summary 문구는 today와 무관합니다.
 * - [maxActive]개가 떠 있을 때 새 tag의 게시는 OS처럼 조용히 거부하고 [rejected]에 남깁니다.
 * - [pruneGate]는 [pruneExpired]가 활성 알림을 읽은 뒤 멈추는 지점입니다. [pruneExpired]는 suspend가
 *   아니라서 호출한 스레드를 막습니다(runBlocking). 테스트 디스패처의 유일한 스레드에서 부르면 풀 수
 *   없으니 다른 스레드에서 실행하고 [pruneEntered]로 도착을 기다린 뒤 `pruneGate.complete(Unit)`로 풉니다.
 */
class FakeReminderPoster(
    @Volatile var canPost: Boolean = true,
    @Volatile var failAfterPost: Throwable? = null,
    val maxActive: Int = 50,
    @Volatile var pruneGate: CompletableDeferred<Unit>? = null
) : ReminderPoster {
    private val postedList = mutableListOf<ReminderPost>()
    private val activeMap = linkedMapOf<String, ReminderPost>()

    /** tag → 게시할 때의 남은 날 문구(Summary는 null). 진짜의 렌더링 내용 비교를 흉내 냅니다. */
    private val labelMap = HashMap<String, DueLabel?>()
    private val rejectedList = mutableListOf<ReminderPost>()
    private val silentList = mutableListOf<ReminderPost>()
    private val cancelledList = mutableListOf<String>()

    /** 아래 컬렉션은 호출 시점의 복사본입니다(동시 테스트에서 안전). */
    val posted: List<ReminderPost> get() = synchronized(this) { postedList.toList() }
    val active: Map<String, ReminderPost> get() = synchronized(this) { activeMap.toMap() }
    val rejected: List<ReminderPost> get() = synchronized(this) { rejectedList.toList() }
    val silentPosts: List<ReminderPost> get() = synchronized(this) { silentList.toList() }
    val cancelledTags: List<String> get() = synchronized(this) { cancelledList.toList() }

    /** [pruneExpired]가 [pruneGate]에 도착하면 완료됩니다. 한 번만 완료되니 다시 쓰려면 [resetEntered]를 부릅니다. */
    @Volatile var pruneEntered: CompletableDeferred<Unit> = CompletableDeferred()

    fun resetEntered() {
        pruneEntered = CompletableDeferred()
    }

    override fun canPost(): Boolean = canPost

    @Synchronized
    override fun shown(): List<ShownReminder> = activeMap.values.flatMap { it.shownItems }

    @Synchronized
    override fun show(posts: List<ReminderPost>, today: LocalDate) {
        if (!canPost) return
        val alreadyShown = activeMap.values.flatMap { it.shownItems }.toSet()
        val wanted = posts.associateBy { it.tag }
        activeMap.keys.filter { it !in wanted }.forEach {
            activeMap.remove(it)
            cancelledList += it
        }
        posts.forEach { post ->
            val tag = post.tag
            val existing = activeMap[tag]
            val label = labelOf(post, today)
            if (existing == post && labelMap[tag] == label) return@forEach
            if (existing == null && activeMap.size >= maxActive) {
                rejectedList += post
                return@forEach
            }
            val silent = post.shownItems.all { it in alreadyShown }
            place(tag, post, silent, label)
            failAfterPost?.let {
                failAfterPost = null
                throw it
            }
        }
    }

    override fun pruneExpired(today: LocalDate, showable: Set<SubscriptionId>) {
        val snapshot = synchronized(this) { activeMap.toMap() }
        pruneGate?.let {
            pruneEntered.complete(Unit)
            runBlocking { withTimeout(GATE_TIMEOUT) { it.await() } }
        }
        synchronized(this) {
            snapshot.forEach { (tag, post) ->
                when (post) {
                    is ReminderPost.Single ->
                        if (post.billingDate < today || post.subscription.id !in showable) {
                            activeMap.remove(tag)
                            cancelledList += tag
                        }

                    is ReminderPost.Summary -> {
                        val kept = post.items.filter {
                            it.second >= today && it.first.id in showable
                        }
                        when {
                            kept.isEmpty() -> {
                                activeMap.remove(tag)
                                cancelledList += tag
                            }

                            kept.size != post.items.size -> place(
                                tag,
                                ReminderPost.Summary(kept),
                                silent = true,
                                label = null
                            )
                        }
                    }
                }
            }
        }
    }

    /** 사용자가 지운 것처럼 [tag]의 알림을 없앱니다. */
    @Synchronized
    fun dismiss(tag: String) {
        activeMap.remove(tag)
    }

    private fun labelOf(post: ReminderPost, today: LocalDate): DueLabel? = when (post) {
        is ReminderPost.Single -> dueLabel(maxOf(post.billingDate, today), today)
        is ReminderPost.Summary -> null
    }

    private fun place(tag: String, post: ReminderPost, silent: Boolean, label: DueLabel?) {
        activeMap[tag] = post
        labelMap[tag] = label
        postedList += post
        if (silent) silentList += post
    }
}
