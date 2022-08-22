package group.phorus.mapper

import kotlin.reflect.KType
import kotlin.reflect.full.createType

typealias Field = String
typealias OriginalField = Field
typealias TargetField = Field
typealias MappingFunction = Function<*>
typealias Value = Any

data class PropertyWrapper<T>(val value: T)

fun KType.removeNullability() = this.let {
    it.classifier?.createType(
        arguments = it.arguments,
        nullable = false,
        annotations = it.annotations,
    ) ?: it
}