#!/bin/bash

set -euo pipefail

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

spring_data_commons_artifactory=$(pwd)/spring-data-commons-artifactory

rm -rf $HOME/.m2/repository/org/springframework/data/commons 2> /dev/null || :

cd spring-data-commons-github

./mvnw -Dmaven.test.skip=true clean deploy \
	-DaltDeploymentRepository=distribution::default::file://${spring_data_commons_artifactory}
