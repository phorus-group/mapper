package group.phorus.mapper.helper

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

typealias Value = Any

/**
 * Wrapper that allows you to traverse through the instance of an object (entity) more comfortably
 * @param T entity type
 * @param entity entity
 */
class OriginEntity<T: Any>(entity: T) {

    /**
     * Entity properties
     */
    val properties: List<OriginNode<T, *>> = entity::class.memberProperties.map { prop -> OriginNode(entity, prop) }

    override fun toString(): String = "$properties"
}

/**
 * Wrapper that allows you to traverse through the properties of an object (entity) more comfortably
 * @param T parent entity type
 * @param parentEntity parent entity, used to get the property value
 * @param property property
 */
class OriginNode<T, B>(parentEntity: T, property: KProperty<B>) {

    val name: String by lazy { property.name }
    val type: KType by lazy { property.returnType }
    val value: B? by lazy { property.getter.call(parentEntity) }

    /**
     * Sub properties, null if value is null
     */
    val properties: List<OriginNode<B, *>> by lazy { value?.let { (type.classifier as KClass<*>).memberProperties
        .map { prop -> OriginNode(it, prop) }} ?: emptyList() }

    override fun toString(): String = "{ \"name\": \"$name\", \"type\": \"$type\", \"value\": \"$value\"" +
            "${if (properties.isEmpty()) "" else ", \"properties\": [ $properties ]"}}"
}