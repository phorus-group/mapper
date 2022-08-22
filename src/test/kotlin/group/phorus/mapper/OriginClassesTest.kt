package group.phorus.mapper

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    private val person = Person(
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

        val result = OriginalEntity(person, typeOf<Person>())

        // Person asserts
        assertEquals(3, result.properties.size)

        assertEquals(typeOf<String>(), result.properties["name"]?.type)
        assertEquals("testPersonName", result.properties["name"]?.value)

        // String classes also have a length property that the mapper can use
        assertEquals(typeOf<Int>(), result.properties["name"]?.properties?.get("length")?.type)
        assertEquals("testPersonName".length, result.properties["name"]?.properties?.get("length")?.value)

        assertEquals(typeOf<Int>(), result.properties["age"]?.type)
        assertEquals(22, result.properties["age"]?.value)

        // Pet asserts
        assertEquals(typeOf<Pet>(), result.properties["pet"]?.type)
        assertEquals(person.pet, result.properties["pet"]?.value)

        val petNode = result.properties["pet"]

        assertEquals(3, petNode?.properties?.size)

        assertEquals(typeOf<String>(), petNode?.properties?.get("petName")?.type)
        assertEquals("testPetName", petNode?.properties?.get("petName")?.value)

        assertEquals(typeOf<Int>(), petNode?.properties?.get("petAge")?.type)
        assertEquals(3, petNode?.properties?.get("petAge")?.value)

        assertEquals(typeOf<String?>(), petNode?.properties?.get("breed")?.type)
        assertNull(petNode?.properties?.get("breed")?.value)
    }

    @Test
    fun `wrap object in OriginEntity and validate bidirectional access`() {
        val result = OriginalEntity(person, typeOf<Person>())

        // Get the parent class of the name field = Person
        assertEquals(person, result.properties["name"]?.parent?.value)

        // Get the pet node
        val petNode = result.properties["pet"]

        // Get the parent class of the breed field in the pet node = Pet
        assertEquals(person.pet, petNode?.properties?.get("breed")?.parent?.value)

        // Get the parent class (Person) of the parent class (Pet) of the breed field in the pet node = Person
        assertEquals(person, petNode?.properties?.get("breed")?.parent?.parent?.value)

        // Get the person node
        val personNode = petNode?.properties?.get("breed")?.parent?.parent

        // Get the parent class (Pet) of the parent class (Person) of the pet field in the person node = Pet
        assertEquals(person.pet, personNode!!.properties["pet"]?.properties?.get("breed")?.parent?.value)
    }

    @Test
    fun `find property`() {
        val location = listOf("pet", "petName")

        val result = OriginalEntity(person, typeOf<Person>()).findProperty(location)

        assertNotNull(result)

        assertEquals(typeOf<String>(), result.type)
        assertEquals("testPetName", result.value)
    }
}