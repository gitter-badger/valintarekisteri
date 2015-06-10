# valintarekisteri

Opiskelijavalintarekisteri


## Käyttö

1. `./sbt run`
2. `curl http://localhost:8080/ensikertalainen/$oid`


## Käännös

`./sbt clean one-jar` tuottaa riippuvuudet sisältävän asennuspaketin.


## Tärkeää

Testit toimivat toistaiseksi vain java 7:lla. Java 8:lla serveri valittaa suljettaessa.

tarvittaessa java homen voi antaa sbt:lle parametrilla

```
./sbt -java-home <pathi jdk 1.7:n>
```
