package common

import arrow.core.Option


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

interface ValueWithError<V> {
    val v: V
    val e: Collection<Error>

    companion object {
        fun <V> V.ve(): ValueWithError<V> = ValueWithErrorImpl(this, emptyList())

        fun <V> ValueWithError<V>.withError(error: Error): ValueWithError<V> =
            ValueWithErrorImpl(this.v, this.e + error)

        fun <V> ValueWithError<V>.withErrors(errors: Collection<Error>): ValueWithError<V> =
            ValueWithErrorImpl(this.v, this.e + errors)

    }
}

data class ValueWithErrorImpl<V>(override val v: V, override val e: Collection<Error>) :
    ValueWithError<V>