package group.phorus.mapper.helper

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance

/**
 * Builds an object and sets its properties using a constructor and then setters, or only setters if the option is active
 *
 * @param T type of the class to build
 * @param properties properties to set with their value
 * @param useSettersOnly option to use setters only
 */
inline fun <reified T: Any> buildObject(
    properties: Map<KProperty<*>, Any?>,
    useSettersOnly: Boolean = false,
): T? {
    val (builtObject, unsetProperties) = buildObjectWithConstructor<T>(
        if (useSettersOnly) emptyMap() else properties.map { it.key.name to it.value }.associate { it }
    )

    if (builtObject == null)
        return null

    properties
        .filter { if (useSettersOnly) true else it.key.name in unsetProperties }
        .forEach { prop ->
            val property = prop.key
            if (property !is KMutableProperty<*>)
                return@forEach

            // TODO If the value type and the property type are different, then return, consider collections

            property.setter.call(builtObject, prop.value)
        }

    return builtObject
}

/**
 * Tries to create an object T setting as many parameters as possible with the constructor
 *
 * @param T type of the class to build
 * @param properties properties to use in the object constructor with their value
 * @return the built object and the properties that couldn't be set with the constructor
 */
inline fun <reified T: Any> buildObjectWithConstructor(
    properties: Map<String, Any?> = emptyMap(),
): Pair<T?, List<String>> {

    // Place to save the constructor params and the matched properties and values, or the optional or nullable
    //  constructor params
    var constructorParams = mapOf<KParameter, Pair<String, Any?>?>()

    // Amount of unneeded params in the saved constructor
    var constructorUnneededParams = Integer.MAX_VALUE

    // Chosen constructor, take the first one by default
    var constructor: KFunction<T>? = null

    // Iterate through all the constructors
    T::class.constructors.forEach nextConstructor@ { constr ->

        // Place to save the constructor params and the matched properties and values
        val params = mutableMapOf<KParameter, Pair<String, Any?>?>()
        var unneededParams = 0

        // Iterate through all the parameters of the constructor
        constr.parameters.forEach { param ->

            // Try to find a property with the same name as the parameter
            val prop = properties.asSequence().firstOrNull { it.key == param.name }?.toPair() // TODO: Change to .value

            // If the lookup failed and the constructor param is not optional or nullable, the
            //  constructor cannot be used because of the missing params, so we'll skip to the next constructor
            if (prop == null) {
                if (!param.isOptional && !param.type.isMarkedNullable) {
                    return@nextConstructor
                }
                // If the lookup failed but the param is optional or nullable, we'll increase the count of
                //  unneeded params in this constructor, this will be used to select the constructor with the most
                //  matched params but also with the less amount of unneeded params
                unneededParams++
            }

            // TODO If the property value and the param has different types, consider collections and:
            //  - If the param is nullable or optional, set the value to null, increase unneeded params, and continue
            //  - If the param is not nullable or optional, return

            // If the constructor param matches one property, or the constructor param is optional or nullable, save
            //  it with the property and its value
            params[param] = prop
        }

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
    val builtObject: T? = try {
        val args = constructorParams.map { it.key to it.value?.second }.associate { it }
        constructor?.callBy(args) ?: T::class.createInstance()
    } catch (e: Exception) {
        // If the constructor is null because the object doesn't have one, and the createInstance() fails because
        //  the object doesn't have a no args constructor, return a null object
        null
    }

    // Get the properties that couldn't be set with the constructor
    // Get a list of used param names
    val usedParams = constructorParams.mapNotNull { consParam -> consParam.value?.let { consParam.key.name } }

    // Filter out the used properties
    val nonMatchedProperties = properties.filterNot { it.key in usedParams }.map { it.key }

    return builtObject to nonMatchedProperties
}