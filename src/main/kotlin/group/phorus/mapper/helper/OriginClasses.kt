package group.phorus.mapper.helper

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

typealias Value = Any

/**
 * Origin classes common interface, used to iterate bidirectionally through the nodes
 * @param T the contained class type
 */
interface NodeInterface<T> {

    val parent: NodeInterface<*>?
        get() = null
    val type: KType?
        get() = null

    val value: T?
    val properties: Map<String, OriginNode<T, *>>
}

/**
 * Wrapper that allows you to traverse through the instance of an object (entity) more comfortably
 * @param T entity type
 * @param entity entity
 */
class OriginEntity<T: Any>(entity: T) : NodeInterface<T> {

    override val value: T = entity

    /**
     * Entity properties
     */
    override val properties: Map<String, OriginNode<T, *>> = entity::class.memberProperties
        .associate { prop -> prop.name to OriginNode(this, prop) }
}

/**
 * Wrapper that allows you to traverse through the properties of an object (entity) more comfortably
 * @param T parent entity type
 * @param parentEntity parent entity, used to get the property value
 * @param property property
 */
class OriginNode<T, B>(parentEntity: NodeInterface<T>, property: KProperty<B>) : NodeInterface<B> {

    override val parent: NodeInterface<T> = parentEntity

    override val type: KType by lazy { property.returnType }
    override val value: B? by lazy { property.getter.call(parentEntity.value) }

    /**
     * Sub properties, null if value is null
     */
    override val properties: Map<String, OriginNode<B, *>> by lazy {
        value?.let { (type.classifier as KClass<*>).memberProperties
            .associate { prop -> prop.name to OriginNode(this, prop) }} ?: emptyMap()
    }
}