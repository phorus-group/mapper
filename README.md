# Phorus Mapper

<a href='https://gitlab.com/phorus-group/public/development/libraries/mapper/-/pipelines?ref=main'><img src='https://gitlab.com/phorus-group/public/development/libraries/mapper/badges/main/pipeline.svg'></a>
<a href='https://gitlab.com/phorus-group/public/development/libraries/mapper/-/pipelines?ref=main'><img src='https://gitlab.com/phorus-group/public/development/libraries/mapper/badges/main/coverage.svg'></a>
[![GitLab license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/group.phorus/mapper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/group.phorus/mapper)

Mapper that allows you to transform an object to another, or update an object based on the properties of another.

The conversion is done through reflection and will map any properties with the same name in original object and 
the target class.
It will also consider extra options like: 
`exclusions`, `mappings`, `functionMappings`, and `@MapFrom` annotations.

All the functionality can be accessed via the following extension functions: `Any.mapTo<Type>()`, `Any.mapToClass<Type>()`, 
`Any.updateFrom(object)`, and `Any.updateFromObject(object)`. To learn more, see [Getting started](#getting-started).

### Notes

> The project runs a vulnerability analysis pipeline regularly, 
> any found vulnerabilities will be fixed as soon as possible.

> The project dependencies are being regularly updated by [Renovate](https://gitlab.com/phorus_renovate).
> Dependency updates that don't break tests will be automatically deployed with an updated patch version.

> The project has been thoroughly tested to ensure that it is safe to use in a production environment.
> Currently, there are more than 100 tests validating all the functionality.

> **Warning**: The project is made to be used with Kotlin code.
> Using it with Java code, although it may work, is **not officially supported**.

## Table of contents
- [Features](#features)
- [Getting started](#getting-started)
    - [Installation](#installation)
    - [Usage](#usage)
    - [Advanced usage](#advanced-usage)
      - [Exclusions](#exclusions)
      - [Mappings](#mappings)
      - [Function mappings](#function-mappings)
      - [@MapFrom annotation](#mapfrom-annotation)
      - [Primitives](#primitives)
      - [Composites: Iterable, Map, Pair, Triple](#composites-iterable-map-pair-triple)
- [Building and Contributing](#building-and-contributing)
- [Authors and acknowledgment](#authors-and-acknowledgment)

***

## Features

- Allows you to transform an object to another type using reflection:
  - Define `exclusions` to exclude fields you are not interested in mapping.
  - Define `mappings` to map two fields with different names.
  - Define  `functionMappings` to map two fields with different names through a mutating function.
  - Map fields based on what is defined in an `@MapFrom` annotation.
  - Map `Number` and `String` primitives between each other if necessary.
  - Supports `Collection`, `Map`, `Pair` and `Triple` classes.
- Allows you to update the properties of an object based on the properties of another:
  - The objects can have different types.
  - Define `UpdateOption.IGNORE_NULLS` to ignore the null properties of the second object, 
    or `UpdateOption.SET_NULLS` to take them into account and set them in the base object.
    in the base object.
  - By default, it only uses setters to update the base object.
  - Define`exclusions`, `mappings`, `functionMappings`, and `@MapFrom` annotations for more advanced usage.
  - Supports mapping primitives like `Number` and `String`, as well as `Collections`, `Map`, `Pair` and `Triple` classes.

## Getting started

### Installation

Make sure that the `mavenCentral` (or any of its mirrors) is added to the repository list of the project.

Binaries and dependency information for Maven and Gradle can be found at [http://search.maven.org](https://search.maven.org/search?q=g:group.phorus%20AND%20a:mapper).

<details open>
<summary>Gradle</summary>

```groovy
implementation 'group.phorus:mapper:x.y.z'
```
</details>

<details open>
<summary>Gradle / Kotlin DSL</summary>

```kotlin
implementation("group.phorus:mapper:x.y.z")
```
</details>

<details open>
<summary>Maven</summary>

```xml
<dependency>
    <groupId>group.phorus</groupId>
    <artifactId>mapper</artifactId>
    <version>x.y.z</version>
</dependency>
```
</details>

### Usage

All the functionality can be accessed via the following extension functions:
- `Any.mapTo<Type>()`: transforms an object to another. (Recommended option)
- `Any.mapToClass<Type>()`: transforms an object to another, but accepts Strings instead of KProperties in the
  extra options.
- `Any.updateFrom(object)`: updates an object based on the properties of another. (Recommended option)
- `Any.updateFromObject(object)`: updates an object based on the properties of another,
  but accepts Strings instead of KProperties in the extra options.

It's recommended to use the functions that accept KProperties in the extra options, since they are recognized by the IDEs
and thus, they are considered in operations like refactors.

One advantage of using String fields instead of KProperties, is that you can specify more complex locations.
This may be difficult to comprehend, so let's see some examples:

<details open>
<summary>mapTo</summary>

```kotlin
val user = User(name = "John", surname = "Wick", password = "password hash")

// In this case, we are ignoring the UserDTO password field, if you refactor the field with the IDE the exclusion will
//  also be considered
val userDTO = user.mapTo<UserDTO>(exclusions = listOf(UserDTO::password))
```
</details>

<details open>
<summary>mapToClass</summary>

```kotlin
val user = User(name = "John", surname = "Wick", data = Data(password = "password hash"))

// In this case, we are ignoring the data/password field, if you refactor the field the exclusion won't be considered,
//  but it allows you to exclude more specific locations instead of simple fields.
val userDTO = user.mapToClass<UserDTO>(exclusions = listOf("data/password"))
```
</details>

<details open>
<summary>updateFrom</summary>

```kotlin
val person = Person(name = "John", surname = "Wick")
val personUpdate = Person(surname = "Lennon")

person.updateFrom(personUpdate)

println(person)
// out: Person(name = "John", surname = "Lennon")
```
</details>

### Advanced usage

You can use certain options to fine-tune the way the functions work. Let's check them more in depth.

These extra options are: `exclusions`, `mappings`, `functionMappings`, `ignoreMapFromAnnotations`, 
`useSettersOnly`, and `mapPrimitives`.

There is also more advanced functionality, such as the @MapFrom annotations, and the way the functions process 
primitives and different composite classes like `Iterable`, `Map`, `Pair`, and `Triple`.

If you are interested in more examples than the ones provided in this README, see [MappingFunctionTest.kt](src/test/kotlin/group/phorus/mapper/mapping/MappingFunctionsTest.kt).

<details open>
<summary>More information</summary>

#### Exclusions

The `exclusions` option allows you to add a list of target field exclusions.
These are fields of the final class that will be completely ignored.

If the excluded field is not nullable, doesn't have a default value, and it's required to build a new object,
the object that cannot be built because of the missing value will be set to null. If that object is the main one,
the function will return null.

<details open>
<summary>Examples</summary>

```kotlin
val user = User(name = "John", surname = "Wick")

val userDTO = user.mapTo<UserDTO>(exclusions = listOf(UserDTO::name))

println(userDTO)
// out: UserDTO(name = null, surname = "Wick")
```

```kotlin
val user = User(name = "John", surname = "Wick")

// If the surname field is not nullable, the object cannot be built, so the function will return null.
val userDTO = user.mapTo<UserDTO>(exclusions = listOf(UserDTO::surname))

println(userDTO)
// out: null
```

```kotlin
val user = User(name = "John", surname = "Wick", data = Data(password = "password hash"))

// If the password field in the Data class is not nullable, the object cannot be built, so Data will be set to null.
val userDTO = user.mapToClass<UserDTO>(exclusions = listOf("data/password"))

println(userDTO)
// out: UserDTO(name = "John", surname = "Wick", data = null)
```
</details>

#### Mappings

The `mappings` option allows you to forcefully map one property from the original object to a target field,
even if they don't share a common name. This option has priority over normal mapping.

You also need to set a `MappingFallback`, used in case the mapping fails:
- `MappingFallback.CONTINUE` will ignore that mapping and continue normally.
- `MappingFallback.NULL` will try to set the target field to null, if the field is not nullable it will do the same as
  `MappingFallback.CONTINUE`.

<details open>
<summary>Examples</summary>

Classes used in the examples:
```kotlin
class Person(
    name: String,
    surname: String,
    data: Data,
)

class PersonDTO(
    nameDTO: String,
    surname: String,
)

class Data(
    otherName: String,
)
```

```kotlin
val person = Person(name = "John", surname = "Wick", data = Data(otherName = "Johnny"))

val person = person.mapTo<PersonDTO>(
    mappings = mapOf(Person::name to (PersonDTO::nameDTO to MappingFallback.NULL))
)
```

```kotlin
val person = Person(name = "John", surname = "Wick", data = Data(otherName = "Johnny"))

val person = person.mapToClass<PersonDTO>(
    mappings = mapOf("name" to ("nameDTO" to MappingFallback.NULL))
)
```

```kotlin
val person = Person(name = "John", surname = "Wick", data = Data(otherName = "Johnny"))

val person = person.mapToClass<PersonDTO>(
    mappings = mapOf("data/otherName" to ("nameDTO" to MappingFallback.NULL))
)
```
</details>

#### Function mappings

The `functionMappings` options works in a very similar way to the [`mappings`](#mappings) option, but it allows you to
specify a mutating function that will run with the original property as input, and use the output to set the
target field.

##### Notes

> The original property will be mapped to the function input type if necessary.

> The function return value will be mapped to the target field type if necessary.

> The function may have at most one parameter.

> The function may have a nullable or optional parameter, or no parameters at all.

<details open>
<summary>Examples</summary>

```kotlin
val person = Person(name = "John", age = 55)

// The function uses the original property as input, and puts the returned value in the target field
val updateAge : (Int) -> Int = {
    (it + 5)
}

val personDTO = person.mapTo<PersonDTO>(
    functionMappings = mapOf(Person::age to (updateAge to (PersonDTO::age to MappingFallback.NULL))),
)

println(personDTO)
// out: PersonDTO(name = "John", age = 60)
```

```kotlin
val person = Person(name = "John", age = 55)

val personDTO = person.mapTo<PersonDTO>(
    functionMappings = mapOf(Person::age to ({ (it + 5).toString() } to (PersonDTO::name to MappingFallback.NULL))),
)

println(personDTO)
// out: PersonDTO(name = "60", age = 55)
```
</details>

#### @MapFrom annotation

Instead of using the `mapping` option, you can use the new `@MapFrom` annotation.
As the name suggests, this annotation should be used on fields (of a target class)
that you are interested in mapping from a specific location.

The annotation accepts two parameters:
- `locations`: An array of locations, the functions will attempt to map the field from the first location
  onwards until one works.
- `fallback`: A `MappingFallback`, used in case every location fails, works in the same way as in [Mappings](#mappings).
  Set to `MappingFallback.CONTINUE` by default.

<details open>
<summary>Examples</summary>

Classes used in the examples:
```kotlin
class Person(
    name: String,
    surname: String,
)

class PersonDTO(
    @MapFrom(["name"])
    nameDTO: String,
    surname: String,
)
```

```kotlin
val person = Person(name = "John", surname = "Wick")

val person = person.mapTo<PersonDTO>()

println(person)
// out: PersonDTO(nameDTO = "John", surname = "Wick")
```

</details>

#### Primitives

The functions support mapping some primitives between each other.

Strings can be transformed to different kind of Numbers, this includes classes 
like: `Double`, `Float`, `Long`, `Int`, `Short`, and `Byte`.

##### Notes

> Numbers can be transformed to String, and to other Numbers as well.

> You can disable this functionality by setting the option `mapPrimitives` to false.

> If the functionality is disabled or the mapping fails for some reason, the field will be set to null, if possible.

<details open>
<summary>Examples</summary>

Classes used in the examples:
```kotlin
class Person(
    name: String,
    age: Int,
)

class PersonDTO(
    name: String,
    age: String?,
)
```

```kotlin
val result = 10.mapTo<String>()

println(result)
// out: "10"
```

```kotlin
val result = 10.mapTo<String>(mapPrimitives = false)

println(result)
// out: null
```

```kotlin
val person = Person(name = "John", age = 55)

val personDTO = person.mapTo<PersonDTO>()

println(result)
// out: Person(name = "John", age = "55")
```

```kotlin
val person = Person(name = "John", age = 55)

val personDTO = person.mapTo<PersonDTO>(mapPrimitives = false)

println(result)
// out: Person(name = "John", age = null)
```
</details>

#### Composites: Iterable, Map, Pair, Triple

The functions support mapping different kind of composite classes.

This includes Iterables like `Set` and `List`, `Map`, `Pair`, and `Triple`.
Any of their subclasses are also supported.

##### Notes

> A composite classes can only be mapped to the same composite class, 
> for example: `List<Person>` can be mapped to `List<PersonDTO>`, but cannot be mapped to `Map<PersonDTO>`.

> `mappings`, `functionMappings`, and other options will be used to map each item in the composite class.

> Arrays are **not supported**. This is not fixable, and its caused by the way they behave with generics.

<details open>
<summary>Examples</summary>

```kotlin
val persons = listOf(
    Person(name = "John", age = 55),
    Person(name = "Martin", age = 35),
)

val result = persons.mapTo<List<PersonDTO>>()
```
</details>

</details>

## Building and contributing

See [Contributing Guidelines](CONTRIBUTING.md).

## Authors and acknowledgment

[Martin Ivan Rios](https://linkedin.com/in/ivr2132)
