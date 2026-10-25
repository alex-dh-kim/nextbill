package dev.lioracode.nextbill.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.lioracode.nextbill.core.designsystem.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

/** M3 슬롯에 없는 모바일용 큰 금액(display-currency-mobile 28/36). */
data class NextBillTypography(val amountLarge: TextStyle)

private fun token(size: Int, line: Int, weight: FontWeight, spacing: TextUnit) = TextStyle(
    fontFamily = Pretendard,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = spacing
)

private val BaseTypography = Typography()

private fun TextStyle.pretendard() = copy(fontFamily = Pretendard)

/** 모든 M3 슬롯에 Pretendard를 적용한 뒤, 매핑된 11개 슬롯만 시안 토큰으로 덮어씁니다. */
internal val NextBillM3Typography = Typography(
    displayLarge = BaseTypography.displayLarge.pretendard(),
    displayMedium = BaseTypography.displayMedium.pretendard(),
    headlineLarge = BaseTypography.headlineLarge.pretendard(),
    titleSmall = BaseTypography.titleSmall.pretendard(),

    displaySmall = token(32, 40, FontWeight.Bold, (-0.02).em), // display-currency
    headlineMedium = token(24, 32, FontWeight.Bold, (-0.01).em), // headline-lg
    headlineSmall = token(20, 28, FontWeight.Bold, (-0.01).em), // headline-md
    titleLarge = token(18, 24, FontWeight.SemiBold, 0.em), // title-lg
    titleMedium = token(16, 22, FontWeight.SemiBold, 0.em), // title-md
    bodyLarge = token(16, 24, FontWeight.Normal, 0.em), // body-lg
    bodyMedium = token(14, 20, FontWeight.Normal, 0.em), // body-md
    bodySmall = token(13, 18, FontWeight.Normal, 0.em), // body-sm
    labelLarge = token(14, 20, FontWeight.SemiBold, 0.01.em), // label-lg
    labelMedium = token(12, 16, FontWeight.SemiBold, 0.01.em), // label-md
    labelSmall = token(11, 14, FontWeight.Medium, 0.02.em) // label-sm
)

internal val DefaultNextBillTypography = NextBillTypography(
    amountLarge = token(28, 36, FontWeight.Bold, (-0.02).em) // display-currency-mobile
)
