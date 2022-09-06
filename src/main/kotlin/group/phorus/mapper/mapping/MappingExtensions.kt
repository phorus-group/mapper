package group.phorus.mapper.mapping

import group.phorus.mapper.*
import group.phorus.mapper.building.buildOrUpdateInternal
import group.phorus.mapper.building.buildWithEntity
import group.phorus.mapper.mapping.functions.parseLocation
import group.phorus.mapper.mapping.functions.processMappings
import group.phorus.mapper.mapping.functions.toProcessMappingFallback
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

/**
 * The update option. Used to know if the mapper should update [fields][Field] to null, or not.
 */
enum class UpdateOption {
    /**
     * Any null [field][Field] will be ignored, and only the non-null ones will be updated.
     */
    IGNORE_NULLS,

    /**
     * Any null and non-null [field][Field] will be updated.
     */
    SET_NULLS,
}

/**
 * Maps one [entity][originalEntity] to a different [type][targetType].
 *
 * The function also offers multiple other options to control the way it maps the fields.
 *
 * @param originalEntity the original entity.
 * @param targetType the target type.
 * @param baseEntity a base entity, optional.
 * @param exclusions a list of [target fields][TargetField] to exclude.
 *
 * @param mappings a map of fields to map forcefully, with the format:
 * [OriginalField] - [TargetField] - [MappingFallback]
 * @param functionMappings a map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField] - [MappingFunction] - [TargetField] - [MappingFallback]
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity.
 * @param useSettersOnly boolean used to forcefully use only setters or not.
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return the mapped entity.
 */
fun mapTo(
    originalEntity: OriginalNodeInterface<*>,
    targetType: KType,
    baseEntity: Pair<Any, UpdateOption>? = null,
    exclusions: List<TargetField> = emptyList(),
    mappings: Mappings = emptyMap(),
    functionMappings: FunctionMappings = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
    mapPrimitives: Boolean = true,
): Any? {
    // If the original entity value is null, return null
    if (originalEntity.value == null)
        return null

    // Remove the nullability from the original type
    originalEntity.type = originalEntity.type.removeNullability()
    val targetKType = targetType.removeNullability()

    if (targetKType.isSupertypeOf(originalEntity.type)
        && baseEntity == null
        && mappings.isEmpty()
        && functionMappings.isEmpty()
        && exclusions.isEmpty())
        return originalEntity.value

    // If the target type and the original entity type are supertypes or the same type as iterable, map, pair,
    //  or triple, try to map them
    val mappedComposite = if (baseEntity != null) {
        // If base entity is not null, use it instead of the original entity, and use the original entity as base
        mapComposite(
            originalEntity = originalEntity,
            targetType = targetKType,
            baseEntity = baseEntity,
            exclusions = exclusions,
            mappings = mappings,
            functionMappings = functionMappings,
            ignoreMapFromAnnotations = ignoreMapFromAnnotations,
            useSettersOnly = useSettersOnly,
            mapPrimitives = mapPrimitives
        )
    } else {
        mapComposite(
            originalEntity = originalEntity,
            targetType = targetKType,
            exclusions = exclusions,
            mappings = mappings,
            functionMappings = functionMappings,
            ignoreMapFromAnnotations = ignoreMapFromAnnotations,
            useSettersOnly = useSettersOnly,
            mapPrimitives = mapPrimitives
        )
    }
    if (mappedComposite != null)
        return mappedComposite.value

    // If the target type and the original entity type are primitives, try to map them
    val mappedPrimitive = mapPrimitives(targetKType, originalEntity, mapPrimitives)
    if (mappedPrimitive != null)
        return mappedPrimitive.value


    // Format the exclusion locations
    val fieldExclusions = exclusions.map { parseLocation(it).joinToString("/") }
    val targetClass = TargetClass(targetKType.classifier as KClass<*>, targetKType)

    // Map all the mappings
    val mappingValues = processMappings(
        originalEntity= originalEntity,
        targetClass = targetClass,
        mappings = mappings.map {
            it.key to (null to (it.value.first to it.value.second.toProcessMappingFallback()))
        }.toMap(),
        exclusions = fieldExclusions,
    )

    // Map all the functionMappings
    val functionMappingValues = processMappings(
        originalEntity= originalEntity,
        targetClass = targetClass,
        mappings = functionMappings.map {
            it.key to (it.value.first to (it.value.second.first to it.value.second.second.toProcessMappingFallback()))
        }.toMap(),
        exclusions = fieldExclusions,
    )

    // Sum all the mapped values, if a key exists is repeated then the rightmost map
    //  will have priority over the other. Priorities: functionMappings > mappings
    val mappedValues = mappingValues + functionMappingValues

    // Map the target class properties from the mapped values, the MapFrom annotation,
    //  or the original entity, in that priority
    val mappedProps = mapProperties(
        originalEntity = originalEntity,
        targetClass = targetClass,
        baseEntity = baseEntity,
        exclusions = fieldExclusions,
        mappedValues = mappedValues,
        ignoreMapFromAnnotations = ignoreMapFromAnnotations,
        useSettersOnly = useSettersOnly
    )

    // If base class is not null, and use setters is false, we'll use a different method that uses constructors to set
    //  as many properties as possible, this will always create a new instance instead of modifying the base entity
    return if (baseEntity != null && !useSettersOnly && mappedProps.isNotEmpty()) {
        buildWithEntity(
            type = targetKType,
            properties = mappedProps,
            entity = baseEntity.first,
        )
    } else {
        buildOrUpdateInternal(
            type = targetKType,
            properties = mappedProps,
            useSettersOnly = useSettersOnly,
            entity = baseEntity?.first,
        )
    }
}

