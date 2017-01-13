# Spring Data Commons

[Spring Data Commons](http://projects.spring.io/spring-data/) is part of the umbrella Spring Data project that provides shared infrastructure across the Spring Data projects. It contains technology neutral repository interfaces as well as a metadata model for persisting Java classes.

## Features

* Powerful Repository and custom object-mapping abstractions
* Support for cross-store persistence
* Dynamic query generation from query method names
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace

## Building the project

### Prerequisites

- Maven 3
- Java 8 (the project produces Java 6 compatible bytecode but partially integrates with Java 8)

```
$ git clone https://github.com/spring-projects/spring-data-commons.git
$ cd spring-data-commons
$ mvn clean install
```

## Getting Help

This README as well as the [reference documentation](http://docs.spring.io/spring-data/data-commons/docs/current/reference/html/) are the best places to start learning about Spring Data Commons.

The main project [website](http://projects.spring.io/spring-data/) contains links to basic project information such as source code, JavaDocs, Issue tracking, etc.

For more detailed questions, please refer to [spring-data on stackoverflow](http://stackoverflow.com/questions/tagged/spring-data). If you are new to Spring as well as to Spring Data, look for information about [Spring projects](https://spring.io/projects). 

## Contributing to Spring Data Commons

Here are some ways for you to get involved in the community:

* Create [JIRA](https://jira.spring.io/browse/DATACMNS) tickets for bugs and new features and comment and vote on the ones that you are interested in.
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](https://spring.io/blog.atom) to springframework.org

Before we accept a non-trivial patch or pull request we will need you to [sign the Contributor License Agreement](https://cla.pivotal.io/sign/spring). Signing the contributor’s agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. If you forget to do so, you'll be reminded when you submit a pull request. Active contributors might be asked to join the core team, and given the ability to merge pull requests.
