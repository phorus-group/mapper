package group.phorus.mapper.helper

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

internal class PropertySetFunctionsTests {

    // Test constructor 0
    data class Pet(var name: String? = null)

    // Test constructor 1
    data class Person(var name: String, var surname: String, var age: Int, var sex: Boolean) {

        var middleName: String? = null

        // Test constructor 2
        constructor(name: String, age: Int) : this(name, "defaultSurname", age, false)

        // Test constructor 3
        constructor (name: String) : this(name, "defaultSurname", 1, false)

        // Test constructor 4
        constructor(name: String?, sex: Boolean) : this(name ?: "defaultName", "defaultSurname", 1, sex)

        // Test constructor 5
        constructor(age: Int = 20, sex: Boolean, surname: String) : this("defaultName", surname, age, sex)
    }

    @Nested
    inner class `Build object with constructor tests`() {

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

            // A constructor with all the params exists
            assertEquals("testName", result.first?.name)
            assertEquals("testSurname", result.first?.surname)
            assertEquals(10, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertEquals("Jr", result.second.asSequence().first { it.key == "middleName" }.value)
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

            // A constructor with 2 of 3 params exists
            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(10, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertEquals("Jr", result.second.asSequence().first { it.key == "middleName" }.value)
        }

        @Test
        fun `find constructor with 1 param - Test constructor 3`() {
            // Try to find a constructor only with the "name" param
            val props = mapOf(
                "name" to "testName",
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // A constructor with 1 of 3 params exists
            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertEquals("Jr", result.second.asSequence().first { it.key == "middleName" }.value)
        }

        @Test
        fun `find constructor with 2 params, but one is nullable - Test constructor 4`() {
            // Try to find a constructor only with the "sex" param
            val props = mapOf(
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // A constructor with 1 of 3 params exists
            assertEquals("defaultName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertEquals("Jr", result.second.asSequence().first { it.key == "middleName" }.value)
        }

        @Test
        fun `find constructor with all params, but one param has a default value - Test constructor 5`() {
            // Try to find a constructor only with the "surname" and "sex" param
            val props = mapOf(
                "surname" to "testSurname",
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildObjectWithConstructor<Person>(props)

            // A constructor with 1 of 3 params exists
            assertEquals("defaultName", result.first?.name)
            assertEquals("testSurname", result.first?.surname)
            assertEquals(20, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertEquals("Jr", result.second.asSequence().first { it.key == "middleName" }.value)
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
            assertEquals(10, result.second.asSequence().first { it.key == "age" }.value)
            assertEquals("Jr", result.second.asSequence().first { it.key == "middleName" }.value)
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

    @Nested
    inner class `Build object tests`() {

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
    }
}