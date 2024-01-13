#!/bin/bash
RED='\033[0;31m'
GREEN='\033[0;32m'
NONE='\033[0m'

function check_status() {
    if [ $1 -eq 0 ]; then 
      echo -e "${GREEN}OK${NONE}" 
    else 
      echo -e "${RED}KO${NONE}"
    fi
}

exitcode=0

if [ ! -d "TDD2024TESTS" ]; then
   echo "Adding main public test repository for branch $2"
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/dlbenseignement/m1-2023-2024/TDD2024TESTS.git 
   pass=$?
   check_status $pass
fi
exitcode=$((exitcode || pass))

if [ ! -d "TDD2021HIDDENTESTS" ]; then
   echo "Adding main hidden test repository for branch $2" 
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/dlbenseignement/m1-2020-2021/TDD2021HIDDENTESTS.git 
   check_status $pass
fi
exitcode=$((exitcode || pass))

if [ ! -d "TDD2024OWNTESTS" ]; then
   echo "Adding own test repository for $1 for branch $2"
   git submodule add -b $2 --force https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.univ-artois.fr/$1/TDD2024OWNTESTS.git 
   check_status $pass
fi

echo "Tests installation"
check_status $exitcode

exit $exitcode
