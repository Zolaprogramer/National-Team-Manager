# Pitanja za odbranu

Odgovori su kratki, a u zagradi je fajl koji treba otvoriti ako profesor želi vidjeti kod.

## Kako aplikacija funkcioniše

**Opišite put jednog zahtjeva, na primjer dodavanje igrača.**
Preglednik šalje POST na `/teams/{teamId}/players`. Spring Security provjerava sesiju i CSRF token. `PlayerController` veže
polja na `PlayerForm` i pokreće Bean Validation. Ako je sve ispravno, poziva `PlayerService.create`, koji u transakciji
provjerava vlasništvo, limit od 30 igrača i jedinstven dres, pa čuva entitet preko repozitorija. Kontroler zatim
preusmjerava na stranicu reprezentacije i prikazuje poruku.

**Zašto redirect nakon uspješnog POST-a?**
To je obrazac Post/Redirect/Get. Osvježavanje stranice ne šalje obrazac ponovo, pa se isti igrač ne dodaje dva puta.

**Gdje se računa statistika?**
U `StatisticsService` i zapisu `TeamStatistics`, iz završenih utakmica, događaja i sastava. Nigdje se ne čuva ručno, pa ne
može postati neusklađena s podacima.

**Šta se računa kao nastup?**
Početak utakmice u prvih 11 ili ulazak s klupe na završenoj utakmici. Koristi se `Set` ID-eva utakmica, pa se jedna utakmica
broji jednom.

**Zašto su ukupan broj pobjeda i poraza uvijek jednaki?**
Obje ekipe na utakmici pripadaju istom korisniku, pa je svaka odlučena utakmica jedna pobjeda i jedan poraz. Zato postoji i
tabela po reprezentacijama.

## Java i OOP

**Gdje se vidi enkapsulacija?**
Entiteti imaju privatna polja i metode sa značenjem, kao `Match.addGoalFor` i `Lineup.addStarter`, umjesto settera za sve.
Prazan konstruktor je `protected` jer je potreban samo Hibernateu.

**Gdje se vidi polimorfizam i nasljeđivanje?**
`AppUserPrincipal` implementira `UserDetails`, pa ga Spring Security koristi preko interfejsa. Repozitoriji nasljeđuju
`JpaRepository`. Izuzeci nasljeđuju `RuntimeException`, a validatori implementiraju `ConstraintValidator`.

**Šta je apstrakcija u ovom projektu?**
Servis poziva `playerRepository.save(...)` i ne zna kako izgleda SQL. Kontroler poziva servis i ne zna ništa o bazi.

**Zašto je `Formation` enum s logikom?**
Skup formacija je konačan i poznat unaprijed. Enum iz broja igrača po linijama sam izračunava koordinate na terenu, pa se
nova formacija dodaje jednim redom (`Formation.java`).

**Šta je `record` i gdje ga koristite?**
Nepromjenljiva klasa koja automatski dobija konstruktor, pristupne metode, `equals` i `hashCode`. Koristim ga za podatke za
prikaz (`TeamStatistics`, `DashboardView`). Obrasci su obične klase jer Spring na njih upisuje vrijednosti preko settera.

**Zašto nema Lomboka?**
Kod je eksplicitan i svaka metoda se vidi, što je lakše objasniti, bez generisanog koda i dodatne konfiguracije u Eclipseu.

## Spring Boot i Spring MVC

**Šta radi `@SpringBootApplication`?**
Spaja `@Configuration`, `@EnableAutoConfiguration` i `@ComponentScan`: skenira komponente i automatski podešava biblioteke
pronađene na classpathu.

**Šta je Dependency Injection i kako ga koristite?**
Objekat ne kreira svoje zavisnosti, nego ih dobija od Spring kontejnera. Koristim injekciju kroz konstruktor, jer su polja
tada `final`, zavisnosti su vidljive, a klasu je lakše testirati.

**Razlika između `@Controller` i `@RestController`?**
`@Controller` vraća naziv Thymeleaf stranice. `@RestController` vraća podatke direktno u tijelo odgovora, najčešće JSON.

**Čemu služi `@ModelAttribute` i `BindingResult`?**
`@ModelAttribute` veže polja zahtjeva na objekat. `BindingResult` čuva greške validacije, pa se umjesto izuzetka stranica
ponovo prikazuje s porukama. Mora stajati odmah iza objekta koji se validira.

**Čemu služi `@ControllerAdvice`?**
Centralno obrađuje izuzetke iz svih kontrolera (`GlobalExceptionHandler`). `ResourceNotFoundException` daje stranicu 404, a
`BusinessRuleException` vraća korisnika na istu stranicu s porukom.

