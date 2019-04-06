package common

import arrow.data.Reader
import arrow.data.State

interface CrudAPI<S, E> {
    fun create(name: String, description: String): Reader<S, State<S, E>>
    fun E.update(newEntity: E): Reader<S, State<S, Unit>>
    fun E.put(newEntity: E): Reader<S, State<S, Unit>>
    fun E.delete(): Reader<S, State<S, Unit>>
}