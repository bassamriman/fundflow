package ledger

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import arrow.syntax.collections.flatten

/**
 * Ledger that holds a record of transactions
 */
data class Ledger<Q, D, F>(val transactions: List<Transaction<Q, D, F>>) {
    companion object {
        fun <Q, D, F> empty(): Ledger<Q, D, F> = Ledger(emptyList())
    }
}

object LedgerApi {

    fun <Q, D, F> Ledger<Q, D, F>.ledgerOf(
        funds: Collection<F>,
        hierarchicalTree: HierarchicalTree<F>
    ): List<SingleFundLedger<Q, D, F>> = funds.map { ledgerOf(it, hierarchicalTree) }.flatten()

    fun <Q, D, F> Ledger<Q, D, F>.ledgerOf(
        fund: F,
        hierarchicalTree: HierarchicalTree<F>
    ): Option<SingleFundLedger<Q, D, F>> =
        this.ledgerOf(fund) { f, flowCoordinates ->
            TransactionCoordinatesAPI.run {
                HierarchicalTreeApi.run {
                    flowCoordinates.source.childOrEqual(f, hierarchicalTree)
                            || flowCoordinates.destination.childOrEqual(f, hierarchicalTree)
                }
            }
        }

    fun <Q, D, F> Ledger<Q, D, F>.ledgerOf(fund: F): Option<SingleFundLedger<Q, D, F>> =
        this.ledgerOf(fund) { f, flowCoordinates -> TransactionCoordinatesAPI.run { f in flowCoordinates } }


    fun <Q, D, F> Ledger<Q, D, F>.ledgerOf(
        fund: F,
        predicate: (F, TransactionCoordinates<F>) -> Boolean
    ): Option<SingleFundLedger<Q, D, F>> {
        val filteredTransaction: List<Transaction<Q, D, F>> =
            this.transactions.filter { predicate(fund, it.transactionCoordinates) }
        return if (filteredTransaction.isEmpty()) None else Some(
            SingleFundLedger(
                fund,
                this.copy(transactions = filteredTransaction)
            )
        )
    }

    fun <Q, D, F> Ledger<Q, D, F>.splitByFund(): List<SingleFundLedger<Q, D, F>> =
        this.transactions.flatMap { transaction ->
            TransactionCoordinatesAPI.run {
                transaction.transactionCoordinates.toList().map { fund -> fund to transaction }
            }
        }.groupBy { entry -> entry.first }.mapValues { entry -> entry.value.map { t -> t.second } }.map { entry ->
            SingleFundLedger(
                fund = entry.key, ledger = this.copy(entry.value)
            )
        }

    fun <Q, D, F> Ledger<Q, D, F>.add(transactions: Collection<Transaction<Q, D, F>>): Ledger<Q, D, F> =
        this.copy(this.transactions + transactions)

    fun <Q, D, F> Ledger<Q, D, F>.remove(transactions: Collection<Transaction<Q, D, F>>): Ledger<Q, D, F> =
        this.copy(this.transactions - transactions)

    fun <Q, D, F> Ledger<Q, D, F>.add(transaction: Transaction<Q, D, F>): Ledger<Q, D, F> =
        this.add(listOf(transaction))

    fun <Q, D, F> Ledger<Q, D, F>.remove(transaction: Transaction<Q, D, F>): Ledger<Q, D, F> =
        this.remove(listOf(transaction))

    operator fun <Q, D, F> Ledger<Q, D, F>.plus(transactions: Collection<Transaction<Q, D, F>>): Ledger<Q, D, F> =
        this.add(transactions)

    operator fun <Q, D, F> Ledger<Q, D, F>.minus(transactions: Collection<Transaction<Q, D, F>>): Ledger<Q, D, F> =
        this.remove(transactions)

    operator fun <Q, D, F> Ledger<Q, D, F>.plus(transaction: Transaction<Q, D, F>): Ledger<Q, D, F> =
        this.add(transaction)

    operator fun <Q, D, F> Ledger<Q, D, F>.minus(transaction: Transaction<Q, D, F>): Ledger<Q, D, F> =
        this.remove(transaction)


    fun <Q, D, F> Ledger<Q, D, F>.removeFunds(funds: Collection<F>): Ledger<Q, D, F> =
        this.copy(this.transactions - transactions.filter {
            TransactionCoordinatesAPI.run {
                it.transactionCoordinates.toList().intersect(funds).isNotEmpty()
            }
        })


    fun <Q, D, F> Ledger<Q, D, F>.remove(transaction: F): Ledger<Q, D, F> =
        this.removeFunds(listOf(transaction))

    fun <Q, D, F, Q1, D1, F1> Ledger<Q, D, F>.mapList(f: (List<Transaction<Q, D, F>>) -> List<Transaction<Q1, D1, F1>>): Ledger<Q1, D1, F1> =
        Ledger(f(transactions))