**Zašto DTO, a ne entitet u obrascu?**
Ako se entitet direktno veže na obrazac, napadač može dodati parametar i promijeniti polje poput vlasnika (mass assignment).
Obrazac sadrži samo dozvoljena polja, a validacija pripada ulazu, ne tabeli.

## JPA, Hibernate i baza

**Razlika između JPA, Hibernatea i Spring Data JPA?**
JPA je specifikacija. Hibernate je njena implementacija. Spring Data JPA gradi repozitorije nad njom, pa upit nastaje iz
naziva metode.

**Kako radi `findByIdAndOwnerId`?**
Spring Data iz naziva metode gradi upit `WHERE id = ? AND owner.id = ?`.

**Objasnite `mappedBy`, `cascade` i `orphanRemoval`.**
`mappedBy` označava da drugu stranu veze posjeduje strana sa stranim ključem. `cascade` prenosi operacije na povezane
entitete, na primjer brisanje utakmice briše njene događaje. `orphanRemoval` briše red koji je izbačen iz kolekcije.

**Šta je LAZY i šta je N+1 problem?**
LAZY učitava vezu tek pri pristupu. N+1 nastaje kada se za svaki od N redova šalje još jedan upit. Rješavam ga anotacijom
`@EntityGraph`, koja vezu učitava jednim JOIN-om (`MatchRepository`).

**Zašto je `open-in-view=false`?**
Tako se upiti ne izvršavaju skriveno tokom prikaza stranice. Servis mora unaprijed učitati sve što stranica koristi, a
greška se odmah primijeti.

**Zašto `@Enumerated(EnumType.STRING)`?**
U bazi je čitljiva vrijednost, a promjena redoslijeda enuma ne kvari postojeće podatke, što bi se desilo s `ORDINAL`.

**Zašto nema `ON DELETE CASCADE`?**
SQL Server ne dozvoljava više kaskadnih putanja do iste tabele. `LineupPlayers` je dostupna preko `Players` i preko `Lineups`,
pa redoslijed brisanja vodi aplikacija.

**Šta se dešava kad obrišem reprezentaciju, igrača, utakmicu ili takmičenje?**
Reprezentacija se briše samo ako nema utakmica, i to zajedno s igračima. Igrač se briše samo ako nije u sastavu ili događaju.
Utakmica se briše zajedno sa sastavima i događajima. Takmičenje se briše, a njegove utakmice postaju prijateljske.

**Zašto `NVARCHAR`?**
`NVARCHAR` čuva Unicode, pa se ispravno čuvaju slova č, ć, š, đ i ž.

**Šta je transakcija i gdje je koristite?**
Niz operacija koji uspije u cjelini ili se poništi u cjelini. Kod gola se u istoj transakciji čuvaju događaj i novi rezultat,
pa ne može nastati gol bez promjene rezultata.

**Šta radi `spring.jpa.hibernate.ddl-auto=update`?**
Hibernate pri pokretanju upoređuje entitete s bazom i dodaje ono što nedostaje. Glavnu šemu kreira SQL skripta. U produkciji
bih koristio `validate` i alat za migracije, kao što je Flyway.

**Zašto `saveAndFlush` pri čuvanju sastava?**
Hibernate izvršava INSERT prije DELETE. Bez ranijeg brisanja, ponovo izabrani igrač bi narušio jedinstveni ključ
(`LineupService.save`).

## Sigurnost

**Kako se čuva lozinka?**
Kao BCrypt hash. BCrypt je jednosmjeran, dodaje slučajni salt i namjerno je spor, pa dvije iste lozinke imaju različit hash.
Pri prijavi se unesena lozinka provjerava metodom `matches`, a nikada se ne dešifruje.

**Kako korisnik ne može vidjeti tuđe podatke?**
Kontroler ID korisnika uzima iz sesije (`@AuthenticationPrincipal`), a ne iz zahtjeva. Svaki servis učitava podatke uz
vlasnika, na primjer `findByIdAndOwnerId`. Tuđi ID daje 404.

**Zašto 404, a ne 403?**
403 bi otkrio da taj ID postoji. 404 ne otkriva ništa.

**Šta je CSRF i kako je riješen?**
Zlonamjerna stranica može navesti preglednik prijavljenog korisnika da pošalje zahtjev. Spring Security traži tajni token u
svakom POST-u, a Thymeleaf ga automatski dodaje u obrasce sa `th:action`. Brisanje je zato POST, a ne link.

