package dev.lioracode.nextbill

/** 설계 문서 §3.1, 계획서 Global Constraints. */
object AndroidSdk {
    const val COMPILE = 37
    const val TARGET = 36
    const val MIN = 26

    /** Robolectric sandbox SDK. JDK 17에서 돌 수 있는 최고 SDK입니다(ADR-0011). */
    const val ROBOLECTRIC = 34
}
