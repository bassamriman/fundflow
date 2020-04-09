package common

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Try
import java.time.LocalDateTime

data class DateTimeInterval(val from: LocalDateTime, val to: LocalDateTime)

object DateTimeIntervalAPI {

    fun infinite() : DateTimeInterval = DateTimeInterval(LocalDateTime.MIN, LocalDateTime.MAX)

    fun rightBounded(to: LocalDateTime): DateTimeInterval = DateTimeInterval(LocalDateTime.MIN, to)
    fun leftBounded(from: LocalDateTime): DateTimeInterval = DateTimeInterval(from, LocalDateTime.MAX)
    fun bounded(from: LocalDateTime, to: LocalDateTime): Try<DateTimeInterval> =
        if (from > to) Try.raiseError(IllegalArgumentException("From cannot be after To")) else Try.just(
            DateTimeInterval(from, to)
        )

    private fun unsafeBounded(from: LocalDateTime, to: LocalDateTime): DateTimeInterval = DateTimeInterval(from, to)

    private fun max(date1: LocalDateTime, date2: LocalDateTime): LocalDateTime = if (date1 > date2) date1 else date2
    private fun min(date1: LocalDateTime, date2: LocalDateTime): LocalDateTime = if (date1 > date2) date2 else date1

    operator fun DateTimeInterval.contains(localDateTime: LocalDateTime): Boolean =
        this.from <= localDateTime && localDateTime <= this.to

    infix fun DateTimeInterval.intersection(other: DateTimeInterval): Option<DateTimeInterval> =
        when {
            this.to >= other.from && this.from <= other.to ->
                when {
                    this.to <= other.to && other.from >= this.from -> Some(
                        unsafeBounded(
                            other.from,
                            this.to
                        )
                    )
                    this.to <= other.to && other.from < this.from -> Some(
                        unsafeBounded(
                            this.from,
                            this.to
                        )
                    )
                    this.to > other.to && other.from >= this.from -> Some(
                        unsafeBounded(
                            other.from,
                            other.to
                        )
                    )
                    this.to > other.to && other.from < this.from -> Some(
                        unsafeBounded(
                            this.from,
                            other.to
                        )
                    )
                    else -> None
                }
            else -> None
        }
}