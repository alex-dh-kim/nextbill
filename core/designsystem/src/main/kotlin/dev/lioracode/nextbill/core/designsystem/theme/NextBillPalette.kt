package dev.lioracode.nextbill.core.designsystem.theme

import androidx.compose.material3.ColorScheme

/**
 * 앱 밖의 표면(Glance 위젯)이 같은 색 값을 쓰기 위한 읽기 전용 입구입니다. 값 자체는 모듈 안에 둡니다.
 */
object NextBillPalette {
    fun scheme(dark: Boolean): ColorScheme = if (dark) DarkColorScheme else LightColorScheme

    fun colors(dark: Boolean): NextBillColors =
        if (dark) DarkNextBillColors else LightNextBillColors
}
