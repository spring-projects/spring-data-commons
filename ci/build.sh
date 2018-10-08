#!/bin/bash

set -euo pipefail

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

spring_commons_artifactory=$(pwd)/spring-commons-artifactory

rm -rf $HOME/.m2/repository/org/springframework/commons 2> /dev/null || :

cd spring-commons-github

./mvnw -Pdistribute -Dmaven.test.skip=true clean deploy \
	-DaltDeploymentRepository=distribution::default::file://${spring_commons_artifactory}