/**
 * Maps primitive types, if possible.
 *
 * @param targetType the target type.
 * @param originalEntity the original entity.
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return a wrapper containing the mapped value or a wrapper containing null in case mapping is not possible,
 * or null if the target type and the original entity are not both primitives.
 */
private fun mapPrimitives(
    targetType: KType,
    originalEntity: OriginalNodeInterface<*>,
    mapPrimitives: Boolean,
): Wrapper<Any?>? {
    // If target type is a primitive or any
    if (typeOf<String>().isSupertypeOf(targetType) || typeOf<Number>().isSupertypeOf(targetType) || targetType.isSupertypeOf(typeOf<Any>())) {
        // But the original entity type is not a primitive, the value can't be mapped, so return null
        if (!typeOf<String>().isSupertypeOf(originalEntity.type)
            && !typeOf<Number>().isSupertypeOf(originalEntity.type)
            && !originalEntity.type.isSupertypeOf(typeOf<Any>()))
            return Wrapper(null)
    } else { // If the target type is not a primitive
        // If the original entity type is also not a primitive, return null
        //  to continue the mapTo function, since the values could be mapped
        return if (!typeOf<String>().isSupertypeOf(originalEntity.type) && !typeOf<Number>().isSupertypeOf(originalEntity.type)) {
            null
        } else Wrapper(null)
        // If the original type is not a primitive, the value can't be mapped, so return null
    }

    // If the types are the same, return the value directly
    if (targetType.isSupertypeOf(originalEntity.type))
        return Wrapper(originalEntity.value)

    if (!mapPrimitives) return Wrapper(null)

    // If the types are Number, return the value mapped with the native function
    if (typeOf<Number>().isSupertypeOf(targetType) && typeOf<Number>().isSupertypeOf(originalEntity.type)) {
        val value: Number? = when (targetType) {
            typeOf<Double>() -> (originalEntity.value as Number).toDouble()
            typeOf<Float>() -> (originalEntity.value as Number).toFloat()
            typeOf<Long>() -> (originalEntity.value as Number).toLong()
            typeOf<Int>() -> (originalEntity.value as Number).toInt()
            typeOf<Short>() -> (originalEntity.value as Number).toShort()
            typeOf<Byte>() -> (originalEntity.value as Number).toByte()
            else -> null
        }
        return value?.let { Wrapper(it) }
    }

    if (typeOf<Number>().isSupertypeOf(targetType) && typeOf<String>().isSupertypeOf(originalEntity.type)) {
        val value: Number? = runCatching { when (targetType) {
            typeOf<Double>() -> (originalEntity.value as String).toDouble()
            typeOf<Float>() -> (originalEntity.value as String).toFloat()
            typeOf<Long>() -> (originalEntity.value as String).toLong()
            typeOf<Int>() -> (originalEntity.value as String).toInt()
            typeOf<Short>() -> (originalEntity.value as String).toShort()
            typeOf<Byte>() -> (originalEntity.value as String).toByte()
            else -> null
        }}.getOrNull()
        return value?.let { Wrapper(it) }
    }

    if (typeOf<String>().isSupertypeOf(targetType) && typeOf<Number>().isSupertypeOf(originalEntity.type))
        return Wrapper(originalEntity.value.toString())

    return Wrapper(null)
}

