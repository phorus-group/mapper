package group.phorus.mapper

import group.phorus.mapper.model.Person
import group.phorus.mapper.model.PersonDTO
import group.phorus.mapper.model.PersonWAnnotationDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

// TODO: Add updateFrom method to replace the "baseObject" property in the mapTo function
//  Add a test checking that mappings and custom mappings don't work if the field is excluded
//  Support vararg params
//  Add a performance test comparing the mapping speed of the library with jackson convertValue function

internal class MappingExtensionsTest {

    @Nested
    inner class `Normal tests` {

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
        fun `map from one object to another ignoring a property`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(exclusions = listOf("surname"))

            assertNotNull(result)

            // Because of the exclusion, the property is null even if it has the same name as a property in the original object
            assertNull(result.surname)

            assertNull(result.nameStr)
            assertNull(result.ageStr)
        }

        @Test
        fun `map from one object to another manually mapping a property`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            assertNotNull(result)

            // The mapper will use the custom mappings to map from an original property to a custom target property
            assertEquals("nameTest", result.nameStr)

            assertEquals("surnameTest", result.surname)
        }

        @Test
        fun `map from one object to another manually mapping a property with the wrong type`() {
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(mappings = mapOf("age" to ("nameStr" to MappingFallback.NULL)))

            assertNotNull(result)

            // If the mapper cannot automatically map the original property to the target one, it will leave it as null
            assertNull(result.nameStr)

            assertEquals("surnameTest", result.surname)
        }

        // TODO: Test native mappings: string to int/long/etc; int/long/etc to string; int to long, double to int, etc

        @Test // TODO: Delete if repeated
        fun `test collections`() {
            val persons = listOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<List<PersonDTO>>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            assertNotNull(result)

            assertEquals(2, result.size)

            assertEquals("nameTest1", result[0].nameStr)
            assertEquals("surnameTest1", result[0].surname)
            assertNull(result[0].ageStr)
            assertEquals("nameTest2", result[1].nameStr)
            assertEquals("surnameTest2", result[1].surname)
            assertNull(result[1].ageStr)
        }

        @Test // TODO: Delete if repeated
        fun `test collections with sets`() {
            val persons = setOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<Set<PersonDTO>>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))
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

        @Test // TODO: Delete if repeated
        fun `test collections with arrays - not supported`() {
            val persons = arrayOf(
                Person(23, "nameTest1", "surnameTest1", 87),
                Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<Array<PersonDTO>>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            // Arrays are not supported, avoid using them
            assertNull(result)
        }

        @Test // TODO: Delete if repeated
        fun `test maps`() {
            val persons = mapOf(
                "0" to Person(23, "nameTest1", "surnameTest1", 87),
                "1" to Person(24, "nameTest2", "surnameTest2", 88),
            )

            val result = persons.mapTo<Map<String, PersonDTO>>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            assertNotNull(result)

            assertEquals(2, result.size)

            assertEquals("nameTest1", result["0"]?.nameStr)
            assertEquals("surnameTest1", result["0"]?.surname)
            assertNull(result["0"]?.ageStr)
            assertEquals("nameTest2", result["1"]?.nameStr)
            assertEquals("surnameTest2", result["1"]?.surname)
            assertNull(result["1"]?.ageStr)
        }

        @Test // TODO: Delete if repeated
        fun `test pair`() {
            val person = "0" to Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<Pair<String, PersonDTO>>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            assertNotNull(result)

            assertEquals("0", result.first)
            assertEquals("nameTest", result.second.nameStr)
            assertEquals("surnameTest", result.second.surname)
            assertNull(result.second.ageStr)
        }

        @Test // TODO: Delete if repeated
        fun `test triple`() {
            val person = Triple("0", 5, Person(23, "nameTest", "surnameTest", 87))

            val result = person.mapTo<Triple<String, Long, PersonDTO>>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            assertNotNull(result)

            assertEquals("0", result.first)
            assertEquals(5, result.second)
            assertEquals("nameTest", result.third.nameStr)
            assertEquals("surnameTest", result.third.surname)
            assertNull(result.third.ageStr)
        }

        @Test // TODO: Delete if repeated
        fun `test composite origin entity to non-composite target class`() {
            val person = Triple("0", 5, Person(23, "nameTest", "surnameTest", 87))

            val result = person.mapTo<String>(mappings = mapOf("name" to ("nameStr" to MappingFallback.NULL)))

            // The mapper cannot map a composite original entity to a non-composite target class
            assertNull(result)
        }

        @Test // TODO: Delete if repeated
        fun `test update from`() {
            val person = Person(23, "nameTest", "surnameTest", 87)
            val person2 = Person(name = "nameTest2")

            val result = person.updateFrom(person2)

            assertNotNull(result)

            assertTrue(result === person)

            assertEquals(23, result.id)
            assertEquals("nameTest2", result.name)
            assertEquals("surnameTest", result.surname)
            assertEquals(87, result.age)
        }

        @Test // TODO: Delete if repeated
        fun `test update from without setters only`() {
            val person = Person(23, "nameTest", "surnameTest", 87)
            val person2 = Person(name = "nameTest2")

            val result = person.updateFrom(person2, useSettersOnly = false)

            assertNotNull(result)

            assertTrue(result != person)

            assertEquals(23, result.id)
            assertEquals("nameTest2", result.name)
            assertEquals("surnameTest", result.surname)
            assertEquals(87, result.age)
        }

        @Test // TODO: Delete if repeated
        fun `test update from with update option set_nulls`() {
            val person = Person(23, "nameTest", "surnameTest", 87)
            val person2 = Person(name = "nameTest2")

            val result = person.updateFrom(person2, UpdateOption.SET_NULLS)

            assertNotNull(result)

            assertTrue(result === person)

            assertNull(result.id)
            assertEquals("nameTest2", result.name)
            assertNull(result.surname)
            assertNull(result.age)
        }

        // TODO: Create compound tests for the last 2 tests

//        @Test
//        fun `map from one object to another manually mapping a nonexistent property`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            val result = person.mapTo(PersonDTO::class, mappings = mapOf("abcdef" to "nameStr"))
//
//            assertNotNull(result)
//
//            // If the mapper cannot map the original property to the target one, it will leave it as null
//            assertNull(result.nameStr)
//
//            assertEquals("surnameTest", result.surname)
//        }
//
//        @Test
//        fun `map from one object to another manually mapping a property with a function`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            // The function uses the original property as input, and puts the returned value in the target property
//            val parseManually : (Int) -> String = {
//                (it + 5).toString()
//            }
//
//            val result = person.mapTo(
//                PersonDTO::class,
//                customMappings = mapOf("age" to (parseManually to "ageStr")),
//            )
//
//            assertNotNull(result)
//
//            // The property contains the returned value of the function
//            assertEquals("92", result.ageStr)
//
//            assertEquals("surnameTest", result.surname)
//        }
//
//        // TODO: Add 2 tests testing the custom mapping functions mapping automatically
//        //  the input and output of the function correctly
//        @Test
//        fun `map from one object to another manually mapping an original property with a function using the wrong input type`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            val parseManually : (Int) -> String = {
//                (it + 5).toString()
//            }
//
//            val result = person.mapTo(
//                PersonDTO::class,
//                customMappings = mapOf("name" to (parseManually to "ageStr")),
//            )
//
//            assertNotNull(result)
//
//            // If the mapper cannot automatically map the input of the function to the expected one,
//            //  it will leave the target property as null
//            assertNull(result.ageStr)
//
//            assertEquals("surnameTest", result.surname)
//        }
//
//        @Test
//        fun `map from one object to another manually mapping a target property with a function using the wrong output type`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            val parseManually : (Int) -> Int = {
//                it + 5
//            }
//
//            val result = person.mapTo(
//                PersonDTO::class,
//                customMappings = mapOf("age" to (parseManually to "ageStr")),
//            )
//
//            assertNotNull(result)
//
//            // If the mapper cannot automatically map the output of the function to the expected one,
//            //  it will leave the target property as null
//            assertNull(result.ageStr)
//
//            assertEquals("surnameTest", result.surname)
//        }
//
//        @Test
//        fun `map from one object to another manually mapping a nonexistent property with a function`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            val parseManually : (Int) -> String = {
//                (it + 5).toString()
//            }
//
//            val result = person.mapTo(
//                PersonDTO::class,
//                customMappings = mapOf("abcdef" to (parseManually to "ageStr")),
//            )
//
//            assertNotNull(result)
//
//            // If the mapper cannot map the original property to the target one, it will leave it as null
//            assertNull(result.ageStr)
//
//            assertEquals("surnameTest", result.surname)
//        }
//
//        @Test
//        fun `map from one object to another manually mapping a property with a function and without a function at the same time`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            val parseManually : (String) -> String = {
//                it.uppercase()
//            }
//
//            val result = person.mapTo(
//                PersonDTO::class,
//                mappings = mapOf("name" to "nameStr"),
//                customMappings = mapOf("name" to (parseManually to "nameStr")),
//            )
//
//            assertNotNull(result)
//
//            // The customMappings field has priority over the normal mappings
//            assertEquals("NAMETEST", result.nameStr)
//
//            assertEquals("surnameTest", result.surname)
//        }
//
//        @Test
//        fun `map from one object to another manually mapping a property with a function, other one without a function, and excluding one property`() {
//            val person = Person(23, "nameTest", "surnameTest", 87)
//
//            val parseManually : (Int) -> String = {
//                (it + 5).toString()
//            }
//
//            val result = person.mapTo(
//                PersonDTO::class,
//                exclusions = listOf("surname"),
//                mappings = mapOf("name" to "nameStr"),
//                customMappings = mapOf("age" to (parseManually to "ageStr")),
//            )
//
//            assertNotNull(result)
//
//            assertNull(result.surname)
//            assertEquals("nameTest", result.nameStr)
//            assertEquals("92", result.ageStr)
//        }
    }

