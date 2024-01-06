#!/bin/bash

if [ ! -d "TDD2024TESTS" ]; then
   echo "Adding main public test repository for branch $2"
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/dlbenseignement/m1-2023-2024/TDD2024TESTS.git 
fi

if [ ! -d "TDD2021HIDDENTESTS" ]; then
   echo "Adding main hidden test repository for branch $2" 
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/dlbenseignement/m1-2020-2021/TDD2021HIDDENTESTS.git 
fi

if [ ! -d "TDD2024OWNTESTS" ]; then
   echo "Adding own test repository for $1 for branch $2"
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/$1/TDD2024OWNTESTS.git 

fi

