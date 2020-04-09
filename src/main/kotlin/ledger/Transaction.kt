package ledger

import arrow.core.Option
import arrow.typeclasses.Monoid
import common.Identifiable
import graph.HierarchicalElement
import graph.HierarchicalElementAPI
import java.util.*

enum class Sign {
    POSITIVE, NEGATIVE
}

interface QuantificationOps<T> {
    operator fun T.unaryMinus(): T
    fun T.isNegative(): Boolean
}

interface CombinableQuantificationOps<T> : QuantificationOps<T>, Monoid<T>

data class TransactionRef(override val id: String = UUID.randomUUID().toString()) : Identifiable

/**
 * Transaction in a ledger that transfer a value of type Asset
 */
data class Transaction<out Q, out D, out E>(
    val quantification: Q,
    val details: D,
    override val transactionCoordinates: TransactionCoordinates<E>,
    val transactionRef: TransactionRef = TransactionRef()
) : HasTransactionCoordinates<E>

/**
 * Transaction Operation
 */
object TransactionApi {

    fun <Q, D, E> Transaction<Q, D, E>.fixOrientation(quantificationOps: QuantificationOps<Q>): Transaction<Q, D, E> {
        val transaction = this
        quantificationOps.run {
            TransactionCoordinatesAPI.run {
                return if (transaction.quantification.isNegative()) transaction.copy(
                    quantification = -transaction.quantification,
                    transactionCoordinates = -transaction.transactionCoordinates
                ) else {
                    transaction
                }
            }
        }
    }

    fun <Q, D, E> Transaction<Q, D, E>.negate(quantificationOps: QuantificationOps<Q>): Transaction<Q, D, E> {
        val transaction = this
        return this.copy(quantification = quantificationOps.run { -transaction.quantification })
    }

    fun <Q, D, E> Transaction<Q, D, E>.forcePositive(
        fund: E,
        quantificationOps: QuantificationOps<Q>
    ): Option<Transaction<Q, D, E>> =
        this.forcePositive(HierarchicalElementAPI.empty(fund), quantificationOps)

    fun <Q, D, E> Transaction<Q, D, E>.forcePositive(
        fund: HierarchicalElement<E>,
        quantificationOps: QuantificationOps<Q>
    ): Option<Transaction<Q, D, E>> =
        this.sign(fund)
            .map { sign ->
                if (sign == Sign.NEGATIVE) this.negate(
                    quantificationOps
                ) else this
            }

    fun <Q, D, E> Transaction<Q, D, E>.isPositive(fund: E): Option<Boolean> =
        this.sign(fund).map { it == Sign.POSITIVE }

    fun <Q, D, E> Transaction<Q, D, E>.isPositive(fund: HierarchicalElement<E>): Option<Boolean> =
        this.sign(fund).map { it == Sign.POSITIVE }

    fun <Q, D, E> Transaction<Q, D, E>.isNegative(fund: E): Option<Boolean> = this.isPositive(fund).map { !it }

    fun <Q, D, E> Transaction<Q, D, E>.isNegative(fund: HierarchicalElement<E>): Option<Boolean> =
        this.isPositive(fund).map { !it }

    fun <Q, D, E> Transaction<Q, D, E>.sign(fund: E): Option<Sign> = this.sign(
        HierarchicalElementAPI.empty(
            fund
        )
    )

    fun <Q, D, E> Transaction<Q, D, E>.sign(fund: HierarchicalElement<E>): Option<Sign> {
        val transaction = this
        return TransactionCoordinatesAPI.run {
            transaction.transactionCoordinates.position(fund)
                .map { position -> if (position == Position.SOURCE) Sign.NEGATIVE else Sign.POSITIVE }
        }
    }

    fun <Q, D, E, TQ, TD, TE> Transaction<Q, D, E>.bimap(
        fl: (Q) -> TQ,
        fm: (D) -> TD,
        fr: (E) -> TE
    ): Transaction<TQ, TD, TE> =
        Transaction(
            fl(quantification),
            fm(details),
            TransactionCoordinatesAPI.run { transactionCoordinates.map(fr).fix() })
}

interface CombinedTransactionDetailFactory<Q, D, E, CD> {
    fun build(combinedTransactions: Collection<Transaction<Q, D, E>>): CD
}

interface CombinedTransactionDetailFactoryMonoid<Q, D, E, CD> :
    CombinedTransactionDetailFactory<Q, D, E, CD>, Monoid<CD>
