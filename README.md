# APT Petclinic

## Description

A re-imagination of [Spring Petclinic](https://github.com/spring-projects/spring-petclinic) 
using [annotation processing libraries](https://github.com/gunnarmorling/awesome-annotation-processing) 
and eventually some HTMLX.

Technology changes:

```
Thymeleaf -> JStachio
Spring DI -> Avaje Inject
Hibernate -> Doma 2
Jackson -> avaje-jsonb
```

## Build

```bash
./env.sh
./docker_db.sh
mvn clean install
```

## Run

There are a variety of ways to run petclinic

### Shell script

```
./run.sh
```

### Manually

```
cd petclinic
java -jar target/petclinic-0.1.0-SNAPSHOT.jar
```

### Reload mode

Install mvnd

```
sdk install mvnd
```

Open two terminals.

Main terminal:

```
cd petclinic/petclinic
run.sh
```

Watching terminal:

```
cd petclinic/petclinic
watch.sh
```


