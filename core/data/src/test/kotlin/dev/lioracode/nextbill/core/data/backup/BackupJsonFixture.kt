package dev.lioracode.nextbill.core.data.backup

/** 유효한 백업 JSON 한 건을 문자열로 만들고, 필드 조각을 바꿔 끼울 수 있게 합니다. */
object BackupJsonFixture {
    @Suppress("LongParameterList")
    fun valid(
        name: String = "Netflix",
        priceMinor: String = "17000",
        currency: String = "KRW",
        unit: String = "MONTHS",
        interval: String = "1",
        anchorDate: String = "2026-01-15",
        category: String = "VIDEO",
        memo: String = "",
        daysBefore: String = "1",
        time: String = "09:00",
        periods: String = """[{"start":"2026-01-15","end":null}]""",
        settingsReminder: String = """{"enabled":true,"daysBefore":1,"time":"09:00"}""",
        defaultCurrency: String = "KRW",
        subscriptionCount: Int = 1
    ): String {
        val one = """{"name":"$name","priceMinor":$priceMinor,"currency":"$currency",""" +
            """"cycle":{"unit":"$unit","interval":$interval},"anchorDate":"$anchorDate",""" +
            """"category":"$category","colorKey":"RED","memo":"$memo",""" +
            """"reminder":{"enabled":true,"daysBefore":$daysBefore,"time":"$time"},"periods":$periods}"""
        return """{"schemaVersion":1,"exportedAt":"2026-11-10T01:00:00Z",""" +
            """"subscriptions":[${List(subscriptionCount) { one }.joinToString(",")}],""" +
            """"settings":{"defaultReminder":$settingsReminder,"themeMode":"SYSTEM",""" +
            """"useDynamicColor":false,"defaultCurrency":"$defaultCurrency"}}"""
    }

    fun period(start: String, end: String?) = """{"start":"$start","end":${end?.let {
        "\"$it\""
    }}}"""

    fun periods(vararg p: String) = p.joinToString(",", "[", "]")
}
