package group.phorus.mapper

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Target classes common interface, provides a function to search a specific property in the entire tree
 * @param T the contained class type
 */
interface TargetNodeInterface<T> {

    val properties: Map<String, TargetNode<*>>

    fun findProperty(location: List<String>): TargetNode<*>? {

        // If the location list is empty, return null
        if (location.isEmpty())
            return null

        // Find the first node of the location, if we cannot find it return null
        val property = properties[location.first()] ?: return null

        // If this is the last location, return the found property
        if (location.size == 1)
            return property

        // Go to the next node
        return property.findProperty(location.drop(1))
    }
}

/**
 * Wrapper that allows you to traverse through a class more comfortably
 * @param T class type
 * @param clazz class
 */
class TargetClass<T: Any>(clazz: KClass<T>, val type: KType) : TargetNodeInterface<T> {

    /**
     * Class properties
     */
    override val properties: Map<String, TargetNode<*>> = clazz.memberProperties.associate { it.name to TargetNode(it) }
}

/**
 * Wrapper that allows you to traverse through the properties of a class more comfortably
 * @param T property type
 * @param property property
 */
class TargetNode<T>(property: KProperty<T>) : TargetNodeInterface<T> {

    val type: KType by lazy { property.returnType }

    /**
     * First MapFrom annotation with non-empty locations
     */
    val mapFrom: MapFrom? by lazy { property.javaField?.annotations
        ?.firstOrNull { it is MapFrom && it.locations.isNotEmpty() } as MapFrom? }

    /**
     * Class sub properties
     */
    override val properties: Map<String, TargetNode<*>> by lazy {
        (type.classifier as KClass<*>).memberProperties.associate { it.name to TargetNode(it) }
    }
}