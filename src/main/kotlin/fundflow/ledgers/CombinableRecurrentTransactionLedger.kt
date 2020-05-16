package fundflow.ledgers

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.typeclasses.Monoid
import common.unit.AmountOps
import common.DateTimeIntervalAPI
import fundflow.*
import fundflow.FlowOps.toDailyFlow
import ledger.*
import java.math.BigDecimal
import java.time.LocalDateTime

data class CombinableRecurrentTransactionQuantification(val flow: Flow)
object CombinableRecurrentTransactionQuantificationOps :
    CombinableQuantificationOps<CombinableRecurrentTransactionQuantification> {
    override fun CombinableRecurrentTransactionQuantification.isNegative(): Boolean =
        this.flow.value < BigDecimal.ZERO

    operator fun List<Flow>.unaryMinus(): List<Flow> = this.map { AmountOps.run { -it } }

    override fun CombinableRecurrentTransactionQuantification.unaryMinus(): CombinableRecurrentTransactionQuantification {
        val quantification = this
        return this.copy(flow = AmountOps.run { -quantification.flow })
    }

    override fun empty(): CombinableRecurrentTransactionQuantification =
        CombinableRecurrentTransactionQuantification(
            DailyFlowOps.ZERO
        )

    override fun CombinableRecurrentTransactionQuantification.combine(b: CombinableRecurrentTransactionQuantification): CombinableRecurrentTransactionQuantification {
        return FlowOps.run {
            CombinableRecurrentTransactionQuantification(AmountOps.run {
                flow.toDailyFlow() + b.flow.toDailyFlow()
            })
        }
    }
}

typealias CombinableRecurrentTransaction = Transaction<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef>

data class CombinableRecurrentTransactionDetail(
    val originalRecurrentTransaction: RecurrentTransaction,
    val viewAt: LocalDateTime
)
typealias CombinedCombinableRecurrentTransactionDetail = CombinedTransactionDetail<CombinableRecurrentTransaction>

object CombinedCombinableRecurrentTransactionDetailMonoid :
    CombinedTransactionDetailMonoid<CombinableRecurrentTransaction>()

object CombinedCombinableRecurrentTransactionDetailFactory :
    CombinedTransactionDetailFactory<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail> {
    override fun build(combinedTransactions: Collection<Transaction<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef>>): CombinedCombinableRecurrentTransactionDetail =
        CombinedCombinableRecurrentTransactionDetail(combinedTransactions)
}

object CombinedCombinableRecurrentTransactionDetailFactoryMonoid :
    CombinedTransactionDetailFactoryMonoid<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail>,
    CombinedTransactionDetailFactory<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail> by CombinedCombinableRecurrentTransactionDetailFactory,
    Monoid<CombinedCombinableRecurrentTransactionDetail> by CombinedCombinableRecurrentTransactionDetailMonoid


typealias CombinableRecurrentTransactionLedger = Ledger<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef>

typealias CombinableRecurrentTransactionLedgerFundSummary = CombinableSingleFundLedgerSummaryWithValue<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail>

typealias CombinableRecurrentTransactionLedgerFundSummaries = LedgerFundSummaries<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail, CombinableRecurrentTransactionLedgerFundSummary>
typealias CombinableRecurrentTransactionLedgerContext = LedgerContext<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail, CombinableRecurrentTransactionLedger, CombinableRecurrentTransactionLedgerFundSummary, CombinableRecurrentTransactionLedgerFundSummaries>
typealias CombinableRecurrentTransactionFundView = FundView<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail, CombinableRecurrentTransactionLedgerFundSummary>

object CombinableRecurrentTransactionFundViewFactory :
    FundViewFactory<CombinableRecurrentTransactionQuantification, CombinableRecurrentTransactionDetail, FundRef, CombinedCombinableRecurrentTransactionDetail, CombinableRecurrentTransactionLedgerFundSummary, CombinableRecurrentTransactionFundView> {
    override fun build(
        fund: Fund,
        fundSummaries: CombinableRecurrentTransactionLedgerFundSummaries
    ): CombinableRecurrentTransactionFundView =
        CombinableRecurrentTransactionFundView(fund, fundSummaries)


}

object CombinableRecurrentTransactionLedgerAPI {
    fun RecurrentTransaction.flowAt(dataTime: LocalDateTime): Option<CombinableRecurrentTransaction> =
        this.let {
            DateTimeIntervalAPI.run {
                if (it.details.recurrence.contains(dataTime)) {
                    Some(
                        CombinableRecurrentTransaction(
                            CombinableRecurrentTransactionQuantification(it.quantification.flow.toDailyFlow()),
                            CombinableRecurrentTransactionDetail(it, dataTime),
                            it.transactionCoordinates
                        )
                    )
                } else {
                    None
                }
            }
        }
}