package fundflow.ledgers

import arrow.core.Option
import arrow.core.getOption
import arrow.core.getOrElse
import arrow.core.k
import arrow.syntax.collections.flatten
import common.*
import common.ValueWithError.Companion.toValue
import common.ValueWithError.Companion.withErrors
import fundflow.*
import graph.HierarchicalTreeApi
import ledger.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * A ledger that maintains a "flow of fund" between two toList
 * Can be seen integral of fund transaction ledger
 */
data class RecurrentTransactionQuantification(val flow: Flow)

data class RecurrentTransactionDetail(val recurrence: DateTimeInterval)

object RecurrentTransactionQuantificationOps :
    QuantificationOps<RecurrentTransactionQuantification> {
    override fun RecurrentTransactionQuantification.isNegative(): Boolean =
        this.flow.value < BigDecimal.ZERO

    operator fun List<Flow>.unaryMinus(): List<Flow> = this.map { AmountOps.run { -it } }

    override fun RecurrentTransactionQuantification.unaryMinus(): RecurrentTransactionQuantification {
        val quantification = this
        return this.copy(flow = AmountOps.run { -quantification.flow })
    }
}

typealias RecurrentTransaction = Transaction<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef>
typealias RecurrentTransactionLedger = Ledger<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef>

typealias RecurrentTransactionLedgerFundSummary = SingleFundLedgerSummary<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef>

data class RecurrentTransactionFundView(
    val fund: Fund,
    val fundSummaries: RecurrentTransactionLedgerFundSummaries
)


object RecurrentTransactionLedgerAPI {

    fun RecurrentTransactionLedger.toDailyFlow(): DailyFlowLedger =
        this.let {
            LedgerApi.run {
                it.mapList {
                    FlowOps.run {
                        it.map {
                            DailyFlowTransaction(
                                it.quantification.flow.toDailyFlow(),
                                Unit,
                                it.transactionCoordinates
                            )
                        }
                    }
                }
            }
        }

    fun RecurrentTransactionLedger.flowAt(dataTime: LocalDateTime): CombinableRecurrentTransactionLedger =
        this.let {
            LedgerApi.run {
                it.mapList {
                    it.map {
                        CombinableRecurrentTransactionLedgerAPI.run {
                            it.flowAt(dataTime)
                        }
                    }.flatten()
                }
            }
        }

    fun RecurrentTransactionLedger.toBalanceLedger(interval: DateTimeInterval): RecurrentBalanceLedger =
        this.let {
            LedgerApi.run {
                it.flatMap {
                    RecurrentTransactionAPI.run {
                        it.toBalanceTransaction(interval)
                    }
                }
            }
        }
}

object RecurrentTransactionAPI {
    fun RecurrentTransaction.toBalanceTransaction(interval: DateTimeInterval): List<RecurrentBalanceTransaction> {
        val recurrentTransaction: RecurrentTransaction = this
        val incrementer =
            TimeFrequencyOps.run { recurrentTransaction.quantification.flow.unit.incrementer() }
        val transactionDates: List<LocalDateTime> = DateTimeIntervalAPI.run {
            (recurrentTransaction.details.recurrence intersection interval)
        }.map { (incrementer.run { it.increment() }) }.getOrElse { emptyList() }

        return transactionDates.map {
            RecurrentBalanceTransaction(
                recurrentTransaction.quantification.flow.value,
                RecurrentBalanceTransactionDetail(it, this),
                recurrentTransaction.transactionCoordinates
            )
        }
    }
}

data class RecurrentTransactionLedgerFundSummaries(
    val fund: FundRef,
    val summary: RecurrentTransactionLedgerFundSummary,
    val summaryWithHierarchy: RecurrentTransactionLedgerFundSummary
)

data class RecurrentTransactionLedgerContext(
    val funds: Map<FundRef, Fund>,
    val fundHierarchy: FundHierarchy,
    val recurrentTransactionLedger: RecurrentTransactionLedger,
    val fundSummaries: Map<FundRef, RecurrentTransactionLedgerFundSummaries>
) {
    companion object {
        fun empty(): RecurrentTransactionLedgerContext =
            RecurrentTransactionLedgerContext(emptyList(), FundHierarchy.empty(), emptyList())

        operator fun invoke(
            funds: Collection<Fund>,
            fundHierarchy: FundHierarchy,
            recurrentTransactions: Collection<RecurrentTransaction>
        ): RecurrentTransactionLedgerContext {
            val recurrentTransactionLedger =
                RecurrentTransactionLedger(recurrentTransactions.toList())
            val fundSummary =
                LedgerApi.run {
                    recurrentTransactionLedger.ledgerOf(
                        funds.map { it.reference },
                        fundHierarchy
                    )
                }.map {
                    SingleFundLedgerAPI.run {
                        it.fund to RecurrentTransactionLedgerFundSummaries(
                            it.fund,
                            it.summary(RecurrentTransactionQuantificationOps),
                            it.summary(RecurrentTransactionQuantificationOps, fundHierarchy)
                        )
                    }
                }.toMap()
            return RecurrentTransactionLedgerContext(
                funds.map { it.reference to it }.toMap(),
                fundHierarchy,
                RecurrentTransactionLedger(recurrentTransactions.toList()),
                fundSummary
            )
        }
    }
}

