# Phorus Mapper

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/group.phorus/mapper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/group.phorus/mapper)
<a href='https://gitlab.com/phorus-group/public/development/libraries/mapper/-/pipelines?ref=main'><img src='https://gitlab.com/phorus-group/public/development/libraries/mapper/badges/main/pipeline.svg'></a>
[![GitLab license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

Mapper that allows you to transform one object to another, or update an object based on the properties of another.

The conversion is done using reflection, mapping properties with the same name and considering extra options like: 
`exclusions`, `mappings`, `functionMappings`, and `@MapFrom` annotations.

All functionality can be accessed via the following extension functions: `Any.mapTo<Type>()`, `Any.mapToClass<Type>()`, 
and `Any.updateFrom(object)`. To learn more, see [Getting started](#getting-started).

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
      - [Composites: Collections, Map, Pair, Triple](#composites-collections-map-pair-triple)
- [Building and Contributing](#building-and-contributing)
- [Authors and acknowledgment](#authors-and-acknowledgment)

***

## Features

- Allows you to transform an object to another type using reflection:
  - Define `exclusions` to exclude fields you are not interested in mapping.
  - Define `mappings`, to map two fields with different names.
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

Make sure that the `mavenCentral` (or any of its mirrors) is added to the repository list of the project, if it doesn't already exist (usually this is the root project):

<details open>
<summary>Kotlin</summary>

```kotlin
repositories {
    mavenCentral()
}
```
</details>

<details>
<summary>Groovy</summary>

```groovy
repositories {
  mavenCentral()
}
```
</details>

Binaries and dependency information for Maven and Gradle and others can be found at [http://search.maven.org](https://search.maven.org/search?q=g:group.phorus%20AND%20a:mapper).

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

Usage examples:

<details open>
<summary>mapTo</summary>

```kotlin
val user = User(name = "John", surname = "Wick", password = "password hash")

val userDTO = user.mapTo<UserDTO>(exclusions = listOf(UserDTO::password))
```
</details>

<details open>
<summary>mapToClass</summary>

```kotlin
val user = User(name = "John", surname = "Wick", data = Data(password = "password hash"))

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

TODO

<details open>
<summary>More information</summary>

#### Exclusions

TODO

<details open>
<summary>Examples</summary>

TODO
</details>

#### Mappings

TODO

<details open>
<summary>Examples</summary>

TODO
</details>

#### Function mappings

TODO

<details open>
<summary>Examples</summary>

TODO
</details>

#### @MapFrom annotation

TODO

<details open>
<summary>Examples</summary>

TODO
</details>

#### Primitives

TODO

<details open>
<summary>Examples</summary>

TODO
</details>

#### Composites: Collections, Map, Pair, Triple

TODO

<details open>
<summary>Examples</summary>

TODO
</details>

</details>

## Building and contributing

See [Contributing Guidelines](CONTRIBUTING.md).

## Authors and acknowledgment

[Martin Ivan Rios](https://linkedin.com/in/ivr2132)