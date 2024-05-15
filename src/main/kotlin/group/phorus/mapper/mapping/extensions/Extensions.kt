package group.phorus.mapper.mapping.extensions

import group.phorus.mapper.*
import group.phorus.mapper.mapping.MappingFallback
import group.phorus.mapper.mapping.UpdateOption
import group.phorus.mapper.mapping.mapTo
import kotlin.reflect.KProperty
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

/**
 * Reified version of the [group.phorus.mapper.mapping.mapTo] function.
 *
 * Maps one entity to a different [type][T].
 *
 * The function also offers multiple other options to control the way it maps the fields.
 *
 * Example:
 *
 * ```
 * val user = User(name = "John", surname = "Wick", data = Data(password = "password hash"))
 *
 * val userDTO = user.mapToClass<UserDTO>(exclusions = listOf("data/password"))
 * ```
 *
 * @receiver the entity to map.
 * @param T the target class type.
 * @param exclusions a list of [target fields][TargetField] to exclude.
 *
 * @param mappings a map of fields to map forcefully, with the format:
 * [OriginalField] - [TargetField] - [MappingFallback]
 * - [OriginalField] refers to a field in the entity to map.
 * - [TargetField] refers to a field in the target class.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param functionMappings a map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField] - [MappingFunction] - [TargetField] - [MappingFallback]
 * - [OriginalField] refers to a field in the entity to map.
 * - [MappingFunction]: the mutating function used to transform the [OriginalField] value to the preferred [TargetField] value.
 * - [TargetField] refers to a field in the target class.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity. Default: false
 *
 * @param useSettersOnly boolean used to forcefully use only setters or not. If [useSettersOnly] is set to false, the
 *  function will try to set most values through a constructor, and use setters as a last resort. Default: false
 *
 *  Note: if [useSettersOnly] is false, the selected constructor will be the one that matches the most with the
 *   properties that need to be set. For example, if the function needs to set 3 properties, a constructor with
 *   those 3 properties will be preferred over one with 2. Constructors with extra nullable or optional properties will
 *   also be considered and used, but will be less preferred over others with the right amount of parameters.
 *
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 * Default: true
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return the mapped entity with type [T].
 *
 * @see [group.phorus.mapper.mapping.mapTo]
 */
