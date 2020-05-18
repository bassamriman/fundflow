package fundflow.ledgers

import arrow.typeclasses.Monoid
import fundflow.Fund
import fundflow.FundRef
import ledger.CombinableSingleFundLedgerSummaryWithValue
import ledger.CombinedTransactionDetailFactory
import ledger.CombinedTransactionDetailFactoryMonoid
import ledger.FundView
import ledger.FundViewFactory
import ledger.Ledger
import ledger.LedgerContext
import ledger.LedgerFundSummaries
import ledger.Transaction
import java.math.BigDecimal
import java.time.LocalDateTime

typealias RecurrentBalanceTransaction = Transaction<BigDecimal, RecurrentBalanceTransactionDetail, FundRef>

data class RecurrentBalanceTransactionDetail(
    val timestamp: LocalDateTime,
    val generatedFrom: RecurrentTransaction
)
typealias CombinedRecurrentBalanceTransactionDetail = CombinedTransactionDetail<RecurrentBalanceTransaction>

object CombinedRecurrentBalanceTransactionDetailMonoid :
    CombinedTransactionDetailMonoid<RecurrentBalanceTransaction>()

object CombinedRecurrentBalanceTransactionDetailFactory :
    CombinedTransactionDetailFactory<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail> {
    override fun build(combinedTransactions: Collection<Transaction<BigDecimal, RecurrentBalanceTransactionDetail, FundRef>>): CombinedRecurrentBalanceTransactionDetail =
        CombinedRecurrentBalanceTransactionDetail(combinedTransactions)
}

object CombinedRecurrentBalanceTransactionDetailFactoryMonoid :
    CombinedTransactionDetailFactoryMonoid<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail>,
    CombinedTransactionDetailFactory<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail> by CombinedRecurrentBalanceTransactionDetailFactory,
    Monoid<CombinedRecurrentBalanceTransactionDetail> by CombinedRecurrentBalanceTransactionDetailMonoid

typealias RecurrentBalanceLedger = Ledger<BigDecimal, RecurrentBalanceTransactionDetail, FundRef>
typealias RecurrentBalanceTransactionLedgerFundSummary = CombinableSingleFundLedgerSummaryWithValue<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail>
typealias RecurrentBalanceTransactionLedgerFundSummaries = LedgerFundSummaries<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail, RecurrentBalanceTransactionLedgerFundSummary>
typealias RecurrentBalanceTransactionLedgerContext = LedgerContext<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail, RecurrentBalanceLedger, RecurrentBalanceTransactionLedgerFundSummary, RecurrentBalanceTransactionLedgerFundSummaries>
typealias RecurrentBalanceTransactionFundView = FundView<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail, RecurrentBalanceTransactionLedgerFundSummary>

object RecurrentBalanceTransactionFundViewFactory :
    FundViewFactory<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail, RecurrentBalanceTransactionLedgerFundSummary, RecurrentBalanceTransactionFundView> {
    override fun build(
        fund: Fund,
        fundSummaries: LedgerFundSummaries<BigDecimal, RecurrentBalanceTransactionDetail, FundRef, CombinedRecurrentBalanceTransactionDetail, RecurrentBalanceTransactionLedgerFundSummary>
    ): RecurrentBalanceTransactionFundView =
        RecurrentBalanceTransactionFundView(fund, fundSummaries)
}
