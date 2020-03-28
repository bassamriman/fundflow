package fundflow

import common.Describable
import common.Identifiable
import common.Nameable
import graph.HierarchicalElement
import graph.HierarchicalTree
import graph.ParentChild
import java.util.*

data class FundRef(override val id: String = UUID.randomUUID().toString()) : Identifiable

data class Fund(
    override val name: String,
    override val description: String,
    val reference: FundRef = FundRef()
) : Describable,
    Nameable {
    companion object {
        fun empty(): Fund = Fund(name = "", description = "")
    }
}

typealias FundHierarchy = HierarchicalTree<FundRef>
typealias HierarchicalFund = HierarchicalElement<FundRef>

typealias FundRelation = ParentChild<FundRef>


