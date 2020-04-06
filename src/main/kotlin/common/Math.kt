package common

import java.math.BigDecimal


//data class Frequency(occurrence : BigDecimal, )

data class Amount<out U : Unit>(val value: BigDecimal, val unit: U) {
    companion object {
        fun <U : Unit> zero(unit: U) = Amount(BigDecimal.ZERO, unit)
    }
}

object AmountOps {
    operator fun <U : Unit> Amount<U>.unaryMinus(): Amount<U> = this.copy(value = -value)
    operator fun <U : Unit> Amount<U>.plus(b: Amount<U>): Amount<U> {
        val a = this.copy(value = this.value + b.value)

        return a
    }
}