package common.unit

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import common.DateTimeInterval
import common.ValueWithError
import common.ValueWithError.Companion.ve
import common.bd
import java.time.LocalDateTime

sealed class TimeFrequency(val name: String, val perAlias: String) : Unit {
    companion object {
        fun all(): List<TimeFrequency> =
            listOf(Daily, Weekly, BiWeekly, SemiMonthly, Monthly, BiMonthly, Annually)

        fun byName(name: String): Option<TimeFrequency> = all().find { it.name == name }.toOption()
    }
}

object Daily : TimeFrequency("Daily", "/ Day")
object Weekly : TimeFrequency("Weekly", "/ Week")
object BiWeekly : TimeFrequency("BiWeekly", "/ Two Weeks")
object SemiMonthly : TimeFrequency("SemiMonthly", "/ Half Month")
object Monthly : TimeFrequency("Monthly", "/ Month")
object BiMonthly : TimeFrequency("BiMonthly", "/ Two Month")
object Annually : TimeFrequency("Annually", "/ Year")

data class DateTimeIncrementer(val increment: (s: LocalDateTime) -> LocalDateTime) {
    private fun LocalDateTime.increment(): LocalDateTime = increment(this)

    operator fun LocalDateTime.rangeTo(b: LocalDateTime): ValueWithError<Option<List<LocalDateTime>>> =
        if (this > b)
            None.ve()
        else {
            this.incrementUntil(b, emptyList()).toOption().ve()
        }

    fun DateTimeInterval.increment(): List<LocalDateTime> =
        this.from.incrementUntil(this.to, emptyList())

    private tailrec fun LocalDateTime.incrementUntil(
        to: LocalDateTime,
        accumulatedIncrements: List<LocalDateTime>
    ): List<LocalDateTime> =
        if (this > to)
            accumulatedIncrements
        else {
            this.increment().incrementUntil(to, accumulatedIncrements + this)
        }
}

object TimeFrequencyOps {
    fun <U : TimeFrequency> U.toDailyConverter(): UnitToUnitConverterImpl<U, Daily> =
        when (this) {
            is Daily -> UnitToUnitConverter(1.bd, this, Daily)
            is Weekly -> UnitToUnitConverter(7.bd, this, Daily)
            is BiWeekly -> UnitToUnitConverter(14.bd, this, Daily)
            is SemiMonthly -> UnitToUnitConverter(30.4375.bd.divide(2.bd), this, Daily)
            is Monthly -> UnitToUnitConverter(30.4375.bd, this, Daily)
            is BiMonthly -> UnitToUnitConverter(30.4375.bd.multiply(2.bd), this, Daily)
            is Annually -> UnitToUnitConverter(365.bd, this, Daily)
            else -> throw IllegalStateException("No converter setup")
        }

    fun TimeFrequency.incrementer(): DateTimeIncrementer =
        when (this) {
            is Daily -> DateTimeIncrementer { it.plusDays(1) }
            is Weekly -> DateTimeIncrementer { it.plusWeeks(1) }
            is BiWeekly -> DateTimeIncrementer { it.plusWeeks(2) }
            is SemiMonthly -> DateTimeIncrementer { it.plusDays(30 / 2) }
            is Monthly -> DateTimeIncrementer { it.plusMonths(1) }
            is BiMonthly -> DateTimeIncrementer { it.plusMonths(2) }
            is Annually -> DateTimeIncrementer { it.plusYears(1) }
        }

    fun <FU : TimeFrequency, TU : TimeFrequency> convert(
        amount: Amount<FU>,
        targetFrequency: TU
    ): Amount<TU> =
        (amount.unit.toDailyConverter() + targetFrequency.toDailyConverter().inverse()).run {
            amount.convert()
        }

    infix fun <FU : TimeFrequency, TU : TimeFrequency> Amount<FU>.convertTo(targetFrequency: TU) =
        convert(this, targetFrequency)
}
