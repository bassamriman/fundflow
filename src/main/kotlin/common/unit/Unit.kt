package common.unit

import common.bd
import java.math.BigDecimal

interface Unit

interface UnitToUnitConverter<S : Unit, T : Unit> {
    val rate: BigDecimal
    val source: S
    val target: T

    fun Amount<S>.convert(): Amount<T> = Amount(this.value / rate, target)

    operator fun <OT : Unit> plus(other: UnitToUnitConverter<T, OT>): UnitToUnitConverterImpl<S, OT> {
        return UnitToUnitConverterImpl(other.rate / rate, source, other.target)
    }

    fun inverse(): UnitToUnitConverter<T, S> = UnitToUnitConverterImpl(1.bd / rate, target, source)

    companion object {
        operator fun <S : Unit, T : Unit> invoke(rate: BigDecimal, source: S, target: T) =
            UnitToUnitConverterImpl(rate, source, target)
    }
}

data class UnitToUnitConverterImpl<S : Unit, T : Unit>(
    override val rate: BigDecimal,
    override val source: S,
    override val target: T
) : UnitToUnitConverter<S, T>
