package ledger

import arrow.Kind
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.typeclasses.Functor
import graph.HierarchicalElement
import graph.HierarchicalElementAPI

enum class Position {
    SOURCE, DESTINATION
}

data class TransactionCoordinates<out F>(val source: F, val destination: F) : TransactionCoordinatesOf<F>

object TransactionCoordinatesAPI : Functor<ForTransactionCoordinates> {

    override fun <A, B> Kind<ForTransactionCoordinates, A>.map(f: (A) -> B): Kind<ForTransactionCoordinates, B> =
        TransactionCoordinates(f(this.fix().source), f(this.fix().destination))

    fun <F> TransactionCoordinates<F>.position(fund: HierarchicalElement<F>): Option<Position> {
        val transactionCoordinates = this
        HierarchicalElementAPI.run {
            return when {
                fund equalsOrChild transactionCoordinates.source -> Some(Position.SOURCE)
                fund equalsOrChild transactionCoordinates.destination -> Some(
                    Position.DESTINATION
                )
                else -> None
            }
        }
    }

    operator fun <F> TransactionCoordinates<F>.contains(fund: F): Boolean =
        this.source == fund || this.destination == fund

    fun <F> TransactionCoordinates<F>.toList(): List<F> = listOf(this.source, this.destination)

    operator fun <F> TransactionCoordinates<F>.unaryMinus(): TransactionCoordinates<F> =
        this.copy(source = this.destination, destination = this.source)
}

interface HasTransactionCoordinates<out F> {
    val transactionCoordinates: TransactionCoordinates<F>
}

class ForTransactionCoordinates private constructor() {
    companion object
}
typealias TransactionCoordinatesOf<F> = arrow.Kind<ForTransactionCoordinates, F>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <F> TransactionCoordinatesOf<F>.fix(): TransactionCoordinates<F> =
    this as TransactionCoordinates<F>

