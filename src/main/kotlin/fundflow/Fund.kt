package fundflow

import common.Describable
import common.Identifiable
import common.Nameable
import fundflow.ledgers.BalanceTransactionLedgerFundSummaries
import fundflow.ledgers.RecurrentTransactionLedgerFundSummaries
import ledger.HierarchicalElement
import ledger.HierarchicalTree
import ledger.ParentChild
import ledger.SingleFundLedgerSummary
import java.util.*

data class FundRef(override val id: UUID) :
    Identifiable {
    companion object {
        fun invoke(
            id: UUID = UUID.randomUUID()
        ): FundRef = FundRef(id)
    }
}

data class Fund(
    override val name: String,
    override val description: String,
    val reference: FundRef = FundRef(UUID.randomUUID())
) : Describable,
    Nameable

typealias FundHierarchy = HierarchicalTree<FundRef>
typealias HierarchicalFund = HierarchicalElement<FundRef>

typealias FundRelation = ParentChild<FundRef>


