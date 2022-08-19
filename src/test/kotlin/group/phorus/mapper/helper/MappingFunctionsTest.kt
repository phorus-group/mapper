package group.phorus.mapper.helper

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

internal class MappingFunctionsTest {

    @Test
    fun `parse a location string`() {
        val locationString = "./../pet/./name/./"

        val location = parseLocation(locationString)

        assertEquals(3, location.size)
        assertEquals("..", location[0])
        assertEquals("pet", location[1])
        assertEquals("name", location[2])
    }


    class MappingTestClasses {
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

        assertEquals(2, result.size)

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
}