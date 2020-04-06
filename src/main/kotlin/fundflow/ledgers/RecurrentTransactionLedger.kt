package fundflow.ledgers

import arrow.core.Option
import arrow.core.getOption
import arrow.core.getOrElse
import arrow.core.k
import arrow.syntax.collections.flatten
import arrow.typeclasses.Monoid
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
object RecurrentTransactionQuantificationOps : CombinableQuantificationOps<RecurrentTransactionQuantification> {
    override fun RecurrentTransactionQuantification.isNegative(): Boolean = this.flow.value < BigDecimal.ZERO

    operator fun List<Flow>.unaryMinus(): List<Flow> = this.map { AmountOps.run { -it } }

    override fun RecurrentTransactionQuantification.unaryMinus(): RecurrentTransactionQuantification {
        val quantification = this
        return this.copy(flow = AmountOps.run { -quantification.flow })
    }

    override fun empty(): RecurrentTransactionQuantification = RecurrentTransactionQuantification(DailyFlowOps.ZERO)

    override fun RecurrentTransactionQuantification.combine(b: RecurrentTransactionQuantification): RecurrentTransactionQuantification {
        return FlowOps.run {
            RecurrentTransactionQuantification(AmountOps.run {
                flow.toDailyFlow() + b.flow.toDailyFlow()
            })
        }
    }
}


typealias RecurrentTransaction = Transaction<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef>

data class RecurrentTransactionDetail(val recurrence: DateTimeInterval)
typealias CombinedRecurrentTransactionDetail = CombinedTransactionDetail<RecurrentTransaction>

object CombinedRecurrentTransactionDetailMonoid : CombinedTransactionDetailMonoid<RecurrentTransaction>()
object CombinedRecurrentTransactionDetailFactory :
    CombinedTransactionDetailFactory<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef, CombinedRecurrentTransactionDetail> {
    override fun build(combinedTransactions: Collection<Transaction<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef>>): CombinedRecurrentTransactionDetail =
        CombinedRecurrentTransactionDetail(combinedTransactions)
}

object CombinedRecurrentTransactionDetailFactoryMonoid :
    CombinedTransactionDetailFactoryMonoid<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef, CombinedRecurrentTransactionDetail>,
    CombinedTransactionDetailFactory<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef, CombinedRecurrentTransactionDetail> by CombinedRecurrentTransactionDetailFactory,
    Monoid<CombinedRecurrentTransactionDetail> by CombinedRecurrentTransactionDetailMonoid


typealias RecurrentTransactionLedger = Ledger<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef>

typealias RecurrentTransactionLedgerFundSummary = CombinableSingleFundLedgerSummaryWithValue<RecurrentTransactionQuantification, RecurrentTransactionDetail, FundRef, CombinedRecurrentTransactionDetail>

