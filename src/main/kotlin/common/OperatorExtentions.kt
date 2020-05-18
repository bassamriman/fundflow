package common

import java.math.BigDecimal
import java.math.RoundingMode

val Int.bd: BigDecimal
    get() = BigDecimal(this).setScale(3, RoundingMode.HALF_UP)

val Double.bd: BigDecimal
    get() = BigDecimal(this).setScale(3, RoundingMode.HALF_UP)
