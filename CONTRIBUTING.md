# Contributing Guidelines

There are two main ways to contribute to the project &mdash; submitting issues and submitting
fixes/changes/improvements via merge requests.

## Submitting issues

Both bug reports and feature requests are welcome.
Submit issues [here](https://gitlab.com/phorus-group/public/development/libraries/mapper/-/issues).

* Search for existing issues to avoid reporting duplicates.
* When submitting a bug report:
    * Test it against the most recently released version. It might have been already fixed.
    * Include all the code needed to reproduce the problem, but try to minimize it as much as possible.
    * If the bug is in behavior, then explain what behavior you've expected and what you've got.
* When submitting a feature request:
    * Explain why you need the feature &mdash; what's your use-case, what's your domain.
    * Explaining the problem you face is more important than suggesting a solution.
      Report your problem even if you don't have any proposed solution.
    * If there is an alternative way to do what you need, then show the code of the alternative.

## Submitting MRs

Submit MRs [here](https://gitlab.com/phorus-group/public/development/libraries/mapper/-/merge_requests).
However, please keep in mind that maintainers will have to support the resulting code of the project,
so do familiarize yourself with the following guidelines.

* All development (both new features and bug fixes) is performed in the `main` branch.
    * Base MRs against the `main` branch.
    * The code in the `main` branch is deployed automatically as a new version.
    * The pipeline on the `main` branch will fail if the project version was not updated. 
      Please update the project version following the [SemVer guidelines](https://semver.org/) to have an MR as ready as possible.
* If you make any code changes:
    * We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
      Use 4 spaces for indentation.
    * [Build the project](#building) to make sure it all works and passes the tests.
* If you fix a bug:
    * Write the test that reproduces the bug.
    * Fixes without tests are accepted only in exceptional circumstances if it can be shown that writing the
      corresponding test is too hard or otherwise impractical.
    * Place a test for the functionality in [unit tests directory](src/test/kotlin)
    * Follow the style of writing tests that is used in this project:
      name test functions as `...`, don't use backticks in test names. Name test classes as `...Test`.
    * Fixes that, in addition to directly solving the bug, add a large piece of new functionality or change the existing one, will be considered as features.
* If you introduce any new features or change the existing behavior:
    * Comment on the existing issue if you want to work on it or create one beforehand.
      Ensure that the issue not only describes a problem, but also describes a solution that had received a positive feedback. 
      Propose a solution if there isn't any.
      MRs with new features, but without a corresponding issue with a positive feedback about the proposed implementation are unlikely to
      be approved.
    * All new or modified features must come with tests.
    * [Contact the maintainers](#contacting-maintainers) to coordinate any big piece of work in advance.
* Drafts are used to demonstrate a prototype solution and discuss it with the community for further implementation

## Building

This plugin is built with Gradle.

* Run `./gradlew build` to build. It also runs all tests.
  things up during development.

## Contacting maintainers

* If something cannot be done, is not convenient, or does not work &mdash; submit an [issue](#submitting-issues).
* "How to do something" questions &mdash; [StackOverflow](https://stackoverflow.com).