package graph

import arrow.core.Option
import arrow.core.extensions.list.foldable.nonEmpty
import arrow.core.getOption
import arrow.core.getOrElse
import arrow.mtl.Reader
import arrow.mtl.map
import arrow.mtl.reader
import arrow.mtl.runId
import common.*
import common.ValueWithError.Companion.toValue
import common.ValueWithError.Companion.withError
import common.ValueWithError.Companion.withErrors

data class HierarchicalElement<out F>(val relations: AncestorAndDescendantAdjacentListTree<F>) {}

object HierarchicalElementAPI {

    fun <F> empty(element: F) = HierarchicalElement(
        AncestorAndDescendantAdjacentListTree.empty(element)
    )

    fun <F> HierarchicalElement<F>.element(): F =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.element()
        }

    fun <F> HierarchicalElement<F>.children(): Set<F> =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.children()
        }

    fun <F> HierarchicalElement<F>.parent(): Set<F> =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.parents()
        }

    fun <F> HierarchicalElement<F>.descendants(): Set<F> =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.descendants()
        }

    fun <F> HierarchicalElement<F>.ancestors(): Set<F> =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.ancestors()
        }

    infix fun <F> HierarchicalElement<F>.isChildOf(element: F): Boolean =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.isChildOf(element)
        }

    infix fun <F> HierarchicalElement<F>.isDescendantOf(element: F): Boolean =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.isDescendantOf(element)
        }

    infix fun <F> HierarchicalElement<F>.isParentOf(element: F): Boolean =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.isParentOf(element)
        }

    infix fun <F> HierarchicalElement<F>.isAncestorsOf(element: F): Boolean =
        BiDirectionalAdjacentListTreeAPI.run {
            relations.isAncestorsOf(element)
        }

    infix fun <F> HierarchicalElement<F>.equalsOrChild(element: F): Boolean =
        this.element() == element || this.isChildOf(element)

    infix fun <F> HierarchicalElement<F>.equalsOrDescendant(element: F): Boolean =
        this.element() == element || this.isDescendantOf(element)

    infix fun <F> HierarchicalElement<F>.addChild(element: F): HierarchicalElement<F> =
        HierarchicalElement(
            BiDirectionalAdjacentListTreeAPI.run {
                relations addChild element
            }
        )

    infix fun <F> HierarchicalElement<F>.addParent(element: F): HierarchicalElement<F> =
        HierarchicalElement(
            BiDirectionalAdjacentListTreeAPI.run {
                relations addParent element
            }
        )

    infix fun <F> HierarchicalElement<F>.addDescendant(element: F): HierarchicalElement<F> =
        this addDescendants listOf(element)

    infix fun <F> HierarchicalElement<F>.addDescendants(elements: Collection<F>): HierarchicalElement<F> =
        HierarchicalElement(
            BiDirectionalAdjacentListTreeAPI.run {
                relations addDescendant elements
            }
        )

    infix fun <F> HierarchicalElement<F>.addAncestor(element: F): HierarchicalElement<F> =
        this addAncestors listOf(element)

    infix fun <F> HierarchicalElement<F>.addAncestors(elements: Collection<F>): HierarchicalElement<F> =
        HierarchicalElement(
            BiDirectionalAdjacentListTreeAPI.run {
                relations addAncestor elements
            }
        )

    operator fun <F> HierarchicalElement<F>.minus(element: F): HierarchicalElement<F> =
        HierarchicalElement(
            BiDirectionalAdjacentListTreeAPI.run {
                relations - element
            }
        )

    infix fun <F> F.toHierarchicalElement(hierarchicalTree: HierarchicalTree<F>): Option<HierarchicalElement<F>> {
        val element = this
        return HierarchicalTreeApi.run {
            element.toHierarchicalElement(hierarchicalTree)
        }
    }
}

data class HierarchicalTree<F>(val elementToHierarchicalElement: Map<F, HierarchicalElement<F>>) {
    companion object {
        fun <F> empty() = HierarchicalTree<F>(emptyMap())
    }
}

data class ParentChild<F>(val parent: F, val child: F)

object HierarchicalTreeApi {
    fun <F> F.toHierarchicalElement(hierarchicalTree: HierarchicalTree<F>): Option<HierarchicalElement<F>> =
        hierarchicalTree.elementToHierarchicalElement.getOption(this)

    infix fun <F> F.isChildOrEqual(element: F): Reader<HierarchicalTree<F>, Boolean> =
        (this isChildOf element).map { this == element || it }

    infix fun <F> F.isDescendantOrEqual(element: F): Reader<HierarchicalTree<F>, Boolean> =
        (this descendantOf element).map { this == element || it }

    infix fun <F> F.isChildOf(parent: F): Reader<HierarchicalTree<F>, Boolean> =
        this.check(
            parent,
            { hierarchicalElement, element ->
                HierarchicalElementAPI.run {
                    hierarchicalElement isChildOf element
                }
            })

    infix fun <F> F.descendantOf(parent: F): Reader<HierarchicalTree<F>, Boolean> =
        this.check(
            parent,
            { hierarchicalElement, element ->
                HierarchicalElementAPI.run {
                    hierarchicalElement isDescendantOf element
                }
            })

    infix fun <F> F.parentOf(child: F): Reader<HierarchicalTree<F>, Boolean> =
        this.check(
            child,
            { hierarchicalElement, element -> HierarchicalElementAPI.run { hierarchicalElement.isParentOf(element) } })

