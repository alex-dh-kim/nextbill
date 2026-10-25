package dev.lioracode.nextbill.feature.detail.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 구독 상세. 해지한 구독이면 읽기 전용으로 보여 줍니다. */
@Serializable
data class DetailNavKey(val subscriptionId: Long) : NavKey
