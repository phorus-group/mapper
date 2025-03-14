package group.phorus.mapper.mapping.functions

import group.phorus.mapper.*
import group.phorus.mapper.mapping.MappingFallback
import group.phorus.mapper.mapping.MappingFallback.CONTINUE
import group.phorus.mapper.mapping.MappingFallback.NULL
import group.phorus.mapper.mapping.mapTo
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

/**
 * Transforms a location string to a location list.
 *
 * @param location the location to be parsed, use "/" as field separators and ".." to refer to the parent node.
 *
 * For example:
 *  - {field}/{field}
 *  - ../{field}/{field}
 *
 * Note: Any single dot will be ignored, since it's considered as the current location.
 *
 * @return the location list.
 */
internal fun parseLocation(location: String): List<String> =
    location.split("/")
        .toMutableList()
        .apply { removeIf { it.isBlank() || it == "." } }


/**
 * Process mapping fallback.
 */
internal enum class ProcessMappingFallback {
    NULL, SKIP, CONTINUE_OR_THROW, NULL_OR_THROW,
}

internal fun ProcessMappingFallback.isNull() =
    this == ProcessMappingFallback.NULL || this == ProcessMappingFallback.NULL_OR_THROW

/**
 * Transforms a [MappingFallback] to [ProcessMappingFallback].
 *
 * @receiver the mapping fallback to transform.
 * @return the process mapping fallback.
 */
internal fun MappingFallback.toProcessMappingFallback() =
    when(this) {
        MappingFallback.NULL -> ProcessMappingFallback.NULL
        MappingFallback.CONTINUE -> ProcessMappingFallback.SKIP
        MappingFallback.CONTINUE_OR_THROW -> ProcessMappingFallback.CONTINUE_OR_THROW
        MappingFallback.NULL_OR_THROW -> ProcessMappingFallback.NULL_OR_THROW
    }

/**
 * Returns a map of target class locations with their mapped values.
 *
 * @param originalEntity the original entity to take the values from.
 * @param targetClass the target class.
 * @param mappings containing the original field, an optional mutation function, the target field, and a
 *  fallback used in case the function fails to process the values.
 * @param exclusions excluded fields from the target class, exclusions take priority
 *  over everything else.
 * @return a map of the target class locations and their mapped values.
 */
internal fun processMappings(
    originalEntity: OriginalNodeInterface<*>,
    targetClass: TargetClass<*>,
    mappings: Map<OriginalField?, Pair<MappingFunction?, Pair<TargetField, ProcessMappingFallback>>>,
    exclusions: List<TargetField>,
): Map<TargetField, Value?> =
    mappings
        .filterNot { isExcluded(it.value.second.first, exclusions) } // Filter out excluded properties
        .mapNotNull { mapping ->
            val originalProp = mapping.key?.let { originalEntity.findProperty(parseLocation(it))}
            val targetField = targetClass.findProperty(parseLocation(mapping.value.second.first))
                ?: return@mapNotNull null

            val originalPropValue: Wrapper<Any?>? = processFunction(
                originalProp = originalProp,
                function = mapping.value.first,
                targetField = targetField,
                fallback = mapping.value.second.second,
            ) ?: if (originalProp == null) {
                // If the original prop is null, skip the value or not based on the mapping fallback
                if (targetField.type.isMarkedNullable && mapping.value.second.second.isNull()) {
                    Wrapper(null)
                } else null
            } else if (originalProp.value == null) {
                // If the original prop value is null and the target property is not nullable, return null to skip
                //  the mapping
                if (targetField.type.isMarkedNullable && mapping.value.second.second.isNull()) {
                    Wrapper(null)
                } else null
            } else {
                // If the mapping has a function and reaches this point, something failed
                if (mapping.value.first != null) {
                    if (targetField.type.isMarkedNullable && mapping.value.second.second.isNull()) {
                        Wrapper(null)
                    } else null
                } else {
                    // If the target prop type is not a supertype or the same type as the original value type, then
                    //  map the value
                    val finalProp = if (!targetField.type.isSupertypeOf(originalProp.type.removeNullability())) {
                        mapTo(originalEntity = originalProp, targetType = targetField.type)
                    } else originalProp.value

                    // If the mapped value is null, return it or return null based on the fallback and the nullability
                    //  of the target field
                    if (finalProp == null) {
                        if (targetField.type.isMarkedNullable && mapping.value.second.second.isNull()) {
                           Wrapper(null)
                        } else null
                    } else Wrapper(finalProp)
                }
            }

            // Return the location of the target property and the mapped value, only if it's not null
            originalPropValue?.let { mapping.value.second.first to it.value }
        }.toMap()

/**
 * Check if the target field or any of its parents are excluded.
 *
 * @param field to check.
 * @param exclusions the exclusions.
 * @return true if the field is excluded, false otherwise.
 */
private fun isExcluded(field: Field, exclusions: List<TargetField>): Boolean {
    var targetFieldExcluded = false
    var targetStringLoc = ""
    parseLocation(field).forEach {
        targetStringLoc+= it
        if (targetStringLoc in exclusions) {
            targetFieldExcluded = true
            return@forEach
        }
        targetStringLoc+= "/"
    }

    return targetFieldExcluded
}

