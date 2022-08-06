package group.phorus.mapper.helper

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.reflect.KProperty
import kotlin.reflect.full.*

internal class PropertySetFunctionsTests {

    // Test constructor 0
    data class Pet(var name: String? = null)

    // Test constructor 1
    data class Person(var name: String, var surname: String, var age: Int, var sex: Boolean) {

        var middleName: String? = null

        var constructorUsed: Int? = 1

        // Test constructor 2
        constructor(name: String, age: Int) : this(name, "defaultSurname", age, false) {
            constructorUsed = 2
        }

        // Test constructor 3
        constructor (name: String) : this(name, "defaultSurname", 1, false) {
            constructorUsed = 3
        }

        // Test constructor 4
        constructor(name: String?, sex: Boolean) : this(name ?: "defaultName", "defaultSurname", 1, sex) {
            constructorUsed = 4
        }

        // Test constructor 5
        constructor(age: Int = 20, sex: Boolean, surname: String) : this("defaultName", surname, age, sex) {
            constructorUsed = 5
        }

        // Test constructor 6
        constructor (name: String, tmp: String?) : this(name, "defaultSurname", 1, false) {
            constructorUsed = 6
        }

        // Test constructor 7
        constructor (name: String, test: Double = 1.0) : this(name, "defaultSurname", 1, false) {
            constructorUsed = 7
        }

        // Test constructor 8
        constructor (name: String, tmp: String, test: Double = 1.0) : this(name, "defaultSurname", 1, false) {
            constructorUsed = 8
        }
    }

