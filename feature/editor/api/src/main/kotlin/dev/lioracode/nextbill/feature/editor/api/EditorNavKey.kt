package dev.lioracode.nextbill.feature.editor.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 구독 편집. [subscriptionId]가 없으면 새 구독을 등록합니다. */
@Serializable
data class EditorNavKey(val subscriptionId: Long? = null) : NavKey
