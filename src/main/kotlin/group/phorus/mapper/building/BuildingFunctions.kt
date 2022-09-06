package group.phorus.mapper.building

import group.phorus.mapper.Field
import group.phorus.mapper.OriginalEntity
import group.phorus.mapper.Value
import group.phorus.mapper.Wrapper
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

/**
 * Reified version of the [buildOrUpdateInternal] function.
 * @see [buildOrUpdateInternal].
 */
inline fun <reified T: Any> buildOrUpdate(
    properties: Map<Field, Value?>,
    useSettersOnly: Boolean = false,
    entity: T? = null,
): T? = buildOrUpdateInternal(
    type = typeOf<T>(),
    properties = properties,
    useSettersOnly = useSettersOnly,
    entity = entity,
) as T?

/**
 * Builds a new entity based on other one, but with the option of adding extra properties.
 *
 * @param type of the class to build.
 * @param properties to set with their value.
 * @param entity the entity to build the new one from.
 * @return the built entity.
 */
internal fun buildWithEntity(
    type: KType,
    properties: Map<Field, Value?>,
    entity: Any,
): Any? = buildOrUpdateInternal(
    type = type,
    properties = OriginalEntity(entity, type).properties.map { it.key to it.value.value }.toMap() + properties,
    useSettersOnly = false,
)

/**
 * Build or update an entity setting its properties through a constructor and setters.
 *
 * If some properties couldn't be set using a constructor, they'll be set through setters if possible.
 *
 * *This function doesn't map anything, the property values must be of the right type, if not the property will be ignored.*
 *
 * @param type the entity type to build.
 * @param properties to set with their value.
 * @param useSettersOnly option to use setters only, not needed if an entity is used.
 * @param entity if specified, the entity will be updated using setters instead of creating a new one.
 * @return the built or updated entity, or null.
 */
fun buildOrUpdateInternal(
    type: KType,
    properties: Map<Field, Value?>,
    useSettersOnly: Boolean = false,
    entity: Any? = null,
): Any? {
    // Build a new object only if base class is null
    val (builtObject, unsetProperties) = if (entity == null) {
        // If settersOnly is true, don't set any property through a constructor and treat all property as an unset
        if (useSettersOnly) {
            buildWithConstructorInternal(type).first to properties.keys
        } else {
            buildWithConstructorInternal(type, properties)
        }
    } else {
        // If base class is present, then use it and treat all properties as an unset
        entity to properties.keys
    }

    if (builtObject == null)
        return null

    // Get class KProperties
    val kProperties = (type.classifier as KClass<*>).memberProperties
        .filter { it.name in unsetProperties } // Only include unset properties
        .associateWith { properties[it.name] } // Get the properties desired value

    kProperties.forEach { prop ->
        val property = prop.key

        // If the property already have the desired value, only happens if a based class is being used
        if (property.getter.call(builtObject) == prop.value || property.getter.call(builtObject) === prop.value)
            return@forEach

        // If the property is not mutable it doesn't have any setters, return
        if (property !is KMutableProperty<*>)
            return@forEach

        if (prop.value == null) {
            // If the prop value is null and the setter is not nullable, return, if not, use the setter
            if (!property.returnType.isMarkedNullable) {
                return@forEach
            } else {
                property.setter.call(builtObject, prop.value)
                return@forEach
            }
        }

        runCatching { property.setter.call(builtObject, prop.value) }
    }

    return builtObject
}

/**
 * Reified version of the [buildWithConstructorInternal] function.
 * @see [buildWithConstructorInternal].
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> buildWithConstructor(
    properties: Map<Field, Value?> = emptyMap(),
): Pair<T?, List<String>> = buildWithConstructorInternal(
    type = typeOf<T>(),
    properties = properties,
) as Pair<T?, List<String>>

/**
 * Creates an entity setting as many parameters as possible with a constructor.
 *
 * *This function doesn't map anything, the property values must be of the right type, if not the property will be ignored.*
 *
 * @param type type of the class to build.
 * @param properties to set with their value.
 * @return the built object and the properties that couldn't be set with a constructor.
 */
