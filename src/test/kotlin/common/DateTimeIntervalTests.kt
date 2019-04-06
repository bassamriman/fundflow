package common

import arrow.core.getOrElse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec

class DateTimeIntervalGen : Gen<DateTimeInterval> {

    private val localDateTimeGen = Gen.localDateTime(1900, 2200)

    override fun constants(): Iterable<DateTimeInterval> = emptyList()

    override fun random(): Sequence<DateTimeInterval> = generateSequence {
        val from = localDateTimeGen.random().first()
        val to = localDateTimeGen.random().filter { it >= from }.first()
        DateTimeInterval(from, to)
    }
}

interface RelativePosition {
    fun DateTimeInterval.generate(): Sequence<DateTimeInterval>
}

object Before : RelativePosition {
    override fun DateTimeInterval.generate(): Sequence<DateTimeInterval> =
        DateTimeIntervalGen().random().filter { interval -> interval.to > from && interval.to > to && interval.from > from && interval.from > to }
}

object LeftPartialOverlap : RelativePosition {
    override fun DateTimeInterval.generate(): Sequence<DateTimeInterval> =
        DateTimeIntervalGen().random().filter { interval -> interval.to in from..to && interval.from < from && interval.from < to }
}

object UnderFullOverlap : RelativePosition {
    override fun DateTimeInterval.generate(): Sequence<DateTimeInterval> =
        DateTimeIntervalGen().random().filter { interval -> interval.to in from..to && interval.from in from..to }
}

object OverFullOverlap : RelativePosition {
    override fun DateTimeInterval.generate(): Sequence<DateTimeInterval> =
        DateTimeIntervalGen().random().filter { interval -> from in interval.from..interval.to && to in interval.from..interval.to }
}

object RightPartialOverlap : RelativePosition {
    override fun DateTimeInterval.generate(): Sequence<DateTimeInterval> =
        DateTimeIntervalGen().random().filter { interval -> interval.to > from && interval.to > to && interval.from in from..to }
}

object After : RelativePosition {
    override fun DateTimeInterval.generate(): Sequence<DateTimeInterval> =
        DateTimeIntervalGen().random().filter { interval -> interval.to > from && interval.to > to && interval.from > from && interval.from > to }
}


class DateTimeIntervalRelativeGen(val dateTimeInterval: DateTimeInterval, val relativePosition: RelativePosition) :
    Gen<DateTimeInterval> {
    override fun constants(): Iterable<DateTimeInterval> = emptyList()
    override fun random(): Sequence<DateTimeInterval> = generateSequence {
        relativePosition.run { dateTimeInterval.generate() }.first()
    }
}


class DateTimeIntervalTests : StringSpec({
    val targetInterval = DateTimeIntervalGen().random().first()

    "There should be no intersection given input interval is before and not overlapping with target interval" {
        forAll(DateTimeIntervalRelativeGen(targetInterval, Before)) {
            DateTimeIntervalAPI.run { it.intersection(targetInterval) }.isEmpty()
        }
    }

    "Intersection interval should have from == targetInterval.from and to == inputInterval.to given inputInterval is partially overlapping targetInterval on the left" {
        forAll(DateTimeIntervalRelativeGen(targetInterval, LeftPartialOverlap)) {
            DateTimeIntervalAPI.run { it.intersection(targetInterval) }
                .map { newInterval -> newInterval.from == targetInterval.from && newInterval.to == it.to }
                .getOrElse { false }
        }
    }

    "Intersection interval should have from == inputInterval.from and to == inputInterval.to given targetInterval fully overlaps over inputInterval" {
        forAll(DateTimeIntervalRelativeGen(targetInterval, UnderFullOverlap)) {
            DateTimeIntervalAPI.run { it.intersection(targetInterval) }
                .map { newInterval -> newInterval.from == it.from && newInterval.to == it.to }
                .getOrElse { false }
        }
    }

    "Intersection interval should have from == targetInterval.from and to == targetInterval.to given inputInterval fully overlaps over targetInterval" {
        forAll(DateTimeIntervalRelativeGen(targetInterval, OverFullOverlap)) {
            DateTimeIntervalAPI.run { it.intersection(targetInterval) }
                .map { newInterval -> newInterval.from == targetInterval.from && newInterval.to == targetInterval.to }
                .getOrElse { false }
        }
    }

    "Intersection interval should have from == targetInterval.from and to == targetInterval.to given inputInterval perfectly overlaps targetInterval" {
        DateTimeIntervalAPI.run { targetInterval.intersection(targetInterval) }
            .map { newInterval -> newInterval.from == targetInterval.from && newInterval.to == targetInterval.to }
            .getOrElse { false }.shouldBeTrue()
    }

    "Intersection interval should have from == inputInterval.from and to == targetInterval.to given inputInterval is partially overlapping targetInterval on the right" {
        forAll(DateTimeIntervalRelativeGen(targetInterval, RightPartialOverlap)) {
            DateTimeIntervalAPI.run { it.intersection(targetInterval) }
                .map { newInterval -> newInterval.from == it.from && newInterval.to == targetInterval.to }
                .getOrElse { false }
        }
    }

    "There should be no intersection given input interval is after and not overlapping with target interval" {
        forAll(DateTimeIntervalRelativeGen(targetInterval, After)) {
            DateTimeIntervalAPI.run { it.intersection(targetInterval) }.isEmpty()
        }
    }
})