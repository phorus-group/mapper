package group.phorus.mapper.helper

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

internal class OriginClassesTest {

    data class Pet(
        val petName: String,
        val petAge: Int,
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
            petName = "testPetName",
            petAge = 3,
            breed = null
        )
    )

    @Test
    fun `wrap object in OriginEntity`() {

        val result = OriginEntity(person)

        // Person asserts
        assertEquals(3, result.properties.size)

        assertEquals(typeOf<String>(), result.properties.single { it.name == "name" }.type)
        assertEquals("testPersonName", result.properties.single { it.name == "name" }.value)

        // String classes also have a length property that the mapper can use
        assertEquals(typeOf<Int>(), result.properties.single { it.name == "name" }.properties.single { it.name == "length" }.type)
        assertEquals("testPersonName".length, result.properties.single { it.name == "name" }.properties.single { it.name == "length" }.value)

        assertEquals(typeOf<Int>(), result.properties.single { it.name == "age" }.type)
        assertEquals(22, result.properties.single { it.name == "age" }.value)

        // Pet asserts
        assertEquals(typeOf<Pet>(), result.properties.single { it.name == "pet" }.type)
        assertEquals(person.pet, result.properties.single { it.name == "pet" }.value)

        val petNode = result.properties.single { it.name == "pet" }

        assertEquals(3, petNode.properties.size)

        assertEquals(typeOf<String>(), petNode.properties.single { it.name == "petName" }.type)
        assertEquals("testPetName", petNode.properties.single { it.name == "petName" }.value)

        assertEquals(typeOf<Int>(), petNode.properties.single { it.name == "petAge" }.type)
        assertEquals(3, petNode.properties.single { it.name == "petAge" }.value)

        assertEquals(typeOf<String?>(), petNode.properties.single { it.name == "breed" }.type)
        assertNull(petNode.properties.single { it.name == "breed" }.value)
    }

    @Test
    fun `wrap object in OriginEntity and validate bidirectional access`() {
        val result = OriginEntity(person)

        // Get the parent class of the name field = Person
        assertEquals(person, result.properties.single { it.name == "name" }.parent.value)

        // Get the pet node
        val petNode = result.properties.single { it.name == "pet" }

        // Get the parent class of the breed field in the pet node = Pet
        assertEquals(person.pet, petNode.properties.single { it.name == "breed" }.parent.value)

        // Get the parent class (Person) of the parent class (Pet) of the breed field in the pet node = Person
        assertEquals(person, petNode.properties.single { it.name == "breed" }.parent.parent?.value)

        // Get the person node
        val personNode = petNode.properties.single { it.name == "breed" }.parent.parent

        // Get the parent class (Pet) of the parent class (Person) of the pet field in the person node = Pet
        assertEquals(person.pet, personNode!!.properties.single { it.name == "pet" }.properties.single { it.name == "breed" }.parent.value)
    }
}