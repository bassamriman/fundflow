package common

import fundflow.FundRef
import io.kotlintest.properties.Gen
import ledger.HierarchicalElement

typealias TestHierarchicalElement = HierarchicalElement<FundRef>

class TestHierarchicalElementGen : Gen<TestHierarchicalElement> {
    override fun constants(): Iterable<TestHierarchicalElement> = emptyList()
    override fun random(): Sequence<TestHierarchicalElement> = generateSequence {
        TestHierarchicalElement(FundRefGen().random().first(), Gen.list(FundRefGen()).random().first())
    }
}