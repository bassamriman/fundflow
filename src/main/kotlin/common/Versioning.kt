package common

import java.util.*

data class Version<out E, out A>(
    val element: E,
    val action: A,
    override val id: String = UUID.randomUUID().toString()
) :
    Identifiable

data class Versioning<out E, out A>(
    val currentVersionIndex: Int,
    val versions: List<Version<E, A>>,
    override val id: String = UUID.randomUUID().toString()
) : Identifiable

object VersioningAPI {

    fun <E, A> Versioning<E, A>.undo(): Versioning<E, A> =
        if (currentVersionIndex - 1 in versions.indices) this.copy(currentVersionIndex - 1) else this

    fun <E, A> Versioning<E, A>.redo(): Versioning<E, A> =
        if (currentVersionIndex + 1 in versions.indices) this.copy(currentVersionIndex + 1) else this

    fun <E, A> Versioning<E, A>.new(version: Version<E, A>): Versioning<E, A> =
        this.copy(
            currentVersionIndex = currentVersionIndex + 1,
            versions = this.versions.subList(0, currentVersionIndex + 1) + version
        )

    fun <E, A> Versioning<E, A>.current(): Version<E, A> = this.versions[this.currentVersionIndex]
}