    fun <Q, D, F, Q1, D1, F1> Ledger<Q, D, F>.flatMap(f: (Transaction<Q, D, F>) -> List<Transaction<Q1, D1, F1>>): Ledger<Q1, D1, F1> =
        this.mapList { transactions -> transactions.flatMap { f(it) } }

}

data class SingleFundLedger<Q, D, F>(val fund: F, val ledger: Ledger<Q, D, F>)

data class SingleFundLedgerSummary<Q, D, F>(
    val incomingTransaction: Collection<Transaction<Q, D, F>>,
    val outgoingTransaction: Collection<Transaction<Q, D, F>>,
    val singleFundLedger: SingleFundLedger<Q, D, F>
)

data class SingleFundLedgerSummaryWithValue<Q, D, F>(
    val fundFlow: Q,
    val incomingTransaction: Collection<Transaction<Q, D, F>>,
    val outgoingTransaction: Collection<Transaction<Q, D, F>>,
    val singleFundLedger: SingleFundLedger<Q, D, F>
)


data class CombinableSingleFundLedgerSummary<Q, D, F, CD>(
    val incomingTransaction: Collection<Transaction<Q, CD, F>>,
    val outgoingTransaction: Collection<Transaction<Q, CD, F>>,
    val singleFundLedger: SingleFundLedger<Q, D, F>
)

data class CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>(
    val fundFlow: Q,
    val incomingTransaction: Collection<Transaction<Q, CD, F>>,
    val outgoingTransaction: Collection<Transaction<Q, CD, F>>,
    val singleFundLedger: SingleFundLedger<Q, D, F>
)

object SingleFundLedgerAPI {

    fun <Q, D, F> SingleFundLedger<Q, D, F>.fundFlow(quantificationOps: CombinableQuantificationOps<Q>): Q {
        val ledger = this
        val positiveTransaction: List<Option<Transaction<Q, D, F>>> =
            this.ledger.transactions.map {
                TransactionApi.run {
                    it.forcePositive(ledger.fund, quantificationOps)
                }
            }

        return quantificationOps.combineAll(positiveTransaction.flatten().map { it.quantification })
    }

    fun <Q, D, F> SingleFundLedger<Q, D, F>.fundFlow(
        quantificationOps: CombinableQuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>
    ): Q {
        val fund = this.fund
        val hierarchicalFund: HierarchicalElement<F> = HierarchicalElementAPI.run {
            fund.toHierarchicalElement(hierarchicalTree)
        }

        val positiveTransaction: List<Option<Transaction<Q, D, F>>> =
            this.ledger.transactions.map {
                TransactionApi.run {
                    it.forcePositive(
                        hierarchicalFund,
                        quantificationOps
                    )
                }
            }

        return quantificationOps.combineAll(positiveTransaction.flatten().map { it.quantification })
    }

    private fun <Q, D, F> SingleFundLedger<Q, D, F>.fixTransactionOrientationAndFilter(
        predicate: (Transaction<Q, D, F>) -> Boolean,
        quantificationOps: QuantificationOps<Q>
    ): List<Transaction<Q, D, F>> {
        val singleFundLedger = this
        return TransactionApi.run {
            singleFundLedger.ledger.transactions.map { it.fixOrientation(quantificationOps) }
                .filter { predicate(it) }
        }
    }

    fun <Q, D, F> SingleFundLedger<Q, D, F>.outgoingTransactions(quantificationOps: QuantificationOps<Q>): List<Transaction<Q, D, F>> {
        val singleFundLedger = this
        return TransactionApi.run {
            singleFundLedger.fixTransactionOrientationAndFilter({
                it.isNegative(
                    singleFundLedger.fund
                ).getOrElse { false }
            }, quantificationOps)
        }
    }

    fun <Q, D, F> SingleFundLedger<Q, D, F>.incomingTransactions(quantificationOps: QuantificationOps<Q>): List<Transaction<Q, D, F>> {
        val singleFundLedger = this
        return TransactionApi.run {
            singleFundLedger.fixTransactionOrientationAndFilter({
                it.isPositive(
                    singleFundLedger.fund
                ).getOrElse { false }
            }, quantificationOps)
        }
    }

    fun <Q, D, F, CD> transactionToDetail(
        transaction: Transaction<Q, D, F>,
        factory: CombinedTransactionDetailFactory<Q, D, F, CD>
    ): CD = this.run {
        factory.build(
            listOf(transaction)
        )
    }

    private fun <Q, D, F, CD> List<Transaction<Q, D, F>>.flatten(
        detailMapper: (Transaction<Q, D, F>, CombinedTransactionDetailFactory<Q, D, F, CD>) -> CD,
        quantificationOps: CombinableQuantificationOps<Q>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): Collection<Transaction<Q, CD, F>> = this.groupBy { it.transactionCoordinates.destination }.mapValues { entry ->
        Transaction(
            quantificationOps.combineAll(entry.value.map { it.quantification }),
            detailFactoryMonoid.combineAll(entry.value.map { detailMapper(it, detailFactoryMonoid) }),
            entry.value.last().transactionCoordinates
        )
    }.values

