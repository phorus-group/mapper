package group.phorus.mapper.mapping

/**
 * The mapping fallback. Used when something fails while mapping a [group.phorus.mapper.Field] value.
 */
enum class MappingFallback {
    /**
     * This option will try to set the [field][group.phorus.mapper.Field] to null.
     * If the [field][group.phorus.mapper.Field] is non-nullable, the mapping will be ignored.
     */
    NULL,

    /**
     * This option will ignore the mapping, and let the mapper continue its process.
     */
    CONTINUE,

    /**
     * This option is only used for function mappings. It rethrows any exception thrown inside the function.
     * If no exceptions are thrown, it'll act as [CONTINUE].
     */
    CONTINUE_OR_THROW,

    /**
     * This option is only used for function mappings. It rethrows any exception thrown inside the function.
     * If no exceptions are thrown, it'll act as [NULL].
     */
    NULL_OR_THROW,
}