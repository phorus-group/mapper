package group.phorus.mapper.helper

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

/**
 * Origin classes common interface, used to iterate bidirectionally through the nodes
 * @param T the contained class type
 */
interface OriginNodeInterface<T> {

    val parent: OriginNodeInterface<*>?
        get() = null

    var type: KType
    var value: Any?
    val properties: Map<String, OriginNode<T, *>>

    /**
     * Looks for a node in a specific location.
     *
     * @param location location of the desired node, formatted as a List<String>. Location example: ["pet", "name"]
     * @return the node, or null if it doesn't exist
     */
    fun findProperty(location: List<String>): OriginNodeInterface<*>? {

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
 * Wrapper that allows you to traverse through the instance of an object (entity) more comfortably
 * @param T entity type
 * @param value entity
 */
class OriginalEntity<T: Any>(
    value: T,
    override var type: KType,
) : OriginNodeInterface<T> {

    override var value: Any? = value

    /**
     * Entity properties
     */
    override val properties: Map<String, OriginNode<T, *>> = value::class.memberProperties
        .associate { prop -> prop.name to OriginNode(this, prop) }
}

/**
 * Wrapper that allows you to traverse through the properties of an object (entity) more comfortably
 * @param T parent entity type
 * @param parent parent entity, used to get the property value
 * @param property property
 */
class OriginNode<T, B>(
    override val parent: OriginNodeInterface<T>,
    property: KProperty<B>,
) : OriginNodeInterface<B> {

    override var type: KType = property.returnType
    override var value: Any? = try { property.getter.call(parent.value) } catch (_: Exception) { null }

    /**
     * Sub properties, null if value is null
     */
    override val properties: Map<String, OriginNode<B, *>> by lazy {
        value?.let { (type.classifier as KClass<*>).memberProperties
            .associate { prop -> prop.name to OriginNode(this, prop) }} ?: emptyMap()
    }
}