/**
 * Map composite classes, if possible.
 *
 * The supported classes are: [List], [Set], [Map], [Pair] and [Triple].
 *
 * @param originalEntity the original entity.
 * @param targetType the target type.
 * @param baseEntity a base entity, optional.
 * @param exclusions a list of [target fields][TargetField] to exclude.
 *
 * @param mappings a map of fields to map forcefully, with the format:
 * [OriginalField] - [TargetField] - [MappingFallback]
 * @param functionMappings a map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField] - [MappingFunction] - [TargetField] - [MappingFallback]
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity.
 * @param useSettersOnly boolean used to forcefully use only setters or not.
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return a wrapper containing the mapped values or a wrapper containing null in case mapping is not possible,
 * or null if the target type and the original entity type aren't supertypes of [Iterable], [Map], [Pair], or [Triple].
 */
private fun mapComposite(
    originalEntity: OriginalNodeInterface<*>,
    targetType: KType,
    baseEntity: Pair<Any, UpdateOption>? = null,
    exclusions: List<TargetField> = emptyList(),
    mappings: Mappings = emptyMap(),
    functionMappings: FunctionMappings = emptyMap(),
    ignoreMapFromAnnotations: Boolean,
    useSettersOnly: Boolean,
    mapPrimitives: Boolean,
): Wrapper<Any?>? {

    val entity = if (baseEntity != null) {
        OriginalEntity(baseEntity.first, targetType)
    } else originalEntity

    // If the target type and original type are not both supertypes or the same type as iterable, map, pair,
    //  or triple, return null
    if ((!typeOf<Iterable<*>>().isSupertypeOf(targetType) && !typeOf<Iterable<*>>().isSupertypeOf(entity.type))
        && (!typeOf<Map<*, *>>().isSupertypeOf(targetType) && !typeOf<Map<*, *>>().isSupertypeOf(entity.type))
        && (!typeOf<Pair<*, *>>().isSupertypeOf(targetType) && !typeOf<Pair<*, *>>().isSupertypeOf(entity.type))
        && (!typeOf<Triple<*, *, *>>().isSupertypeOf(targetType) && !typeOf<Triple<*, *, *>>().isSupertypeOf(entity.type)))
        return null

    // If the target type and original type are both supertypes or the same type as iterable, map each item
    if (typeOf<Iterable<*>>().isSupertypeOf(targetType) && typeOf<Iterable<*>>().isSupertypeOf(entity.type)) {
        val subTargetType = targetType.arguments.first().type ?: return null

        return (entity.value as Iterable<*>).mapNotNull { item ->
            item?.let {
                val subOriginalType = it::class.starProjectedType
                val subOriginalEntity = if (baseEntity != null) {
                    OriginalEntity(originalEntity.value!!, subOriginalType)
                } else  OriginalEntity(it, subOriginalType)

                mapTo(
                    originalEntity = subOriginalEntity,
                    targetType = subTargetType,
                    baseEntity = baseEntity?.let { base -> it to base.second },
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            }
        }.let { value ->
            val finalValue: Any = if (typeOf<Set<*>>().isSupertypeOf(targetType)) {
                value.toSet()
            } else value

            Wrapper(finalValue)
        }
    }

    // If the target type and original type are both supertypes or the same type as map, map each item
    if (typeOf<Map<*, *>>().isSupertypeOf(targetType) && typeOf<Map<*, *>>().isSupertypeOf(entity.type)) {
        val subTargetType = targetType.arguments

        return (entity.value as Map<*, *>).mapNotNull { (key, item) ->
            val keyTargetType = subTargetType[0].type ?: return null
            val itemTargetType = subTargetType[1].type ?: return null

            key?.let {
                val subOriginalType = it::class.starProjectedType
                val subOriginalEntity = if (baseEntity != null) {
                    OriginalEntity(originalEntity.value!!, subOriginalType)
                } else  OriginalEntity(it, subOriginalType)

                mapTo(
                    originalEntity = subOriginalEntity,
                    targetType = keyTargetType,
                    baseEntity = baseEntity?.let { base -> it to base.second },
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            } to item?.let {
                val subOriginalType = it::class.starProjectedType
                val subOriginalEntity = if (baseEntity != null) {
                    OriginalEntity(originalEntity.value!!, subOriginalType)
                } else  OriginalEntity(it, subOriginalType)

                mapTo(
                    originalEntity = subOriginalEntity,
                    targetType = itemTargetType,
                    baseEntity = baseEntity?.let { base -> it to base.second },
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            }
        }.toMap().let { value -> Wrapper(value) }
    }

    // If the target type and original type are both supertypes or the same type as pair, map each item
    if (typeOf<Pair<*, *>>().isSupertypeOf(targetType) && typeOf<Pair<*, *>>().isSupertypeOf(entity.type)) {
        val subTargetType = targetType.arguments

        return (entity.value as Pair<*, *>).let { (first, second) ->
            val firstTargetType = subTargetType[0].type ?: return null
            val secondTargetType = subTargetType[1].type ?: return null

            first?.let {
                val subOriginalType = it::class.starProjectedType
                val subOriginalEntity = if (baseEntity != null) {
                    OriginalEntity(originalEntity.value!!, subOriginalType)
                } else  OriginalEntity(it, subOriginalType)

                mapTo(
                    originalEntity = subOriginalEntity,
                    targetType = firstTargetType,
                    baseEntity = baseEntity?.let { base -> it to base.second },
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            } to second?.let {
                val subOriginalType = it::class.starProjectedType
                val subOriginalEntity = if (baseEntity != null) {
                    OriginalEntity(originalEntity.value!!, subOriginalType)
                } else  OriginalEntity(it, subOriginalType)

                mapTo(
                    originalEntity = subOriginalEntity,
                    targetType = secondTargetType,
                    baseEntity = baseEntity?.let { base -> it to base.second },
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            }
        }.let { value -> Wrapper(value) }
    }

    // If the target type and original type are both supertypes or the same type as triple, map each item
    if (typeOf<Triple<*, *, *>>().isSupertypeOf(targetType) && typeOf<Triple<*, *, *>>().isSupertypeOf(entity.type)) {
        val subTargetType = targetType.arguments

        return (entity.value as Triple<*, *, *>).let { (first, second, third) ->
            val firstTargetType = subTargetType[0].type ?: return null
            val secondTargetType = subTargetType[1].type ?: return null
            val thirdTargetType = subTargetType[2].type ?: return null

            Triple(
                first?.let {
                    val subOriginalType = it::class.starProjectedType
                    val subOriginalEntity = if (baseEntity != null) {
                        OriginalEntity(originalEntity.value!!, subOriginalType)
                    } else  OriginalEntity(it, subOriginalType)

                    mapTo(
                        originalEntity = subOriginalEntity,
                        targetType = firstTargetType,
                        baseEntity = baseEntity?.let { base -> it to base.second },
                        exclusions = exclusions,
                        mappings = mappings,
                        functionMappings = functionMappings,
                        ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                        useSettersOnly = useSettersOnly,
                        mapPrimitives = mapPrimitives,
                    )},
                second?.let {
                val subOriginalType = it::class.starProjectedType
                    val subOriginalEntity = if (baseEntity != null) {
                        OriginalEntity(originalEntity.value!!, subOriginalType)
                    } else  OriginalEntity(it, subOriginalType)

                    mapTo(
                        originalEntity = subOriginalEntity,
                    targetType = secondTargetType,
                    baseEntity = baseEntity?.let { base -> it to base.second },
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )},
                third?.let {
                    val subOriginalType = it::class.starProjectedType
                    val subOriginalEntity = if (baseEntity != null) {
                        OriginalEntity(originalEntity.value!!, subOriginalType)
                    } else  OriginalEntity(it, subOriginalType)

                    mapTo(
                        originalEntity = subOriginalEntity,
                        targetType = thirdTargetType,
                        baseEntity = baseEntity?.let { base -> it to base.second },
                        exclusions = exclusions,
                        mappings = mappings,
                        functionMappings = functionMappings,
                        ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                        useSettersOnly = useSettersOnly,
                        mapPrimitives = mapPrimitives,
                    )},
            )
        }.let { value -> Wrapper(value) }
    }

    // If target type is composite but original type is not, or vice versa, we cannot map the values,
    //  return property value null
    return Wrapper(null)
}

/**
 * Returns the mapped properties from: the already mapped [values][Value], or the [@MapFrom][MapFrom] annotation,
 * or the original entity, in that order.
 *
 * @param originalEntity the original entity.
 * @param targetClass the target class.
 * @param baseEntity a base entity, optional.
 * @param exclusions a list of [target fields][TargetField] to exclude.
 * @param mappedValues the manually mapped values.
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity.
 * @param useSettersOnly boolean used to forcefully use only setters or not.
 * @return a map containing all the [target fields][TargetField] and their [values][Value].
 */
private fun mapProperties(
    originalEntity: OriginalNodeInterface<*>,
    targetClass: TargetClass<*>,
    baseEntity: Pair<Any, UpdateOption>?,
    exclusions: List<TargetField>,
    mappedValues: Map<TargetField, Value?>,
    ignoreMapFromAnnotations: Boolean,
    useSettersOnly: Boolean,
): Map<TargetField, Value?> {
    val props = mutableMapOf<TargetField, Value?>()
    val base = baseEntity?.first?.let { OriginalEntity(it, targetClass.type) }

    // Iterate through all the target class fields
    targetClass.properties.forEach { targetField ->
        val targetFieldName = targetField.key

        // If the target field is in the target field exclusions list, skip
        if (targetFieldName in exclusions)
            return@forEach

        var prop: Wrapper<Any?>? = null
        if (mappedValues.containsKey(targetFieldName)) {
            // If the target field has been mapped with a mapping or a function mapping, use that mapped value
            prop = Wrapper(mappedValues[targetFieldName])
        } else if (targetField.value.mapFrom != null) {
            // If the target field has a mapFrom annotation, use its value
            val mapFromMappedValue = if (!ignoreMapFromAnnotations) {
                var mapFromValue: OriginalNodeInterface<*>? = null
                for (location in targetField.value.mapFrom!!.locations) {
                    val mapFromProp = originalEntity.findProperty(parseLocation(location))
                    if (mapFromProp != null) {
                        mapFromValue = mapFromProp.apply {
                            if (value != null) type = type.removeNullability()
                        }
                    }
                }
                mapFromValue
            } else null

            prop = if (mapFromMappedValue != null) {
                if (mapFromMappedValue.value != null && targetField.value.type.isSupertypeOf(mapFromMappedValue.type)) {
                    Wrapper(mapFromMappedValue.value)
                } else if (mapFromMappedValue.value != null) {
                    Wrapper(mapTo(originalEntity = mapFromMappedValue, targetType= targetField.value.type))
                } else Wrapper(null)
            } else {
                if (targetField.value.mapFrom!!.fallback == MappingFallback.NULL) {
                    Wrapper(null)
                } else null
            }
        }

        if (prop == null) {
            var value: Any? = null
            // If the target property wasn't excluded or mapped until this point, look for the value in the original entity
            val originalProp = originalEntity.properties[targetFieldName]
            // If there's no property with the same name in the original entity, skip
                ?: return@forEach

            if (originalProp.value != null) {
                // If the target field is the same type or a supertype of the original property, use that property
                value = if (targetField.value.type.isSupertypeOf(originalProp.type.removeNullability())) {
                    originalProp.value
                } else {
                    // If not, map it
                    mapTo(originalEntity = originalProp, targetType= targetField.value.type)
                }
            }

            prop = if (value == null && baseEntity?.second != UpdateOption.SET_NULLS) {
                null
            } else Wrapper(value)
        }

        // Get the subfield mapped values, if any
        val subFieldMappedValues = mappedValues.mapNotNull {
            val newLoc = parseLocation(it.key).let { loc ->
                if (loc.firstOrNull() == targetFieldName && loc.size > 1) {
                    loc.drop(1).joinToString("/")
                } else null
            } ?: return@mapNotNull null

            newLoc to it.value
        }.toMap()

        // Get the subfield exclusions, if any
        val subExclusions = exclusions.mapNotNull { exclusion ->
            parseLocation(exclusion).let { loc ->
                if (loc.first() == targetFieldName && loc.size > 1) {
                    loc.drop(1).joinToString("/")
                } else null
            }
        }

        val propValue = if (subFieldMappedValues.isNotEmpty() || subExclusions.isNotEmpty()) {
            val newBaseEntity = (base?.properties?.get(targetFieldName)?.value)?.let {
                it to baseEntity.second
            }

            if (prop?.value == null) {
                val nextMappedValues = subFieldMappedValues.filter {
                    parseLocation(it.key).size == 1
                }

                val newProp = buildOrUpdateInternal(
                    type = targetField.value.type,
                    properties = nextMappedValues,
                    useSettersOnly = useSettersOnly,
                    entity = newBaseEntity,
                )
                if (newProp != null) {
                    val newPropEntity = originalEntity.properties[targetFieldName]?.apply {
                        this.value = newProp
                    } ?: OriginalEntity(newProp, newProp::class.starProjectedType)

                    val remainingMappedValues = subFieldMappedValues.filterNot { it.key in nextMappedValues.keys }
                    mapTo(
                        originalEntity = newPropEntity,
                        targetType = targetField.value.type,
                        exclusions = subExclusions,
                        functionMappings = remainingMappedValues.map {
                            null to ({ it.value } to (it.key to MappingFallback.NULL))
                        }.toMap(),
                        baseEntity = newBaseEntity
                    )
                } else null
            } else {
                // If the prop is not null, but there are subfield mapped values or subfield exclusions, call the mapTo
                //  function with the subfield exclusions and subfield mapped values
                mapTo(
                    originalEntity = OriginalEntity(prop.value!!, targetField.value.type),
                    targetType = targetField.value.type,
                    exclusions = subExclusions,
                    functionMappings = subFieldMappedValues.map {
                        UUID.randomUUID().toString() to ({ it.value } to (it.key to MappingFallback.NULL))
                    }.toMap(),
                    baseEntity = newBaseEntity,
                    useSettersOnly = useSettersOnly,
                )
            }
        } else prop.let { it ?: return@forEach }.value

        props[targetFieldName] = propValue
    }

    return props
}