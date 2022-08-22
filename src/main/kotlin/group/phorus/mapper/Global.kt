package group.phorus.mapper

typealias Field = String
typealias OriginalField = Field
typealias TargetField = Field
typealias MappingFunction = Function<*>
typealias Value = Any

data class PropertyWrapper<T>(val value: T)