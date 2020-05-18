package fundflow.ledgers

import arrow.typeclasses.Monoid
import fundflow.Fund
import fundflow.FundRef
import java.math.BigDecimal
import java.time.LocalDateTime
import ledger.CombinableQuantificationOps
import ledger.CombinableSingleFundLedgerSummaryWithValue
import ledger.CombinedTransactionDetailFactory
import ledger.CombinedTransactionDetailFactoryMonoid
import ledger.FundView
import ledger.FundViewFactory
import ledger.Ledger
import ledger.LedgerContext
import ledger.LedgerFundSummaries
import ledger.Transaction

/**
 * A ledger that maintains a "flow of fund" between two toList
 * Can be seen integral of fund transaction ledger
 */
object BalanceTransactionOps : CombinableQuantificationOps<BigDecimal> {
    override fun BigDecimal.isNegative(): Boolean = this < BigDecimal.ZERO
    override fun BigDecimal.unaryMinus(): BigDecimal = this.negate()
    override fun empty(): BigDecimal = BigDecimal.ZERO
    override fun BigDecimal.combine(b: BigDecimal): BigDecimal = this.add(b)
}

data class CombinedTransactionDetail<T>(val combinedTransactions: Collection<T>)
open class CombinedTransactionDetailMonoid<T>() : Monoid<CombinedTransactionDetail<T>> {
    override fun empty(): CombinedTransactionDetail<T> = CombinedTransactionDetail(emptyList())
    override fun CombinedTransactionDetail<T>.combine(b: CombinedTransactionDetail<T>): CombinedTransactionDetail<T> =
        CombinedTransactionDetail(this.combinedTransactions + b.combinedTransactions)
}

data class BalanceTransactionDetail(val timestamp: LocalDateTime)
typealias CombinedBalanceTransactionDetail = CombinedTransactionDetail<BalanceTransaction>

object CombinedBalanceTransactionDetailMonoid :
    CombinedTransactionDetailMonoid<BalanceTransaction>()

object CombinedBalanceTransactionDetailFactory :
    CombinedTransactionDetailFactory<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail> {
    override fun build(combinedTransactions: Collection<Transaction<BigDecimal, BalanceTransactionDetail, FundRef>>): CombinedBalanceTransactionDetail =
        CombinedBalanceTransactionDetail(combinedTransactions)
}

object CombinedBalanceTransactionDetailFactoryMonoid :
    CombinedTransactionDetailFactoryMonoid<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail>,
    CombinedTransactionDetailFactory<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail> by CombinedBalanceTransactionDetailFactory,
    Monoid<CombinedBalanceTransactionDetail> by CombinedBalanceTransactionDetailMonoid

typealias BalanceTransaction = Transaction<BigDecimal, BalanceTransactionDetail, FundRef>

typealias BalanceLedger = Ledger<BigDecimal, BalanceTransactionDetail, FundRef>
typealias BalanceTransactionLedgerFundSummary = CombinableSingleFundLedgerSummaryWithValue<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail>
typealias BalanceTransactionLedgerFundSummaries = LedgerFundSummaries<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail, BalanceTransactionLedgerFundSummary>
typealias BalanceTransactionLedgerContext = LedgerContext<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail, BalanceLedger, BalanceTransactionLedgerFundSummary, BalanceTransactionLedgerFundSummaries>
typealias BalanceTransactionFundView = FundView<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail, BalanceTransactionLedgerFundSummary>

object BalanceTransactionFundViewFactory :
    FundViewFactory<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail, BalanceTransactionLedgerFundSummary, BalanceTransactionFundView> {
    override fun build(
        fund: Fund,
        fundSummaries: LedgerFundSummaries<BigDecimal, BalanceTransactionDetail, FundRef, CombinedBalanceTransactionDetail, BalanceTransactionLedgerFundSummary>
    ): BalanceTransactionFundView = BalanceTransactionFundView(fund, fundSummaries)
}
