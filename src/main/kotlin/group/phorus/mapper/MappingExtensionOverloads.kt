package group.phorus.mapper

import kotlin.reflect.KProperty
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

inline fun <reified T: Any> T.updateFrom(
    entity: Any,
    updateOption: UpdateOption = UpdateOption.IGNORE_NULLS,
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = true,
    mapPrimitives: Boolean = true,
): T? = mapTo(
    originalEntity = OriginalEntity(entity, entity::class.starProjectedType),
    targetType = typeOf<T>(),
    baseEntity = this to updateOption,
    exclusions = exclusions,
    mappings = mappings,
    functionMappings = functionMappings,
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T?

inline fun <reified T: Any> Any.mapToClass(
    exclusions: List<TargetField> = emptyList(),
    mappings: Map<OriginalField, Pair<TargetField, MappingFallback>> = emptyMap(),
    functionMappings: Map<OriginalField?, Pair<MappingFunction, Pair<TargetField, MappingFallback>>> = emptyMap(),
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

inline fun <reified T: Any> Any.mapTo(
    exclusions: List<KProperty<*>> = emptyList(),
    mappings: Map<KProperty<*>, Pair<KProperty<*>, MappingFallback>> = emptyMap(),
    functionMappings: Map<KProperty<*>?, Pair<MappingFunction, Pair<KProperty<*>, MappingFallback>>> = emptyMap(),
    ignoreMapFromAnnotations: Boolean = false,
    useSettersOnly: Boolean = false,
    mapPrimitives: Boolean = true,
): T? = mapTo(
    originalEntity = OriginalEntity(this, this::class.starProjectedType),
    targetType = typeOf<T>(),
    exclusions = exclusions.map { it.name },
    mappings = mappings.map { it.key.name to (it.value.first.name to it.value.second) }.toMap(),
    functionMappings = functionMappings
        .map { it.key?.name to (it.value.first to (it.value.second.first.name to it.value.second.second)) }.toMap(),
    ignoreMapFromAnnotations = ignoreMapFromAnnotations,
    useSettersOnly = useSettersOnly,
    mapPrimitives = mapPrimitives,
) as T?