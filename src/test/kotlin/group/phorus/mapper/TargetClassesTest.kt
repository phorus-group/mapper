package group.phorus.mapper

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Utility function to use TargetClass with a reified class type
 */
inline fun <reified T: Any> targetClass() = TargetClass(T::class, T::class.starProjectedType)

internal class TargetClassesTest {

    private data class Pet(
        @field:MapFrom(["../name"], MappingFallback.NULL)
        val petName: String,
        val petAge: Int,
        val breed: String?
    )

    private data class Person(
        val name: String,
        val age: Int,
        val pet: Pet,
    )

    @Test
    fun `wrap class in TargetClass`() {
        val result = targetClass<Person>()

        println(result)

        // Person asserts
        assertEquals(3, result.properties.size)

        assertEquals(typeOf<String>(), result.properties["name"]?.type)
        assertNull(result.properties["name"]?.mapFrom)

        // String classes also have a length property that the mapper can use
        assertEquals(typeOf<Int>(), result.properties["name"]?.properties?.get("length")?.type)

        assertEquals(typeOf<Int>(), result.properties["age"]?.type)
        assertNull(result.properties["age"]?.mapFrom)

        // Pet asserts
        assertEquals(typeOf<Pet>(), result.properties["pet"]?.type)
        assertNull(result.properties["pet"]?.mapFrom)

        val petNode = result.properties["pet"]

        assertEquals(3, petNode?.properties?.size)

        assertEquals(typeOf<String>(), petNode?.properties?.get("petName")?.type)
        assertEquals("../name", petNode?.properties?.get("petName")?.mapFrom?.locations?.first())
        assertEquals(MappingFallback.NULL, petNode?.properties?.get("petName")?.mapFrom?.fallback)

        assertEquals(typeOf<Int>(), petNode?.properties?.get("petAge")?.type)
        assertNull(petNode?.properties?.get("petAge")?.mapFrom)

        assertEquals(typeOf<String?>(), petNode?.properties?.get("breed")?.type)
        assertNull(petNode?.properties?.get("breed")?.mapFrom)
    }

    @Test
    fun `find property`() {
        val location = listOf("pet", "petName")

        val result = targetClass<Person>().findProperty(location)

        assertNotNull(result)

        assertEquals(typeOf<String>(), result.type)
    }
}