data class RecurrentTransactionFundView(val fund: Fund, val fundSummaries: RecurrentTransactionLedgerFundSummaries)


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
        val incrementer = TimeFrequencyOps.run { recurrentTransaction.quantification.flow.unit.incrementer() }
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
            val recurrentTransactionLedger = RecurrentTransactionLedger(recurrentTransactions.toList())
            val fundSummary =
                LedgerApi.run { recurrentTransactionLedger.ledgerOf(funds.map { it.reference }, fundHierarchy) }.map {
                    SingleFundLedgerAPI.run {
                        it.fund to RecurrentTransactionLedgerFundSummaries(
                            it.fund,
                            it.summary(
                                RecurrentTransactionQuantificationOps,
                                CombinedRecurrentTransactionDetailFactoryMonoid
                            ),
                            it.summary(
                                RecurrentTransactionQuantificationOps,
                                fundHierarchy,
                                CombinedRecurrentTransactionDetailFactoryMonoid
                            )
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

typealias RecurrentTransactionLedgerContextVersion = Version<RecurrentTransactionLedgerContext, FundFlowActionAcknowledgement<*, *>>

typealias RecurrentTransactionLedgerContextVersioning = Versioning<RecurrentTransactionLedgerContext, FundFlowActionAcknowledgement<*, *>>

object RecurrentTransactionLedgerContextAPI {
    fun RecurrentTransactionLedgerContext.addFunds(funds: Collection<Fund>): RecurrentTransactionLedgerContext =
        this.copy(funds = this.funds + funds.map { it.reference to it })

    fun RecurrentTransactionLedgerContext.removeFunds(funds: Collection<FundRef>): RecurrentTransactionLedgerContext =
        this.copy(
            funds = this.funds - funds,
            fundHierarchy = HierarchicalTreeApi.run { fundHierarchy - funds },
            recurrentTransactionLedger = LedgerApi.run { recurrentTransactionLedger.removeFunds(funds) },
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
                                newFundHierarchy.v,
                                CombinedRecurrentTransactionDetailFactoryMonoid
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
        return this.copy(
            recurrentTransactionLedger = LedgerApi.run { recurrentTransactionLedger + transactions },
            fundSummaries = LedgerApi.run { recurrentTransactionLedger.splitByFund() }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentTransactionLedgerFundSummaries(
                        it.fund,
                        it.summary(
                            RecurrentTransactionQuantificationOps,
                            CombinedRecurrentTransactionDetailFactoryMonoid
                        ),
                        it.summary(
                            RecurrentTransactionQuantificationOps,
                            fundHierarchy,
                            CombinedRecurrentTransactionDetailFactoryMonoid
                        )
                    )
                }
            }.toMap()
        )
    }

    fun RecurrentTransactionLedgerContext.removeRecurrentTransactions(transactions: Collection<RecurrentTransaction>): RecurrentTransactionLedgerContext {
        return this.copy(
            recurrentTransactionLedger = LedgerApi.run { recurrentTransactionLedger - transactions },
            fundSummaries = LedgerApi.run { recurrentTransactionLedger.splitByFund() }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentTransactionLedgerFundSummaries(
                        it.fund,
                        it.summary(
                            RecurrentTransactionQuantificationOps,
                            CombinedRecurrentTransactionDetailFactoryMonoid
                        ),
                        it.summary(
                            RecurrentTransactionQuantificationOps,
                            fundHierarchy,
                            CombinedRecurrentTransactionDetailFactoryMonoid
                        )
                    )
                }
            }.toMap()
        )
    }

    fun RecurrentTransactionLedgerContext.updateRecurrentTransactions(
        before: Collection<RecurrentTransaction>,
        after: Collection<RecurrentTransaction>
    ): RecurrentTransactionLedgerContext {
        return this.copy(
            recurrentTransactionLedger = LedgerApi.run { (recurrentTransactionLedger - before) + after },
            fundSummaries = LedgerApi.run { recurrentTransactionLedger.splitByFund() }.map {
                SingleFundLedgerAPI.run {
                    it.fund to RecurrentTransactionLedgerFundSummaries(
                        it.fund,
                        it.summary(
                            RecurrentTransactionQuantificationOps,
                            CombinedRecurrentTransactionDetailFactoryMonoid
                        ),
                        it.summary(
                            RecurrentTransactionQuantificationOps,
                            fundHierarchy,
                            CombinedRecurrentTransactionDetailFactoryMonoid
                        )
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
            RecurrentTransactionLedgerAPI.run { recurrentTransactionLedger.toBalanceLedger(timeInterval) }

        val fundSummaries = LedgerApi.run { globalBalanceLedger.ledgerOf(funds.keys, fundHierarchy) }.map {
            SingleFundLedgerAPI.run {
                it.fund to RecurrentBalanceTransactionLedgerFundSummaries(
                    timeInterval,
                    it.fund,
                    it.summary(BalanceTransactionOps, CombinedRecurrentBalanceTransactionDetailFactoryMonoid),
                    it.summary(
                        BalanceTransactionOps,
                        fundHierarchy,
                        CombinedRecurrentBalanceTransactionDetailFactoryMonoid
                    )
                )
            }
        }.toMap()
        return RecurrentBalanceTransactionLedgerContext(funds, timeInterval, globalBalanceLedger, fundSummaries)
    }

    fun RecurrentTransactionLedgerContext.view(fundRef: FundRef): Option<RecurrentTransactionFundView> =
        this.funds.k().getOption(fundRef).flatMap { fund ->
            this.fundSummaries.getOption(fundRef).map { RecurrentTransactionFundView(fund, it) }
        }

    fun RecurrentTransactionLedgerContext.viewAll(): Map<FundRef, RecurrentTransactionFundView> =
        this.funds.values.map { fund ->
            this.fundSummaries.getOption(fund.reference)
                .map { Pair(fund.reference, RecurrentTransactionFundView(fund, it)) }
        }.flatten().toMap()
}

