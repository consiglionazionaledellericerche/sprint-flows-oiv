Scrivania Digitale OIV
======================

Prerequisiti:
Java 8
Maven 3.2

Build and run

```shell script
./run.sh
```

oppure 

```shell script
mvn clean compile package -DskipTests 
java -jar target/sprint-flows-0.2.1-SNAPSHOT.war --spring.profiles.active=dev,oiv,swagger
```

L'applicazione partira' in modalita' dev con un database locale integrato

---

Per configurare l'applicazione per il dialogo con altri sistemi (database, email, Elenco, ...) usare i file di properties, o inserire i paramentri direttamente nella 
riga di commando (veder il file run.sh)

