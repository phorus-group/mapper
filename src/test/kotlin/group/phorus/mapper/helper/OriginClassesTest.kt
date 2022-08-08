package group.phorus.mapper.helper

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

internal class OriginClassesTest {

    @Test
    fun `wrap class in OriginEntity`() {
        data class Pet(
            val name: String,
            val age: Int,
            val breed: String?
        )

        data class Person(
            val name: String,
            val age: Int,
            val pet: Pet,
        )

        val person = Person(
            name = "testPersonName",
            age = 22,
            pet = Pet(
                name = "testPetName",
                age = 3,
                breed = null
            )
        )

        val result = OriginEntity(person)

        // Person asserts
        assertEquals(3, result.properties.size)

        assertEquals(typeOf<String>(), result.properties.single { it.name == "name" }.type)
        assertEquals("testPersonName", result.properties.single { it.name == "name" }.value)

        assertEquals(typeOf<Int>(), result.properties.single { it.name == "age" }.type)
        assertEquals(22, result.properties.single { it.name == "age" }.value)

        // Pet asserts
        assertEquals(typeOf<Pet>(), result.properties.single { it.name == "pet" }.type)
        assertEquals(person.pet, result.properties.single { it.name == "pet" }.value)

        val petNode = result.properties.single { it.name == "pet" }

        assertEquals(3, petNode.properties.size)

        assertEquals(typeOf<String>(), petNode.properties.single { it.name == "name" }.type)
        assertEquals("testPetName", petNode.properties.single { it.name == "name" }.value)

        assertEquals(typeOf<Int>(), petNode.properties.single { it.name == "age" }.type)
        assertEquals(3, petNode.properties.single { it.name == "age" }.value)

        assertEquals(typeOf<String?>(), petNode.properties.single { it.name == "breed" }.type)
        assertNull(petNode.properties.single { it.name == "breed" }.value)
    }
}