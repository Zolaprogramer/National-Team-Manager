# National Team Manager

Web aplikacija u kojoj korisnik kreira svoje fudbalske reprezentacije, vodi igrače, organizuje utakmice, pravi sastave
na terenu, podešava taktiku, vodi stadione, evidentira događaje i prati statistiku izračunatu iz baze. Svaki korisnik vidi i mijenja
isključivo svoje podatke. Interfejs je dostupan na srpskom i engleskom.

Aplikacija radi lokalno i ne koristi cloud servise.

## Tehnologije

| Oblast | Tehnologija |
|---|---|
| Jezik | Java 21 |
| Framework | Spring Boot 3.5, Spring MVC |
| Pristup podacima | Spring Data JPA, Hibernate 6 |
| Baza | Microsoft SQL Server (2019, 2022, Express) |
| Sigurnost | Spring Security, BCrypt |
| Prikaz | Thymeleaf, Bootstrap 5, HTML, CSS, JavaScript |
| Jezici | Spring MessageSource, `messages_sr.properties`, `messages_en.properties` |
| Zastave | lokalna biblioteka flag-icons (WebJar) |
| Build | Maven (Maven Wrapper je uključen) |
| Testovi | JUnit 5, MockMvc, H2 (samo za testove) |

## Zahtjevi

- JDK 21
- Microsoft SQL Server 2022 i SQL Server Management Studio (ili alat `sqlcmd`)
- Eclipse IDE for Enterprise Java and Web Developers, verzija **2023-12 ili novija** (starije verzije ne podržavaju Javu 21)

JDK 21 se može instalirati ovom naredbom:

```bash
winget install Microsoft.OpenJDK.21
```

Verzija za Javu 11 i Spring Boot 2.7 sačuvana je u folderu `national-team-manager-java11`, za slučaj da je potrebno
pokretanje na JDK 11.

Node.js i JHipster nisu potrebni. Bootstrap i ikone dolaze kroz Maven (WebJars), a JavaScript su obični fajlovi koje servira
Spring Boot. JHipster je generator koji pravi drugačiju vrstu projekta (REST API s Angular ili React frontendom), pa se ova
aplikacija ne pravi njime.

U SSMS-u se skripte iz foldera `database` mogu otvoriti i pokrenuti umjesto `sqlcmd` naredbi: **File > Open > File**, pa
**Execute**. Skriptu `03_create_login.sql` tada pokrenite u **SQLCMD Mode** (meni **Query > SQLCMD Mode**), uz red
`:setvar AppPassword "VASA_LOZINKA"` na početku.

## Povezivanje s bazom

Naredbe se pokreću iz foldera projekta. Primjeri su za instancu `localhost\SQLEXPRESS`.

### 1. Omogućite TCP/IP i SQL prijavu

JDBC driver se na SQL Server spaja isključivo preko TCP/IP-a, a on je kod Express izdanja podrazumijevano isključen.
Skripta to rješava jednom naredbom. PowerShell mora biti otvoren **kao Administrator**:

```bash
powershell -ExecutionPolicy Bypass -File .\database\01_enable_tcp_and_sql_login.ps1
```

Isto se može uraditi ručno:

1. U SQL Server Configuration Manageru uključite TCP/IP za instancu i pod `IPAll` postavite `TCP Port` na 1433.
2. U SSMS-u pod Server Properties > Security izaberite "SQL Server and Windows Authentication mode".
3. Restartujte SQL Server servis.

### 2. Kreirajte bazu i tabele

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\02_create_schema.sql
```

### 3. Kreirajte SQL prijavu za aplikaciju

Zamijenite `VASA_LOZINKA` jakom lozinkom (velika i mala slova, brojevi, najmanje 8 znakova):

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -i database\03_create_login.sql -v AppPassword="VASA_LOZINKA"
```

### 4. Dodajte početne podatke (opciono)

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\04_seed_data.sql
```

Time nastaje demo nalog s korisničkim imenom `demo` i lozinkom `Demo1234!`, tri reprezentacije, 48 igrača, utakmice,
sastavi i događaji.

### Nadogradnja postojeće baze

Ako je baza napravljena prije dodavanja taktike, jezika, zastava i uklanjanja težine, pokrenite ove skripte. One ne brišu
postojeće podatke i mogu se pokrenuti više puta:

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\05_tactics_coach_language_upgrade.sql
```

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\06_player_weight_optional.sql
```

Za modul stadiona pokrenite i ovu skriptu:

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\08_stadiums.sql
```

Za logotipe takmičenja i ulogu administratora pokrenite i ovu skriptu:

```bash
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\09_competition_logos.sql
```

Skripta `07_optional_drop_player_weight.sql` je opciona i nepovratno briše kolonu s težinom igrača.

## Podešavanje application.properties

Konekcija je definisana u `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=NationalTeamManager;encrypt=true;trustServerCertificate=true
spring.datasource.username=${DB_USERNAME:ntm_app}
spring.datasource.password=${DB_PASSWORD:}
```

Lozinka nije upisana u kod. Kopirajte `application-local.properties.example` u `application-local.properties` (isti folder)
i upišite lozinku iz koraka 3:

```properties
spring.datasource.username=ntm_app
spring.datasource.password=VASA_LOZINKA
```

Fajl `application-local.properties` je u `.gitignore`. Umjesto njega se mogu koristiti varijable okruženja `DB_USERNAME`
i `DB_PASSWORD`.

## Pokretanje u Eclipse IDE-u

