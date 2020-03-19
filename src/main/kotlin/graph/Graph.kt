package graph

data class AdjacentListGraph<out F>(val element: F, val adjacentElements: Set<F>) {
    companion object {
        fun <F> empty(element: F): AdjacentListGraph<F> =
            AdjacentListGraph(element, emptySet())
    }
}

object AdjacentListGraphAPI {
    infix fun <F> AdjacentListGraph<F>.isAdjacentTo(element: F) =
        adjacentElements.contains(element)

    operator fun <F> AdjacentListGraph<F>.plus(element: F): AdjacentListGraph<F> =
        this + listOf(element)

    operator fun <F> AdjacentListGraph<F>.plus(elements: Collection<F>): AdjacentListGraph<F> =
        this.copy(adjacentElements = this.adjacentElements + elements)

    operator fun <F> AdjacentListGraph<F>.minus(element: F): AdjacentListGraph<F> =
        this - listOf(element)

    operator fun <F> AdjacentListGraph<F>.minus(elements: Collection<F>): AdjacentListGraph<F> =
        this.copy(adjacentElements = this.adjacentElements + elements)
}

data class BiDirectionalAdjacentListGraph<out F>(
    val element: F,
    val incoming: AdjacentListGraph<F>,
    val outgoing: AdjacentListGraph<F>
) {
    companion object {
        fun <F> empty(element: F): BiDirectionalAdjacentListGraph<F> =
            BiDirectionalAdjacentListGraph(
                element = element,
                incoming = AdjacentListGraph.empty(element),
                outgoing = AdjacentListGraph.empty(element)
            )
    }
}

object BiDirectionalAdjacentListGraphAPI {

    infix fun <F> BiDirectionalAdjacentListGraph<F>.pointsTo(element: F) =
        AdjacentListGraphAPI.run { outgoing.isAdjacentTo(element) }

    infix fun <F> BiDirectionalAdjacentListGraph<F>.pointedToBy(element: F) =
        AdjacentListGraphAPI.run { incoming.isAdjacentTo(element) }

    infix fun <F> BiDirectionalAdjacentListGraph<F>.addOutgoing(element: F): BiDirectionalAdjacentListGraph<F> =
        this addOutgoing listOf(element)

    infix fun <F> BiDirectionalAdjacentListGraph<F>.addOutgoing(elements: Collection<F>): BiDirectionalAdjacentListGraph<F> =
        this.copy(outgoing = AdjacentListGraphAPI.run { outgoing + elements })

    infix fun <F> BiDirectionalAdjacentListGraph<F>.addIncoming(element: F): BiDirectionalAdjacentListGraph<F> =
        this addIncoming listOf(element)

    infix fun <F> BiDirectionalAdjacentListGraph<F>.addIncoming(elements: Collection<F>): BiDirectionalAdjacentListGraph<F> =
        this.copy(incoming = AdjacentListGraphAPI.run { incoming + elements })

    infix fun <F> BiDirectionalAdjacentListGraph<F>.removeOutgoing(element: F): BiDirectionalAdjacentListGraph<F> =
        this removeOutgoing listOf(element)

    infix fun <F> BiDirectionalAdjacentListGraph<F>.removeOutgoing(elements: Collection<F>): BiDirectionalAdjacentListGraph<F> =
        this.copy(outgoing = AdjacentListGraphAPI.run { outgoing - elements })

    infix fun <F> BiDirectionalAdjacentListGraph<F>.removeIncoming(element: F): BiDirectionalAdjacentListGraph<F> =
        this removeIncoming listOf(element)

    infix fun <F> BiDirectionalAdjacentListGraph<F>.removeIncoming(elements: Collection<F>): BiDirectionalAdjacentListGraph<F> =
        this.copy(incoming = AdjacentListGraphAPI.run { incoming - elements })

    operator fun <F> BiDirectionalAdjacentListGraph<F>.minus(element: F): BiDirectionalAdjacentListGraph<F> =
        this - listOf(element)

    operator fun <F> BiDirectionalAdjacentListGraph<F>.minus(elements: Collection<F>): BiDirectionalAdjacentListGraph<F> =
        this.removeOutgoing(elements).removeIncoming(elements)
}


