package common

import arrow.core.Try
import arrow.data.k
import java.util.*
import kotlin.reflect.KClass

interface Entity : Identifiable
data class EntityRef<E : Entity>(override val id: String) : Identifiable
/*

interface EntityService {
    fun <E : Any> safeCast(entity: Entity, clazz: KClass<E>): E = clazz.javaObjectType.cast(this)
    fun <E : Entity> create(entity: E):  <UUID>
    fun <E : Any> load(id: UUID, clazz: KClass<E>): E
    fun <E : Any> update(id: UUID, entity: E, clazz: KClass<E>): E
    fun delete(id: UUID): Unit
}

class InMemoryEntityService : EntityService{

    var cache : Map<String, Entity> = emptyMap()

    override fun <E : Entity> create(entity: E): Try<UUID> = cache.k().get

    override fun <E : Any> load(id: UUID, clazz: KClass<E>): E {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <E : Any> update(id: UUID, entity: E, clazz: KClass<E>): E {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(id: UUID): Unit {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

inline fun <reified E : Entity> EntityService.load(id: UUID): E = this.load(id, E::class)
inline fun <reified E : Entity> EntityService.create(e: E): UUID = this.create(e)
inline fun <reified E : Entity> EntityService.udpate(id: UUID, e: E): E = this.update(id, e, E::class)
fun EntityService.delete(id: UUID): Unit = this.delete(id)

inline fun <reified E : Entity> EntityRef<E>.load(service: EntityService): E = service.load(id)
inline fun <reified E : Entity> EntityRef<E>.update(e: E, service: EntityService): E = service.udpate(id, e)
inline fun <reified E : Entity> EntityRef<E>.delete(service: EntityService): Unit = service.delete(id)


*/