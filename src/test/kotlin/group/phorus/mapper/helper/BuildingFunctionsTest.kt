package group.phorus.mapper.helper

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested

internal class BuildingFunctionsTest {

    class BuildTestClasses {

        // Test constructor 1
        data class Person(var name: String, var surname: String, var age: Int, var sex: Boolean) {

            var middleName: String? = null

            var constructorUsed = 1.0

            // Test constructor 2
            constructor(name: String, age: Int) : this(name, "defaultSurname", age, false) {
                constructorUsed = 2.0
            }

            // Test constructor 3
            constructor (name: String) : this(name, "defaultSurname", 1, false) {
                constructorUsed = 3.0
            }

            // Test constructor 3.2
            constructor (name: Int) : this("defaultName", "defaultSurname", 1, false) {
                constructorUsed = 3.2
            }

            // Test constructor 4
            constructor(name: String?, sex: Boolean) : this(name ?: "defaultName", "defaultSurname", 1, sex) {
                constructorUsed = 4.0
            }

            // Test constructor 5
            constructor(age: Int = 20, sex: Boolean, surname: String) : this("defaultName", surname, age, sex) {
                constructorUsed = 5.0
            }

            // Test constructor 6
            constructor (name: String, tmp: String?) : this(name, "defaultSurname", 1, false) {
                constructorUsed = 6.0
            }

            // Test constructor 7
            constructor (name: String, test: Double = 1.0) : this(name, "defaultSurname", 1, false) {
                constructorUsed = 7.0
            }

            // Test constructor 8
            constructor (name: String, tmp: String, test: Double = 1.0) : this(name, "defaultSurname", 1, false) {
                constructorUsed = 8.0
            }
        }
    }

