package group.phorus.mapper

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

/**
 * [OriginalEntity] and [OriginalNode] common interface.
 * Contains the [findProperty] function and common fields.
 *
 * @param T the wrapped entity type.
 */
interface OriginalNodeInterface<T> {

    /**
     * The parent entity, or null if it doesn't exist.
     */
    val parent: OriginalNodeInterface<*>?
        get() = null

    /**
     * The entity type.
     */
    var type: KType

    /**
     * The entity.
     */
    var value: Any?

    /**
     * The entity properties presented in a [Map] with the property name as key, and the property
     * wrapped with [OriginalNode] as value.
     */
    val properties: Map<Field, OriginalNode<T, *>>

    /**
     * Looks for a [node][OriginalNodeInterface] in a specific location.
     *
     * @param location location of the desired node, formatted as a [list][List] of [fields][Field].
     *
     * For example: {"pet", "name"}
     *
     * @return the found [node][OriginalNodeInterface], or null if it doesn't exist.
     */
    fun findProperty(location: List<Field>): OriginalNodeInterface<*>? {

        // If the location list is empty, return null
        if (location.isEmpty())
            return null

        // Find the first node of the location, if we cannot find it return null
        val property = if (location.first() == "..") {
            parent ?: return null
        } else {
            properties[location.first()] ?: return null
        }

        // If this is the last location, return the found property
        if (location.size == 1)
            return property

        // Go to the next node
        return property.findProperty(location.drop(1))
    }
}

/**
 * Implementation of the [OriginalNodeInterface] interface.
 * Wraps around an entity of [type][type] [T].
 *
 * @param T the entity type.
 * @param value the entity.
 * @param type the [T] type as a [KType].
 * This is required since kotlin cannot extract a [KType] from a class without loosing its type arguments.
 */
class OriginalEntity<T: Any>(
    value: T,

    /**
     * @see [OriginalNodeInterface.type]
     */
    override var type: KType,
) : OriginalNodeInterface<T> {

    /**
     * @see [OriginalNodeInterface.value]
     */
    override var value: Any? = value

    /**
     * @see [OriginalNodeInterface.properties]
     */
    override val properties: Map<Field, OriginalNode<T, *>> by lazy { value::class.memberProperties
        .associate { prop -> prop.name to OriginalNode(this, prop) } }
}

/**
 * Implementation of the [OriginalNodeInterface] interface.
 * Wraps around a property of type [B].
 *
 * @param T the parent type.
 * @param B the entity type.
 * @param parent the entity parent.
 * @param property the property of type [B].
 */
class OriginalNode<T, B>(
    /**
     * @see [OriginalNodeInterface.parent]
     */
    override val parent: OriginalNodeInterface<T>,
    property: KProperty<B>,
) : OriginalNodeInterface<B> {

    /**
     * @see [OriginalNodeInterface.type]
     */
    override var type: KType = property.returnType

    /**
     * @see [OriginalNodeInterface.value]
     */
    override var value: Any? = runCatching { property.getter.call(parent.value) }.getOrNull()

    /**
     * @see [OriginalNodeInterface.properties]
     */
    override val properties: Map<Field, OriginalNode<B, *>> by lazy {
        value?.let { (type.classifier as KClass<*>).memberProperties
            .associate { prop -> prop.name to OriginalNode(this, prop) }} ?: emptyMap()
    }
}