#!/bin/bash
fswatch ./src/main | xargs -n1 -I{} curl "http://localhost:8080/shutdown"