fun buildWithConstructorInternal(
    type: KType,
    properties: Map<Field, Value?> = emptyMap(),
): Pair<Any?, List<String>> {

    // Place to save the constructor params and the matched properties and values, or the optional or nullable
    //  constructor params
    // We wrap the values to be able to differentiate if the value was set to null explicitly or not
    var constructorParams = mapOf<KParameter, Wrapper<Value?>?>()

    // Amount of unneeded params in the saved constructor
    var constructorUnneededParams = Integer.MAX_VALUE

    // Chosen constructor, take the first one by default
    var constructor: KFunction<Any>? = null

    // Iterate through all the constructors
    (type.classifier as KClass<*>).constructors.forEach nextConstructor@ { constr ->

        // Place to save the constructor params and the matched properties and values
        val params = mutableMapOf<KParameter, Wrapper<Value?>?>()
        var unneededParams = 0

        // Iterate through all the parameters of the constructor
        constr.parameters.forEach { param ->

            // Try to find a property with the same name as the parameter
            val prop = properties.asSequence().firstOrNull { it.key == param.name }

            // If the lookup failed and the constructor param is not optional or nullable, the
            //  constructor cannot be used because of the missing params, so we'll skip to the next constructor
            if (prop == null) {
                if (!param.isOptional && !param.type.isMarkedNullable)
                    return@nextConstructor

                // If the lookup failed but the param is optional or nullable, we'll increase the count of
                //  unneeded params in this constructor, this will be used to select the constructor with the most
                //  matched params but also with the less amount of unneeded params
                unneededParams++

                // If the param is nullable, save null as the value
                if (param.type.isMarkedNullable)
                    params[param] = null

            } else if (prop.value == null) {
                // If the lookup worked but the property value is null, that means the user is trying to explicitly
                //  set the property to null

                // If the constructor param is nor nullable or optional, we have a property that wants to set a not
                //  nullable param to null, and that param cannot be skipped, so we'll skip the constructor
                //  since it cannot be used
                if (!param.type.isMarkedNullable && !param.isOptional)
                    return@nextConstructor

                // If the param is nullable, save the property and its null value
                if (param.type.isMarkedNullable) {
                    params[param] = Wrapper(prop.value)
                } else {
                    // If the param is not nullable, but it's optional, then we are not going to use the null value
                    //  at all, so we should increase the unneededParams count
                    unneededParams++
                }
            } else {
                // Save the property and its value if the param and prop have the same types
                if ((param.type.classifier as KClass<*>).starProjectedType.isSupertypeOf(prop.value!!::class.starProjectedType))
                    params[param] = Wrapper(prop.value)
            }
        }

        // Count the amount of params matched with properties, we include the ones with value == null since they're
        //  explicitly set
        val paramsMatched = params.values.count { it != null }
        val savedParamsMatched = constructorParams.values.count { it != null }
        if ((paramsMatched == savedParamsMatched && unneededParams <= constructorUnneededParams)
            || paramsMatched > savedParamsMatched) {
            // If the matched params are the same in this constructor and the saved one, choose the one with less
            //  unneeded params
            // If not, save the constructor and the params if there are more property matches than the one currently saved
            constructorParams = params
            constructorUnneededParams = unneededParams
            constructor = constr
        }
    }

    // Remove the optional params, we want to use the defaults instead of setting them to null
    // Don't remove the matched params or the params that are not optional, if a param is not optional
    //  and the value is null that means the param is nullable
    constructorParams = constructorParams.filter { it.value != null || !it.key.isOptional }

    // Create the object using the saved constructor and matched params
    val builtObject = runCatching {
        val args = constructorParams.map { it.key to it.value?.value }.associate { it }
        constructor?.callBy(args) ?: (type.classifier as KClass<*>).createInstance()
    }.getOrNull()
    // If the constructor is null because the object doesn't have one, and the createInstance() fails because
    //  the object doesn't have a no args constructor, return a null object

    // Get the properties that couldn't be set with the constructor
    // Get a list of used param names
    val usedParams = constructorParams.mapNotNull { consParam -> consParam.value?.let { consParam.key.name } }

    // Filter out the used properties
    val nonMatchedProperties = properties.filterNot { it.key in usedParams }.map { it.key }

    return builtObject to nonMatchedProperties
}