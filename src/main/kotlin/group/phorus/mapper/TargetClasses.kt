package group.phorus.mapper

import group.phorus.mapper.mapping.MapFrom
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * [TargetClass] and [TargetNode] common interface.
 * Contains the [findProperty] function and the common [properties] field.
 *
 * @param T the wrapped class type.
 */
interface TargetNodeInterface<T> {

    /**
     * The class properties presented in a [Map] with the property name as key, and the property
     * wrapped with [TargetNode] as value.
     */
    val properties: Map<Field, TargetNode<*>>

    /**
     * Looks for a [node][TargetNode] in a specific location.
     *
     * @param location location of the desired node, formatted as a [list][List] of [fields][Field].
     *
     * For example: {"pet", "name"}
     *
     * @return the found [node][TargetNode], or null if it doesn't exist.
     */
    fun findProperty(location: List<Field>): TargetNode<*>? {

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
 * Implementation of the [TargetNodeInterface] interface.
 * Wraps around a class of [type][type] [T].
 *
 * @param T the class type.
 * @param clazz the class.
 * @param type the [T] type as a [KType].
 * This is required since kotlin cannot extract a [KType] from a class without loosing its type arguments.
 */
class TargetClass<T: Any>(
    clazz: KClass<T>,

    /**
     * The class type.
     */
    val type: KType,
) : TargetNodeInterface<T> {

    /**
     * @see [TargetNodeInterface.properties]
     */
    override val properties: Map<Field, TargetNode<*>> = clazz.memberProperties.associate { it.name to TargetNode(it) }
}

/**
 * Implementation of the [TargetNodeInterface] interface.
 * Wraps around a property of type [T].
 *
 * @param T the class type.
 * @param property the property of type [T].
 */
class TargetNode<T>(property: KProperty<T>) : TargetNodeInterface<T> {

    /**
     * The class type.
     */
    val type: KType by lazy { property.returnType }

    /**
     * The property [@MapFrom][MapFrom] annotation, if present.
     */
    val mapFrom: MapFrom? by lazy { property.javaField?.annotations
        ?.firstOrNull { it is MapFrom && it.locations.isNotEmpty() } as MapFrom? }

    /**
     * @see [TargetNodeInterface.properties]
     */
    override val properties: Map<Field, TargetNode<*>> by lazy {
        (type.classifier as KClass<*>).memberProperties.associate { it.name to TargetNode(it) }
    }
}