data class AncestorAndDescendantAdjacentListTree<out F>(
    val element: F,
    val parentsChildren: BiDirectionalAdjacentListGraph<F>,
    val ancestorsDescendants: BiDirectionalAdjacentListGraph<F>
) {
    companion object {
        fun <F> empty(element: F): AncestorAndDescendantAdjacentListTree<F> =
            AncestorAndDescendantAdjacentListTree(
                element = element,
                parentsChildren = BiDirectionalAdjacentListGraph.empty(element),
                ancestorsDescendants = BiDirectionalAdjacentListGraph.empty(element)
            )
    }
}

object BiDirectionalAdjacentListTreeAPI {
    fun <F> AncestorAndDescendantAdjacentListTree<F>.element(): F = this.element
    fun <F> AncestorAndDescendantAdjacentListTree<F>.parents(): Set<F> = this.parentsChildren.incoming.adjacentElements
    fun <F> AncestorAndDescendantAdjacentListTree<F>.children(): Set<F> = this.parentsChildren.outgoing.adjacentElements
    fun <F> AncestorAndDescendantAdjacentListTree<F>.ancestors(): Set<F> =
        this.ancestorsDescendants.incoming.adjacentElements

    fun <F> AncestorAndDescendantAdjacentListTree<F>.descendants(): Set<F> =
        this.ancestorsDescendants.outgoing.adjacentElements

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.isChildOf(element: F): Boolean =
        BiDirectionalAdjacentListGraphAPI.run { parentsChildren.pointedToBy(element) }

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.isDescendantOf(element: F): Boolean =
        BiDirectionalAdjacentListGraphAPI.run { ancestorsDescendants.pointedToBy(element) }

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.isParentOf(element: F): Boolean =
        BiDirectionalAdjacentListGraphAPI.run { parentsChildren.pointsTo(element) }

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.isAncestorsOf(element: F): Boolean =
        BiDirectionalAdjacentListGraphAPI.run { ancestorsDescendants.pointsTo(element) }

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addChild(child: F): AncestorAndDescendantAdjacentListTree<F> =
        this addChildren listOf(child)

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addChildren(children: Collection<F>): AncestorAndDescendantAdjacentListTree<F> {
        return this.copy(
            parentsChildren = BiDirectionalAdjacentListGraphAPI.run { parentsChildren addOutgoing children }
        ).addDescendant(children)
    }

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addDescendant(descendant: F): AncestorAndDescendantAdjacentListTree<F> =
        this addDescendant listOf(descendant)

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addDescendant(descendants: Collection<F>): AncestorAndDescendantAdjacentListTree<F> =
        this.copy(
            ancestorsDescendants = BiDirectionalAdjacentListGraphAPI.run { ancestorsDescendants addOutgoing descendants }
        )

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addParent(parent: F): AncestorAndDescendantAdjacentListTree<F> =
        this addParents listOf(parent)

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addParents(parents: Collection<F>): AncestorAndDescendantAdjacentListTree<F> {
        return this.copy(
            parentsChildren = BiDirectionalAdjacentListGraphAPI.run { parentsChildren addIncoming parents }
        ).addAncestor(parents)
    }

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addAncestor(ancestor: F): AncestorAndDescendantAdjacentListTree<F> =
        this addAncestor listOf(ancestor)

    infix fun <F> AncestorAndDescendantAdjacentListTree<F>.addAncestor(ancestors: Collection<F>): AncestorAndDescendantAdjacentListTree<F> =
        this.copy(
            ancestorsDescendants = BiDirectionalAdjacentListGraphAPI.run { ancestorsDescendants addIncoming ancestors }
        )

    operator fun <F> AncestorAndDescendantAdjacentListTree<F>.minus(element: F): AncestorAndDescendantAdjacentListTree<F> =
        this - listOf(element)

    operator fun <F> AncestorAndDescendantAdjacentListTree<F>.minus(elements: Collection<F>): AncestorAndDescendantAdjacentListTree<F> =
        this.copy(
            parentsChildren = BiDirectionalAdjacentListGraphAPI.run { parentsChildren - elements },
            ancestorsDescendants = BiDirectionalAdjacentListGraphAPI.run { ancestorsDescendants - elements }
        )

}