#!/bin/bash
#while inotifywait src/main/resources/templates -e close_write; do 
#  sleep 1; 
#  find src/main/java -name \*Html.java -exec touch {} \;;
#done

#find /Users/agent/project/petclinic/petclinic/src/main/java -name \*Html.java -exec touch {} \; #&& mvn install && mh run

#find src/main/java -name \*Html.java -exec touch {} \; #&& mvn install && mh run

fswatch ./src/main/resources | xargs -n1 -I{} ./touch-templates.sh {}