**Kako je riješen XSS?**
`th:text` automatski pretvara HTML znakove u bezopasan tekst.

**Kako je riješen SQL Injection?**
JPA koristi parametrizovane upite, pa unos korisnika nikad ne postaje dio SQL naredbe.

**Gdje je lozinka za bazu?**
U `application-local.properties`, koji je u `.gitignore`, ili u varijablama okruženja. U kodu je nema.

**Kako je zaštićen upload fotografije?**
Veličina je ograničena na 2 MB, tip se provjerava po sadržaju fajla, naziv se generiše kao UUID, a fotografija se servira
kroz kontroler uz provjeru vlasnika.

## Thymeleaf i frontend

**Kako radi zajednički izgled stranica?**
`layout.html` je fragment s parametrima. Svaka stranica joj preko `th:replace` predaje naslov i svoj `#content`.

**Razlika između `th:field` i `th:text`?**
`th:field` veže polje obrasca na objekat i postavlja `id`, `name` i `value`. `th:text` samo ispisuje vrijednost.

**Kako radi teren?**
`Formation` računa koordinate pozicija u procentima, a CSS postavlja igrača apsolutnim pozicioniranjem. U obrascu isti raspored
dobija JavaScript, pa se teren odmah mijenja pri izboru formacije.

**Zašto se Bootstrap ne učitava s CDN-a?**
Dolazi kao WebJar kroz Maven, pa aplikacija radi i kada na odbrani nema interneta.

## Taktika, jezik i zastave

**Kako je taktika povezana s utakmicom?**
Tabela `Lineups` ima jedinstven par (`matchId`, `teamId`), a postavke su njene kolone. Svaka reprezentacija zato ima svoju
taktiku na svakoj utakmici.

**Šta je `@Embeddable` i zašto ga koristite?**
Klasa čija polja postaju kolone tabele vlasnika. `TacticalSettings` grupiše osam postavki bez posebne tabele i bez spajanja.

**Gdje se provjeravaju pravila table?**
JavaScript sprečava pogrešne poteze radi lakšeg rada, a ista pravila ponovo provjeravaju `LineupService` i `TacticsService`.

**Kako radi "Vrati prethodni sastav"?**
Prije svakog čuvanja trenutno stanje se kao JSON upiše u `Lineups.previousState`. Vraćanje ga pročita i sačuva kroz ista pravila.
Trenutno stanje tada postaje prethodno, pa se i vraćanje može poništiti.

**Kako radi promjena jezika?**
`LanguageController` postavlja jezik preko `LocaleResolver`, koji ga čuva u kolačiću, a prijavljenom korisniku i u bazi.
`LanguageLoginSuccessHandler` pri prijavi vraća jezik iz baze. Thymeleaf tekst čita izrazom `#{kljuc}` iz `MessageSource`.

**Kako se prevode validacione poruke i greške servisa?**
Anotacije koriste `message = "{kljuc}"`. Servisi bacaju izuzetak s ključem, a `GlobalExceptionHandler` ili `binding.reject` biraju
tekst na jeziku korisnika.

**Zašto enum ima `getMessageKey()`?**
Interfejs `TranslatableEnum` pravi ključ iz naziva enuma i vrijednosti, pa enum ne sadrži tekst nijednog jezika.

**Kako se prepoznaje zastava?**
`CountryFlagService` pravi mapu naziva država iz Jave na više jezika, uz nekoliko dodatnih naziva, i upoređuje ih bez dijakritika
i razlike u slovima. SVG je u lokalnoj biblioteci, pa nema eksternog API-ja.

**Zašto upload zastave ne prima SVG?**
SVG može sadržavati JavaScript, što bi otvorilo XSS. Zato se primaju samo JPG, PNG i WEBP, provjereni po sadržaju fajla.

**Zašto kolona `weightKg` nije obrisana?**
Brisanje je nepovratno i nije potrebno da bi aplikacija radila. Kolona je postala neobavezna, entitet je više ne koristi, a opciona
skripta je može obrisati kada podaci sigurno nisu potrebni.

## Stadioni

**Kako je osigurano da reprezentacija ima samo jedan glavni stadion?**
Servis prije postavljanja nove oznake skine staru i odmah je upiše pozivom `saveAndFlush`. U bazi to dodatno štiti filtrirani
jedinstveni indeks `UX_Stadiums_one_main_per_team`, koji važi samo za redove gdje je `mainStadium = 1`.

