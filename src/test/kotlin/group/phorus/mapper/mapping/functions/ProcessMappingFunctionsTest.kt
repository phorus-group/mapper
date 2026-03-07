package group.phorus.mapper.mapping.functions

import group.phorus.mapper.OriginalEntity
import group.phorus.mapper.targetClass
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

@Suppress("UNUSED")
internal class ProcessMappingFunctionsTest {

    @Test
    fun `parse a location string`() {
        val locationString = "./../pet/./name/./"

        val location = parseLocation(locationString)

        assertEquals(3, location.size)
        assertEquals("..", location[0])
        assertEquals("pet", location[1])
        assertEquals("name", location[2])
    }


    private class MappingTestClasses {
        class Person(
            val name: String,
            val age: Int,
        )

        class PersonNullable(
            val name: String?,
            val age: Int,
        )

        class PersonDTO(
            val nameStr: String,
            val ageTmp: Int,
        )

        class PersonDTONullable(
            val nameStr: String?,
            val ageTmp: Int,
        )

        class Pet(
            val name: String,
            val breed: Breed,
        )

        open class Breed(
            val breedName: String,
        )

        class PetDTO(
            val name: String,
            val breedDTO: BreedDTO,
        )

        class BreedDTO(
            val breedName: String,
        )
    }