/**
 * Process a function mapping if possible.
 *
 * @param originalProp original property.
 * @param function function to execute, only functions with 1 or fewer parameters are accepted.
 *
 * Note that:
 *  - If the original property value isn't the same type or a supertype of the function parameter (if present),
 *    we'll try to map it to the expected type
 *  - If the function return value is not the same type or a supertype of the target field, we'll try to map it
 *    to the expected type
 *
 * @param targetField target field.
 * @return the final value after executing the desired function. If the final value cannot be mapped, return null.
 *
 * Possible causes to return a null value:
 *  - The function is null
 *  - The function needs a non-nullable and non-optional parameter, but the original field doesn't exist or its null
 *  - The function return value is null or couldn't be mapped to the right type
 *  - The function doesn't return anything
 *  - The function needs more than 1 parameter
 *  - The function throws an exception
 *  
 *  If the mapping fallback is null in any of these cases, we'll try to return a wrapper with null,
 *    but if the target field is non-nullable we'll return null directly.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalReflectionOnLambdas::class)
private fun processFunction(
    originalProp: OriginalNodeInterface<*>?,
    function: MappingFunction?,
    targetField: TargetNode<*>,
    fallback: ProcessMappingFallback,
): Wrapper<Any?>? = function?.let mapProp@{
    // Value returned in case something fails in the mapping
    // If the fallback is null, then return a wrapper with a null value, if not
    //  return null to continue with the normal mapping
    val exitValue = if ((fallback == ProcessMappingFallback.NULL || fallback == ProcessMappingFallback.NULL_OR_THROW) && targetField.type.isMarkedNullable) {
        Wrapper<Any?>(null)
    } else null

    // If the function has more than 1 param, return the exit value
    val functionParam = (function.reflect()?.parameters
        ?: runCatching { (function as KFunction<*>).parameters }.getOrNull())
        .let {
            if (it == null || it.size > 1)
                return@mapProp exitValue
            if (it.isEmpty()) null else it.single()
        }

    // Get the function input type
    val inputType = functionParam?.type

    // If the original prop is null and function param is not optional or nullable, return the exit value
    if (originalProp?.value == null && functionParam?.isOptional == false && !functionParam.type.isMarkedNullable)
        return@mapProp exitValue

    // If the original prop value is not null, remove the nullability of the type
    val originalPropType = originalProp?.value?.let {
        originalProp.type.removeNullability()
    }

    // If the function input type is not a supertype or the same type as the original property, then
    //  map the property, if the input type is null then do nothing
    val inputProp = originalPropType?.let {
        if (inputType?.isSupertypeOf(it) == false) {
            mapTo(originalEntity = originalProp, targetType = inputType)
        } else originalProp.value
    }

    // Call the function and save the returned value
    val returnValue = runCatching {
        Wrapper(
            // If the function param is null
            if (inputProp == null) {
                // If the function param is null
                if (functionParam == null) {
                    // Then there are no params, call the function without anything
                    (function as Function0<*>).invoke()
                } else if (functionParam.isOptional) {
                    // If the inputProp is null, but the function param is optional, then call the function
                    (function as KFunction<*>).callBy(emptyMap())
                    // If the inputProp is null and the function param is not optional, return the exit value
                } else if (functionParam.type.isMarkedNullable) {
                    // If the inputProp is null, but the function param is nullable, then call the function
                    (function as Function1<Any?, *>).invoke(null)
                    // If the input prop is null, and the input type is present, and it's not nullable
                    //   or optional we cannot go further, return the exit value
                } else return@mapProp exitValue
            } else if (functionParam != null) {
                // If the input prop and the function param are not null, call the function with
                //  the input prop
                (function as Function1<Any?, *>).invoke(inputProp)
            } else {
                // If the input param is not null but the function param is null, then ignore the input
                //  param and call the function
                (function as Function0<*>).invoke()
            }
        )
    }.getOrElse {
        // If the function throws an exception we cannot go further, return the exit value or throw based on the selected ProcessMappingFallback
        if (fallback == ProcessMappingFallback.CONTINUE_OR_THROW || fallback == ProcessMappingFallback.NULL_OR_THROW) {
            throw it
        } else return@mapProp exitValue
    }

    // If the returned value is null, return it directly since we don't need to check the type
    if (returnValue.value == null)
        if (targetField.type.isMarkedNullable) {
            return@mapProp Wrapper<Any?>(null)
        } else return@mapProp null

    // If the target prop type is not a supertype or the same type as the function return
    //  type, then map the returned value
    val returnProp = if (!targetField.type.isSupertypeOf(returnValue.value::class.starProjectedType)) {
        mapTo(
            originalEntity = OriginalEntity(returnValue.value, returnValue.value::class.starProjectedType),
            targetType = targetField.type,
        )
    } else returnValue.value

    // If the mapped value is null, return it or return null based on the nullability of the target field
    if (returnProp == null) {
        if (targetField.type.isMarkedNullable) {
            return@mapProp Wrapper<Any?>(null)
        } else return@mapProp null
    } else Wrapper(returnProp)
}