package group.phorus.mapper.helper

import group.phorus.mapper.mapTo
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmErasure

internal fun mapValue(
    prop1: KProperty<*>,
    prop2: KProperty<*>,
    targetPropType: String,
    originalPropType: String,
    originalProp: Any,
): Any? {
    // If the props are iterables, then map the subProps
    if (MutableCollection::class.isSuperclassOf(prop1.returnType.jvmErasure)
        && Iterable::class.isSuperclassOf(prop2.returnType.jvmErasure))
        return getMappedCollection(prop1, originalProp).let { if (it?.isEmpty() == true) null else it }

    // If the props don't have the same type and if the target type isn't Any, map using the mapTo function
    if (originalPropType != targetPropType && targetPropType != Any::class.qualifiedName.toString()) {
        return originalProp.mapTo(prop1.returnType.jvmErasure)
    }

    // If not, map directly
    return originalProp
}

@PublishedApi
internal fun getMappedCollection(
    prop: KProperty<*>,
    value: Any,
): MutableCollection<Any>? {

    // If the target prop is not a MutableList / MutableSet, return null
    val collection = when (prop.returnType.jvmErasure) {
        MutableList::class -> mutableListOf<Any>()
        MutableSet::class -> mutableSetOf()
        else -> null
    } ?: return null

    // Iterate through every subProp of the originalProp
    (value as Iterable<*>).forEach subPropsLoop@{ subProp ->

        // If the subProp is null, continue to the next one
        if (subProp == null) return@subPropsLoop

        // Get the class of the original Iterable's type
        val clazz = prop.returnType.arguments[0].type?.jvmErasure as KClass<out Any>

        // If the original Iterable's type is different from the subProp type, map using the mapTo function
        if (getClassType(clazz) != getClassType(subProp::class)
            && getClassType(clazz) != Any::class.qualifiedName.toString()) {
            subProp.mapTo(clazz)?.let { collection.add(it) }
        } else {
            collection.add(subProp)
        }
    }

    // Return the mapped collection
    return collection
}

@PublishedApi
internal fun getPropTypes(prop1: KProperty<*>, prop2: KProperty<*>): Pair<String, String> =
    Pair(getPropType(prop1), getPropType(prop2))

internal fun getPropType(prop: KProperty<*>): String =
    prop.returnType.toString().replace("?", "")

@PublishedApi
internal fun <A : Any> getClassType(clazz: KClass<A>): String =
    clazz.qualifiedName.toString().replace("?", "")