package group.phorus.mapper.mapping

import group.phorus.mapper.Field

/**
 * Annotation made to be used in object properties.
 *
 * The mapper will take the annotation into account, and try to map the value from one of the [locations].
 *
 * If no [location][locations] could be used, the mapper will use the specified [MappingFallback].
 */
@Target(AnnotationTarget.FIELD)
annotation class MapFrom(

    /**
     * [Field] locations to map from. The first locations will have priority over the next ones.
     */
    val locations: Array<Field>,

    /**
     * The [MappingFallback]. Used after every location failed.
     */
    val fallback: MappingFallback = MappingFallback.CONTINUE
)