    @Nested
    inner class `Build object with constructor`() {

        @Test
        fun `find a constructor with no params - Test constructor 0`() {
            class Pet(var name: String? = null)

            // Try to find a constructor only with all the params
            val props = emptyMap<String, String>()

            val result = buildWithConstructor<Pet>(props)

            // A no args constructor exists
            assertNotNull(result.first)
            assertEquals(0, result.second.size)
            assertNull(result.first?.name)
        }

        @Test
        fun `find a constructor with all the params - Test constructor 1`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "name" to "testName",
                "surname" to "testSurname",
                "age" to 10,
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            assertEquals(1.0, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("testSurname", result.first?.surname)
            assertEquals(10, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a constructor with 2 params - Test constructor 2`() {
            // Try to find a constructor only with the "name" and "age" param
            val props = mapOf(
                "name" to "testName",
                "age" to 10,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            assertEquals(2.0, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(10, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a constructor with 1 param - Test constructor 3`() {
            // Try to find a constructor only with the "name" param
            val props = mapOf(
                "name" to "testName",
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            // The function will use the constructor 3, and not the 6 and 7 because they have more unneeded params
            assertEquals(3.0, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a constructor with 1 param, but use the wrong type - Test constructor 3`() {
            // Try to find a constructor only with the "name" param
            val props = mapOf(
                "name" to true,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            // The function won't use the constructor 3, since specified value has the wrong type
            assertNull(result.first)

            // 1 property couldn't be set using the constructor
            assertEquals(2, result.second.size)
            assertTrue(result.second.contains("name"))
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a constructor with 1 param with the right type - Test constructor 3`() {
            // Try to find a constructor only with the "name" param
            val props = mapOf(
                "name" to 5,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            // The function will use the constructor 3.2 instead of the 3 because of the value type
            assertEquals(3.2, result.first?.constructorUsed)

            assertEquals("defaultName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a constructor with 2 params, but one is nullable - Test constructor 4`() {
            // Try to find a constructor only with the "sex" param
            val props = mapOf(
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            assertEquals(4.0, result.first?.constructorUsed)

            assertEquals("defaultName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `find a constructor with all params, but one param has a default value - Test constructor 5`() {
            val props = mapOf(
                "surname" to "testSurname",
                "sex" to true,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            assertEquals(5.0, result.first?.constructorUsed)

            assertEquals("defaultName", result.first?.name)
            assertEquals("testSurname", result.first?.surname)
            assertEquals(20, result.first?.age)
            assertEquals(true, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `set a nullable property as null explicitly - Test constructor 6`() {
            val props = mapOf(
                "name" to "testName",
                "tmp" to null,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            // The function will use the constructor 6, because we are trying to explicitly set the "tmp" field to null
            assertEquals(6.0, result.first?.constructorUsed)

            assertEquals("testName", result.first?.name)
            assertEquals("defaultSurname", result.first?.surname)
            assertEquals(1, result.first?.age)
            assertEquals(false, result.first?.sex)

            // 1 property couldn't be set using the constructor
            assertEquals(1, result.second.size)
            assertTrue(result.second.contains("middleName"))
        }

        @Test
        fun `try to set a non-nullable property as null explicitly - Test constructor 6`() {
            val props = mapOf(
                "name" to "testName",
                "tmp" to "tmpTest",
                "test" to null,
                "middleName" to "Jr",
            )

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

            // The function will find the constructor 8, but we are trying to explicitly set the "test" field to
            //  null and the field is not nullable. In this case the function will take the constructor into account but
            //  will also consider this parameter as unneeded, and will try to find a constructor without it
            // That's why it should find the constructor 6
            assertEquals(6.0, result.first?.constructorUsed)

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

            val result = buildWithConstructor<BuildTestClasses.Person>(props)

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

            val result = buildWithConstructor<PersonNoArgs>(props)

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


    class BuildWSettersTestClasses {
        // Test constructor 1
        class Person(var name: String, var surname: String, var age: Int, var sex: Boolean) {

            var middleName: String? = null

            val address: String = "defaultAddress"
        }

        // Test constructor 2
        // The function can autocomplete the nullable fields with null, it's not necessary to have a null default value
        class Person2(var name: String?, var surname: String?, var age: Int?, var sex: Boolean?) {

            var middleName: String? = null

            val address: String = "defaultAddress"
        }

        // Test constructor 3
        // The function can autocomplete the nullable fields with null, it's not necessary to have a null default value
        class Person3(var name: String?, var surname: String?, var age: Int?, var sex: Boolean?) {

            var constructorUsed = 0

            init {
                constructorUsed = 1
            }

            constructor() : this(null, null, null, null) {
                constructorUsed = 2
            }

            var middleName: String? = null

            val address: String = "defaultAddress"
        }

        // Test constructor 4
        class Person4 {

            var middleName: String? = null

            val address: String = "defaultAddress"
        }

        // Test constructor 5
        class Person5 {

            var middleName: String = "defaultMiddleName"

            val address: String = "defaultAddress"
        }
    }

    @Nested
    inner class `Build object with constructor and setters`() {

        @Test
        fun `find a constructor with all the params, and set the last property with setters - Test constructor 1`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "name" to "testName",
                "surname" to "testSurname",
                "age" to 10,
                "sex" to true,
                "middleName" to "Jr",
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person>(props)

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
        fun `find no args constructor to set all the properties with setters forcefully - Test constructor 2`() {
            val props = mapOf(
                "name" to "testName",
                "surname" to "testSurname",
                "age" to 10,
                "sex" to true,
                "middleName" to "Jr",
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person2>(props, useSettersOnly = true)

            // All properties have been set with setters
            assertEquals("testName", result?.name)
            assertEquals("testSurname", result?.surname)
            assertEquals(10, result?.age)
            assertEquals(true, result?.sex)
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `update a base class instead of building a new object - Test constructor 3`() {
            val baseClass = BuildWSettersTestClasses.Person3(
                name = "testName",
                surname = "testSurname",
                age = 5,
                sex = false,
            ).apply { constructorUsed = 0 } // Change the constructor used property back to 0

            val props = mapOf(
                "middleName" to "Jr",
                "address" to "testAddress",
            )

            val result = buildOrUpdate(props, baseEntity = baseClass)

            // Since the class was not built again the constructor used is still 0
            assertEquals(0, result?.constructorUsed)

            assertEquals("testName", result?.name)
            assertEquals("testSurname", result?.surname)
            assertEquals(5, result?.age)
            assertEquals(false, result?.sex)

            // Only this property was set, since the other ones were already set in the base class
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `find no args constructor with less unneeded amount of optional or nullable parameters - Test constructor 3`() {
            val props = mapOf(
                "name" to "testName",
                "surname" to "testSurname",
                "age" to 10,
                "sex" to true,
                "middleName" to "Jr",
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person3>(props, useSettersOnly = true)

            // The constructor 2 is used since it has the least unneeded amount of optional and nullable params
            assertEquals(2, result?.constructorUsed)

            // All properties have been set with setters
            assertEquals("testName", result?.name)
            assertEquals("testSurname", result?.surname)
            assertEquals(10, result?.age)
            assertEquals(true, result?.sex)
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `find no args constructor, and set the properties with setters only - Test constructor 4`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "middleName" to "Jr",
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person4>(props)

            // A no args constructor exists
            // This property has been set using the setters
            assertEquals("Jr", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `set a property to null with setters - Test constructor 4`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "middleName" to null,
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person4>(props)

            // A no args constructor exists
            // This property has been set using the setters
            assertNull(result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `try to set a non-nullable property to null with setters - Test constructor 5`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "middleName" to null,
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person5>(props)

            // A no args constructor exists
            // This property has not been set using the setters, because it's non-nullable
            assertEquals("defaultMiddleName", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }

        @Test
        fun `try to set a property with the wrong type with setters - Test constructor 5`() {
            // Try to find a constructor only with all the params
            val props = mapOf(
                "middleName" to 5,
                "address" to "testAddress",
            )

            val result = buildOrUpdate<BuildWSettersTestClasses.Person5>(props)

            // A no args constructor exists
            // This property has not been set using the setters, the value was ignored since it had a wrong type
            assertEquals("defaultMiddleName", result?.middleName)

            // This property couldn't be set because it is a val, so it doesn't have setters
            assertEquals("defaultAddress", result?.address)
        }
    }
}