package common

import java.math.BigDecimal


object UnitToUnitConverter {
    fun <FU : Unit, TU : Unit, B : Unit> convert(
        fromAmount: Amount<FU>,
        to: TU,
        fromConverter: UnitToBaseConverter<FU, B>,
        toConverter: UnitToBaseConverter<TU, B>
    ): Amount<TU> {
        val amountAsB: Amount<B> = fromConverter.run { fromAmount.convert() }
        val amountAsTU = toConverter.run {
            amountAsB.reverseConvert()
        }
        return amountAsTU
    }
}

interface UnitToBaseConverter<U : Unit, B : Unit> {
    val factor: BigDecimal
    val unitToConvertFrom: U
    val base: B
    fun ratioWithUnit(): Amount<Div<B, U>> = Amount(factor, Div.of(base, unitToConvertFrom))

    fun Amount<U>.convert(): Amount<B> = Amount(this.value/factor, base)
    fun Amount<B>.reverseConvert(): Amount<U> = Amount( factor * this.value, unitToConvertFrom)
}