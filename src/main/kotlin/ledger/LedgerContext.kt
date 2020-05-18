package ledger

import arrow.core.Option
import arrow.core.getOption
import arrow.core.k
import arrow.syntax.collections.flatten
import common.DateTimeInterval
import fundflow.Fund
import fundflow.FundRef

data class LedgerFundSummaries<Q, D, F, CD, S : CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>>(
    val timeInterval: DateTimeInterval,
    val fund: FundRef,
    val summary: S,
    val summaryWithHierarchy: S
)

data class LedgerContext<Q, D, F, CD, L : Ledger<Q, D, F>, S : CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>, LS : LedgerFundSummaries<Q, D, F, CD, S>>(
    val funds: Map<FundRef, Fund>,
    val timeInterval: DateTimeInterval,
    val globalBalanceLedger: L,
    val fundSummaries: Map<FundRef, LS>
)

data class FundView<Q, D, F, CD, S : CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>>(
    val fund: Fund,
    val fundSummaries: LedgerFundSummaries<Q, D, F, CD, S>
)

interface FundViewFactory<Q, D, F, CD, S : CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>, FV : FundView<Q, D, F, CD, S>> {
    fun build(fund: Fund, fundSummaries: LedgerFundSummaries<Q, D, F, CD, S>): FV
}

object LedgerContextAPI {
    fun <Q, D, F, CD,
            L : Ledger<Q, D, F>,
            S : CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>,
            LS : LedgerFundSummaries<Q, D, F, CD, S>,
            FV : FundView<Q, D, F, CD, S>>
            LedgerContext<Q, D, F, CD, L, S, LS>.view(
                fundRef: FundRef,
                fundView: FundViewFactory<Q, D, F, CD, S, FV>
            ): Option<FV> =
        this.funds.k().getOption(fundRef).flatMap { fund ->
            this.fundSummaries.getOption(fundRef).map { fundView.build(fund, it) }
        }

    fun <Q, D, F, CD,
            L : Ledger<Q, D, F>,
            S : CombinableSingleFundLedgerSummaryWithValue<Q, D, F, CD>,
            LS : LedgerFundSummaries<Q, D, F, CD, S>,
            FV : FundView<Q, D, F, CD, S>>
            LedgerContext<Q, D, F, CD, L, S, LS>.viewAll(fundView: FundViewFactory<Q, D, F, CD, S, FV>): List<FV> {
        val maybeFundViews: List<Option<FV>> = this.funds.values.map { fund ->
            this.fundSummaries.getOption(fund.reference).map { fundView.build(fund, it) }
        }
        return maybeFundViews.flatten()
    }
}
