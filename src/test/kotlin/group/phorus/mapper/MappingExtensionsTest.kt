package group.phorus.mapper

import group.phorus.mapper.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

// TODO:
//  Add a test checking that mappings and custom mappings don't work if the field is excluded
//  Support vararg params
//  Add a performance test comparing the mapping speed of the library with jackson convertValue function
//  Add subfield tests and subfield exclusion tests

@Suppress("ClassName")
internal class MappingExtensionsTest {

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
            val person = Person(23, "nameTest", "surnameTest", 87)

            val result = person.mapTo<PersonDTO>(mappings = mapOf(Person::age to (PersonDTO::nameStr to MappingFallback.NULL)))

            assertNotNull(result)

            // If the mapper cannot automatically map the original property to the target one, it will leave it as null
            assertNull(result.nameStr)

            assertEquals("surnameTest", result.surname)
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
        fun `map from a string to an int`() {
            val result = "10".mapTo<Int>()

            assertNotNull(result)

            // The property is mapped to an int
            assertEquals(10, result)
        }
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
            val person = Person(23, "nameTest", "surnameTest", 87)
            val person2 = Person(name = "nameTest2")

            val result = person.updateFrom(person2)

            assertNotNull(result)

            // The result is still the same instance
            assertTrue(result === person)

            assertEquals(23, result.id)
            assertEquals("nameTest2", result.name) // Only the name field was updated
            assertEquals("surnameTest", result.surname)
            assertEquals(87, result.age)
        }

        @Test
        fun `update an object from another object with setters only = false`() {
            val person = Person(23, "nameTest", "surnameTest", 87)
            val person2 = Person(name = "nameTest2")

            val result = person.updateFrom(person2, useSettersOnly = false)

            assertNotNull(result)

            // The result is no longer the same instance, since it was built using a constructor
            assertTrue(result != person)

            assertEquals(23, result.id)
            assertEquals("nameTest2", result.name) // Only the name field was updated
            assertEquals("surnameTest", result.surname)
            assertEquals(87, result.age)
        }

        @Test
        fun `update an object from another object with the update option set_nulls`() {
            val person = Person(23, "nameTest", "surnameTest", 87)
            val person2 = Person(name = "nameTest2")

            val result = person.updateFrom(person2, UpdateOption.SET_NULLS)

            assertNotNull(result)

            // The result is still the same instance
            assertTrue(result === person)

            // Since we are using the SET_NULL options, nulls are no longer ignored thus every field is updated
            assertNull(result.id)
            assertEquals("nameTest2", result.name)
            assertNull(result.surname)
            assertNull(result.age)
        }

        @Test
        fun `update a collection of objects from another object`() {
            val persons = listOf(Person(23, "nameTest", "surnameTest", 87))
            val person2 = Person(name = "nameTest2")

            val result = persons.updateFrom(person2)

            assertNotNull(result)

            // The result list still has the same instances
            assertTrue(result.first() === persons.first())

            assertEquals(23, result.first().id)
            assertEquals("nameTest2", result.first().name) // Only the name field was updated
            assertEquals("surnameTest", result.first().surname)
            assertEquals(87, result.first().age)
        }

        @Test
        fun `update a collection of objects from another collection`() {
            val persons = listOf(Person(23, "nameTest", "surnameTest", 87))
            val person2 = listOf(Person(name = "nameTest2"))

            val result = persons.updateFrom(person2)

            assertNotNull(result)

            // The result list still has the same instances
            assertTrue(result.first() === persons.first())

            assertEquals(23, result.first().id)

            // The mapper doesn't update collections of objects from other collections, because it makes no sense
            assertEquals("nameTest", result.first().name)
            assertEquals("surnameTest", result.first().surname)
            assertEquals(87, result.first().age)
        }

        @Test
        fun `update a collection of objects from another object with setters only = false`() {
            val persons = listOf(Person(23, "nameTest", "surnameTest", 87))
            val person2 = Person(name = "nameTest2")

            val result = persons.updateFrom(person2, useSettersOnly = false)

            assertNotNull(result)

            // The result list doesn't have the same instances, since it was built using a constructor
            assertTrue(result.first() != persons.first())

            assertEquals(23, result.first().id)
            assertEquals("nameTest2", result.first().name) // Only the name field was updated
            assertEquals("surnameTest", result.first().surname)
            assertEquals(87, result.first().age)
        }

        @Test
        fun `update a collection of objects from another object with the update option set_nulls`() {
            val persons = listOf(Person(23, "nameTest", "surnameTest", 87))
            val person2 = Person(name = "nameTest2")

            val result = persons.updateFrom(person2, UpdateOption.SET_NULLS)

            assertNotNull(result)

            // The result list still has the same instances
            assertTrue(result.first() === persons.first())

            // Since we are using the SET_NULL options, nulls are no longer ignored thus every field is updated
            assertNull(result.first().id)
            assertEquals("nameTest2", result.first().name)
            assertNull(result.first().surname)
            assertNull(result.first().age)
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

    @Nested
    inner class `Compound tests` {

        @Test
        fun `map from one compound object to another`() {
            val room = Room(
                guest = Person(23, "nameTest", "surnameTest"),
                roomName = "roomNameTest",
            )

            val result = room.mapTo<RoomDTO>()

            assertNotNull(result)
            assertNotNull(result.guest)

            // The mapper will try to map any properties automatically
            assertEquals("roomNameTest", result.roomName)
            assertEquals("surnameTest", result.guest!!.surname)
        }

        @Test
        fun `map from one double compound object to another`() {
            val wifi = Wifi(
                room = Room(
                    guest = Person(23, "nameTest", "surnameTest"),
                    roomName = "roomNameTest",
                ),
                wifiPassword = 12,
            )

            val result = wifi.mapTo<WifiDTO>()

            assertNotNull(result)
            assertNotNull(result.room)
            assertNotNull(result.room!!.guest)

            // The mapper works recursively, so properties of properties will also be mapped
            assertEquals(12, result.wifiPassword)
            assertEquals("roomNameTest", result.room!!.roomName)
            assertEquals("surnameTest", result.room!!.guest!!.surname)
        }

        @Test
        fun `map from one double compound object to another with a function`() {
            val wifi = Wifi(
                room = Room(
                    guest = Person(23, "nameTest", "surnameTest"),
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
            assertNotNull(result.room!!.guest)

            assertEquals(17, result.wifiPassword)
            assertEquals("roomNameTest", result.room!!.roomName)
            assertEquals("surnameTest", result.room!!.guest!!.surname)
        }
    }


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