    fun <Q, D, F, CD> SingleFundLedger<Q, D, F>.outgoingTransactions(
        quantificationOps: CombinableQuantificationOps<Q>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): Collection<Transaction<Q, CD, F>> =
        this.outgoingTransactions(quantificationOps).flatten(
            SingleFundLedgerAPI::transactionToDetail,
            quantificationOps,
            detailFactoryMonoid
        )

    fun <Q, D, F, CD> SingleFundLedger<Q, D, F>.incomingTransactions(
        quantificationOps: CombinableQuantificationOps<Q>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): Collection<Transaction<Q, CD, F>> =
        this.incomingTransactions(quantificationOps).flatten(
            SingleFundLedgerAPI::transactionToDetail,
            quantificationOps,
            detailFactoryMonoid
        )

    fun <Q, D, F> SingleFundLedger<Q, D, F>.outgoingTransactions(
        quantificationOps: QuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>
    ): List<Transaction<Q, D, F>> {
        val singleFundLedger = this
        val hierarchicalFund: HierarchicalElement<F> = HierarchicalElementAPI.run {
            fund.toHierarchicalElement(hierarchicalTree)
        }
        return TransactionApi.run {
            singleFundLedger.fixTransactionOrientationAndFilter({
                it.isNegative(
                    hierarchicalFund
                ).getOrElse { false }
            }, quantificationOps)
        }
    }

    fun <Q, D, F> SingleFundLedger<Q, D, F>.incomingTransactions(
        quantificationOps: QuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>
    ): List<Transaction<Q, D, F>> {
        val singleFundLedger = this
        val hierarchicalFund: HierarchicalElement<F> = HierarchicalElementAPI.run {
            fund.toHierarchicalElement(hierarchicalTree)
        }
        return TransactionApi.run {
            singleFundLedger.fixTransactionOrientationAndFilter({
                it.isPositive(
                    hierarchicalFund
                ).getOrElse { false }
            }, quantificationOps)
        }
    }

    fun <Q, D, F, CD> SingleFundLedger<Q, D, F>.outgoingTransactions(
        quantificationOps: CombinableQuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): Collection<Transaction<Q, CD, F>> =
        this.outgoingTransactions(quantificationOps, hierarchicalTree).flatten(
            SingleFundLedgerAPI::transactionToDetail,
            quantificationOps,
            detailFactoryMonoid
        )


    fun <Q, D, F, CD> SingleFundLedger<Q, D, F>.incomingTransactions(
        quantificationOps: CombinableQuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): Collection<Transaction<Q, CD, F>> =
        this.incomingTransactions(quantificationOps, hierarchicalTree).flatten(
            SingleFundLedgerAPI::transactionToDetail,
            quantificationOps,
            detailFactoryMonoid
        )

    fun <Q, D, F> SingleFundLedger<Q, D, F>.summary(
        quantificationOps: QuantificationOps<Q>
    ): SingleFundLedgerSummary<Q, D, F> = SingleFundLedgerSummary(
        this.incomingTransactions(quantificationOps),
        this.outgoingTransactions(quantificationOps),
        this
    )

    fun <Q, D, F, CD> SingleFundLedger<Q, D, F>.summary(
        quantificationOps: CombinableQuantificationOps<Q>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD> = CombinableSingleFundLedgerSummaryWithValue(
        this.fundFlow(quantificationOps),
        this.incomingTransactions(quantificationOps, detailFactoryMonoid),
        this.outgoingTransactions(quantificationOps, detailFactoryMonoid),
        this
    )

    fun <Q, D, F> SingleFundLedger<Q, D, F>.summary(
        quantificationOps: QuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>
    ): SingleFundLedgerSummary<Q, D, F> = SingleFundLedgerSummary(
        this.incomingTransactions(quantificationOps, hierarchicalTree),
        this.outgoingTransactions(quantificationOps, hierarchicalTree),
        this
    )

    fun <Q, D, F, CD> SingleFundLedger<Q, D, F>.summary(
        quantificationOps: CombinableQuantificationOps<Q>,
        hierarchicalTree: HierarchicalTree<F>,
        detailFactoryMonoid: CombinedTransactionDetailFactoryMonoid<Q, D, F, CD>
    ): CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD> = CombinableSingleFundLedgerSummaryWithValue(
        this.fundFlow(quantificationOps, hierarchicalTree),
        this.incomingTransactions(quantificationOps, hierarchicalTree, detailFactoryMonoid),
        this.outgoingTransactions(quantificationOps, hierarchicalTree, detailFactoryMonoid),
        this
    )
}