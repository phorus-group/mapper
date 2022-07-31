package group.phorus.mapper

import group.phorus.mapper.enums.MappingFallback

@Target(AnnotationTarget.FIELD)
annotation class MapFrom(
    val locations: Array<String>,
    val fallback: MappingFallback = MappingFallback.CONTINUE
)