//
//    @Nested
//    inner class `Compound tests` {
//
//        @Test
//        fun `map from one compound object to another`() {
//            val room = Room(
//                guest = Person(23, "nameTest", "surnameTest"),
//                roomName = "roomNameTest",
//            )
//
//            val result = room.mapTo(RoomDTO::class)
//
//            assertNotNull(result)
//            assertNotNull(result.guest)
//
//            // The mapper will try to map any non-native properties automatically
//            assertEquals("roomNameTest", result.roomName)
//            assertEquals("surnameTest", result.guest!!.surname)
//        }
//
//        @Test
//        fun `map from one double compound object to another`() {
//            val wifi = Wifi(
//                room = Room(
//                    guest = Person(23, "nameTest", "surnameTest"),
//                    roomName = "roomNameTest",
//                ),
//                wifiPassword = 12,
//            )
//
//            val result = wifi.mapTo(WifiDTO::class)
//
//            assertNotNull(result)
//            assertNotNull(result.room)
//            assertNotNull(result.room!!.guest)
//
//            // The mapper works recursively, so properties of properties will also be mapped
//            assertEquals(12, result.wifiPassword)
//            assertEquals("roomNameTest", result.room!!.roomName)
//            assertEquals("surnameTest", result.room!!.guest!!.surname)
//        }
//
//        // TODO: implement and test a function to be executed inside a prop, like "room.guest.age" to "room.guest.age",
//        //  "room.guest.age" to "room.roomName" and "room.roomName" to "room.guest.name"
//        @Test
//        fun `map from one double compound object to another with a function`() {
//            val wifi = Wifi(
//                room = Room(
//                    guest = Person(23, "nameTest", "surnameTest"),
//                    roomName = "roomNameTest",
//                ),
//                wifiPassword = 12,
//            )
//
//            val addFiveToInt : (Int) -> Int = {
//                it + 5
//            }
//
//            val result = wifi.mapTo(
//                WifiDTO::class,
//                customMappings = mapOf("wifiPassword" to (addFiveToInt to "wifiPassword")),
//            )
//
//            assertNotNull(result)
//            assertNotNull(result.room)
//            assertNotNull(result.room!!.guest)
//
//            assertEquals(17, result.wifiPassword)
//            assertEquals("roomNameTest", result.room!!.roomName)
//            assertEquals("surnameTest", result.room!!.guest!!.surname)
//        }
//    }
//
//
//    @Nested
//    inner class `Collection tests` {
//
//        @Test
//        fun `map from one object with a collection to another`() {
//            val user = User(
//                addresses = listOf(
//                    "addr1",
//                    "addr2",
//                ),
//                age = 12,
//            )
//
//            val result = user.mapTo(UserDTO::class)
//
//            assertNotNull(result)
//            assertNotNull(result.addresses)
//
//            // The mapper will map all the items of any collection automatically
//            assertEquals(2, result.addresses!!.size)
//            assertEquals("addr1", result.addresses!!.toList()[0] as String)
//            assertEquals("addr2", result.addresses!!.toList()[1] as String)
//
//            assertEquals(12, result.age)
//        }
//
//        @Test
//        fun `map from one object with a collection of objects to another with a function`() {
//            val hotel = Hotel(
//                hotelRooms = setOf(
//                    Room(Person(23, "nameTest", "surnameTest"), "roomName1"),
//                    Room(Person(25, "nameTest2", "surnameTest2"), "roomName2"),
//                ),
//                numberOfGuests = 2,
//            )
//
//            val addFiveToInt : (Int) -> Int = {
//                it + 5
//            }
//
//            val result = hotel.mapTo(
//                HotelDTO::class,
//                customMappings = mapOf("numberOfGuests" to (addFiveToInt to "numberOfGuests")),
//            )
//
//            assertNotNull(result)
//            assertNotNull(result.hotelRooms)
//
//            // The mapper will map all the items of any collection automatically
//            assertEquals(2, result.hotelRooms!!.size)
//            assertEquals("roomName1", result.hotelRooms!![0].roomName)
//            assertEquals("surnameTest", result.hotelRooms!![0].guest?.surname)
//            assertEquals("roomName2", result.hotelRooms!![1].roomName)
//            assertEquals("surnameTest2", result.hotelRooms!![1].guest?.surname)
//
//            assertEquals(7, result.numberOfGuests)
//        }
//
//        @Test
//        fun `map from one object to another with a function returning a collection that needs to be automatically mapped`() {
//            val hotel = Hotel(
//                hotelRooms = setOf(
//                    Room(Person(23, "nameTest", "surnameTest"), "roomName1"),
//                    Room(Person(25, "nameTest2", "surnameTest2"), "roomName2"),
//                ),
//                numberOfGuests = 2,
//            )
//
//            val functionReturningColl : (Set<Room>) -> Set<Room> = {
//                it
//            }
//
//            val result = hotel.mapTo(
//                HotelDTO::class,
//                customMappings = mapOf("hotelRooms" to (functionReturningColl to "hotelRooms")),
//            )
//
//            assertNotNull(result)
//            assertNotNull(result.hotelRooms)
//
//            // The mapper can also automatically map the output of functions even if they are collections
//            assertEquals(2, result.hotelRooms!!.size)
//            assertEquals("roomName1", result.hotelRooms!![0].roomName)
//            assertEquals("surnameTest", result.hotelRooms!![0].guest?.surname)
//            assertEquals("roomName2", result.hotelRooms!![1].roomName)
//            assertEquals("surnameTest2", result.hotelRooms!![1].guest?.surname)
//
//            assertEquals(2, result.numberOfGuests)
//        }
//
//        @Test
//        fun `map from one object to another with a function using a collection that needs to be automatically mapped`() {
//            val hotel = Hotel(
//                setOf(
//                    Room(Person(23, "nameTest", "surnameTest"), "roomName1"),
//                    Room(Person(25, "nameTest2", "surnameTest2"), "roomName2"),
//                ),
//                2,
//            )
//
//            val functionReturningColl : (Set<RoomDTO>) -> Set<RoomDTO> = {
//                it
//            }
//
//            val result = hotel.mapTo(
//                HotelDTO::class,
//                customMappings = mapOf("hotelRooms" to (functionReturningColl to "hotelRooms")),
//            )
//
//            assertNotNull(result)
//            assertNotNull(result.hotelRooms)
//
//            // The mapper can also automatically map the input of functions even if they are collections
//            assertEquals(2, result.hotelRooms!!.size)
//            assertEquals("roomName1", result.hotelRooms!![0].roomName)
//            assertEquals("surnameTest", result.hotelRooms!![0].guest?.surname)
//            assertEquals("roomName2", result.hotelRooms!![1].roomName)
//            assertEquals("surnameTest2", result.hotelRooms!![1].guest?.surname)
//
//            assertEquals(2, result.numberOfGuests)
//        }
//    }
//
//
    @Nested
    inner class `MapFrom annotation tests` {

        @Nested
        inner class `Normal tests` {

            @Test
            fun `map from one object to another relying in the MapFrom annotation`() {
                val person = Person(23, "nameTest", "surnameTest", 87)

                val result = person.mapTo<PersonWAnnotationDTO>()

                assertNotNull(result)

                // The mapper used the content of the MapFrom annotation
                assertEquals("nameTest", result.nameStr)

                assertEquals("surnameTest", result.surname)
                assertEquals(87, result.age)
            }
        }
//
//
//        @Nested
//        inner class `Fallback tests` {
//
//            @Test
//            fun `map from one object to another relying in the MapFrom annotation, but failing to map and relying on normal mapping`() {
//                val person = Person(23, "nameTest", "surnameTest", 87)
//
//                val result = person.mapTo(PersonWAnnotationFallbackDTO::class)
//
//                assertNotNull(result)
//
//                // The property is null because there's no property with the same name in the original object
//                assertNull(result.nameStr)
//
//                assertEquals("surnameTest", result.surname)
//                assertEquals(87, result.age)
//            }
//
//            @Test
//            fun `map from one object to another relying in the MapFrom annotation, but failing to map and using the null fallback strategy`() {
//                val person = Person(23, "nameTest", "surnameTest", 87)
//
//                val result = person.mapTo(PersonWAnnotationNullFallbackDTO::class)
//
//                assertNotNull(result)
//                assertNull(result.nameStr)
//
//                // The property is null even if there's a property with the same name in the original object, because
//                //  the MapFrom annotation has the option MappingFallback.NULL
//                assertNull(result.surname)
//
//                assertEquals(87, result.age)
//            }
//        }
//
//
//        @Nested
//        inner class `Compound tests` {
//
//            @Test
//            fun `map from one compound object to another relying in the MapFrom annotation`() {
//                val room = Room(
//                    guest = Person(1, "nameTest", "surnameTest", 87),
//                    roomName = "roomNameTest",
//                )
//
//                val result = room.mapTo(RoomWAnnotationDTO::class)
//
//                assertNotNull(result)
//                assertNotNull(result.guestDTO)
//
//                assertEquals("nameTest", result.guestDTO!!.nameStr)
//                assertEquals("roomNameTest", result.roomName)
//            }
//
//            @Test
//            fun `map from one compound object to another relying in a compound location in the MapFrom annotation`() {
//                val room = Room(
//                    guest = Person(1, "nameTest", "surnameTest", 87),
//                    roomName = "roomNameTest",
//                )
//
//                val result = room.mapTo(RoomWAnnotation2DTO::class)
//
//                assertNotNull(result)
//
//                assertEquals("nameTest", result.guestName)
//                assertEquals("roomNameTest", result.roomName)
//            }
//
//            @Test
//            fun `map from one double compound object to another relying in a compound location in the MapFrom annotation`() {
//                val reservation = Reservation(
//                    room = Room(
//                        guest = Person(1, "nameTest", "surnameTest", 76),
//                        roomName = "roomNameTest"),
//                    description = "descriptionTest",
//                )
//
//                val result = reservation.mapTo(RoomW3AnnotationsFallbackDTO::class)
//
//                assertNotNull(result)
//
//                assertEquals("nameTest", result.name)
//                assertEquals("descriptionTest", result.description)
//            }
//        }
//
//
//        @Nested
//        inner class `Locations tests` {
//
//            @Test
//            fun `map from one object to another relying in the MapFrom annotation, but only the last location is valid`() {
//                val room = Room(
//                    guest = Person(1, "nameTest", "surnameTest", 87),
//                    roomName = "roomNameTest",
//                )
//
//                val result = room.mapTo(RoomW3AnnotationsDTO::class)
//
//                assertNotNull(result)
//
//                assertEquals("nameTest", result.guestName)
//                assertEquals("roomNameTest", result.roomName)
//            }
//        }
//
//
//        @Nested
//        inner class `Collections tests` {
//
//            @Test
//            fun `map from one object with a collection to another relying in the MapFrom annotation`() {
//                val user = User(
//                    addresses = listOf(
//                        "addr1",
//                        "addr2",
//                    ),
//                    age = 12,
//                )
//
//                val result = user.mapTo(UserWAnnotationDTO::class)
//
//                assertNotNull(result)
//                assertNotNull(result.addrs)
//
//                // The mapper will map all the items of any collection automatically
//                assertEquals(2, result.addrs!!.size)
//                assertEquals("addr1", result.addrs!!.toList()[0] as String)
//                assertEquals("addr2", result.addrs!!.toList()[1] as String)
//
//                assertEquals(12, result.age)
//            }
//
//            @Test
//            fun `map from one object with a compound collection to another relying in the MapFrom annotation`() {
//                val hotel = Hotel(
//                    hotelRooms = setOf(
//                        Room(Person(23, "nameTest", "surnameTest"), "roomName1"),
//                        Room(Person(25, "nameTest2", "surnameTest2"), "roomName2"),
//                    ),
//                    numberOfGuests = 12,
//                )
//
//                val addFiveToInt : (Int) -> Int = {
//                    it + 5
//                }
//
//                val result = hotel.mapTo(
//                    HotelWAnnotationDTO::class,
//                    customMappings = mapOf("numberOfGuests" to (addFiveToInt to "numberOfGuests")),
//                )
//
//                assertNotNull(result)
//                assertNotNull(result.roomDTOs)
//
//                assertEquals(2, result.roomDTOs!!.size)
//                assertEquals("roomName1", result.roomDTOs!![0].roomName)
//                assertEquals("surnameTest", result.roomDTOs!![0].guest?.surname)
//                assertEquals("roomName2", result.roomDTOs!![1].roomName)
//                assertEquals("surnameTest2", result.roomDTOs!![1].guest?.surname)
//
//                assertEquals(17, result.numberOfGuests)
//            }
//        }
    }
}