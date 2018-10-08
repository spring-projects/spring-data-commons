#!/bin/bash

set -euo pipefail

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

rm -rf $HOME/.m2/repository/org/springframework/commons 2> /dev/null || :

cd spring-commons-github

./mvnw clean dependency:list test -P${PROFILE} -Dsort
