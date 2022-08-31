package group.phorus.mapper

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import group.phorus.mapper.MappingExtensionsTest.TestClasses.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

@Suppress("UNUSED", "ClassName")
internal class MappingExtensionsTest {

    class TestClasses {

        class User(
            var addresses: List<String>,
            var age: Int,
        )

        class Person(
            var id: Long,
            var name: String,
            var surname: String,
            var age: Int,
        )

        class PersonDTO(
            var nameStr: String? = null,
            var ageStr: String? = null,
            var surname: String? = null,
        )
        class Room(
            var guest: Person,
            var roomName: String,
        )

        class RoomDTO(
            var guest: PersonDTO,
            var roomName: String,
        )

        class Wifi(
            var room: Room,
            var wifiPassword: Int,
        )

        class WifiDTO(
            var room: RoomDTO,
            var wifiPassword: Int,
        )

        class Reservation(
            var room: Room,
            var description: String,
        )

        class Hotel(
            var hotelRooms: Set<Room>,
            var numberOfGuests: Int,
        )
    }

    @Nested
    inner class `Test performance` {
        private inline fun<T> time(function: () -> T): Pair<T, Long> {
            val startTime = System.currentTimeMillis()
            val result: T = function.invoke()
            val endTime = System.currentTimeMillis()

            return Pair(result, endTime - startTime)
        }

        @Test
        fun `mapper performance - normal mapping`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val mapperResult = time { person.mapTo<PersonDTO>() }

            assertNotNull(mapperResult.first)
            assertEquals("surnameTest", mapperResult.first?.surname)

            println("Mapper time: ${mapperResult.second}")

            // Results:
            // Mapper time: 777
            // Mapper time: 916
            // Mapper time: 908
            // Mapper time: 784

            // Average: 846.25
        }

        @Test
        fun `jackson performance - normal mapping`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val jacksonResult = time {
                jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .convertValue<PersonDTO>(person)
            }

            assertNotNull(jacksonResult.first)
            assertEquals("surnameTest", jacksonResult.first.surname)

            println("Jackson time: ${jacksonResult.second}")

            // Results:
            // Jackson time: 1237
            // Jackson time: 1257
            // Jackson time: 1303
            // Jackson time: 1100

