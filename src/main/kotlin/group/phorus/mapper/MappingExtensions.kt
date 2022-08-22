package group.phorus.mapper

import kotlin.reflect.*
import kotlin.reflect.full.*

inline fun <reified T: Any> T.updateFrom(
    entity: Any,
    updateOption: UpdateOption = UpdateOption.IGNORE_NULLS,
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = true,
    mapPrimitives: Boolean = true,
): T? = mapTo(
    originalEntity = OriginalEntity(entity, this::class.starProjectedType),
    targetType = typeOf<T>(),
    baseEntity = this to updateOption,
    exclusions = exclusions,
    mappings = mappings,
    functionMappings = functionMappings,
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T?

inline fun <reified T: Any> Any.mapTo(
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
    mapPrimitives: Boolean = true,
): T? = mapTo(
    originalEntity = OriginalEntity(this, this::class.starProjectedType),
    targetType = typeOf<T>(),
    exclusions = exclusions,
    mappings = mappings,
    functionMappings = functionMappings,
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T?


enum class UpdateOption {
    SET_NULLS, IGNORE_NULLS
}

/**
 *
 *
 * @param targetType target class type
 * @param exclusions list of original class field exclusions. Excludes any mapping using
 *  specifically any of the specified original fields.
 */
fun mapTo(
    originalEntity: OriginNodeInterface<*>,
    targetType: KType,
    baseEntity: Pair<Any, UpdateOption>? = null,
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
    mapPrimitives: Boolean = true,
): Any? {
    // If the original entity value is null, return null
    if (originalEntity.value == null)
        return null

    // Remove the nullability from the original type
    originalEntity.type = originalEntity.type.let {
        it.classifier?.createType(
            arguments = it.arguments,
            nullable = false,
            annotations = it.annotations,
        ) ?: it
    }

    // If the target type and the original entity type are supertypes or the same type as iterable, map, pair,
    //  or triple, try to map them
    val mappedComposite = mapComposite(targetType, originalEntity, exclusions, mappings,
        functionMappings, ignoreMapFromAnnotations, useSettersOnly, mapPrimitives)
    if (mappedComposite != null)
        return mappedComposite.value

    // If the target type and the original entity type are primitives, try to map them
    val mappedPrimitive = mapPrimitives(targetType, originalEntity, mapPrimitives)
    if (mappedPrimitive != null)
        return mappedPrimitive.value


    // Format the exclusion locations
    val fieldExclusions = exclusions.map { parseLocation(it).joinToString("/") }
    val targetClass = TargetClass(targetType.classifier as KClass<*>)
    val base = baseEntity?.first?.let { OriginalEntity(it, targetType) }

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

    val props = mutableMapOf<TargetField, Value?>()

    // Iterate through all the target class fields
    targetClass.properties.forEach { targetField ->
        val targetFieldName = targetField.key

        // If the target field is in the target field exclusions list, skip
        if (targetFieldName in fieldExclusions)
            return@forEach

        var prop: PropertyWrapper<Any?>? = null
        if (mappedValues.containsKey(targetFieldName)) {
            // If the target field has been mapped with a mapping or a function mapping, use that mapped value
            prop = PropertyWrapper(mappedValues[targetFieldName])
        } else if (targetField.value.mapFrom != null) {
            // If the target field has a mapFrom annotation, use its value
            val mapFromMappedValue = if (!ignoreMapFromAnnotations) {
                var mapFromValue: OriginNodeInterface<*>? = null
                for (location in targetField.value.mapFrom!!.locations) {
                    val mapFromProp = originalEntity.findProperty(parseLocation(location))
                    if (mapFromProp != null) {
                        mapFromValue = mapFromProp
                    }
                }
                mapFromValue
            } else null

            prop = if (mapFromMappedValue != null) {
                if (mapFromMappedValue.value != null && targetField.value.type.isSupertypeOf(mapFromMappedValue.type)) {
                    PropertyWrapper(mapFromMappedValue.value)
                } else if (mapFromMappedValue.value != null) {
                    PropertyWrapper(mapTo(originalEntity = mapFromMappedValue, targetType= targetField.value.type))
                } else PropertyWrapper(null)
            } else {
                if (targetField.value.mapFrom!!.fallback == MappingFallback.NULL) {
                    PropertyWrapper(null)
                } else null
            }
        }

        // TODO: Seguir implementando updateFrom desde aca para abajo
        //  - Si hay un base entity, en caso que el updateoption sea ignore_null, en caso que mappings, function
        //    mappings y default mappings devuelvan un value null, utilizar la propiedad del base entity
        //  - Si hay un base entity y updateoption es set_null, en caso que mappings, function mappings y default
        //    mappings devuelvan value null, setearlo, en caso que no devuelvan null utilizar el value de la base entity
        //  - A la hora de construir el objeto, se tendra una lista de propiedades que incluyen
        if (prop == null) {
            var value: Any? = null
            // If the target property wasn't excluded or mapped until this point, look for the value in the original entity
            val originalProp = originalEntity.properties[targetFieldName]
            // If there's no property with the same name in the original entity, skip
                ?: return@forEach

            if (originalProp.value != null) {
                // If the target field is the same type or a supertype of the original property, use that property
                value = if (targetField.value.type.isSupertypeOf(originalProp.type)) {
                    originalProp.value
                } else {
                    // If not, map it
                    mapTo(originalEntity = originalProp, targetType= targetField.value.type)
                }
            }

            prop = if (value == null && baseEntity?.second != UpdateOption.IGNORE_NULLS) {
                null
            } else PropertyWrapper(value)
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
        val subfieldExclusions = fieldExclusions.mapNotNull { exclusion ->
            parseLocation(exclusion).let { loc ->
                if (loc.first() == targetFieldName && loc.size > 1) {
                    loc.drop(1).joinToString("/")
                } else null
            }
        }

        val finalProp = if (subFieldMappedValues.isNotEmpty() || subfieldExclusions.isNotEmpty()) {
            val newBaseEntity = (base?.properties?.get(targetFieldName)?.value)?.let {
                it to baseEntity.second
            }

            if (prop?.value == null) {
                val nextMappedValues = subFieldMappedValues.filter {
                    parseLocation(it.key).size == 1
                }

                val newProp = buildOrUpdate(
                    type = targetField.value.type,
                    properties = nextMappedValues,
                    useSettersOnly = useSettersOnly,
                    baseEntity = newBaseEntity,
                )
                if (newProp != null) {
                    val newPropEntity = originalEntity.properties[targetFieldName]?.apply {
                        this.value = newProp
                    } ?: OriginalEntity(newProp, newProp::class.starProjectedType)

                    val remainingMappedValues = subFieldMappedValues.filterNot { it.key in nextMappedValues.keys }
                    mapTo(
                        originalEntity = newPropEntity,
                        targetType = targetField.value.type,
                        exclusions = subfieldExclusions,
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
                    exclusions = subfieldExclusions,
                    functionMappings = subFieldMappedValues.map {
                        null to ({ it.value } to (it.key to MappingFallback.NULL))
                    }.toMap(),
                    baseEntity = newBaseEntity
                )
            }
        } else prop?.value ?: return@forEach

        props[targetFieldName] = finalProp
    }

    return if (baseEntity != null && !useSettersOnly && props.isNotEmpty()) {
        buildWithBaseEntity(
            type = targetType,
            properties = props,
            baseEntity = baseEntity.first,
        )
    } else {
        buildOrUpdate(
            type = targetType,
            properties = props,
            useSettersOnly = useSettersOnly,
            baseEntity = baseEntity?.first,
        )
    }
}

/**
 * Maps primitive types if possible
 *
 * @param targetType the target type
 * @param originalEntity the original entity
 * @param mapPrimitives boolean to enable the mapping between string and number primitives
 * @return null if the target type and the original entity are not primitives, else a property wrapper with containing
 *  the mapped value or null in case mapping is not possible
 */
private fun mapPrimitives(
    targetType: KType,
    originalEntity: OriginNodeInterface<*>,
    mapPrimitives: Boolean,
): PropertyWrapper<Any?>? {
    // If target type is a primitive
    if (targetType.isSubtypeOf(typeOf<String>()) || targetType.isSubtypeOf(typeOf<Number>())) {
        // But the original entity type is not a primitive, the value can't be mapped, so return null
        if (!originalEntity.type.isSubtypeOf(typeOf<String>()) && !originalEntity.type.isSubtypeOf(typeOf<Number>()))
            return PropertyWrapper(null)
    } else { // If the target type is not a primitive
        // If the original entity type is also not a primitive, return null
        //  to continue the mapTo function, since the values could be mapped
        return if (!originalEntity.type.isSubtypeOf(typeOf<String>()) && !originalEntity.type.isSubtypeOf(typeOf<Number>())) {
            null
        } else PropertyWrapper(null)
        // If the original type is not a primitive, the value can't be mapped, so return null
    }

    if (!mapPrimitives) PropertyWrapper(null)

    // If the types are String, return the value directly
    if (targetType.isSubtypeOf(typeOf<String>()) && originalEntity.type.isSubtypeOf(typeOf<String>()))
        return PropertyWrapper(originalEntity.value)

    // If the types are Number, return the value mapped with the native function
    if (targetType.isSubtypeOf(typeOf<Number>()) && originalEntity.type.isSubtypeOf(typeOf<Number>())) {
        val value: Number? = when (targetType) {
            typeOf<Double>() -> (originalEntity.value as Number).toDouble()
            typeOf<Float>() -> (originalEntity.value as Number).toFloat()
            typeOf<Long>() -> (originalEntity.value as Number).toLong()
            typeOf<Int>() -> (originalEntity.value as Number).toInt()
            typeOf<Short>() -> (originalEntity.value as Number).toShort()
            typeOf<Byte>() -> (originalEntity.value as Number).toByte()
            else -> null
        }
        return value?.let { PropertyWrapper(it) }
    }

    if (targetType.isSubtypeOf(typeOf<Number>()) && originalEntity.type.isSubtypeOf(typeOf<String>())) {
        val value: Number? = try { when (targetType) {
            typeOf<Double>() -> (originalEntity.value as String).toDouble()
            typeOf<Float>() -> (originalEntity.value as String).toFloat()
            typeOf<Long>() -> (originalEntity.value as String).toLong()
            typeOf<Int>() -> (originalEntity.value as String).toInt()
            typeOf<Short>() -> (originalEntity.value as String).toShort()
            typeOf<Byte>() -> (originalEntity.value as String).toByte()
            else -> null
        }} catch (_: Exception) { null }
        return value?.let { PropertyWrapper(it) }
    }

    if (targetType.isSubtypeOf(typeOf<String>()) && originalEntity.type.isSubtypeOf(typeOf<Number>()))
        return PropertyWrapper(originalEntity.value.toString())

    return PropertyWrapper(null)
}

/**
 * Map composite classes, if possible
 *
 * @param targetType the target type
 * @param originalEntity the original entity
 * @return null if the target type and the original entity type aren't supertypes of iterable, map, pair, or triple.
 *  Else, return a property wrapper containing the mapped values or null in case mapping is not possible
 */
private fun mapComposite(
    targetType: KType,
    originalEntity: OriginNodeInterface<*>,
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean,
    useSettersOnly: Boolean,
    mapPrimitives: Boolean,
): PropertyWrapper<Any?>? {

    // If the target type and original type are not both supertypes or the same type as iterable, map, pair,
    //  or triple, return null
    if ((!typeOf<Iterable<*>>().isSupertypeOf(targetType) && !typeOf<Iterable<*>>().isSupertypeOf(originalEntity.type))
        && (!typeOf<Map<*, *>>().isSupertypeOf(targetType) && !typeOf<Map<*, *>>().isSupertypeOf(originalEntity.type))
        && (!typeOf<Pair<*, *>>().isSupertypeOf(targetType) && !typeOf<Pair<*, *>>().isSupertypeOf(originalEntity.type))
        && (!typeOf<Triple<*, *, *>>().isSupertypeOf(targetType) && !typeOf<Triple<*, *, *>>().isSupertypeOf(originalEntity.type)))
        return null


    // If the target type and original type are both supertypes or the same type as iterable, map each item
    if (typeOf<Iterable<*>>().isSupertypeOf(targetType) && typeOf<Iterable<*>>().isSupertypeOf(originalEntity.type)) {
        val subTargetType = targetType.arguments.first().type ?: return null

        return (originalEntity.value as Iterable<*>).mapNotNull { item ->
            item?.let {
                val subOriginalType = it::class.starProjectedType

                mapTo(
                    originalEntity = OriginalEntity(it, subOriginalType),
                    targetType = subTargetType,
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            }
        }.let { value -> PropertyWrapper(value) }
    }

    // If the target type and original type are both supertypes or the same type as map, map each item
    if (typeOf<Map<*, *>>().isSupertypeOf(targetType) && typeOf<Map<*, *>>().isSupertypeOf(originalEntity.type)) {
        val subTargetType = targetType.arguments

        return (originalEntity.value as Map<*, *>).map { (key, item) ->
            val keyTargetType = subTargetType[0].type ?: return null
            val itemTargetType = subTargetType[1].type ?: return null

            key?.let {
                val subOriginalType = it::class.starProjectedType

                mapTo(
                    originalEntity = OriginalEntity(it, subOriginalType),
                    targetType = keyTargetType,
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            } to item?.let {
                val subOriginalType = it::class.starProjectedType

                mapTo(
                    originalEntity = OriginalEntity(it, subOriginalType),
                    targetType = itemTargetType,
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            }
        }.toMap().let { value -> PropertyWrapper(value) }
    }

    // If the target type and original type are both supertypes or the same type as pair, map each item
    if (typeOf<Pair<*, *>>().isSupertypeOf(targetType) && typeOf<Pair<*, *>>().isSupertypeOf(originalEntity.type)) {
        val subTargetType = targetType.arguments

        return (originalEntity.value as Pair<*, *>).let { (first, second) ->
            val firstTargetType = subTargetType[0].type ?: return null
            val secondTargetType = subTargetType[1].type ?: return null

            first?.let {
                val subOriginalType = it::class.starProjectedType

                mapTo(
                    originalEntity = OriginalEntity(it, subOriginalType),
                    targetType = firstTargetType,
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            } to second?.let {
                val subOriginalType = it::class.starProjectedType

                mapTo(
                    originalEntity = OriginalEntity(it, subOriginalType),
                    targetType = secondTargetType,
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )
            }
        }.let { value -> PropertyWrapper(value) }
    }

    // If the target type and original type are both supertypes or the same type as triple, map each item
    if (typeOf<Triple<*, *, *>>().isSupertypeOf(targetType) && typeOf<Triple<*, *, *>>().isSupertypeOf(originalEntity.type)) {
        val subTargetType = targetType.arguments

        return (originalEntity.value as Triple<*, *, *>).let { (first, second, third) ->
            val firstTargetType = subTargetType[0].type ?: return null
            val secondTargetType = subTargetType[1].type ?: return null
            val thirdTargetType = subTargetType[2].type ?: return null

            Triple(
                first?.let {
                    val subOriginalType = it::class.starProjectedType

                    mapTo(
                        originalEntity = OriginalEntity(it, subOriginalType),
                        targetType = firstTargetType,
                        exclusions = exclusions,
                        mappings = mappings,
                        functionMappings = functionMappings,
                        ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                        useSettersOnly = useSettersOnly,
                        mapPrimitives = mapPrimitives,
                    )},
                second?.let {
                val subOriginalType = it::class.starProjectedType

                mapTo(
                    originalEntity = OriginalEntity(it, subOriginalType),
                    targetType = secondTargetType,
                    exclusions = exclusions,
                    mappings = mappings,
                    functionMappings = functionMappings,
                    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                    useSettersOnly = useSettersOnly,
                    mapPrimitives = mapPrimitives,
                )},
                third?.let {
                    val subOriginalType = it::class.starProjectedType

                    mapTo(
                        originalEntity = OriginalEntity(it, subOriginalType),
                        targetType = thirdTargetType,
                        exclusions = exclusions,
                        mappings = mappings,
                        functionMappings = functionMappings,
                        ignoreMapFromAnnotations = ignoreMapFromAnnotations,
                        useSettersOnly = useSettersOnly,
                        mapPrimitives = mapPrimitives,
                    )},
            )
        }.let { value -> PropertyWrapper(value) }
    }

    // If target type is composite but original type is not, or vice versa, we cannot map the values,
    //  return property value null
    return PropertyWrapper(null)
}
