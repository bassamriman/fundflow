package common

import arrow.core.Try
import java.math.BigDecimal
import java.time.LocalDateTime


sealed class TimeFrequency : Unit
interface TimeIncrementer {
    fun LocalDateTime.increment(): LocalDateTime

    private tailrec fun LocalDateTime.incrementUntil(
        to: LocalDateTime,
        accumulatedIncrements: List<LocalDateTime>
    ): List<LocalDateTime> =
        if (this > to)
            accumulatedIncrements
        else {
            this.increment().incrementUntil(to, accumulatedIncrements + this)
        }

    operator fun LocalDateTime.rangeTo(b: LocalDateTime): Try<List<LocalDateTime>> =
        if (this > b)
            Try.raiseError(java.lang.IllegalStateException("From date cannot be after To date"))
        else {
            Try.just(this.incrementUntil(b, emptyList()))
        }

    fun DateTimeInterval.increment(): List<LocalDateTime> = this.from.incrementUntil(this.to, emptyList())

}

object Daily : TimeFrequency()
object DayIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusDays(1)
}

object Weekly : TimeFrequency()
object WeekIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusWeeks(1)
}

object BiWeekly : TimeFrequency()
object BiWeekIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusWeeks(2)
}

object SemiMonthly : TimeFrequency()
object SemiMonthIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusDays(30 / 2)
}

object Monthly : TimeFrequency()
object MonthIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusMonths(1)
}

object BiMonthly : TimeFrequency()
object BiMonthIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusMonths(2)
}


object Annually : TimeFrequency()
object YearIncrementer : TimeIncrementer {
    override fun LocalDateTime.increment(): LocalDateTime = this.plusYears(1)
}


object TimeFrequencyOps {
    val dailyToDailyConverter = ToDailyConverter(1.bd, Daily)
    val weeklyToDailyConverter = ToDailyConverter(7.bd, Weekly)
    val biWeeklyToDailyConverter = ToDailyConverter(14.bd, BiWeekly)
    val semiMonthlyToDailyConverter = ToDailyConverter(30.4375.bd.divide(2.bd), SemiMonthly)
    val monthlyToDailyConverter = ToDailyConverter(30.4375.bd, Monthly)
    val biMonthlyToDailyConverter = ToDailyConverter(30.4375.bd.multiply(2.bd), Monthly)
    val annuallyToDailyConverter = ToDailyConverter(365.bd, Annually)

    fun <TF : TimeFrequency> TF.converter(): ToDailyConverter<TF> =
        when (this) {
            is Daily -> dailyToDailyConverter
            is Weekly -> weeklyToDailyConverter
            is BiWeekly -> biWeeklyToDailyConverter
            is SemiMonthly -> semiMonthlyToDailyConverter
            is Monthly -> monthlyToDailyConverter
            is BiMonthly -> biMonthlyToDailyConverter
            is Annually -> annuallyToDailyConverter
            else -> throw IllegalStateException()
        } as ToDailyConverter<TF>

    fun TimeFrequency.incrementer(): TimeIncrementer =
        when (this) {
            is Daily -> DayIncrementer
            is Weekly -> WeekIncrementer
            is BiWeekly -> BiWeekIncrementer
            is SemiMonthly -> SemiMonthIncrementer
            is Monthly -> MonthIncrementer
            is BiMonthly -> BiMonthIncrementer
            is Annually -> YearIncrementer
        }


    fun <FU : TimeFrequency, TU : TimeFrequency> convert(amount: Amount<FU>, targetFrequency: TU): Amount<TU> {
        val sourceUnitToDailyUnitConverter: ToDailyConverter<FU> = amount.unit.converter()
        val destinationUnitToDailyUnitConverter: ToDailyConverter<TU> = targetFrequency.converter()
        val amountDaily = sourceUnitToDailyUnitConverter.run {
            amount.convert()
        }
        return destinationUnitToDailyUnitConverter.run {
            amountDaily.reverseConvert()
        }
    }

    infix fun <FU : TimeFrequency, TU : TimeFrequency> Amount<FU>.convertTo(targetFrequency: TU) =
        convert(this, targetFrequency)

}

data class ToDailyConverter<U : Unit>(override val factor: BigDecimal, override val unitToConvertFrom: U) :
    UnitToBaseConverter<U, Daily> {
    override val base: Daily = Daily
}
