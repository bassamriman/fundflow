package common


interface Error {
    val message: String
}

data class ChildCannotAlsoBeAnAncestorOfSameElementError<F>(val parent: F, val child: F) : Error {
    override val message: String =
        "Element $child is ancestor of $parent. Hence, it cannot be also be a child"
}

data class ParentCannotAlsoBeADescendantOfSameElementError<F>(val parent: F, val child: F) : Error {
    override val message: String =
        "Element $parent is a descendant of $child. Hence, it cannot be also be a parent"
}

data class ChildCannotBeParentOfItselfError<F>(val element: F) : Error {
    override val message: String =
        "Element $element cannot be parent of itself"
}

data class ValueWithError<V>(val v: V, val e: Collection<Error>) {
    companion object {
        fun <V> V.toValue(): ValueWithError<V> = ValueWithError(this, emptyList())
        fun <V> ValueWithError<V>.withError(error: Error): ValueWithError<V> =
            ValueWithError(this.v, this.e + error)

        fun <V> ValueWithError<V>.withErrors(errors: Collection<Error>): ValueWithError<V> =
            ValueWithError(this.v, this.e + errors)
    }
}
