package group.phorus.mapper.helper

import group.phorus.mapper.MapFrom
import group.phorus.mapper.enums.MappingFallback
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

internal class TargetClassesTest {

    data class Pet(
        @field:MapFrom(["../name"], MappingFallback.NULL)
        val petName: String,
        val petAge: Int,
        val breed: String?
    )

    data class Person(
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

        assertEquals(typeOf<String>(), result.properties.single { it.name == "name" }.type)
        assertNull(result.properties.single { it.name == "name" }.mapFrom)

        // String classes also have a length property that the mapper can use
        assertEquals(typeOf<Int>(), result.properties.single { it.name == "name" }.properties.single { it.name == "length" }.type)

        assertEquals(typeOf<Int>(), result.properties.single { it.name == "age" }.type)
        assertNull(result.properties.single { it.name == "age" }.mapFrom)

        // Pet asserts
        assertEquals(typeOf<Pet>(), result.properties.single { it.name == "pet" }.type)
        assertNull(result.properties.single { it.name == "pet" }.mapFrom)

        val petNode = result.properties.single { it.name == "pet" }

        assertEquals(3, petNode.properties.size)

        assertEquals(typeOf<String>(), petNode.properties.single { it.name == "petName" }.type)
        assertEquals("../name", petNode.properties.single { it.name == "petName" }.mapFrom?.locations?.first())
        assertEquals(MappingFallback.NULL, petNode.properties.single { it.name == "petName" }.mapFrom?.fallback)

        assertEquals(typeOf<Int>(), petNode.properties.single { it.name == "petAge" }.type)
        assertNull(petNode.properties.single { it.name == "petAge" }.mapFrom)

        assertEquals(typeOf<String?>(), petNode.properties.single { it.name == "breed" }.type)
        assertNull(petNode.properties.single { it.name == "breed" }.mapFrom)
    }
}