    @Test
    fun `process mappings`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to (null to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // Result contains a map of the target fields and the mapped values
        assertEquals("testName", result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings excluding a target field`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to (null to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = listOf("nameStr"),
        )

        assertEquals(1, result.size)

        // Result shouldn't contain the nameStr mapping, since any mappings with an excluded origin or
        //  target field will be ignored
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a non-existent target field`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to (null to ("nonExistentField" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result shouldn't contain the nameStr mapping, if the target field doesn't exist the mapping will be ignored
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a non-existent original field and fallback null`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "nonExistentField" to (null to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // Result should contain the nameStr mapping with null value, since we used the fallback null and the target
        //  field is nullable
        assertTrue(result.containsKey("nameStr"))
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a null original field value and a nullable target field`() {
        val person = MappingTestClasses.PersonNullable(name = null, age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "name" to (null to ("nameStr" to ProcessMappingFallback.SKIP)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a null original field value and a non-nullable target field`() {
        val person = MappingTestClasses.PersonNullable(name = null, age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to (null to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null  to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a non-existent original field and fallback null, but the target field is non-nullable`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "nonExistentField" to (null to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result shouldn't contain the nameStr mapping with null value, since the target field is not nullable anyway
        assertFalse(result.containsKey("nameStr"))
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a non-existent original field and fallback continue`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "nonExistentField" to (null to ("nameStr" to ProcessMappingFallback.SKIP)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result shouldn't contain the nameStr mapping with null value, since we used the fallback continue
        assertFalse(result.containsKey("nameStr"))
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process mappings with a composite original field`() {
        val pet = MappingTestClasses.Pet(
            name = "testName",
            breed = MappingTestClasses.Breed(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<MappingTestClasses.Pet>()),
            targetClass = targetClass<MappingTestClasses.BreedDTO>(),
            mappings = mapOf(
                "breed/breedName" to (null to ("breedName" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breedName mapping
        assertEquals("testBreed", result["breedName"])
    }

    @Test
    fun `process mappings with a composite target field`() {
        val breed = MappingTestClasses.Breed(breedName = "testBreed")

        val result = processMappings(
            originalEntity = OriginalEntity(breed, typeOf<MappingTestClasses.Breed>()),
            targetClass = targetClass<MappingTestClasses.PetDTO>(),
            mappings = mapOf(
                "breedName" to (null to ("breedDTO/breedName" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breedName mapping
        assertEquals("testBreed", result["breedDTO/breedName"])
    }

    @Test
    fun `process mappings with a composite target field excluding the parent of the target class`() {
        val breed = MappingTestClasses.Breed(breedName = "testBreed")

        val result = processMappings(
            originalEntity = OriginalEntity(breed, typeOf<MappingTestClasses.Breed>()),
            targetClass = targetClass<MappingTestClasses.PetDTO>(),
            mappings = mapOf(
                "breedName" to (null to ("breedDTO/breedName" to ProcessMappingFallback.NULL)),
            ),
            exclusions = listOf("breedDTO"),
        )

        // Since the entire "breedDTO" target field was excluded, any mappings inside it were excluded as well
        assertEquals(0, result.size)
    }

    @Test
    fun `process mappings of object with different types`() {
        val pet = MappingTestClasses.Pet(
            name = "testName",
            breed = MappingTestClasses.Breed(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<MappingTestClasses.Pet>()),
            targetClass = targetClass<MappingTestClasses.PetDTO>(),
            mappings = mapOf(
                "breed" to (null to ("breedDTO" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breedDTO mapping
        assertTrue(result["breedDTO"] is MappingTestClasses.BreedDTO)

        // The breedDTO object should be mapped with the mapTo function
        assertEquals("testBreed", (result["breedDTO"] as MappingTestClasses.BreedDTO).breedName)
    }

    @Test
    fun `process normal mappings but the target field type is a superclass of the original field`() {
        class BreedChild(breedName: String) : MappingTestClasses.Breed(breedName)
        class Pet2(
            val name: String,
            val breed: BreedChild,
        )

        val pet = Pet2(
            name = "testName",
            breed = BreedChild(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<Pet2>()),
            targetClass = targetClass<MappingTestClasses.Pet>(),
            mappings = mapOf(
                "breed" to (null to ("breed" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )
        
        assertEquals(1, result.size)

        // Result should contain the breed mapping
        assertTrue(result["breed"] is MappingTestClasses.Breed)
        
        // The breed object should be mapped with the mapTo function, since the target field is a superclass of the
        //  original field, thus the original object can be assigned to the target field
        assertEquals("testBreed", (result["breed"] as MappingTestClasses.Breed).breedName)
    }

    @Test
    fun `process function mappings`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to ({ str: String -> str.uppercase() } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // The result contains the return value of the function after using the original prop value as the input
        assertEquals("TESTNAME", result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings with an exception inside the function`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val function: (String) -> String = { throw Exception("fail") }

        assertThrows<Exception> {
            processMappings(
                originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
                targetClass = targetClass<MappingTestClasses.PersonDTO>(),
                mappings = mapOf(
                    "name" to (function to ("nameStr" to ProcessMappingFallback.NULL_OR_THROW)),
                    "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
                ),
                exclusions = emptyList(),
            )
        }
    }

    @Test
    fun `process function mappings catching an exception inside the function`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val function: (String) -> String = { throw Exception("fail") }

        assertDoesNotThrow {
            processMappings(
                originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
                targetClass = targetClass<MappingTestClasses.PersonDTO>(),
                mappings = mapOf(
                    "name" to (function to ("nameStr" to ProcessMappingFallback.NULL)),
                    "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
                ),
                exclusions = emptyList(),
            )
        }
    }

    // If you want to use a function with default values, the function needs to be outside any other function
    // Kotlin limitations
    fun function(str: String = "input was null") = str.uppercase()

    @Test
    fun `process function mappings where the input is optional and the original field is null`() {
        val person = MappingTestClasses.PersonNullable(name = null, age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to (::function to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // The result contains the return value of the function after using the original prop value as the input
        // Since the original prop value is null, the default param value was used
        assertEquals("INPUT WAS NULL", result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the input is nullable and the original field is null`() {
        val person = MappingTestClasses.PersonNullable(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                null to ({ str: String? -> str?.uppercase() ?: "input was null" } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // The result contains the return value of the function after using the original prop value as the input
        // Since the original prop is null, the default param value was used
        assertEquals("input was null", result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the input is nullable and the original field value is null`() {
        val person = MappingTestClasses.PersonNullable(name = null, age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to ({ str: String? -> str?.uppercase() ?: "input was null" } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // The result contains the return value of the function after using the original prop value as the input
        // Since the original prop value is null, the default param value was used
        assertEquals("input was null", result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings with a function without params and a null original field`() {
        val person = MappingTestClasses.PersonNullable(name = null, age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                null to ({ "no params function" } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // The result contains the return value of the function after using the original prop value as the input
        // Since the original prop value is null, the default param value was used
        assertEquals("no params function", result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the input is not optional or nullable and the original field is null`() {
        val person = MappingTestClasses.PersonNullable(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                null to ({ str: String -> str.uppercase() } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // Since the mapping fallback is null, the result contains null
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the input is not optional or nullable and the original field value is null`() {
        val person = MappingTestClasses.PersonNullable(name = null, age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.PersonNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "name" to ({ str: String -> str.uppercase() } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // Since the mapping fallback is null, the result contains null
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings with the wrong input type`() {
        val pet = MappingTestClasses.Pet(
            name = "testName",
            breed = MappingTestClasses.Breed(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<MappingTestClasses.Pet>()),
            targetClass = targetClass<MappingTestClasses.PetDTO>(),
            mappings = mapOf(
                "breed" to ({ breedDTO: MappingTestClasses.BreedDTO -> breedDTO } to ("breedDTO" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breedDTO mapping
        assertTrue(result["breedDTO"] is MappingTestClasses.BreedDTO)

        // The breedDTO object should be mapped with the mapTo function
        assertEquals("testBreed", (result["breedDTO"] as MappingTestClasses.BreedDTO).breedName)
    }

    @Test
    fun `process function mappings with the wrong return type`() {
        val pet = MappingTestClasses.Pet(
            name = "testName",
            breed = MappingTestClasses.Breed(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<MappingTestClasses.PetDTO>()),
            targetClass = targetClass<MappingTestClasses.PetDTO>(),
            mappings = mapOf(
                "breed" to ({ breed: MappingTestClasses.Breed -> breed } to ("breedDTO" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breedDTO mapping
        assertTrue(result["breedDTO"] is MappingTestClasses.BreedDTO)

        // The breedDTO object should be mapped with the mapTo function
        assertEquals("testBreed", (result["breedDTO"] as MappingTestClasses.BreedDTO).breedName)
    }

    @Test
    fun `process function mappings but the input field is a superclass of the original field`() {
        class BreedChild(breedName: String) : MappingTestClasses.Breed(breedName)
        class Pet2(
            val name: String,
            val breed: BreedChild,
        )

        val pet = Pet2(
            name = "testName",
            breed = BreedChild(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<Pet2>()),
            targetClass = targetClass<MappingTestClasses.Pet>(),
            mappings = mapOf(
                "breed" to ({ br: MappingTestClasses.Breed ->
                    MappingTestClasses.Breed(breedName = br.breedName.uppercase())
                } to ("breed" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breed mapping
        assertTrue(result["breed"] is MappingTestClasses.Breed)

        // The breed object should present, since the function input is a superclass of the original field, thus
        //  the original object can be used to run the function
        assertEquals("TESTBREED", (result["breed"] as MappingTestClasses.Breed).breedName)
    }

    @Test
    fun `process function mappings but the target field is a superclass of the return value`() {
        class BreedChild(breedName: String) : MappingTestClasses.Breed(breedName)
        class Pet2(
            val name: String,
            val breed: BreedChild,
        )

        val pet = Pet2(
            name = "testName",
            breed = BreedChild(breedName = "testBreed"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(pet, typeOf<Pet2>()),
            targetClass = targetClass<MappingTestClasses.Pet>(),
            mappings = mapOf(
                "breed" to ({ br: BreedChild ->
                    BreedChild(breedName = br.breedName.uppercase())
                } to ("breed" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result should contain the breed mapping
        assertTrue(result["breed"] is MappingTestClasses.Breed)

        // The breed object should present, since the target field is a superclass of the function return value, thus
        //  the returned value can be assigned to the target field
        assertEquals("TESTBREED", (result["breed"] as MappingTestClasses.Breed).breedName)
    }

    @Test
    fun `process function mappings where the returned value is null and the target field is nullable`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "name" to ({ null } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the returned value is null and the target field is not nullable`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to ({ null } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Since the mapping fallback is null, the result doesn't contain nameStr
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the function throws an exception, and the fallback is null`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "name" to ({ throw Exception() } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(2, result.size)

        // Since the mapping fallback is null, the result doesn't contain nameStr
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the function throws an exception, and the fallback is null, but the field is not nullable`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to ({ throw Exception() } to ("nameStr" to ProcessMappingFallback.NULL)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)
        assertEquals(7, result["ageTmp"])
    }

    @Test
    fun `process function mappings where the function throws an exception, and the fallback is continue`() {
        val person = MappingTestClasses.Person(name = "testName", age = 7)

        val result = processMappings(
            originalEntity = OriginalEntity(person, typeOf<MappingTestClasses.Person>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "name" to ({ throw Exception() } to ("nameStr" to ProcessMappingFallback.SKIP)),
                "age" to (null to ("ageTmp" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)

        // Result shouldn't contain the nameStr mapping with null value since we are using fallback continue
        assertFalse(result.containsKey("nameStr"))
        assertNull(result["nameStr"])
        assertEquals(7, result["ageTmp"])
    }

    private class FunctionMappingTestClasses {
        class Source(
            val name: String,
            val breed: Breed,
            val tags: List<String>,
        )

        class SourceNullable(
            val name: String?,
            val breed: Breed?,
        )

        class Breed(
            val breedName: String,
        )

        class BreedDTO(
            val breedName: String,
        )

        class Target(
            val name: String,
            val breedDTO: BreedDTO,
        )

        class TargetNullable(
            val name: String?,
            val breedDTO: BreedDTO?,
        )

        class TagHolder(
            val tags: List<String>,
        )
    }

    @Test
    fun `process function mappings with a lambda that transforms a string`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a", "b"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to ({ str: String -> str.uppercase() } to ("nameStr" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        // The result contains the return value of the function after using the original prop value as the input
        assertEquals("TESTNAME", result["nameStr"])
    }

    @Test
    fun `process function mappings with a lambda that requires type mapping of input`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a", "b"),
        )

        // The lambda expects BreedDTO but the original field is Breed, it should be mapped automatically
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.Target>(),
            mappings = mapOf(
                "breed" to ({ dto: FunctionMappingTestClasses.BreedDTO -> dto } to ("breedDTO" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)
        assertTrue(result["breedDTO"] is FunctionMappingTestClasses.BreedDTO)
        assertEquals("testBreed", (result["breedDTO"] as FunctionMappingTestClasses.BreedDTO).breedName)
    }

    @Test
    fun `process function mappings with a no-param lambda and null original field`() {
        val source = FunctionMappingTestClasses.SourceNullable(name = null, breed = null)

        // The function has no params and the original field is null, it should be invoked without params
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.SourceNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                null to ({ "constant value" } to ("nameStr" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals("constant value", result["nameStr"])
    }

    @Test
    fun `process function mappings with a nullable-param lambda and null original value`() {
        val source = FunctionMappingTestClasses.SourceNullable(name = null, breed = null)

        // The function param is nullable, so it should receive null when the original value is null
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.SourceNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTO>(),
            mappings = mapOf(
                "name" to ({ str: String? -> str?.uppercase() ?: "was null" } to ("nameStr" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals("was null", result["nameStr"])
    }

    @Test
    fun `process function mappings where the function throws an exception, and the fallback is continue or throw`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        // The function throws an exception, and the fallback is continue or throw, so the exception should be rethrown
        assertThrows<RuntimeException> {
            processMappings(
                originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
                targetClass = targetClass<FunctionMappingTestClasses.Target>(),
                mappings = mapOf(
                    "breed" to ({ _: FunctionMappingTestClasses.BreedDTO -> throw RuntimeException("test error") } to ("breedDTO" to ProcessMappingFallback.CONTINUE_OR_THROW)),
                ),
                exclusions = emptyList(),
            )
        }
    }

    @Test
    fun `process function mappings where the function throws an exception with the wrong input type, and the fallback is skip`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        // The function throws an exception, and the fallback is skip, so the mapping should be skipped
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.TargetNullable>(),
            mappings = mapOf(
                "breed" to ({ _: FunctionMappingTestClasses.BreedDTO -> throw RuntimeException("fail") } to ("breedDTO" to ProcessMappingFallback.SKIP)),
            ),
            exclusions = emptyList(),
        )

        assertFalse(result.containsKey("breedDTO"))
    }

    @Test
    fun `process function mappings where the function throws an exception with the wrong input type, and the fallback is null`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        val function: (FunctionMappingTestClasses.BreedDTO) -> FunctionMappingTestClasses.BreedDTO = { throw RuntimeException("fail") }

        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.TargetNullable>(),
            mappings = mapOf(
                "breed" to (function to ("breedDTO" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        // Since the mapping fallback is null and the target is nullable, the result contains null
        assertTrue(result.containsKey("breedDTO"))
        assertNull(result["breedDTO"])
    }

    @Test
    fun `process function mappings with a lambda that transforms a list of strings`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a", "b", "c"),
        )

        // The function transforms a list of strings, the collection type should be preserved
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.TagHolder>(),
            mappings = mapOf(
                "tags" to ({ tags: List<String> -> tags.map { it.uppercase() } } to ("tags" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)
        @Suppress("UNCHECKED_CAST")
        val tags = result["tags"] as List<String>
        assertEquals(listOf("A", "B", "C"), tags)
    }

    @Test
    fun `process function mappings with a lambda returning a different type than the target`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        // The function returns Breed but the target expects BreedDTO, the return value should be mapped
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.Target>(),
            mappings = mapOf(
                "breed" to ({ breed: FunctionMappingTestClasses.Breed -> breed } to ("breedDTO" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertEquals(1, result.size)
        assertTrue(result["breedDTO"] is FunctionMappingTestClasses.BreedDTO)
        assertEquals("testBreed", (result["breedDTO"] as FunctionMappingTestClasses.BreedDTO).breedName)
    }

    @Test
    fun `process function mappings with a non-nullable param lambda and null original value`() {
        val source = FunctionMappingTestClasses.SourceNullable(name = null, breed = null)

        // The function param is non-nullable but the original field value is null
        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.SourceNullable>()),
            targetClass = targetClass<MappingTestClasses.PersonDTONullable>(),
            mappings = mapOf(
                "name" to ({ str: String -> str.uppercase() } to ("nameStr" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        // Since the mapping fallback is null, the result contains null
        assertTrue(result.containsKey("nameStr"))
        assertNull(result["nameStr"])
    }

    @Test
    fun `process function mappings where the function throws an exception, and the fallback is null or throw`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        val function: (String) -> String = { throw IllegalStateException("test") }

        assertThrows<IllegalStateException> {
            processMappings(
                originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
                targetClass = targetClass<MappingTestClasses.PersonDTO>(),
                mappings = mapOf(
                    "name" to (function to ("nameStr" to ProcessMappingFallback.NULL_OR_THROW)),
                ),
                exclusions = emptyList(),
            )
        }
    }

    @Test
    fun `process function mappings with a lambda returning null and a nullable target field`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.TargetNullable>(),
            mappings = mapOf(
                "name" to ({ _: String -> null } to ("name" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        assertTrue(result.containsKey("name"))
        assertNull(result["name"])
    }

    @Test
    fun `process function mappings with a lambda returning null and a non-nullable target field`() {
        val source = FunctionMappingTestClasses.Source(
            name = "testName",
            breed = FunctionMappingTestClasses.Breed(breedName = "testBreed"),
            tags = listOf("a"),
        )

        val result = processMappings(
            originalEntity = OriginalEntity(source, typeOf<FunctionMappingTestClasses.Source>()),
            targetClass = targetClass<FunctionMappingTestClasses.Target>(),
            mappings = mapOf(
                "name" to ({ _: String -> null } to ("name" to ProcessMappingFallback.NULL)),
            ),
            exclusions = emptyList(),
        )

        // Non-nullable target + null return -> mapping is skipped
        assertFalse(result.containsKey("name"))
    }
}