    @Nested
    inner class `Build object with constructor`() {

        @Test
        fun `find constructor with no params - Test constructor 0`() {
            // Try to find a constructor only with all the params
            val props = emptyMap<String, String>()

            val result = buildObjectWithConstructor<Pet>(props)

            // A no args constructor exists
            assertNotNull(result.first)
            assertEquals(0, result.second.size)
            assertNull(result.first?.name)
        }

        @Test
        fun `find constructor with all the params - Test constructor 1`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "name" to "testName",
                "surname" to "testSurname",
                "age" to 10,
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            assertEquals(1, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("testSurname", result.first?.surname)
            assertEquals(10, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find constructor with 2 params - Test constructor 2`() {
            // Try to find a constructor only with the "name" and "age" param
            val props = mapOf(
                "name" to "testName",
                "age" to 10,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            assertEquals(2, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(10, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find constructor with 1 param - Test constructor 3`() {
            // Try to find a constructor only with the "name" param
            val props = mapOf(
                "name" to "testName",
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // The function will use the constructor 3, and not the 6 and 7 because they have more unneeded params
            assertEquals(3, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find constructor with 2 params, but one is nullable - Test constructor 4`() {
            // Try to find a constructor only with the "sex" param
            val props = mapOf(
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            assertEquals(4, result.first?.constructorUsed)

            assertEquals("defaultName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find constructor with all params, but one param has a default value - Test constructor 5`() {
            val props = mapOf(
                "surname" to "testSurname",
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            assertEquals(5, result.first?.constructorUsed)

            assertEquals("defaultName", result.first?.name)
            assertEquals("testSurname", result.first?.surname)
            assertEquals(20, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find constructor with 2 params while trying to set one property as null explicitly - Test constructor 6`() {
            val props = mapOf(
                "name" to "testName",
                "tmp" to null,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // The function will use the constructor 6, because we are trying to explicitly set the "tmp" field to null
            assertEquals(6, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find constructor with 3 params while trying to set one property as null explicitly - Test constructor 8`() {
            val props = mapOf(
                "name" to "testName",
                "tmp" to "tmpTest",
                "test" to null,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // The function will use the constructor 8, because we are trying to explicitly set the "test" field to
            //  null, but the field is not nullable, luckily the field is optional so the constructor
            //  can be used anyway
            assertEquals(8, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 2 property couldn't be set using the constructor
            assertEquals(2, result.second.size)
            assertTrue(result.second.contains("test"))
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `try to find non-existent constructor with 1 param`() {
            // Try to find a constructor only with the "age" param
            val props = mapOf(
                "age" to 10,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // A constructor only with the "age" param doesn't exist
            assertNull(result.first)

            // 2 properties couldn't be set using the constructor
            assertEquals(2, result.second.size)
            assertTrue(result.second.contains("age"))
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a no args constructor`() {
            class PersonNoArgs() {
                var constructorUsed = 1

                // Test constructor 2
                constructor(tmp: String?) : this() {
                    constructorUsed = 2
                }
            }

            // Try to find a constructor only with the "age" param
            val props = mapOf(
                "age" to 10,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<PersonNoArgs>(props)

            // A no args constructor exists
            assertNotNull(result.first)

            // The no args constructor is used because it has less unneeded params
            assertEquals(1, result.first?.constructorUsed)

            // 2 properties couldn't be set using the constructor
            assertEquals(2, result.second.size)
            assertTrue(result.second.contains("age"))
            assertTrue(result.second.contains("middleName"))
        }
    }


    // Test constructor 1
    data class Person2(var name: String, var surname: String, var age: Int, var sex: Boolean) {

        var middleName: String? = null

        val address: String = "defaultAddress"
    }

    // Test constructor 2
    // The function can autocomplete the nullable fields with null, it's not necessary to have a null default value
    data class Person3(var name: String?, var surname: String?, var age: Int?, var sex: Boolean?) {

        var middleName: String? = null

        val address: String = "defaultAddress"
    }

    // Test constructor 3
    class Person4() {

        var middleName: String? = null

        val address: String = "defaultAddress"
    }

    // Test constructor 4
    class Person5() {

        var middleName: String = "defaultMiddleName"

        val address: String = "defaultAddress"
    }

    @Nested
    inner class `Build object with constructor and setters`() {

        @Test
        fun `find constructor with all the params, and set the last property with setters - Test constructor 1`() {
            // Try to find a constructor only with all the params
            val props: Map<KProperty<*>, Any?> = Person2::class.memberProperties
                .associate {
                    when(it.name) {
                        "name" -> it to "testName"
                        "surname" -> it to "testSurname"
                        "age" -> it to 10
                        "sex" -> it to true
                        "middleName" -> it to "Jr"
                        "address" -> it to "testAddress"
                        else -> it to null
                    }
                }

            val result = buildObject<Person2>(props)

            // A constructor with all the params exists
            assertEquals("testName", result?.name)
            assertEquals("testSurname", result?.surname)
            assertEquals(10, result?.age)
            assertEquals(true, result?.sex)

            // This property has been set using the setters
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `find no args constructor, and set the properties with setters forcefully - Test constructor 2`() {
            // Try to find a constructor only with all the params
            val props: Map<KProperty<*>, Any?> = Person3::class.memberProperties
                .associate {
                    when(it.name) {
                        "name" -> it to "testName"
                        "surname" -> it to "testSurname"
                        "age" -> it to 10
                        "sex" -> it to true
                        "middleName" -> it to "Jr"
                        "address" -> it to "testAddress"
                        else -> it to null
                    }
                }

            val result = buildObject<Person3>(props, useSettersOnly = true)

            // A constructor with all the params exists
            assertEquals("testName", result?.name)
            assertEquals("testSurname", result?.surname)
            assertEquals(10, result?.age)
            assertEquals(true, result?.sex)

            // This property has been set using the setters
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `find no args constructor, and set the properties with setters only - Test constructor 3`() {
            // Try to find a constructor only with all the params
            val props: Map<KProperty<*>, Any?> = Person4::class.memberProperties
                .associate {
                    when(it.name) {
                        "middleName" -> it to "Jr"
                        "address" -> it to "testAddress"
                        else -> it to null
                    }
                }

            val result = buildObject<Person4>(props)

            // A no args constructor exists
            // This property has been set using the setters
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `set a property to null with setters - Test constructor 3`() {
            // Try to find a constructor only with all the params
            val props: Map<KProperty<*>, Any?> = Person4::class.memberProperties
                .associate {
                    when(it.name) {
                        "middleName" -> it to null
                        "address" -> it to "testAddress"
                        else -> it to null
                    }
                }

            val result = buildObject<Person4>(props)

            // A no args constructor exists
            // This property has been set using the setters
            assertNull(result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `try to set a non-nullable property to null with setters - Test constructor 4`() {
            // Try to find a constructor only with all the params
            val props: Map<KProperty<*>, Any?> = Person4::class.memberProperties
                .associate {
                    when(it.name) {
                        "middleName" -> it to null
                        "address" -> it to "testAddress"
                        else -> it to null
                    }
                }

            val result = buildObject<Person5>(props)

            // A no args constructor exists
            // This property has not been set using the setters, because it's non-nullable
            assertEquals("defaultMiddleName", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }
    }
}