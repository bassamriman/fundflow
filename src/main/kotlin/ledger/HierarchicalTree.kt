package ledger

import arrow.core.*
import arrow.data.*
import arrow.data.extensions.list.foldable.find

data class HierarchicalTree<F>(val childrenOf: Map<F, Set<F>>, val parentsOf: Map<F, Set<F>>) {
    companion object {
        fun <F> empty() = HierarchicalTree<F>(emptyMap(), emptyMap())
    }
}

data class ParentChild<F>(val parent: F, val child: F)

object HierarchicalTreeException {
    data class RelationCausesCycleInTree(val relation: ParentChild<*>) :
        IllegalArgumentException("This cause cycle in tree")
}

data class HierarchicalElement<F>(val element: F, val children: Collection<F>)

object HierarchicalElementAPI {

    fun <F> empty(element: F) = HierarchicalElement(element, emptyList())

    infix fun <F> HierarchicalElement<F>.equals(element: F): Boolean =
        this.element == element || children.contains(element)

    infix fun <F> F.toHierarchicalElement(hierarchicalTree: HierarchicalTree<F>): HierarchicalElement<F> {
        val element = this
        return HierarchicalTreeApi.run {
            HierarchicalElement(element, element.children(hierarchicalTree))
        }
    }
}

object HierarchicalTreeApi {

    infix fun <F> ParentChild<F>.equals(element: F): Reader<HierarchicalTree<F>, Boolean> =
        { hierarchicalTree: HierarchicalTree<F> -> element.childOrEqual(this.parent, hierarchicalTree) }.reader()

    fun <F> HierarchicalTree<F>.formParentChild(parent: F, child: F): Option<ParentChild<F>> =
        if (child.childOrEqual(parent, this)) Some(ParentChild(parent, child)) else None

    fun <F> F.childOrEqual(parent: F, hierarchicalTree: HierarchicalTree<F>): Boolean =
        this == parent || hierarchicalTree.parentsOf.k().getOption(this).map { it.contains(parent) }.getOrElse { false }

    infix fun <F> F.parentOf(child: F): Reader<HierarchicalTree<F>, Boolean> =
        { hierarchicalTree: HierarchicalTree<F> ->
            hierarchicalTree.childrenOf.k().getOption(this).map { it.contains(child) }.getOrElse { false }
        }.reader()


    infix fun <F> F.childOf(parent: F): Reader<HierarchicalTree<F>, Boolean> =
        { hierarchicalTree: HierarchicalTree<F> ->
            hierarchicalTree.parentsOf.k().getOption(this).map { it.contains(parent) }.getOrElse { false }
        }.reader()

    fun <F> F.parents(hierarchicalTree: HierarchicalTree<F>): Set<F> =
        hierarchicalTree.parentsOf.k().getOption(this).getOrElse { emptySet() }

    fun <F> F.children(hierarchicalTree: HierarchicalTree<F>): Set<F> =
        hierarchicalTree.childrenOf.k().getOption(this).getOrElse { emptySet() }


    operator fun <F> HierarchicalTree<F>.plus(
        relations: Collection<ParentChild<F>>
    ): Try<HierarchicalTree<F>> = relations.toList().k().find { this.causesCycle(it) }
        .map { Try.raiseError(HierarchicalTreeException.RelationCausesCycleInTree(it)) }
        .getOrElse { Try.just(this.unsafeAdd(relations)) }

    private tailrec fun <F> HierarchicalTree<F>.unsafeAdd(
        relations: Collection<ParentChild<F>>
    ): HierarchicalTree<F> =
        if (relations.isEmpty())
            this
        else {
            this.unsafeAdd(relations.first()).unsafeAdd(relations - relations.first())
        }

    private fun <F> HierarchicalTree<F>.causesCycle(relation: ParentChild<F>): Boolean =
        relation.parent.childOf(relation.child).runId(this) || relation.child.parentOf(relation.parent).runId(this)

    operator fun <F> HierarchicalTree<F>.plus(
        relation: ParentChild<F>
    ): Try<HierarchicalTree<F>> =
        if (this.causesCycle(relation)) Try.raiseError(
            HierarchicalTreeException.RelationCausesCycleInTree(relation)
        ) else {
            Try.just(this.unsafeAdd(relation))
        }

    private fun <F> HierarchicalTree<F>.unsafeAdd(
        relation: ParentChild<F>
    ): HierarchicalTree<F> {
        val parentsOfParent = relation.parent.parents(this)
        val childrenOfChild = relation.child.children(this)

        val elementsToAddToChildrenOfChild = relation.parent.parents(this) + relation.parent
        val elementsToAddToParentsOfParent = relation.child.children(this) + relation.child
        val newTree = HierarchicalTree(
            this.childrenOf + this.childrenOf.filterKeys { parentsOfParent.contains(it) }.mapValues { it.value + elementsToAddToParentsOfParent } + Pair(
                relation.parent,
                this.childrenOf.getOrElseEmpty(relation.parent) + elementsToAddToParentsOfParent
            ),
            this.parentsOf + this.parentsOf.filterKeys { childrenOfChild.contains(it) }.mapValues { it.value + elementsToAddToChildrenOfChild } + Pair(
                relation.child,
                this.parentsOf.getOrElseEmpty(relation.child) + elementsToAddToChildrenOfChild
            )
        )
        return newTree
    }


    operator fun <F> HierarchicalTree<F>.minus(element: F): HierarchicalTree<F> =
        HierarchicalTree(
            (this.childrenOf - element).mapValues { it.value - element },
            (this.parentsOf - element).mapValues { it.value - element }
        )

    tailrec operator fun <F> HierarchicalTree<F>.minus(elements: Collection<F>): HierarchicalTree<F> =
        if (elements.isEmpty()) this else {
            this.minus(elements.first()) - (elements - elements.first())
        }

    private fun <F> Map<F, Set<F>>.getOrElseEmpty(element: F): Set<F> =
        this.k().getOption(element).getOrElse { emptySet() }

}