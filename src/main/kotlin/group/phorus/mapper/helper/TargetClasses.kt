package group.phorus.mapper.helper

import group.phorus.mapper.MapFrom
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Wrapper that allows you to traverse through a class more comfortably
 * @param T class type
 * @param clazz class
 */
class TargetClass<T: Any>(clazz: KClass<T>) {

    /**
     * Class properties
     */
    val properties: Map<String, TargetNode<*>> = clazz.memberProperties.associate { it.name to TargetNode(it) }
}

/**
 * Utility function to use TargetClass with a reified class type
 */
inline fun <reified T: Any> targetClass() = TargetClass(T::class)

/**
 * Wrapper that allows you to traverse through the properties of a class more comfortably
 * @param T property type
 * @param property property
 */
class TargetNode<T>(property: KProperty<T>) {

    val type: KType by lazy { property.returnType }

    /**
     * First MapFrom annotation with non-empty locations
     */
    val mapFrom: MapFrom? by lazy { property.javaField?.annotations
        ?.firstOrNull { it is MapFrom && it.locations.isNotEmpty() } as MapFrom? }

    /**
     * Class sub properties
     */
    val properties: Map<String, TargetNode<*>> by lazy {
        (type.classifier as KClass<*>).memberProperties.associate { it.name to TargetNode(it) }
    }
}