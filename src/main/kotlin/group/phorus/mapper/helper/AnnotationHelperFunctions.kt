package group.phorus.mapper.helper

import group.phorus.mapper.enums.MappingFallback
import group.phorus.mapper.MapFrom
import java.util.HashMap
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

internal fun handleMapFromAnnotation(
    prop: KMutableProperty<*>,
    originalEntity: Any,
    entity: Any,
    exclusions: List<String>? = null,
    mappings: HashMap<String, String>? = null,
): Boolean {
    val (locations, fallback) = getAnnotationFields(prop)
    locations?.forEachIndexed locationsLoop@{ index, location ->
        originalEntity.javaClass.kotlin.memberProperties.forEach targetLoop@{
            if (exclusions?.contains(prop.name) == true) return true
            val originalProp = it.get(originalEntity) ?: return@targetLoop

            // TODO Add test verifying that the customMappings and mappings override the mapFrom annotation
            if (mappings?.contains(prop.name) == true) return@targetLoop

            val returnVal = mapAnnotation(prop, it, originalProp, location)

            if (returnVal != null) {
                prop.setter.call(entity, returnVal)
                return true
            }
        }

        if (index == locations.size-1 && fallback == MappingFallback.NULL) {
            prop.setter.call(entity, null)
            return true
        }
    }

    return false
}

@PublishedApi
internal inline fun <reified A, reified B> handleMapFromAnnotation(
    prop: KMutableProperty<*>,
    originalEntity: Any,
    entity: Any,
    exclusions: List<String>? = null,
    mappings: HashMap<String, String>? = null,
    customMappings: HashMap<String, Pair<String, (source: A) -> B>>? = null,
): Boolean {
    val (locations, fallback) = getAnnotationFields(prop)
    locations?.forEachIndexed locationsLoop@{ index, location ->
        originalEntity.javaClass.kotlin.memberProperties.forEach targetLoop@{
            if (exclusions?.contains(prop.name) == true) return true
            val originalProp = it.get(originalEntity) ?: return@targetLoop

            if (mappings?.contains(prop.name) == true) return@targetLoop
            if (customMappings?.contains(prop.name) == true) return@targetLoop

            val returnVal = mapAnnotation(prop, it, originalProp, location)

            if (returnVal != null) {
                prop.setter.call(entity, returnVal)
                return true
            }
        }

        if (index == locations.size-1 && fallback == MappingFallback.NULL) {
            prop.setter.call(entity, null)
            return true
        }
    }

    return false
}


@PublishedApi
internal fun getAnnotationFields(
    prop: KProperty<*>,
): Pair<Array<String>?, MappingFallback?> {
    var locations: Array<String>? = null
    var fallback: MappingFallback? = null
    prop.javaField?.annotations?.forEach { annotation ->
        if (annotation is MapFrom && annotation.locations.isNotEmpty()) {
            locations = annotation.locations
            fallback = annotation.fallback
        }
    }

    return Pair(locations, fallback)
}

@PublishedApi
internal fun mapAnnotation(
    prop1: KProperty<*>,
    prop2: KProperty<*>,
    originalProp: Any,
    location: String,
): Any? {
    if (location.isBlank()) return null
    val (targetPropType, originalPropType) = getPropTypes(prop1, prop2)

    if (location.contains(".")) {
        val subLocations: MutableList<String> = location.split(".").toMutableList()
        if (subLocations.size == 1) return null

        // If any subLocation is blank return null to try the next location
        subLocations.forEach { subLocation -> if (subLocation.isBlank()) return null }

        return mapSubLocations(prop1, prop2, originalProp, subLocations)
    } else if (prop2.name == location) {
        return mapValue(prop1, prop2, targetPropType, originalPropType, originalProp)
    }

    return null
}

internal fun mapSubLocations(
    prop1: KProperty<*>,
    prop2: KProperty<*>,
    originalProp: Any,
    subLocations: MutableList<String>,
): Any? {
    if (subLocations.size > 1) {
        if (prop2.name == subLocations[0]) {
            subLocations.removeAt(0)

            originalProp.javaClass.kotlin.memberProperties.forEach {
                if (it.name == subLocations[0]) {
                    val newOrigProp = it.get(originalProp) ?: return null
                    return mapSubLocations(prop1, it, newOrigProp, subLocations)
                }
            }
        }
    } else {
        val (targetPropType, originalPropType) = getPropTypes(prop1, prop2)
        return mapValue(prop1, prop2, targetPropType, originalPropType, originalProp)
    }

    return null
}