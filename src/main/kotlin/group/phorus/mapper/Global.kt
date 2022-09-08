package group.phorus.mapper

import group.phorus.mapper.mapping.MappingFallback
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * [String] that represents a field. Can be just a field name, or a field location.
 *
 * If it's a location, use "/" as separator and ".." to refer to the parent location.
 *
 * For example:
 * - "name"
 * - "pet/name"
 * - "../id"
 */
typealias Field = String

/**
 * [Field] from the original class or entity.
 */
typealias OriginalField = Field

/**
 * [Field] from the target class or entity.
 */
typealias TargetField = Field

/**
 * A mutating function. Usually used to transform an [OriginalField] value to a preferred [TargetField] value.
 */
typealias MappingFunction = Function<*>

/**
 * A value.
 */
typealias Value = Any

/**
 * A map of fields to map forcefully, with the format:
 * [OriginalField] - [TargetField] - [MappingFallback]
 */
typealias Mappings = Map<OriginalField, Pair<TargetField, MappingFallback>>

/**
 * a map of fields to map forcefully, with the format:
 * [OriginalField][KProperty] - [TargetField][KProperty] - [MappingFallback]
 */
typealias KMappings = Map<KProperty<*>, Pair<KProperty<*>, MappingFallback>>

/**
 * A map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField] - [MappingFunction] - [TargetField] - [MappingFallback]
 */
typealias FunctionMappings = Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>>

/**
 * A map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField][KProperty] - [MappingFunction] - [TargetField][KProperty] - [MappingFallback]
 */
typealias KFunctionMappings = Map<KProperty<*>?, Pair<MappingFunction, Pair<KProperty<*>, MappingFallback>>>

/**
 * A value wrapper.
 *
 * @param T the value type.
 */
internal data class Wrapper<T>(val value: T)

/**
 * Removes the nullability of a [KType].
 *
 * @receiver type to remove the nullability from.
 * @see [createType].
 */
internal fun KType.removeNullability() = this.let {
    it.classifier?.createType(
        arguments = it.arguments,
        nullable = false,
        annotations = it.annotations,
    ) ?: it
}