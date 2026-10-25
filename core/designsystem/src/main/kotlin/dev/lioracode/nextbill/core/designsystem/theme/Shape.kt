package dev.lioracode.nextbill.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object NbShapes {
    val Card = RoundedCornerShape(16.dp)
    val Chip = CircleShape
    val Button = RoundedCornerShape(12.dp)
}

internal val NextBillM3Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = NbShapes.Button,
    large = NbShapes.Card
)