1. Otvorite **Window > Preferences > Java > Installed JREs** i dodajte JDK 21 kao podrazumijevani.
2. Izaberite **File > Import > Maven > Existing Maven Projects** i odaberite folder projekta (onaj u kojem je `pom.xml`).
3. Sačekajte da Maven preuzme zavisnosti. Ako Eclipse prijavi greške, pokrenite **desni klik na projekat > Maven > Update Project**.
4. Otvorite `src/main/java/com/nationalteammanager/NationalTeamManagerApplication.java`.
5. Izaberite **Run As > Java Application**.
6. Otvorite <http://localhost:8080> u pregledniku.

Radni folder Eclipse konfiguracije mora ostati folder projekta (to je podrazumijevano), jer se tamo traži
`application-local.properties` i kreira folder `uploads`.

Pokretanje bez Eclipsea:

```bash
.\mvnw.cmd spring-boot:run
```

Maven Wrapper zahtijeva da varijabla `JAVA_HOME` pokazuje na JDK 21.

Prevodni fajlovi su u UTF-8. Ako Eclipse u njima pogrešno prikazuje slova č, ć, š, đ i ž, otvorite
**Window > Preferences > General > Content Types > Text > Java Properties File** i kao Default encoding upišite `UTF-8`.

## Početak korištenja

1. Registrujte se na `/register` ili se prijavite demo nalogom.
2. Kreirajte najmanje dvije reprezentacije.
3. U reprezentaciju dodajte igrače. Za sastav su potrebni najmanje jedan golman i deset ostalih igrača.
4. Po želji kreirajte takmičenje. Utakmica bez takmičenja se vodi kao prijateljska.
5. Kreirajte utakmicu i na njenoj stranici napravite sastave.
6. Na stranici utakmice otvorite "Taktika" i rasporedite igrače, formaciju i postavke.
7. Kada utakmica počne, upišite događaje i rezultat. Gol automatski povećava rezultat.
8. Pregledajte Dashboard i Statistiku.

Jezik se bira dugmadima Srpski i English u navigaciji. Fotografija selektora i zastava podešavaju se u obrascu reprezentacije.
Detalji su u [docs/IZMJENE.md](docs/IZMJENE.md).

## Administrator i logotipi takmičenja

Administrator vodi zvanična takmičenja, koja vide svi korisnici, i jedini postavlja, mijenja i briše njihove logotipe.
Svaki korisnik i dalje može napraviti lično takmičenje, koje nema logo i prikazuje podrazumijevanu sliku.
Nalog `demo` iz `04_seed_data.sql` je administrator. Bilo koji drugi nalog postaje administrator jednom naredbom u SSMS-u,
nakon koje se korisnik treba ponovo prijaviti:

```sql
UPDATE dbo.Users SET role = 'ADMIN' WHERE username = N'korisnicko_ime';
```

Logotipi se čuvaju u folderu `uploads/competitions` (podešava se kroz `app.upload-root`), a u bazi je samo putanja
(`Competitions.logoPath`). Dozvoljeni su PNG, JPG, JPEG i SVG do 5 MB. Ekstenzija mora odgovarati sadržaju fajla,
a SVG sa skriptom ili vanjskim sadržajem se odbija.

## Testovi

```bash
.\mvnw.cmd test
```

Testovi koriste bazu u memoriji, pa ne zahtijevaju SQL Server. Pokrivaju registraciju, izolaciju podataka među korisnicima
i sva poslovna pravila, a svaka stranica se prikazuje s pravim podacima.

## Struktura projekta

```text
national-team-manager
├── database                 skripte za SQL Server (TCP, šema, prijava, demo podaci)
├── docs                     analiza i priprema za odbranu
├── pom.xml
└── src
    ├── main
    │   ├── java/com/nationalteammanager
    │   │   ├── config       Spring Security i prijavljeni korisnik
    │   │   ├── controller   HTTP zahtjevi i izbor stranice, bez poslovne logike
    │   │   ├── dto          obrasci (Form) i podaci za prikaz
    │   │   ├── entity       JPA entiteti
    │   │   ├── enums        pozicije, statusi, formacije...
    │   │   ├── exception    izuzeci i globalni @ControllerAdvice
    │   │   ├── repository   Spring Data JPA repozitoriji
    │   │   ├── service      poslovna pravila i transakcije
    │   │   └── validation   vlastite Bean Validation anotacije
    │   └── resources
    │       ├── static       CSS i JavaScript
    │       ├── templates    Thymeleaf stranice
    │       └── application.properties
    └── test                 integracioni testovi
```

Detaljan opis modela podataka i odluka nalazi se u [docs/ANALIZA.md](docs/ANALIZA.md), a pitanja za odbranu u
[docs/PITANJA_ZA_ODBRANU.md](docs/PITANJA_ZA_ODBRANU.md).

## Rješavanje problema

| Poruka | Uzrok i rješenje |
|---|---|
| `The TCP/IP connection to the host localhost, port 1433 has failed` ili `Unable to determine Dialect without JDBC metadata` | SQL Server ne prima TCP konekcije. Pokrenite skriptu iz koraka 1 i provjerite da servis radi. |
| `Login failed for user 'ntm_app'` | Lozinka nije ista kao u koraku 3 ili SQL prijava nije uključena. Ponovite korake 1 i 3. |
| `Cannot open database "NationalTeamManager"` | Pokrenite skriptu iz koraka 2, a zatim korak 3. |
| `release version 21 not supported` | Maven ili Eclipse koriste stariju Javu. Podesite JDK 21 i `JAVA_HOME`. |
| Port 8080 je zauzet | Promijenite `server.port` u `application.properties`. |
