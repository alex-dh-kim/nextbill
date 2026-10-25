package dev.lioracode.nextbill

private const val MODEL = ":core:model"
private const val DOMAIN = ":core:domain"
private const val DATA = ":core:data"
private val STORAGE = setOf(":core:database", ":core:datastore")
private val FEATURE_IMPL = Regex("""^:feature:([^:]+):impl$""")
private val TEST_PREFIXES = listOf("test", "androidTest", "kspTest")

/**
 * 모듈 의존 규칙(설계 문서 §5.2) 위반 목록을 돌려줍니다.
 * [edges]의 키는 모듈 경로, 값은 그 모듈이 의존하는 프로젝트 경로들입니다.
 * 한 의존 관계(edge)는 아래 순서에서 처음 걸리는 규칙 하나로만 보고합니다.
 */
fun violations(edges: Map<String, Set<String>>): List<String> =
    edges.entries.sortedBy { it.key }.flatMap { (from, targets) ->
        targets.sorted().mapNotNull { to -> reasonFor(from, to)?.let { "$from -> $to ($it)" } }
    }

private fun reasonFor(from: String, to: String): String? {
    val fromImpl = FEATURE_IMPL.matchEntire(from)
    val toImpl = FEATURE_IMPL.matchEntire(to)
    return when {
        from == MODEL -> "core:model must have no project dependencies"

        from == DOMAIN && to != MODEL -> "core:domain may depend only on core:model"

        fromImpl != null && toImpl != null -> "feature impl must depend only on other features' api"

        from.startsWith(":feature:") && to == DATA ->
            "features must not depend on core:data; app assembles it"

        to in STORAGE && from != DATA ->
            "only core:data may depend on core:database and core:datastore"

        else -> null
    }
}

/** 이름이 `test`·`androidTest`·`kspTest`로 시작하지 않는 의존 설정만 검사합니다. */
fun isCheckedConfiguration(name: String): Boolean = TEST_PREFIXES.none { name.startsWith(it) }
