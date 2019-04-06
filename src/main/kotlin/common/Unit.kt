package common

interface Unit

object UnitArithmeticOperations {
    operator fun <U : Unit> U.times(other: NoUnit): U = this
    operator fun <U : Unit, OU : Unit> U.times(other: OU): Times<U, OU> =
        Times.of(this, other)
    operator fun <U : Unit, OU : Unit> U.div(other: OU): Div<U, OU> =
        Div.of(this, other)
    operator fun <U : Unit> U.div(other: NoUnit): U = this
    operator fun <U : Unit, OU : Unit> U.plus(other: OU): Plus<U, OU> =
        Plus.of(this, other)
    operator fun <U : Unit, OU : Unit> U.minus(other: OU): Minus<U, OU> =
        Minus.of(this, other)
}

object NoUnitArithmeticOperations {
    operator fun <OU : Unit> NoUnit.times(other: OU): OU = other
    operator fun NoUnit.times(other: NoUnit): NoUnit = NoUnit
    operator fun <OU : Unit> NoUnit.div(other: OU): Div<NoUnit, OU> =
        Div.of(NoUnit, other)
    operator fun NoUnit.div(other: NoUnit): NoUnit = NoUnit
    operator fun <OU : Unit> NoUnit.plus(other: OU): Plus<NoUnit, OU> =
        Plus.of(NoUnit, other)
    operator fun NoUnit.plus(other: NoUnit): NoUnit = NoUnit
    operator fun <OU : Unit> NoUnit.minus(other: OU): Minus<NoUnit, OU> =
        Minus.of(NoUnit, other)
    operator fun NoUnit.minus(other: NoUnit): NoUnit = NoUnit
}

object UnitSelfOperations {
    operator fun <U : Unit> U.div(other: U): NoUnit = NoUnit
    operator fun <U : Unit> U.plus(other: U): U = other
    operator fun <U : Unit> U.minus(other: U): U = other
}

object NoUnit : Unit

interface UnitOperation<U1 : Unit, U2 : Unit> : Unit {
    val unit1: U1
    val unit2: U2
}

object DivOperations {
    operator fun <U1 : Unit, U2 : Unit, D : Div<U1, U2>, OU1 : Unit, OU2 : Unit> D.times(other: Div<OU1, OU2>): Div<Times<U1, OU1>, Times<U2, OU2>> =
        Div.of(
            Times.of(this.unit1, other.unit1),
            Times.of(this.unit2, other.unit2)
        )

    operator fun <U1 : Unit, U2 : Unit, D : Div<U1, U2>> D.times(other: Div<U2, U1>): NoUnit =
        NoUnit

    infix fun <U1 : Unit, U2 : Unit, D : Div<U1, U2>, OU : Unit> D.xdec(other: Div<OU, U1>): Div<OU, U2> {
        val operand1 = this
        return UnitOperations.run {
            other.unit1 / operand1.unit2
        }
    }

    infix fun <U1 : Unit, U2 : Unit, D : Div<U1, U2>, OU : Unit> D.xinc(other: Div<U2, OU>): Div<U1, OU> {
        val operand1 = this
        return UnitOperations.run {
            operand1.unit1 / other.unit2
        }
    }
}

interface Div<U1 : Unit, U2 : Unit> : Unit, UnitOperation<U1, U2> {
    companion object {
        fun <U1 : Unit, U2 : Unit> of(unit1: U1, unit2: U2): Div<U1, U2> =
            DivImpl(unit1, unit2)
    }
}

interface Times<U1 : Unit, U2 : Unit> : Unit, UnitOperation<U1, U2> {
    companion object {
        fun <U1 : Unit, U2 : Unit> of(unit1: U1, unit2: U2): Times<U1, U2> =
            TimesImpl(unit1, unit2)
    }
}

interface Plus<U1 : Unit, U2 : Unit> : Unit, UnitOperation<U1, U2> {
    companion object {
        fun <U1 : Unit, U2 : Unit> of(unit1: U1, unit2: U2): Plus<U1, U2> =
            PlusImpl(unit1, unit2)
    }
}

interface Minus<U1 : Unit, U2 : Unit> : Unit, UnitOperation<U1, U2> {
    companion object {
        fun <U1 : Unit, U2 : Unit> of(unit1: U1, unit2: U2): Minus<U1, U2> =
            MinusImpl(unit1, unit2)
    }
}

data class DivImpl<U1 : Unit, U2 : Unit>(override val unit1: U1, override val unit2: U2) :
    Div<U1, U2>
data class TimesImpl<U1 : Unit, U2 : Unit>(override val unit1: U1, override val unit2: U2) :
    Times<U1, U2>
data class PlusImpl<U1 : Unit, U2 : Unit>(override val unit1: U1, override val unit2: U2) :
    Plus<U1, U2>
data class MinusImpl<U1 : Unit, U2 : Unit>(override val unit1: U1, override val unit2: U2) :
    Minus<U1, U2>


object UnitOperations {
    operator fun <U1 : Unit, U2 : Unit> U1.div(unit2: U2): Div<U1, U2> =
        Div.of(this, unit2)
    operator fun <U1 : Unit, U2 : Unit> U1.times(unit2: U2): Times<U1, U2> =
        Times.of(this, unit2)
    operator fun <U1 : Unit, U2 : Unit> U1.plus(unit2: U2): Plus<U1, U2> =
        Plus.of(this, unit2)
    operator fun <U1 : Unit, U2 : Unit> U1.minus(unit2: U2): Minus<U1, U2> =
        Minus.of(this, unit2)
}