object RecurrentTransactionLedgerContextAPI {
    fun RecurrentTransactionLedgerContext.addFunds(funds: Collection<Fund>): RecurrentTransactionLedgerContext =
        this.copy(funds = this.funds + funds.map { it.reference to it })

    fun RecurrentTransactionLedgerContext.removeFunds(funds: Collection<FundRef>): RecurrentTransactionLedgerContext =
        this.copy(
            funds = this.funds - funds,
            fundHierarchy = HierarchicalTreeApi.run { fundHierarchy - funds },
            recurrentTransactionLedger = LedgerApi.run {
                recurrentTransactionLedger.removeFunds(
                    funds
                )
            },
            fundSummaries = this.fundSummaries - funds
        )

    fun RecurrentTransactionLedgerContext.updateFunds(funds: Collection<Fund>): RecurrentTransactionLedgerContext =
        this.copy(funds = this.funds - funds.map { it.reference } + funds.map { it.reference to it })

    fun RecurrentTransactionLedgerContext.addFund(fund: Fund): RecurrentTransactionLedgerContext =
        this.addFunds(listOf(fund))

    fun RecurrentTransactionLedgerContext.removeFund(fund: FundRef): RecurrentTransactionLedgerContext =
        this.removeFunds(listOf(fund))

    fun RecurrentTransactionLedgerContext.updateFund(fund: Fund): RecurrentTransactionLedgerContext =
        this.updateFunds(listOf(fund))

    fun RecurrentTransactionLedgerContext.addFundRelation(
        relation: FundRelation
    ): ValueWithError<RecurrentTransactionLedgerContext> {
        val newFundHierarchy = HierarchicalTreeApi.run { fundHierarchy + relation }
        val newFundSummaries: Map<FundRef, RecurrentTransactionLedgerFundSummaries> =
            if (newFundHierarchy.e.isNotEmpty()) {
                this.fundSummaries.map { entry ->
                    LedgerApi.run {
                        recurrentTransactionLedger.ledgerOf(entry.key)
                    }.map { singleFundLedger ->
                        entry.key to entry.value.copy(summaryWithHierarchy = SingleFundLedgerAPI.run {
                            singleFundLedger.summary(
                                RecurrentTransactionQuantificationOps,
                                newFundHierarchy.v
                            )
                        })
                    }
                }.flatten().toMap()
            } else {
                this.fundSummaries
            }

        return this.copy(
            fundHierarchy = newFundHierarchy.v,
            fundSummaries = newFundSummaries
        ).toValue().withErrors(newFundHierarchy.e)
    }

