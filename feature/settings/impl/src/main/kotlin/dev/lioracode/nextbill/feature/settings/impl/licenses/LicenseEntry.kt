package dev.lioracode.nextbill.feature.settings.impl.licenses

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 오픈소스 고지 한 줄(ADR-10). [artifact]는 `group:name`이고, 전이 의존성 묶음은 줄마다 하나씩입니다.
 * [license]는 SPDX 식별자로 [LicenseCatalog.texts]의 전문을 가리킵니다. [notice]는 NOTICE 파일 내용입니다.
 */
@Serializable
data class LicenseEntry(
    val name: String,
    val artifact: String,
    val license: String,
    val url: String,
    val notice: String? = null
)

/** `res/raw/licenses.json` 전체. */
@Serializable
internal data class LicenseCatalog(val entries: List<LicenseEntry>, val texts: Map<String, String>)

private val json = Json { ignoreUnknownKeys = false }

internal fun parseLicenseCatalog(text: String): LicenseCatalog = json.decodeFromString(text)
