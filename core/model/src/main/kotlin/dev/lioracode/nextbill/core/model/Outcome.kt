package dev.lioracode.nextbill.core.model

/** 직접 정의한 결과 타입. 검증 실패 같은 예상된 오류를 값으로 돌려줍니다. */
sealed interface Outcome<out E, out A> {
    data class Ok<A>(val value: A) : Outcome<Nothing, A>

    data class Err<E>(val error: E) : Outcome<E, Nothing>
}
