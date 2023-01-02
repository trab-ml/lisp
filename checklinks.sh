#!/bin/bash

if [ ! -d "TDD2023TESTS" ]; then
   echo "Adding main public test repository for branch $2"
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/dlbenseignement/m1-2022-2023/TDD2023TESTS.git 
fi

if [ ! -d "TDD2021HIDDENTESTS" ]; then
   echo "Adding main hidden test repository for branch $2" 
   git submodule add -b $2 --force https://oauth2:cnubdNfHsLYzw-jZB23_@gitlab.univ-artois.fr/dlbenseignement/m1-2020-2021/TDD2021HIDDENTESTS.git 
fi

if [ ! -d "TDD2023OWNTESTS" ]; then
   echo "Adding own test repository for $1 for branch $2"
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/$1/TDD2023OWNTESTS.git 

fi

