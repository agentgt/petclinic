#!/bin/bash
_version=$(grep "<version>" pom.xml | head -n1 | sed -e "s/<version>//g" -e "s/<\/version>//g" -e "s/ //g")
_artifact=$(grep "<artifactId>" pom.xml | sed -n '2 p' | sed -e "s/<artifactId>//g" -e "s/<\/artifactId>//g" -e "s/ //g")

_result=2

while [[ "$_result" == "2" ]] ; do

  echo "Purging JStache Models"
  find target/classes -name \*Html.class -exec rm {} \;
  mvnd install -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true
  java -jar target/$_artifact-$_version.jar
  ## No we wait for planned shutdown via /shutdown
  _result=$?
  echo "Exit code: $_result"

done