inline fun <reified T: Any> Any.mapToClass(
    exclusions: List<TargetField> = emptyList(),
    mappings: Mappings = emptyMap(),
    functionMappings: FunctionMappings = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
    mapPrimitives: Boolean = true,
): T? = mapTo(
    originalEntity = OriginalEntity(this, this::class.starProjectedType),
    targetType = typeOf<T>(),
    exclusions = exclusions,
    mappings = mappings,
    functionMappings = functionMappings,
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T?

/**
 * A different (and recommended) version of the [mapToClass] function.
 * Accepts [KProperties][KProperty] instead of [fields][Field], this allows IDEs to also refactor the fields used in
 * the function call.
 *
 * Since this method doesn't use [fields][group.phorus.mapper.Field], it doesn't support field locations.
 * If you need them, use [mapToClass].
 *
 * Example:
 *
 * ```
 * val user = User(name = "John", surname = "Wick", password = "password hash")
 *
 * val userDTO = user.mapTo<UserDTO>(exclusions = listOf(UserDTO::password))
 * ```
 *
 * @receiver the entity to map.
 * @param T the target class type.
 * @param exclusions a list of [target fields][KProperty] to exclude.
 *
 * @param mappings a map of fields to map forcefully, with the format:
 * [OriginalField][KProperty] - [TargetField][KProperty] - [MappingFallback]
 * - [OriginalField][KProperty] refers to a field in the entity to map.
 * - [TargetField][KProperty] refers to a field in the target class.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param functionMappings a map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField][KProperty] - [MappingFunction] - [TargetField][KProperty] - [MappingFallback]
 * - [OriginalField][KProperty] refers to a field in the entity to map.
 * - [MappingFunction]: the mutating function used to transform the [OriginalField] value to the preferred [TargetField] value.
 * - [TargetField][KProperty] refers to a field in the target class.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity. Default: false
 *
 * @param useSettersOnly boolean used to forcefully use only setters or not. If [useSettersOnly] is set to false, the
 *  function will try to set most values through a constructor, and use setters as a last resort. Default: false
 *
 *  Note: if [useSettersOnly] is false, the selected constructor will be the one that matches the most with the
 *   properties that need to be set. For example, if the function needs to set 3 properties, a constructor with
 *   those 3 properties will be preferred over one with 2. Constructors with extra nullable or optional properties will
 *   also be considered and used, but will be less preferred over others with the right amount of parameters.
 *
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 * Default: true
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return the mapped entity with type [T].
 *
 * @see [mapToClass]
 */
inline fun <reified T: Any> Any.mapTo(
    exclusions: List<KProperty<*>> = emptyList(),
    mappings: KMappings = emptyMap(),
    functionMappings: KFunctionMappings = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
    mapPrimitives: Boolean = true,
): T? = this.mapToClass(
    exclusions = exclusions.map { it.name },
    mappings = mappings.map { it.key.name to (it.value.first.name to it.value.second) }.toMap(),
    functionMappings = functionMappings
        .map { it.key?.name to (it.value.first to (it.value.second.first.name to it.value.second.second)) }.toMap(),
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T?

/**
 * Updates an entity with other one.
 *
 * The second entity don't need to have the same type as the original entity.
 *
 * Example:
 *
 * ```
 * val person = Person(name = "John", surname = "Wick")
 * val personUpdate = Person(surname = "Lennon")
 *
 * person.updateFrom(personUpdate)
 * println(person)
 * ```
 * *out: Person(name = "John", surname = "Lennon")*
 *
 * @receiver the entity to update.
 * @param T the type of the entity to update.
 * @param entity to update from.
 *
 * @param updateOption can be [UpdateOption.IGNORE_NULLS] or [UpdateOption.SET_NULLS]:
 * - [UpdateOption.IGNORE_NULLS]: any null [field][Field] in the [entity] will be ignored, and only the non-null ones will be
 *   updated in the original entity
 * - [UpdateOption.SET_NULLS]: any null and non-null [field][Field] in the [entity] will be updated in the original entity
 *
 * @param exclusions a list of [target fields][TargetField] to exclude.
 *
 * @param mappings a map of fields to map forcefully, with the format:
 * [OriginalField] - [TargetField] - [MappingFallback]
 * - [OriginalField] refers to a field in the [entity].
 * - [TargetField] refers to a field in the original entity.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param functionMappings a map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField] - [MappingFunction] - [TargetField] - [MappingFallback]
 * - [OriginalField] refers to a field in the [entity].
 * - [MappingFunction]: the mutating function used to transform the [OriginalField] value to the preferred [TargetField] value.
 * - [TargetField] refers to a field in the original entity.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity. Default: false
 *
 * @param useSettersOnly boolean used to forcefully use only setters or not. If [useSettersOnly] is set to false, the
 *  function will try to set most values through a constructor, and use setters as a last resort.
 *  It's recommended to set to false only if you don't need to modify the original entity and the original entity
 *  doesn't have setters for some properties. Default: true
 *
 *  Note: if [useSettersOnly] is false, the selected constructor will be the one that matches the most with the
 *   properties that need to be set. For example, if the function needs to set 3 properties, a constructor with
 *   those 3 properties will be preferred over one with 2. Constructors with extra nullable or optional properties will
 *   also be considered and used, but will be less preferred over others with the right amount of parameters.
 *
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 * Default: true
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return the updated original entity. If [useSettersOnly] = true, it'll be the same instance as the original entity.
 */
inline fun <reified T: Any> T.updateFromObject(
    entity: Any,
    updateOption: UpdateOption = UpdateOption.IGNORE_NULLS,
    exclusions: List<TargetField> = emptyList(),
    mappings: Mappings = emptyMap(),
    functionMappings: FunctionMappings = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = true,
    mapPrimitives: Boolean = true,
): T = mapTo(
    originalEntity = OriginalEntity(entity, entity::class.starProjectedType),
    targetType = typeOf<T>(),
    baseEntity = this to updateOption,
    exclusions = exclusions,
    mappings = mappings,
    functionMappings = functionMappings,
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T

/**
 * A different (and recommended) version of the [updateFromObject] function.
 * Accepts [KProperties][KProperty] instead of [fields][Field], this allows IDEs to also refactor the fields used in
 * the function call.
 *
 * Since this method doesn't use [fields][group.phorus.mapper.Field], it doesn't support field locations.
 * If you need them, use [updateFromObject].
 *
 * The second entity don't need to have the same type as the original entity.
 *
 * Example:
 *
 * ```
 * val person = Person(name = "John", surname = "Wick")
 * val personUpdate = Person(surname = "Lennon")
 *
 * person.updateFrom(personUpdate)
 * println(person)
 * ```
 * *out: Person(name = "John", surname = "Lennon")*
 *
 * @receiver the entity to update.
 * @param T the type of the entity to update.
 * @param entity to update from.
 *
 * @param updateOption can be [UpdateOption.IGNORE_NULLS] or [UpdateOption.SET_NULLS]:
 * - [UpdateOption.IGNORE_NULLS]: any null [field][Field] in the [entity] will be ignored, and only the non-null ones will be
 *   updated in the original entity
 * - [UpdateOption.SET_NULLS]: any null and non-null [field][Field] in the [entity] will be updated in the original entity
 *
 * @param exclusions a list of [target fields][TargetField] to exclude.
 *
 * @param mappings a map of fields to map forcefully, with the format:
 * [OriginalField] - [TargetField] - [MappingFallback]
 * - [OriginalField] refers to a field in the [entity].
 * - [TargetField] refers to a field in the original entity.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param functionMappings a map of fields to map forcefully with a mutating function, with the format:
 * [OriginalField] - [MappingFunction] - [TargetField] - [MappingFallback]
 * - [OriginalField] refers to a field in the [entity].
 * - [MappingFunction]: the mutating function used to transform the [OriginalField] value to the preferred [TargetField] value.
 * - [TargetField] refers to a field in the original entity.
 * - [MappingFallback] is the fallback used in case the mapping fails.
 *
 * @param ignoreMapFromAnnotations boolean used to ignore or not the [@MapFrom][group.phorus.mapper.mapping.MapFrom]
 * annotations in the target entity. Default: false
 *
 * @param useSettersOnly boolean used to forcefully use only setters or not. If [useSettersOnly] is set to false, the
 *  function will try to set most values through a constructor, and use setters as a last resort.
 *  It's recommended to set to false only if you don't need to modify the original entity and the original entity
 *  doesn't have setters for some properties. Default: true
 *
 *  Note: if [useSettersOnly] is false, the selected constructor will be the one that matches the most with the
 *   properties that need to be set. For example, if the function needs to set 3 properties, a constructor with
 *   those 3 properties will be preferred over one with 2. Constructors with extra nullable or optional properties will
 *   also be considered and used, but will be less preferred over others with the right amount of parameters.
 *
 * @param mapPrimitives boolean used to map or not primitives types. If true, [String] will be mapped to [Number]
 * if necessary, and vice versa. Different implementations of [Number] will also be mapped between each other.
 * Default: true
 *
 * The supported [Number] implementations are: [Double], [Float], [Long], [Int], [Short] and [Byte].
 *
 * @return the updated original entity. If [useSettersOnly] = true, it'll be the same instance as the original entity.
 */
inline fun <reified T: Any> T.updateFrom(
    entity: Any,
    updateOption: UpdateOption = UpdateOption.IGNORE_NULLS,
    exclusions: List<KProperty<*>> = emptyList(),
    mappings: KMappings = emptyMap(),
    functionMappings: KFunctionMappings = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = true,
    mapPrimitives: Boolean = true,
): T = mapTo(
    originalEntity = OriginalEntity(entity, entity::class.starProjectedType),
    targetType = typeOf<T>(),
    baseEntity = this to updateOption,
    exclusions = exclusions.map { it.name },
    mappings = mappings.map { it.key.name to (it.value.first.name to it.value.second) }.toMap(),
    functionMappings = functionMappings
        .map { it.key?.name to (it.value.first to (it.value.second.first.name to it.value.second.second)) }.toMap(),
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T