    fun RecurrentTransactionLedgerContext.removeFundRelation(
        parent: FundRef,
        child: FundRef
    ): RecurrentTransactionLedgerContext {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun RecurrentTransactionLedgerContext.addRecurrentTransactions(transactions: Collection<RecurrentTransaction>): RecurrentTransactionLedgerContext {
        val newTransactionLedger = LedgerApi.run { recurrentTransactionLedger + transactions }
        return this.copy(
            recurrentTransactionLedger = newTransactionLedger,
            fundSummaries = LedgerApi.run { newTransactionLedger.splitByFund() }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentTransactionLedgerFundSummaries(
                        it.fund,
                        it.summary(RecurrentTransactionQuantificationOps),
                        it.summary(RecurrentTransactionQuantificationOps, fundHierarchy)
                    )
                }
            }.toMap()
        )
    }

    fun RecurrentTransactionLedgerContext.removeRecurrentTransactions(transactions: Collection<RecurrentTransaction>): RecurrentTransactionLedgerContext {
        val newTransactionLedger = LedgerApi.run { recurrentTransactionLedger + transactions }
        return this.copy(
            recurrentTransactionLedger = LedgerApi.run { newTransactionLedger },
            fundSummaries = LedgerApi.run { newTransactionLedger.splitByFund() }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentTransactionLedgerFundSummaries(
                        it.fund,
                        it.summary(RecurrentTransactionQuantificationOps),
                        it.summary(RecurrentTransactionQuantificationOps, fundHierarchy)
                    )
                }
            }.toMap()
        )
    }

    fun RecurrentTransactionLedgerContext.updateRecurrentTransactions(
        before: Collection<RecurrentTransaction>,
        after: Collection<RecurrentTransaction>
    ): RecurrentTransactionLedgerContext {
        val newTransactionLedger = LedgerApi.run { (recurrentTransactionLedger - before) + after }
        return this.copy(
            recurrentTransactionLedger = LedgerApi.run { newTransactionLedger },
            fundSummaries = LedgerApi.run { newTransactionLedger.splitByFund() }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentTransactionLedgerFundSummaries(
                        it.fund,
                        it.summary(RecurrentTransactionQuantificationOps),
                        it.summary(RecurrentTransactionQuantificationOps, fundHierarchy)
                    )
                }
            }.toMap()
        )
    }

    fun RecurrentTransactionLedgerContext.addRecurrentTransaction(transaction: RecurrentTransaction): RecurrentTransactionLedgerContext =
        this.addRecurrentTransactions(listOf(transaction))

    fun RecurrentTransactionLedgerContext.removeRecurrentTransaction(transaction: RecurrentTransaction): RecurrentTransactionLedgerContext =
        this.removeRecurrentTransactions(listOf(transaction))

    fun RecurrentTransactionLedgerContext.updateRecurrentTransaction(
        before: RecurrentTransaction,
        after: RecurrentTransaction
    ): RecurrentTransactionLedgerContext =
        this.updateRecurrentTransactions(listOf(before), listOf(after))

    fun RecurrentTransactionLedgerContext.rollOutBalanceTransactionsIn(timeInterval: DateTimeInterval): RecurrentBalanceTransactionLedgerContext {
        val globalBalanceLedger: RecurrentBalanceLedger =
            RecurrentTransactionLedgerAPI.run {
                recurrentTransactionLedger.toBalanceLedger(
                    timeInterval
                )
            }

        val fundSummaries =
            LedgerApi.run { globalBalanceLedger.ledgerOf(funds.keys, fundHierarchy) }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentBalanceTransactionLedgerFundSummaries(
                        timeInterval,
                        it.fund,
                        it.summary(
                            BalanceTransactionOps,
                            CombinedRecurrentBalanceTransactionDetailFactoryMonoid
                        ),
                        it.summary(
                            BalanceTransactionOps,
                            fundHierarchy,
                            CombinedRecurrentBalanceTransactionDetailFactoryMonoid
                        )
                    )
                }
            }.toMap()
        return RecurrentBalanceTransactionLedgerContext(
            funds,
            timeInterval,
            globalBalanceLedger,
            fundSummaries
        )
    }

    fun RecurrentTransactionLedgerContext.flowAt(dateTime: LocalDateTime): CombinableRecurrentTransactionLedgerContext {
        val combinableRecurrentTransactionLedger: CombinableRecurrentTransactionLedger =
            RecurrentTransactionLedgerAPI.run {
                recurrentTransactionLedger.flowAt(dateTime)
            }

        val fundSummaries =
            LedgerApi.run {
                combinableRecurrentTransactionLedger.ledgerOf(
                    funds.keys,
                    fundHierarchy
                )
            }.map {
                SingleFundLedgerAPI.run {
                    it.fund to CombinableRecurrentTransactionLedgerFundSummaries(
                        DateTimeIntervalAPI.run { point(dateTime) },
                        it.fund,
                        it.summary(
                            CombinableRecurrentTransactionQuantificationOps,
                            CombinedCombinableRecurrentTransactionDetailFactoryMonoid
                        ),
                        it.summary(
                            CombinableRecurrentTransactionQuantificationOps,
                            fundHierarchy,
                            CombinedCombinableRecurrentTransactionDetailFactoryMonoid
                        )
                    )
                }
            }.toMap()
        return CombinableRecurrentTransactionLedgerContext(
            funds,
            DateTimeIntervalAPI.run { point(dateTime) },
            combinableRecurrentTransactionLedger,
            fundSummaries
        )
    }

    fun RecurrentTransactionLedgerContext.view(fundRef: FundRef): Option<RecurrentTransactionFundView> =
        this.funds.k().getOption(fundRef).flatMap { fund ->
            this.fundSummaries.getOption(fundRef).map { RecurrentTransactionFundView(fund, it) }
        }

    fun RecurrentTransactionLedgerContext.viewAll(): Map<FundRef, RecurrentTransactionFundView> =
        this.funds.values.map { fund ->
            this.fundSummaries.getOption(fund.reference)
                .map { Pair(fund.reference, RecurrentTransactionFundView(fund, it)) }.getOrElse {
                    Pair(
                        fund.reference, RecurrentTransactionFundView(
                            fund,
                            RecurrentTransactionLedgerFundSummaries(
                                fund.reference,
                                RecurrentTransactionLedgerFundSummary(
                                    emptyList(),
                                    emptyList(),
                                    SingleFundLedger(fund.reference, Ledger.empty())
                                ),
                                RecurrentTransactionLedgerFundSummary(
                                    emptyList(),
                                    emptyList(),
                                    SingleFundLedger(fund.reference, Ledger.empty())
                                )
                            )
                        )
                    )
                }
        }.toMap()
}

