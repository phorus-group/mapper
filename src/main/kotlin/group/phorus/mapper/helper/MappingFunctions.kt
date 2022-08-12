package group.phorus.mapper.helper

import group.phorus.mapper.enums.MappingFallback
import group.phorus.mapper.mapTo
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

/**
 * Transforms a location string to a location list
 * @param location the location to be parsed, use "/" as field separators and ".." to refer to the parent node.
 *  Format examples:
 *      {field}/{field}
 *      ../{field}/field
 *  Note: Any single dot will be ignored, since it's considered as the current location
 * @return the location list
 */
fun parseLocation(location: String): List<String> =
    location.split("/")
        .toMutableList()
        .apply { removeIf { it.isBlank() || it == "." } }



enum class ProcessMappingFallback {
    NULL, SKIP
}

fun MappingFallback.toProcessMappingFallback() =
    when(this) {
        MappingFallback.NULL -> ProcessMappingFallback.NULL
        MappingFallback.CONTINUE -> ProcessMappingFallback.SKIP
    }

/**
 * Returns a list of target class locations with the parsed values from the original entity
 *
 * @param originalEntity original entity to take the values from
 * @param targetClass with the desired location
 * @param mappings mappings TODO: Explain a ton more
 * @param exclusions excluded fields from the target class, exclusions take priority
 *  over everything else
 * @return a map with the target class locations and mapped values
 */
@OptIn(ExperimentalReflectionOnLambdas::class)
fun processMappings(
    originalEntity: OriginNodeInterface<*>,
    targetClass: TargetClass<*>,
    mappings: Map<OriginalField?, Pair<MappingFunction?, Pair<TargetField, ProcessMappingFallback>>>,
    exclusions: List<TargetField>,
): Map<TargetField, Value?> =
    mappings
        .filterNot { mapping ->
            // Filter out excluded properties
            // Get the locations of the target field
            val targetFieldLocation = parseLocation(mapping.value.second.first)

            // Check if the target field or any of its parents is excluded
            var targetFieldExcluded = false
            var targetStringLoc = ""
            targetFieldLocation.forEach {
                targetStringLoc+= it
                if (targetStringLoc in exclusions) {
                    targetFieldExcluded = true
                    return@forEach
                }
                targetStringLoc+= "/"
            }

            // If the target field is excluded, skip the mapping
            targetFieldExcluded
        }
        .mapNotNull { mapping ->
            val originalProp = mapping.key?.let { originalEntity.findProperty(parseLocation(it))}
            val targetProp = targetClass.findProperty(parseLocation(mapping.value.second.first))
                ?: return@mapNotNull null

            // Test donde la propiedad no es null, pero el valor si es null

            val originalPropValue: PropertyWrapper<Any?>? = mapping.value.first?.let mapProp@{ function: Function<*> ->

                // Value returned in case something fails in the mapping
                // If the fallback is null, then return a property wrapper with a null value, if not
                //  return null to continue with the normal mapping
                val exitValue = if (mapping.value.second.second == ProcessMappingFallback.NULL && targetProp.type.isMarkedNullable) {
                    PropertyWrapper<Any?>(null)
                } else null

                // If the function has more than 1 param, return the exit value
                val functionParam = (function.reflect()?.parameters
                    ?: try { (function as KFunction<*>).parameters } catch (_: Exception) { null })
                    .let {
                        if (it == null || it.size > 1)
                            return@mapProp exitValue
                        if (it.isEmpty()) null else it.single()
                    }

                // Get the function input type
                val inputType = functionParam?.type

                val retType = function.reflect()?.returnType
                    // If it's null, try to get process the function as a KFunction
                    ?: try { (function as KFunction<*>).returnType } catch (_: Exception) { null }
                    // If the output type is null we cannot go further, return the exit value
                    ?: return@mapProp exitValue

                // If the original prop is null and function param is not optional or nullable, return the exit value
                if (originalProp?.value == null && functionParam?.isOptional == false && !functionParam.type.isMarkedNullable)
                    return@mapProp exitValue

                // If the original prop type is nullable, but the original prop value is not null, remove the
                //  nullability of the type
                val originalPropType = originalProp?.value?.let {
                    val type = originalProp.type
                    if (type.isMarkedNullable) {
                        // Then remove the nullability of the function return type
                        type.classifier?.createType(
                            arguments = type.arguments,
                            nullable = false,
                            annotations = type.annotations,
                        ) ?: type
                    } else {
                        type
                    }
                }

                // If the function input type is not a supertype or the same type as the original property, then
                //  map the property, if the input type is null then do nothing
                val inputProp = originalPropType?.let {
                    if (inputType?.isSupertypeOf(it) == false) {
                        mapTo(originalEntity = originalProp, targetType = inputType)
                    } else originalProp.value
                }

                // Call the function and save the returned value
                val returnValue = try {
                    PropertyWrapper(
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
                } catch (_: Exception) {
                    // If the function throws an exception we cannot go further, return the exit value
                    return@mapProp exitValue
                }

                // If the returned value is null, return it directly since we don't need to check the type
                if (returnValue.value == null)
                    if (targetProp.type.isMarkedNullable) {
                        return@mapProp PropertyWrapper<Any?>(null)
                    } else return@mapProp null

                // The function returned value cannot be null at this point, so remove the nullability of the function
                //  return type
                val returnType = retType.let { type ->
                    type.classifier?.createType(
                        arguments = type.arguments,
                        nullable = false,
                        annotations = type.annotations,
                    ) ?: type
                }

                // If the target prop type is not a supertype or the same type as the function return
                //  type, then map the returned value
                val returnProp = if (!targetProp.type.isSupertypeOf(returnType)) {
                    mapTo(
                        originalEntity = OriginalEntity(returnValue.value, returnType),
                        targetType = targetProp.type,
                    )
                } else returnValue.value

                PropertyWrapper(returnProp)
            } ?: if (originalProp == null) {
                // If the original prop is null, skip the value or not based on the mapping fallback
                if (targetProp.type.isMarkedNullable && mapping.value.second.second == ProcessMappingFallback.NULL) {
                    PropertyWrapper(null)
                } else null
            } else if (originalProp.value == null) {
                // If the original prop value is null and the target property is not nullable, return null to skip
                //  the mapping
                if (targetProp.type.isMarkedNullable) {
                    PropertyWrapper(null)
                } else null
            } else {
                // If the mapping has a function and reaches this point, something failed
                if (mapping.value.first != null) {
                    if (targetProp.type.isMarkedNullable && mapping.value.second.second == ProcessMappingFallback.NULL) {
                        PropertyWrapper(null)
                    } else null
                } else {
                    // Make the original prop type non-nullable, since we already check that the original
                    //  property value is not null
                    val originalValueType = originalProp.type.let { type ->
                        type.classifier?.createType(
                            arguments = type.arguments,
                            nullable = false,
                            annotations = type.annotations,
                        ) ?: type
                    }

                    // If the target prop type is not a supertype or the same type as the original value type, then
                    //  map the value
                    if (!targetProp.type.isSupertypeOf(originalValueType)) {
                        PropertyWrapper(mapTo(originalEntity = originalProp, targetType = targetProp.type))
                    } else PropertyWrapper(originalProp.value)
                }
            }

            // Return the location of the target property and the mapped value, only if it's not null
            originalPropValue?.let { mapping.value.second.first to it.value }
        }.toMap()