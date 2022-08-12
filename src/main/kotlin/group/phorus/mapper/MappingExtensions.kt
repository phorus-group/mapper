package group.phorus.mapper

import group.phorus.mapper.enums.MappingFallback
import group.phorus.mapper.helper.*
import kotlin.reflect.*
import kotlin.reflect.full.*


/**
 *
 * 1- Iterar entre todos los props del target y guardar en un Map<String, KMutableProperty<*>>
 * 2- Iterar entre todos los props del origin y guardar en un Map<String, KMutableProperty<*>>
 * 3- Mapear campos con mismo nombre / key, seteando mediante un setter / constructor, o llamando a mapTo nuevamente
 * 4- Para mapear usando el constructor, usar kclass.constructor.parameters y ver si el existe un constructor
 *      con los parametros necesarios, en caso que exista utilizarlo con constructor.callBy
 * 5- Al buscar dicho constructor, guardar los parametros que si existen en el constructor y los que no pero tienen setter
 * 6- Crear anotacion de clase para preferir mappear con setters o con constructores primero, tambien dar la opcion
 *      en el metodo para hacerlo en el mappeo entero
 * 7- Hacer que la recursividad del mapTo sea llamando a un metodo interno que devuelva un map de referencias
 *      Map<String, KMutableProperty<*>>, de forma tal que todos los mapeos se pueda hacer al final de la primera ejecucion
 *
 * 8- Crear test que valide un mapFrom dede un map keys y/o un map values a un list
 * 9- Puede que agregar tambien un parametro functions que acepte un Map<String, (source: Any) -> Any>
 * 10- Agregar variaciones del metodo mapTo simplificadas en la parte de mappings y functionmappings para utilizar
 *      el Fallback continue por defualt
 */

inline fun <reified T: Any> Any.mapTo(
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
): T? = mapTo(
    originalEntity = OriginalEntity(this, this::class.starProjectedType /* TODO: Check that doesn't cause problems*/),
    targetType = typeOf<T>(),
    exclusions = exclusions,
    mappings = mappings,
    functionMappings = functionMappings,
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
) as T?

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
    baseObject: Any? = null,
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
): Any? {
    // Format the exclusion locations
    val fieldExclusions = exclusions.map { parseLocation(it).joinToString("/") }

    // TODO: use the KType to validate: typeOf<Collection<*>>().isSuperTypeOf(kType), the same for Map and for Pair and Triple
    val targetClass = TargetClass(targetType.classifier as KClass<*>)
    val baseEntity = baseObject?.let { OriginalEntity(it, targetType) }

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

            prop = PropertyWrapper(value)
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
            if (prop.value == null) {
                val nextMappedValues = subFieldMappedValues.filter {
                    parseLocation(it.key).size == 1
                }

                val newProp = buildOrUpdate(
                    type = targetField.value.type,
                    properties = nextMappedValues,
                    useSettersOnly = useSettersOnly,
                    baseEntity = baseEntity,
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
                        baseObject = baseEntity?.properties?.get(targetFieldName)?.value
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
                    baseObject = baseEntity?.properties?.get(targetFieldName)?.value
                )
            }
        } else prop.value

        props[targetFieldName] = finalProp
    }

    return if (baseEntity != null && !useSettersOnly) {
        buildWithBaseEntity(
            type = targetType,
            properties = props,
            baseEntity = baseEntity,
        )
    } else {
        buildOrUpdate(
            type = targetType,
            properties = props,
            useSettersOnly = useSettersOnly,
            baseEntity = baseEntity,
        )
    }
}
