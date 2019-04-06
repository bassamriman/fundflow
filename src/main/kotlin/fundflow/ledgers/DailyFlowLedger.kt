package fundflow.ledgers

import common.AmountOps
import fundflow.DailyFlow
import fundflow.DailyFlowOps
import fundflow.FundRef
import ledger.CombinableQuantificationOps
import ledger.Ledger
import ledger.Transaction
import java.math.BigDecimal


object DailyFlowQuantificationOps : CombinableQuantificationOps<DailyFlow> {
    override fun DailyFlow.isNegative(): Boolean = this.value < BigDecimal.ZERO
    override fun empty(): DailyFlow = DailyFlowOps.ZERO
    override fun DailyFlow.combine(b: DailyFlow): DailyFlow {
        val a = this
        return AmountOps.run {
            a + b
        }
    }

    override fun DailyFlow.unaryMinus(): DailyFlow = this.let { AmountOps.run { -it } }
}

typealias DailyFlowTransaction = Transaction<DailyFlow, Unit, FundRef>
typealias DailyFlowLedger = Ledger<DailyFlow, Unit, FundRef>

