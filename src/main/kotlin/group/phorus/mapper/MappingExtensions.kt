package group.phorus.mapper

import group.phorus.mapper.enums.MappingReturnCodes
import group.phorus.mapper.helper.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


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
 *
 */

inline fun <T : Any, reified A, reified B> Any.mapTo(
    entityClass: KClass<out T>? = null,
    exclusions: List<String>? = null,
    mappings: Map<String, String>? = null,
    customMappings: Map<String, Pair<(source: A) -> B, String>>? = null,
    baseObject: T? = null,
): T? {
    val entity: T
    try {
        entity = baseObject ?: (entityClass?.createInstance() ?: return null)
        if (entity is String) return null
    } catch (e: Exception) {
        return null
    }

    entity::class.memberProperties.forEach { prop ->

        if (prop !is KMutableProperty<*>)
            return@forEach

        handleMapFromAnnotation(prop, this, entity, exclusions, mappings, customMappings)
            .also { if (it) return@forEach }

        javaClass.kotlin.memberProperties.forEach targetLoop@{
            if (exclusions?.contains(prop.name) == true) return@forEach
            val originalProp: Any
            try {
                originalProp = it.get(this) ?: return@targetLoop
            } catch (e: Exception) {
                return null
            }

            // If the value is mapped through custom mappings, ignore the normal mapping
            var newValue = getNewValueCustomMappings(prop, it, customMappings, originalProp)
                ?: getNewValue(prop, it, mappings, originalProp)

            if (newValue == MappingReturnCodes.CUSTOM_MAPPING)
                return@targetLoop

            if (newValue == MappingReturnCodes.NORMAL_MAPPING)
                newValue = getNewValue(prop, it, mappings, originalProp)


            newValue?.let { value ->
                prop.setter.call(entity, value)
                return@forEach
            }
        }
    }

    return entity
}

fun <T : Any> Any.mapTo(
    entityClass: KClass<out T>? = null,
    exclusions: List<String>? = null,
    mappings: Map<String, String>? = null,
    baseObject: T? = null,
): T? {
    val entity: T
    try {
        entity = baseObject ?: (entityClass?.createInstance() ?: return null)
        if (entity is String) return null
    } catch (e: Exception) {
        return null
    }

    entity::class.memberProperties.forEach { prop ->

        if (prop !is KMutableProperty<*>)
            return@forEach

        handleMapFromAnnotation(prop, this, entity, exclusions, mappings)
            .also { if (it) return@forEach }

        javaClass.kotlin.memberProperties.forEach targetLoop@{
            if (exclusions?.contains(prop.name) == true) return@forEach
            val originalProp: Any
            try {
                originalProp = it.get(this) ?: return@targetLoop
            } catch (e: Exception) {
                return null
            }

            getNewValue(prop, it, mappings, originalProp)
                ?.let { value ->
                    prop.setter.call(entity, value)
                    return@forEach
                }
        }
    }

    return entity
}


@PublishedApi
internal fun getNewValue(
    prop1: KProperty<*>,
    prop2: KProperty<*>,
    mappings: Map<String, String>?,
    originalProp: Any,
): Any? {
    val (targetPropType, originalPropType) = getPropTypes(prop1, prop2)

    // If the original property is present in mappings, return if the target prop is the one expected
    if (mappings?.contains(prop2.name) == true) {
        return if (prop1.name == mappings[prop2.name]) {
            mapValue(prop1, prop2, targetPropType, originalPropType, originalProp)
        } else { // if not return null
            null
        }
    }

    // If the props have the same name, return
    if (prop1.name == prop2.name)
        return mapValue(prop1, prop2, targetPropType, originalPropType, originalProp)

    // If it was not possible to map the value, return null
    return null
}

@PublishedApi
internal inline fun <reified A, reified B> getNewValueCustomMappings(
    prop1: KProperty<*>,
    prop2: KProperty<*>,
    customMappings: Map<String, Pair<(source: A) -> B, String>>? = null,
    originalProp: Any,
): Any? {
    val (targetPropType, originalPropType) = getPropTypes(prop1, prop2)

    // If no custom mappings are provided, or if no custom mappings uses the
    //  current originalProp as source, return null
    if (customMappings?.contains(prop2.name) != true)
        return MappingReturnCodes.NORMAL_MAPPING

    // If the target prop is not found, skip and look for the right one
    if (prop1.name != customMappings[prop2.name]?.second)
        return MappingReturnCodes.CUSTOM_MAPPING

    // If source of the data don't have the same type as the input of the function, map using the normal mapping
    if (originalPropType != A::class.qualifiedName.toString().replace("?", ""))
        return MappingReturnCodes.NORMAL_MAPPING

    // If the return value is null, return null
    val newVal = customMappings[prop2.name]?.first!!(originalProp as A) ?: return MappingReturnCodes.CUSTOM_MAPPING


    // If the props are iterables, then map the subProps
    if (MutableCollection::class.isSuperclassOf(prop1.returnType.jvmErasure)
        && Iterable::class.isSuperclassOf(newVal::class))
        return getMappedCollection(prop1, newVal).let { if (it?.isEmpty() == true) null else it }

    // If the type of the return value is different from the target's type, map using the mapTo function
    if (targetPropType != getClassType(newVal::class) && targetPropType != Any::class.qualifiedName.toString()) {
        return newVal.mapTo(prop1.returnType.jvmErasure)
    }

    // If not, map directly
    return newVal
}