**Zašto `Matches` ima i `stadium` i `stadiumId`?**
`stadiumId` je veza sa stadionom iz evidencije. Tekstualni naziv ostaje za neutralne terene i starije utakmice, pa `stadiumId` nije
obavezan. `Match.getStadiumName()` daje prednost povezanom stadionu, pa promjena njegovog naziva odmah važi i na utakmicama.

**Gdje se provjerava da stadion pripada domaćinu?**
U `MatchService`. JavaScript u obrascu samo skriva ostale stadione, jer se server ne smije oslanjati na preglednik.

**Šta se dešava pri brisanju stadiona?**
Stadion s utakmicama se ne briše. Reprezentacija se briše samo ako nema utakmica, i tada s njom nestaju njeni stadioni i njihove fotografije.

## Logotipi takmičenja

**Zašto se u bazi čuva samo putanja, a ne sama slika?**
Baza ostaje mala, a slika se servira direktno s diska. Putanja je relativna (`competitions/uuid.png`), pa se folder `uploads` može premjestiti promjenom `app.upload-root`.

**Kako je osigurano da samo administrator mijenja logotipe?**
U `CompetitionService`: zvanično takmičenje mijenja samo administrator, a novi fajl ili brisanje logotipa bez uloge administratora baca `AccessDeniedException`. `GlobalExceptionHandler` to prikazuje kao stranicu 403. Skrivena dugmad u šablonu su samo pomoć korisniku, a ne zaštita.

**Kako se provjerava tip fajla?**
Po ekstenziji i po sadržaju: PNG i JPG prema prvim bajtovima, a SVG prema oznaci `<svg`. Fajl `slika.png` koji je zapravo JPG se odbija. SVG može sadržati JavaScript, pa se odbija svaki sa `<script>`, događajima poput `onload` ili vanjskim linkovima, a servira se uz zaglavlje `Content-Security-Policy: sandbox`.

**Zašto se logo ne izgubi kad se uredi takmičenje?**
Ako obrazac ne pošalje novi fajl niti oznaku za brisanje, `CompetitionLogoStorage.replace` vraća postojeću putanju.

**Zašto se u adresi slike nalazi `?v=`?**
Pri zamjeni logotipa naziv fajla se mijenja, pa se mijenja i adresa. Preglednik zato ne prikazuje staru sliku iz keša.

## Validacija

**Zašto se pravila provjeravaju i u JavaScriptu i na serveru?**
JavaScript samo olakšava unos i lako se zaobilazi. Server je jedino mjesto kojem se vjeruje.

**Kako je napravljena vlastita validacija?**
Anotacija s `@Constraint` i klasa koja implementira `ConstraintValidator`, na primjer `@DifferentTeams` i `DifferentTeamsValidator`.

**Zašto se jedinstven broj dresa provjerava na dva mjesta?**
Servis daje razumljivu poruku. Jedinstveni ključ u bazi štiti podatke i kod dva istovremena zahtjeva.

**Kako biste dodali novo pravilo, na primjer najmanje 18 godina za igrača?**
Dodao bih provjeru u `PlayerService.applyForm` ili vlastitu anotaciju na `PlayerForm`, i test u `ApplicationFlowTest`.

## Testiranje i pitanja za razmišljanje

**Kako ste testirali aplikaciju?**
`ApplicationFlowTest` pokreće cijelu aplikaciju i kroz MockMvc šalje stvarne HTTP zahtjeve. Testovi nisu `@Transactional`, pa
bi greške s lijenim učitavanjem izašle na vidjelo.

**Zašto testovi koriste H2?**
Testovi tako rade bez instalacije SQL Servera i svaki put kreću od čiste baze. Aplikacija u radu koristi SQL Server.

**Šta biste poboljšali?**
Straničenje dugih lista, Flyway za migracije, više uloga korisnika, zaključavanje pri provjeri limita od 30 igrača i izmjene
sa stvarnom vezom "ulazi-izlazi" umjesto opisa.

Demonstracija novih funkcionalnosti je u [IZMJENE.md](IZMJENE.md).

## Kratka demonstracija

1. Prijavite se kao `demo` i pokažite Dashboard.
2. Otvorite završenu utakmicu, pa zatim sastav na terenu.
3. Dodajte gol na utakmici i pokažite da se rezultat promijenio.
4. Pokušajte dodati igrača s već zauzetim brojem dresa i pokažite poruku.
5. Pokušajte obrisati igrača koji je u sastavu i pokažite poruku.
6. Otvorite Statistiku i pokažite da se novi gol već računa.
7. Registrujte novi nalog, zatim u adresu upišite `/teams/1` i pokažite stranicu 404.