    infix fun <F> F.ancestorOf(parent: F): Reader<HierarchicalTree<F>, Boolean> =
        this.check(
            parent,
            { hierarchicalElement, element ->
                HierarchicalElementAPI.run {
                    hierarchicalElement isAncestorsOf element
                }
            })

    private fun <F> F.check(
        element: F,
        predicate: (HierarchicalElement<F>, F) -> Boolean
    ): Reader<HierarchicalTree<F>, Boolean> =
        { hierarchicalTree: HierarchicalTree<F> ->
            this.toHierarchicalElement(hierarchicalTree).map {
                predicate(it, element)
            }.getOrElse { false }
        }.reader()

    fun <F> F.ancestors(): Reader<HierarchicalTree<F>, Set<F>> =
        this.mapHierarchicalElement { hierarchicalElement ->
            HierarchicalElementAPI.run { hierarchicalElement.ancestors() }
        }

    fun <F> F.descendants(): Reader<HierarchicalTree<F>, Set<F>> =
        this.mapHierarchicalElement { hierarchicalElement ->
            HierarchicalElementAPI.run { hierarchicalElement.descendants() }
        }

    private fun <F> F.mapHierarchicalElement(
        map: (HierarchicalElement<F>) -> Set<F>
    ): Reader<HierarchicalTree<F>, Set<F>> =
        { hierarchicalTree: HierarchicalTree<F> ->
            this.toHierarchicalElement(hierarchicalTree).map {
                map(it)
            }.getOrElse { emptySet() }
        }.reader()

    operator fun <F> HierarchicalTree<F>.plus(
        relation: ParentChild<F>
    ): ValueWithError<HierarchicalTree<F>> =
        this + listOf(relation)

    operator fun <F> HierarchicalTree<F>.plus(
        relations: Collection<ParentChild<F>>
    ): ValueWithError<HierarchicalTree<F>> =
        relations.fold(
            this.toValue(),
            { previous, newRelation ->
                previous.v.addParentChildRelation(newRelation.parent, newRelation.child).withErrors(previous.e)
            })

    fun <F> HierarchicalTree<F>.addParentChildRelation(parent: F, child: F): ValueWithError<HierarchicalTree<F>> =
        if (parent == child) {
            this.toValue().withError(ChildCannotBeParentOfItselfError(child))
        } else {
            val errors: List<Error> = (parent descendantOf child).map {
                if (it)
                    listOf(ParentCannotAlsoBeADescendantOfSameElementError(parent, child))
                else emptyList()
            }.runId(this) + (child ancestorOf parent).map {
                if (it)
                    listOf(ChildCannotAlsoBeAnAncestorOfSameElementError(parent, child))
                else emptyList()
            }.runId(this)

            if (errors.nonEmpty()) this.toValue().withErrors(errors)
            else this.unsafeAdd(parent, child).toValue()
        }


    private fun <F> HierarchicalTree<F>.unsafeAdd(
        parent: F, child: F
    ): HierarchicalTree<F> {

        val ancestorsOfParent: Set<F> = parent.ancestors().runId(this)
        val descendantOfChild: Set<F> = child.descendants().runId(this)

        val ancestorUpdates = this.elementToHierarchicalElement
            .filterKeys { ancestorsOfParent.contains(it) }
            .mapValues {
                HierarchicalElementAPI.run {
                    it.value addDescendants (descendantOfChild + child)
                }
            }

        val descendantUpdates = this.elementToHierarchicalElement
            .filterKeys { descendantOfChild.contains(it) }
            .mapValues {
                HierarchicalElementAPI.run {
                    it.value addAncestors (ancestorsOfParent + child)
                }
            }

        val parentUpdates = this.elementToHierarchicalElement.getOption(parent).map {
            listOf(
                Pair(parent,
                    HierarchicalElementAPI.run {
                        it addChild child addDescendants descendantOfChild
                    }
                )
            )
        }.getOrElse {
            listOf(
                Pair(parent,
                    HierarchicalElementAPI.run {
                        empty(parent) addChild child addDescendants descendantOfChild
                    }
                )
            )
        }

        val childUpdates = this.elementToHierarchicalElement.getOption(child).map {
            listOf(
                Pair(child,
                    HierarchicalElementAPI.run {
                        it addParent parent addAncestors ancestorsOfParent
                    }
                )
            )
        }.getOrElse {
            listOf(
                Pair(child,
                    HierarchicalElementAPI.run {
                        empty(child) addParent parent addAncestors ancestorsOfParent
                    }
                )
            )
        }

        return HierarchicalTree(
            this.elementToHierarchicalElement + descendantUpdates + ancestorUpdates + childUpdates + parentUpdates
        )
    }

    operator fun <F> HierarchicalTree<F>.minus(element: F): HierarchicalTree<F> {
        val ancestors: Set<F> = element.ancestors().runId(this)
        val descendant: Set<F> = element.descendants().runId(this)

        return HierarchicalTree(elementToHierarchicalElement +
                HierarchicalElementAPI.run {
                    elementToHierarchicalElement
                        .filterKeys { (ancestors + descendant).contains(it) }.mapValues { it.value - element }
                } - element)
    }

    tailrec operator fun <F> HierarchicalTree<F>.minus(elements: Collection<F>): HierarchicalTree<F> =
        if (elements.isEmpty()) this else {
            this - elements.first() - (elements - elements.first())
        }
}