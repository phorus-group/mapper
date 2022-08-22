package group.phorus.mapper

@Target(AnnotationTarget.FIELD)
annotation class MapFrom(
    val locations: Array<String>,
    val fallback: MappingFallback = MappingFallback.CONTINUE
)