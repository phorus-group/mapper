package group.phorus.mapper.model

import group.phorus.mapper.MapFrom
import group.phorus.mapper.MappingFallback


/**
 * A nullable property and a non-nullable property are treated in the same way,
 *  so name should be mapped to nameStr without problems
 */
class Person(
    var id: Long? = null,
    var name: String,
    var surname: String? = null,
    var age: Int? = null
)

/**
 * The target class must have a no args constructor and all properties that want to be mapped must be var
  */
class PersonDTO(
    var nameStr: String? = null,
    var ageStr: String? = null,
    var surname: String? = null
)


class Room(
    var guest: Person? = null,
    var roomName: String? = null,
)

class RoomDTO(
    var guest: PersonDTO? = null,
    var roomName: String? = null,
)


class Wifi(
    var room: Room? = null,
    var wifiPassword: Int? = null,
)

class WifiDTO(
    var room: RoomDTO? = null,
    var wifiPassword: Int? = null,
)

/**
 * Any collection of the original object will work with the mapper, since the target
 *  object uses Any as its collection type
  */
class User(
    var addresses: List<String>? = null,
    var age: Int? = null,
)

/**
 * Only mutable collections of the target object will work with the mapper
 */
class UserDTO(
    var addresses: MutableSet<Any>? = null,
    var age: Int? = null,
)


class Hotel(
    var hotelRooms: Set<Room>? = null,
    var numberOfGuests: Int? = null,
)

data class HotelDTO(
    var hotelRooms: MutableList<RoomDTO>? = null,
    var numberOfGuests: Int? = null,
)


/**
 * The MapFrom annotation will only work on the target objects
  */
class PersonWAnnotationDTO(

    @field:MapFrom(["name"])
    var nameStr: String? = null,

    var surname: String? = null,
    var age: Int? = null,
)

class PersonWAnnotationFallbackDTO(
    var nameStr: String? = null,

    @field:MapFrom(["isGoingToFail"])
    var surname: String? = null,
    var age: Int? = null,
)

class PersonWAnnotationNullFallbackDTO(
    var nameStr: String? = null,

    @field:MapFrom(["isGoingToFail"], MappingFallback.NULL)
    var surname: String? = null,
    var age: Int? = null,
)

class Reservation(
    var room: Room? = null,
    var description: String? = null
)

class RoomWAnnotationDTO(
    @field:MapFrom(["guest"])
    var guestDTO: PersonWAnnotationDTO? = null,
    var roomName: String? = null,
)

class RoomWAnnotation2DTO(
    @field:MapFrom(["guest.name"])
    var guestName: String? = null,
    var roomName: String? = null,
)

/**
 * Only the first valid location will be used
 * If none of the locations is being able to parse anything, then the field will use the fallback
 */
class RoomW3AnnotationsDTO(
    @field:MapFrom(["isGoingToFail", "alsoGoingToFail", "guest.name"])
    var guestName: String? = null,
    var roomName: String? = null,
)

/**
 * If the MappingFallback is set to NULL, then if all the annotations fail the property will be
 *  set to null instead of normally mapped
 */
class RoomW3AnnotationsFallbackDTO(
    @field:MapFrom(["room.guest.name"], MappingFallback.NULL)
    var name: String? = null,
    var description: String? = null,
)

/**
 * Only mutable collections of the target object will work with the mapper
 */
class UserWAnnotationDTO(
    @field:MapFrom(["addresses"])
    var addrs: MutableSet<Any>? = null,
    var age: Int? = null,
)

data class HotelWAnnotationDTO(
    @field:MapFrom(["hotelRooms"])
    var roomDTOs: MutableList<RoomDTO>? = null,
    var numberOfGuests: Int? = null,
)