            // Average: 1224.25
        }

        // Performance improvement over jackson - normal mapping: 44%~

        @Test
        fun `mapper performance - compound mapping`() {
            val room = Room(
                guest = Person(23, "nameTest", "surnameTest", 88),
                roomName = "roomNameTest",
            )

            val mapperResult = time { room.mapTo<RoomDTO>() }

            assertNotNull(mapperResult.first)
            assertNotNull(mapperResult.first?.guest)
            assertEquals("roomNameTest", mapperResult.first?.roomName)
            assertEquals("surnameTest", mapperResult.first?.guest?.surname)

            println("Mapper time: ${mapperResult.second}")

            // Results:
            // Mapper time: 930
            // Mapper time: 898
            // Mapper time: 1019
            // Mapper time: 794

            // Average: 910.25
        }

        @Test
        fun `jackson performance - compound mapping`() {
            val room = Room(
                guest = Person(23, "nameTest", "surnameTest", 88),
                roomName = "roomNameTest",
            )

            val jacksonResult = time {
                jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .convertValue<RoomDTO>(room)
            }

            assertNotNull(jacksonResult.first)
            assertNotNull(jacksonResult.first.guest)
            assertEquals("roomNameTest", jacksonResult.first.roomName)
            assertEquals("surnameTest", jacksonResult.first.guest.surname)

            println("Jackson time: ${jacksonResult.second}")

            // Results:
            // Jackson time: 1114
            // Jackson time: 1151
            // Jackson time: 1490
            // Jackson time: 1392

            // Average: 1286.75
        }

        // Performance improvement over jackson - compound mapping: 41%~
    }

    @Nested
    inner class `Test normal mappings` {

        @Test
        fun `map from one object to another`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>()

            assertNotNull(result)

            // The property is mapped from the original object property because they're called the same way
            assertEquals("surnameTest", result.surname)

            // The other target object properties are not called in the same way as any of the original object properties, so
            //  they stay as null
            assertNull(result.nameStr)
            assertNull(result.ageStr)
        }

        @Test
        fun `map from one object to another ignoring a property name`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapToClass<PersonDTO>(exclusions = listOf("surname"))

            assertNotNull(result)

            // Because of the exclusion, the property is null even if it has the same name as a property in the original object
            assertNull(result.surname)

            assertNull(result.nameStr)
            assertNull(result.ageStr)
        }

        @Test
        fun `map from one object to another ignoring a property`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(exclusions = listOf(PersonDTO::surname))

            assertNotNull(result)

            // Because of the exclusion, the property is null even if it has the same name as a property in the original object
            assertNull(result.surname)

            assertNull(result.nameStr)
            assertNull(result.ageStr)
        }

        @Test
        fun `map from one object to another manually mapping a property`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL)))

            assertNotNull(result)

            // The mapper will use the custom mappings to map from an original property to a custom target property
            assertEquals("nameTest", result.nameStr)

            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property with the wrong type`() {
            val user = User(
                addresses = listOf(
                    "addr1",
                    "addr2",
                ),
                age = 12,
            )

            val result = user.mapTo<PersonDTO>(mappings = mapOf(
                User::addresses to (PersonDTO::nameStr to MappingFallback.NULL),
                User::age to (PersonDTO::ageStr to MappingFallback.NULL),
            ))

            assertNotNull(result)

            // If the mapper cannot automatically map the original property to the target one, it will leave it as null
            assertNull(result.nameStr)

            // The mapper will map the primitive automatically
            assertEquals("12", result.ageStr)
        }
    }

    @Nested
    inner class `Test primitives` {

        @Test
        fun `map from a string to other string`() {
            val result = "10".mapTo<String>()

            assertNotNull(result)

            // The property doesn't need mapping
            assertEquals("10", result)
        }

        @Test
        fun `map from a string to other string - disable map primitives`() {
            val result = "10".mapTo<String>(mapPrimitives = false)

            assertNotNull(result)

            // The property doesn't need mapping
            assertEquals("10", result)
        }

        @Test
        fun `map from an int to other int`() {
            val result = 10.mapTo<Int>()

            assertNotNull(result)

            // The property doesn't need mapping
            assertEquals(10, result)
        }

        @Test
        fun `map from an int to other int - disable map primitives`() {
            val result = 10.mapTo<Int>(mapPrimitives = false)

            assertNotNull(result)

            // The property doesn't need mapping
            assertEquals(10, result)
        }

        @Test
        fun `map from an int to a double`() {
            val result = 10.mapTo<Double>()

            assertNotNull(result)

            // The property is mapped to an int
            assertEquals(10.0, result)
        }

        @Test
        fun `map from a int to a double - disable map primitives`() {
            val result = 10.mapTo<Double>(mapPrimitives = false)

            // The property is not mapped
            assertNull(result)
        }

        @Test
        fun `map from a string to an int`() {
            val result = "10".mapTo<Int>()

            assertNotNull(result)

            // The property is mapped to an int
            assertEquals(10, result)
        }

        @Test
        fun `map from a int to a string`() {
            val result = 10.mapTo<String>()

            assertNotNull(result)

            // The property is mapped to a string
            assertEquals("10", result)
        }

        @Test
        fun `map from a int to a string - disable map primitives`() {
            val result = 10.mapTo<String>(mapPrimitives = false)

            // The property is not mapped
            assertNull(result)
        }
    }

    
    class CompositeTestClasses {

        class UserDTO(
            var addresses: List<String>,
            var age: Int,
        )

        class UserSet(
            var addresses: Set<String>,
            var age: Int,
        )

        class UserSetDTO(
            var addresses: Set<String>,
            var age: Int,
        )

        class UserMap(
            var addresses: Map<Int, String>,
            var age: Int,
        )

        class UserMapDTO(
            var addresses: Map<Int, String>,
            var age: Int,
        )

        class UserPair(
            var addresses: Pair<String, String>,
            var age: Int,
        )

        class UserPairDTO(
            var addresses: Pair<String, String>,
            var age: Int,
        )

        class UserTriple(
            var addresses: Triple<String, String, String>,
            var age: Int,
        )

        class UserTripleDTO(
            var addresses: Triple<String, String, String>,
            var age: Int,
        )
    }
    
    @Nested
    inner class `Test composites` {

        @Test
        fun `map from a list of objects to another`() {
            val persons = listOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<List<PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )

            assertNotNull(result)

            assertEquals(2, result.size)

            assertEquals("nameTest1", result[0].nameStr)
            assertEquals("surnameTest1", result[0].surname)
            assertNull(result[0].ageStr)
            assertEquals("nameTest2", result[1].nameStr)
            assertEquals("surnameTest2", result[1].surname)
            assertNull(result[1].ageStr)
        }

        @Test
        fun `map from an object with a list to another`() {
            val users = User(
                addresses = listOf("addr1", "addr2"),
                age = 27,
            )

            val result = users.mapTo<CompositeTestClasses.UserDTO>()

            assertNotNull(result)

            assertEquals("addr1", result.addresses[0])
            assertEquals("addr2", result.addresses[1])
            assertEquals(27, result.age)
        }

        @Test
        fun `map from a list of objects to another with a function`() {
            val persons = listOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            // The function uses the original property as input, and puts the returned value in the target property
            val parseManually : (Int) -> String = {
                (it + 5).toString()
            }

            val result = persons.mapTo<List<PersonDTO>>(
                functionMappings = mapOf(Person::age to (parseManually to (PersonDTO::ageStr to MappingFallback.NULL)))
            )

            assertNotNull(result)

            assertEquals(2, result.size)

            assertEquals("92", result[0].ageStr)
            assertEquals("surnameTest1", result[0].surname)
            assertEquals("93", result[1].ageStr)
            assertEquals("surnameTest2", result[1].surname)
        }

        @Test
        fun `map from a set of objects to another`() {
            val persons = setOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<Set<PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )
                ?.toList()

            assertNotNull(result)

            assertEquals(2, result.size)

            assertEquals("nameTest1", result[0].nameStr)
            assertEquals("surnameTest1", result[0].surname)
            assertNull(result[0].ageStr)
            assertEquals("nameTest2", result[1].nameStr)
            assertEquals("surnameTest2", result[1].surname)
            assertNull(result[1].ageStr)
        }

        @Test
        fun `map from an object with a set to another`() {
            val users = CompositeTestClasses.UserSet(
                addresses = setOf("addr1", "addr2"),
                age = 27,
            )

            val result = users.mapTo<CompositeTestClasses.UserSetDTO>()

            assertNotNull(result)

            assertEquals("addr1", result.addresses.toList()[0])
            assertEquals("addr2", result.addresses.toList()[1])
            assertEquals(27, result.age)
        }

        @Test
        fun `try to map from an array of objects to another - not supported`() {
            val persons = arrayOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<Array<PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )

            // Arrays are not supported, avoid using them
            assertNull(result)
        }

        @Test
        fun `map from a map of objects to another`() {
            val persons = mapOf(
                "0" to Person(23, "nameTest1", "surnameTest1", 87),
                "1" to Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<Map<String, PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )

            assertNotNull(result)

            assertEquals(2, result.size)

            assertEquals("nameTest1", result["0"]?.nameStr)
            assertEquals("surnameTest1", result["0"]?.surname)
            assertNull(result["0"]?.ageStr)
            assertEquals("nameTest2", result["1"]?.nameStr)
            assertEquals("surnameTest2", result["1"]?.surname)
            assertNull(result["1"]?.ageStr)
        }

        @Test
        fun `map from an object with a map to another`() {
            val users = CompositeTestClasses.UserMap(
                addresses = mapOf(1 to "addr1", 2 to "addr2"),
                age = 27,
            )

            val result = users.mapTo<CompositeTestClasses.UserMapDTO>()

            assertNotNull(result)

            assertEquals("addr1", result.addresses[1])
            assertEquals("addr2", result.addresses[2])
            assertEquals(27, result.age)
        }

        @Test
        fun `map from a pair of objects to another`() {
            val person = "0" to Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<Pair<String, PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )

            assertNotNull(result)

            assertEquals("0", result.first)
            assertEquals("nameTest", result.second.nameStr)
            assertEquals("surnameTest", result.second.surname)
            assertNull(result.second.ageStr)
        }

        @Test
        fun `map from an object with a pair to another`() {
            val users = CompositeTestClasses.UserPair(
                addresses = "addr1" to "addr2",
                age = 27,
            )

            val result = users.mapTo<CompositeTestClasses.UserPairDTO>()

            assertNotNull(result)

            assertEquals("addr1", result.addresses.first)
            assertEquals("addr2", result.addresses.second)
            assertEquals(27, result.age)
        }

        @Test
        fun `map from a triple to another`() {
            val person = Triple("0", 5, Person(23, "nameTest", "surnameTest", 87))

            val result = person.mapTo<Triple<String, Long, PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )

            assertNotNull(result)

            assertEquals("0", result.first)
            assertEquals(5, result.second)
            assertEquals("nameTest", result.third.nameStr)
            assertEquals("surnameTest", result.third.surname)
            assertNull(result.third.ageStr)
        }

        @Test
        fun `map from an object with a triple to another`() {
            val users = CompositeTestClasses.UserTriple(
                addresses = Triple("addr1", "addr2", "addr3"),
                age = 27,
            )

            val result = users.mapTo<CompositeTestClasses.UserTripleDTO>()

            assertNotNull(result)

            assertEquals("addr1", result.addresses.first)
            assertEquals("addr2", result.addresses.second)
            assertEquals("addr3", result.addresses.third)
            assertEquals(27, result.age)
        }

        @Test
        fun `try to map a composite to a type that isn't the same as the original entity`() {
            val person = Triple("0", 5, Person(23, "nameTest", "surnameTest", 87))

            val result = person.mapTo<List<PersonDTO>>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL))
            )

            // If the original entity is a composite, the mapper cannot map it to a different composite type
            // For example: you can map List<Person> to List<PersonDTO>, but you cannot map List<Person>
            //  to Pair<String, PersonDTO>
            assertNull(result)
        }
    }

    @Nested
    inner class `Test update from function` {

        @Test
        fun `update an object from another object`() {
            val person = PersonDTO("nameTest", "87", "surnameTest")
            val person2 = PersonDTO(nameStr = "nameTest2")

            val result = person.updateFrom(person2)

            assertNotNull(result)

            // The result is still the same instance
            assertTrue(result === person)

            assertEquals("nameTest2", result.nameStr) // Only the name field was updated
            assertEquals("surnameTest", result.surname)
            assertEquals("87", result.ageStr)

            // The instance was changed using setters
            assertEquals("nameTest2", person.nameStr)
            assertEquals("surnameTest", person.surname)
            assertEquals("87", person.ageStr)
        }

        @Test
        fun `update an object from another object with setters only = false`() {
            val person = PersonDTO("nameTest", "87", "surnameTest")
            val person2 = PersonDTO(nameStr = "nameTest2")

            val result = person.updateFrom(person2, useSettersOnly = false)

            assertNotNull(result)

            // The result is no longer the same instance, since it was built using a constructor
            assertTrue(result != person)

            assertEquals("nameTest2", result.nameStr) // Only the name field was updated
            assertEquals("surnameTest", result.surname)
            assertEquals("87", result.ageStr)
        }

        @Test
        fun `update an object from another object with the update option set_nulls`() {
            val person = PersonDTO("nameTest", "87", "surnameTest")
            val person2 = PersonDTO(nameStr = "nameTest2")

            val result = person.updateFrom(person2, UpdateOption.SET_NULLS)

            assertNotNull(result)

            // The result is still the same instance
            assertTrue(result === person)

            // Since we are using the SET_NULL options, nulls are no longer ignored thus every field is updated
            assertEquals("nameTest2", result.nameStr)
            assertNull(result.surname)
            assertNull(result.ageStr)
        }

        @Test
        fun `update a collection of objects from another object`() {
            val persons = listOf(PersonDTO("nameTest", "87", "surnameTest"))
            val person2 = PersonDTO(nameStr = "nameTest2")

            val result = persons.updateFrom(person2)

            assertNotNull(result)

            // The result list still has the same instances
            assertTrue(result.first() === persons.first())

            assertEquals("nameTest2", result.first().nameStr) // Only the name field was updated
            assertEquals("surnameTest", result.first().surname)
            assertEquals("87", result.first().ageStr)
        }

        @Test
        fun `update a collection of objects from another collection`() {
            val persons = listOf(PersonDTO("nameTest", "87", "surnameTest"))
            val person2 = listOf(PersonDTO(nameStr = "nameTest2"))

            val result = persons.updateFrom(person2)

            assertNotNull(result)

            // The result list still has the same instances
            assertTrue(result.first() === persons.first())

            // The mapper doesn't update collections of objects from other collections, because it makes no sense
            assertEquals("nameTest", result.first().nameStr)
            assertEquals("surnameTest", result.first().surname)
            assertEquals("87", result.first().ageStr)
        }

        @Test
        fun `update a collection of objects from another object with setters only = false`() {
            val persons = listOf(PersonDTO("nameTest", "87", "surnameTest"))
            val person2 = PersonDTO(nameStr = "nameTest2")

            val result = persons.updateFrom(person2, useSettersOnly = false)

            assertNotNull(result)

            // The result list doesn't have the same instances, since it was built using a constructor
            assertTrue(result.first() != persons.first())

            assertEquals("nameTest2", result.first().nameStr) // Only the name field was updated
            assertEquals("surnameTest", result.first().surname)
            assertEquals("87", result.first().ageStr)
        }

        @Test
        fun `update a collection of objects from another object with the update option set_nulls`() {
            val persons = listOf(PersonDTO("nameTest", "87", "surnameTest"))
            val person2 = PersonDTO(nameStr = "nameTest2")

            val result = persons.updateFrom(person2, UpdateOption.SET_NULLS)

            assertNotNull(result)

            // The result list still has the same instances
            assertTrue(result.first() === persons.first())

            // Since we are using the SET_NULL options, nulls are no longer ignored thus every field is updated
            assertEquals("nameTest2", result.first().nameStr)
            assertNull(result.first().surname)
            assertNull(result.first().ageStr)
        }
    }

    @Nested
    inner class `Test manual mappings` {

        @Test
        fun `map from one object to another manually mapping a property name`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapToClass<PersonDTO>(
                mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)),
            )

            assertNotNull(result)

            assertEquals("nameTest", result.nameStr)
            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL)),
            )

            assertNotNull(result)

            assertEquals("nameTest", result.nameStr)
            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property name with a function`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            // The function uses the original property as input, and puts the returned value in the target property
            val parseManually : (Int) -> String = {
                (it + 5).toString()
            }

            val result = person.mapToClass<PersonDTO>(
                functionMappings = mapOf("age" to (parseManually to ("ageStr" to MappingFallback.NULL))),
            )

            assertNotNull(result)

            // The property contains the returned value of the function
            assertEquals("92", result.ageStr)

            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property with a function`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            // The function uses the original property as input, and puts the returned value in the target property
            val parseManually : (Int) -> String = {
                (it + 5).toString()
            }

            val result = person.mapTo<PersonDTO>(
                functionMappings = mapOf(Person::age to (parseManually to (PersonDTO::ageStr to MappingFallback.NULL))),
            )

            assertNotNull(result)

            // The property contains the returned value of the function
            assertEquals("92", result.ageStr)

            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property - test mapping priorities`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            // The function uses the original property as input, and puts the returned value in the target property
            val parseManually : (String) -> String = {
                it + "_test"
            }

            val result = person.mapTo<PersonDTO>(
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL)),
                functionMappings = mapOf(Person::name to (parseManually to (PersonDTO::nameStr to MappingFallback.NULL))),
            )

            assertNotNull(result)

            // The property contains the returned value of the function, since it has priority
            assertEquals("nameTest_test", result.nameStr)
            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property with exclusions`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            // The function uses the original property as input, and puts the returned value in the target property
            val parseManually : (String) -> String = {
                it + "_test"
            }

            val result = person.mapTo<PersonDTO>(
                exclusions = listOf(PersonDTO::nameStr),
                mappings = mapOf(Person::name to (PersonDTO::nameStr to MappingFallback.NULL)),
                functionMappings = mapOf(Person::name to (parseManually to (PersonDTO::nameStr to MappingFallback.NULL))),
            )

            assertNotNull(result)

            // The property is null, because exclusions have priority over everything
            assertNull(result.nameStr)
            assertEquals("surnameTest", result.surname)
        }
    }


    class SubTestClasses {
        class Person(
            var name: String,
            var house: House,
        )

        class PersonDTO(
            var name: String,
            var house: House,
        )

        class PersonDTO2(
            var name: String,
            var houseTmp: House,
        )

        class PersonDTOMapFrom(
            var name: String,

            @field:MapFrom(["house"])
            var houseTmp: House?,
        )

        class House(
            var number: Int?,
            var address: Address?,
        )
        class Address(
            val id: Int?,
            var value: String?,
        )
    }

    @Nested
    inner class `Test sub-fields and sub-exclusions` {

        @Test
        fun `map from one object to another with subfield mappings`() {
            val person = SubTestClasses.Person(
                name = "testName",
                house = SubTestClasses.House(
                    number = 14,
                    address = SubTestClasses.Address(
                        id = 3,
                        value = "testAddress"
                    )
                )
            )

            val result = person.mapToClass<SubTestClasses.PersonDTO>(functionMappings = mapOf(
                "1" to ({ 22 } to ("house/number" to MappingFallback.NULL)),
                "2" to ({ "testAddress2" } to ("house/address/value" to MappingFallback.NULL)),
            ))

            assertNotNull(result)

            assertEquals("testName", result.name)
            assertEquals(22, result.house.number)
            assertEquals(3, result.house.address?.id)
            assertEquals("testAddress2", result.house.address?.value)
        }
    }

    @Nested
    inner class `Tests compounds` {

        @Test
        fun `map from one compound object to another`() {
            val room = Room(
                guest = Person(23, "nameTest", "surnameTest", 88),
                roomName = "roomNameTest",
            )

            val result = room.mapTo<RoomDTO>()

            assertNotNull(result)
            assertNotNull(result.guest)

            // The mapper will try to map any properties automatically
            assertEquals("roomNameTest", result.roomName)
            assertEquals("surnameTest", result.guest.surname)
        }

        @Test
        fun `map from one double compound object to another`() {
            val wifi = Wifi(
                room = Room(
                    guest = Person(23, "nameTest", "surnameTest", 88),
                    roomName = "roomNameTest",
                ),
                wifiPassword = 12,
            )

            val result = wifi.mapTo<WifiDTO>()

            assertNotNull(result)
            assertNotNull(result.room)
            assertNotNull(result.room.guest)

            // The mapper works recursively, so properties of properties will also be mapped
            assertEquals(12, result.wifiPassword)
            assertEquals("roomNameTest", result.room.roomName)
            assertEquals("surnameTest", result.room.guest.surname)
        }

        @Test
        fun `map from one double compound object to another with a function`() {
            val wifi = Wifi(
                room = Room(
                    guest = Person(23, "nameTest", "surnameTest", 88),
                    roomName = "roomNameTest",
                ),
                wifiPassword = 12,
            )

            val addFiveToInt : (Int) -> Int = {
                it + 5
            }

            val result = wifi.mapTo<WifiDTO>(
                functionMappings = mapOf(Wifi::wifiPassword to (addFiveToInt to (WifiDTO::wifiPassword to MappingFallback.NULL))),
            )

            assertNotNull(result)
            assertNotNull(result.room)
            assertNotNull(result.room.guest)

            assertEquals(17, result.wifiPassword)
            assertEquals("roomNameTest", result.room.roomName)
            assertEquals("surnameTest", result.room.guest.surname)
        }
    }


    class MapFromClasses {

        class UserDTO(
            @field:MapFrom(["addresses"])
            var addrs: MutableSet<Any>,
            var age: Int,
        )

        class PersonDTO(
            @field:MapFrom(["name"])
            var nameStr: String,
            var surname: String,
            var age: Int,
        )

        // If the MapFrom fails, the fallback will be used, the default one is CONTINUE
        // With fallback continue, the mapper will try to map the field normally
        class PersonFallbackDTO(
            val nameStr: String?,

            @field:MapFrom(["isGoingToFail"])
            val surname: String,
            val age: Int,
        )

        // If the MapFrom fails and the fallback is null, the mapper will try to set the property to null
        class PersonNullFallbackDTO(
            val nameStr: String?,

            @field:MapFrom(["isGoingToFail"], MappingFallback.NULL)
            val surname: String?,
            val age: Int,
        )

        class RoomFallbackDTO(
            @field:MapFrom(["room/guest/name"], MappingFallback.NULL)
            var name: String?,
            var description: String,
        )

        // Only the first valid location will be used
        // If none of the locations is being able to parse anything, then the fallback will be used
        class RoomDTO(
            @field:MapFrom(["isGoingToFail", "alsoGoingToFail", "guest/name"])
            val guestName: String,
            val guestAge: String?,
            val roomName: String?,
        )

        class HotelDTO(
            @field:MapFrom(["hotelRooms"])
            var roomDTOs: MutableList<RoomDTO>,
            var numberOfGuests: Int,
        )

        class PersonParentDTO(
            @field:MapFrom(["../name"])
            var roomName: String,
            var nameStr: String,
        )

        class RoomParentDTO(
            @field:MapFrom(["isGoingToFail", "alsoGoingToFail", "guest/name"])
            val guestName: String,
            val guestAge: String?,
            val roomName: String?,
        )
    }

    @Nested
    inner class `Test MapFrom annotations` {

        @Test
        fun `map from one object to another with the MapFrom annotation`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<MapFromClasses.PersonDTO>()

            assertNotNull(result)

            // The mapper used the content of the MapFrom annotation
            assertEquals("nameTest", result.nameStr)

            assertEquals("surnameTest", result.surname)
            assertEquals(87, result.age)
        }

        @Test
        fun `map from one object to another with the MapFrom annotation, but failing to map and relying on normal mapping`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<MapFromClasses.PersonFallbackDTO>()

            assertNotNull(result)

            // The property is null because there's no property with the same name in the original object
            assertNull(result.nameStr)

            assertEquals("surnameTest", result.surname)
            assertEquals(87, result.age)
        }

        @Test
        fun `map from one object to another with the MapFrom annotation, but failing to map and using the null fallback strategy`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<MapFromClasses.PersonNullFallbackDTO>()

            assertNotNull(result)
            assertNull(result.nameStr)

            // The property is null even if there's a property with the same name in the original object, because
            //  the MapFrom annotation has the option MappingFallback.NULL
            assertNull(result.surname)

            assertEquals(87, result.age)
        }

        @Test
        fun `map from a compound object to another with the MapFrom annotation`() {
            val reservation = Reservation(
                room = Room(
                    guest = Person(1, "nameTest", "surnameTest", 76),
                    roomName = "roomNameTest"),
                description = "descriptionTest",
            )

            val result = reservation.mapTo<MapFromClasses.RoomFallbackDTO>()

            assertNotNull(result)

            assertEquals("nameTest", result.name)
            assertEquals("descriptionTest", result.description)
        }

        @Test
        fun `map from one object to another with the MapFrom annotation, but only the last location is valid`() {
            val room = Room(
                guest = Person(1, "nameTest", "surnameTest", 87),
                roomName = "roomNameTest",
            )

            val result = room.mapTo<MapFromClasses.RoomDTO>()

            assertNotNull(result)

            assertEquals("nameTest", result.guestName)
            assertNull(result.guestAge)
            assertEquals("roomNameTest", result.roomName)
        }

        @Test
        fun `map from one object with a collection to another with the MapFrom annotation`() {
            val user = User(
                addresses = listOf(
                    "addr1",
                    "addr2",
                ),
                age = 12,
            )

            val result = user.mapTo<MapFromClasses.UserDTO>()

            assertNotNull(result)
            assertNotNull(result.addrs)

            // The mapper will map all the items of any collection automatically
            assertEquals(2, result.addrs.size)
            assertEquals("addr1", result.addrs.toList()[0] as String)
            assertEquals("addr2", result.addrs.toList()[1] as String)

            assertEquals(12, result.age)
        }

        @Test
        fun `map from one object with a compound collection to another with the MapFrom annotation`() {
            val hotel = Hotel(
                hotelRooms = setOf(
                    Room(Person(23, "nameTest", "surnameTest", 88), "roomName1"),
                    Room(Person(25, "nameTest2", "surnameTest2", 89), "roomName2"),
                ),
                numberOfGuests = 12,
            )

            val addFiveToInt : (Int) -> Int = {
                it + 5
            }

            val result = hotel.mapTo<MapFromClasses.HotelDTO>(functionMappings = mapOf(
                Hotel::numberOfGuests to (addFiveToInt to (MapFromClasses.HotelDTO::numberOfGuests to MappingFallback.NULL)),
                Hotel::hotelRooms to ({ rooms: List<Room> ->
                    rooms.map { MapFromClasses.RoomDTO(it.guest.name, it.guest.age.toString(), it.roomName) }
                } to (MapFromClasses.HotelDTO::roomDTOs to MappingFallback.NULL)),
            ))

            assertNotNull(result)
            assertNotNull(result.roomDTOs)

            assertEquals(2, result.roomDTOs.size)
            assertEquals("roomName1", result.roomDTOs[0].roomName)
            assertEquals("nameTest", result.roomDTOs[0].guestName)
            assertEquals("88", result.roomDTOs[0].guestAge)
            assertEquals("roomName2", result.roomDTOs[1].roomName)
            assertEquals("nameTest2", result.roomDTOs[1].guestName)
            assertEquals("89", result.roomDTOs[1].guestAge)

            assertEquals(17, result.numberOfGuests)
        }
    }

    // TODO:
    //  -x Crear test que mappee un original entity con un campo que sea un objeto con otro dentro, y que
    //    que el target type tengo el mismo campo con un objeto con otro dentro de los mismo tipos, agregar mappings de
    //    un campo de el primer objeto, y otro mapping de un subcampo del campo
    //  - Crear un test igual al anterior, pero con una exclusion de un campo, y una exclusion de un subcampo
    //    de otro campo no excluido
    //  - Crear un test como el primero, pero donde los campos del original entity y target class tengan diferente
    //  nombre y se mapeen tambien mediante un mapping
    //  - Mismo a lo anterior pero con el mapfrom
    //  - Mismo caso a lo anterior, pero con un house null en el original entity, y mappings que necesiten crear un house
}