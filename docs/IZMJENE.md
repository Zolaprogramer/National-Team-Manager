# Nadogradnja: taktika, fotografije, selektor, jezik, zastave, težina i stadioni

## Nove funkcionalnosti

### Taktička tabla

Stranica `/matches/{id}/lineups/{teamId}/tactics` otvara se nad već napravljenim sastavom.

- Igrači se raspoređuju prevlačenjem mišem ili klikom na igrača pa na odredište, što radi i na telefonu.
- Svaka zamjena zadržava 11 igrača na terenu, pa isti igrač ne može biti na dvije pozicije, niti dva igrača na istoj.
- Formacije su 4-3-3, 4-4-2, 4-2-3-1, 3-5-2, 3-4-3 i 5-3-2. Promjena formacije pomjera pozicije, a igrači ostaju na svojim mjestima.
- Igrač van svoje pozicije ima isprekidan okvir, a golman ima poseban izgled.
- Svaki sastav ima svoje postavke: stil igre, mentalitet, širinu, tempo, presing, visinu odbrane, način dodavanja i smjer napada.
- Kapiten i izvođači penala, slobodnih udaraca i kornera biraju se među starterima. Svaki igrač u sastavu ima i ofanzivnu, uravnoteženu ili defanzivnu ulogu.
- "Poništi izmjene" vraća posljednje sačuvano stanje u pregledniku. "Vrati prethodni sastav" vraća stanje prije posljednjeg čuvanja iz baze.

Svaku ulogu može imati samo jedan igrač, jer je uloga jedan strani ključ. Jedan igrač može imati više uloga, na primjer kapiten i izvođač penala.

### Fotografije na tabli

Teren i klupa koriste postojeći upload fotografija. Fotografija je okrugla (`object-fit: cover`), a igrač bez fotografije dobija siluetu.

### Fotografija selektora

Dodaje se, mijenja i briše u obrascu reprezentacije. Prikazuje se na listi, detaljima, Dashboardu i taktičkoj tabli. Fajlovi se čuvaju u istom folderu kao fotografije igrača.

### Jezik

Izbor Srpski/English vidljiv je u navigaciji i na stranicama prijave i registracije. Svi tekstovi su u `messages_sr.properties` i `messages_en.properties`, uključujući validaciju, poruke o greškama i vrijednosti enuma. Podaci kao što su imena i države se ne prevode.

Jezik se čuva u kolačiću preglednika, a za prijavljenog korisnika i u koloni `Users.preferredLanguage`. Zato se vraća i pri prijavi iz drugog preglednika.

Srpski tekst koristi postojeći ijekavski latinični interfejs.

### Zastave

- Zastave država dolaze iz lokalne biblioteke `flag-icons` (WebJar), a nazivi država iz Jave na više jezika. Nema eksternih API-ja.
- Država se prepoznaje dok se kuca, bez obzira na velika slova i dijakritike: Njemačka, Nemačka, njemacka i Germany daju istu zastavu.
- Ručni upload prima JPG, PNG i WEBP do 2 MB. SVG nije dozvoljen, jer može sadržavati JavaScript.
- Polje "Zastava" nudi: bez promjene, automatski po državi i ukloni zastavu.
- Redoslijed prikaza je vlastita slika, zatim zastava države, zatim neutralna zastava.
- Zastava se prikazuje na listi, detaljima, Dashboardu, utakmicama i taktičkoj tabli, uz `object-fit: contain` radi očuvanja proporcija.

### Uklanjanje težine

Težina je uklonjena iz entiteta, obrazaca, validacije, prikaza i prevoda. Ostali podaci igrača se ne mijenjaju.

### Stadioni

Stranice `/stadiums` nude kartice s filterom po reprezentaciji, detalje s velikom fotografijom i obrazac za dodavanje i uređivanje.

- Jedna reprezentacija ima više stadiona, a stadion pripada jednoj reprezentaciji. Stadioni su prikazani i na stranici reprezentacije.
- Označavanje novog glavnog stadiona skida oznaku s prethodnog. Filtrirani jedinstveni indeks u bazi garantuje najviše jedan glavni stadion.
- Fotografija se dodaje, mijenja i briše kroz postojeći lokalni upload. Stadion bez fotografije prikazuje podrazumijevanu sliku.
- U obrascu utakmice nude se samo stadioni domaćina, a glavni se predlaže automatski. Može se izabrati i sporedni stadion.
- Server provjerava da stadion pripada domaćinu, i kad se domaćin kasnije promijeni.
- Stadion s utakmicama ne može se obrisati ni prebaciti na drugu reprezentaciju. Brisanje reprezentacije bez utakmica briše i njene stadione s fotografijama.

Utakmica može i dalje imati samo upisan naziv stadiona ("Drugi stadion"), na primjer za neutralni teren ili utakmice napravljene prije ovog modula.
Zato je kolona `Matches.stadium` zadržana, a nova kolona `Matches.stadiumId` je neobavezna. Kod povezanog stadiona prikazuje se njegov trenutni naziv.

Klase: `Stadium`, `StadiumRepository`, `StadiumService`, `StadiumController`, `StadiumForm`, `StadiumSurface`.
Izmijenjeno: `Match`, `MatchForm`, `MatchService`, `MatchController`, `MatchRepository`, `NationalTeam`, `NationalTeamService`, `TeamDetails`, šabloni utakmica i reprezentacija, navigacija, prevodi.
Novo: šabloni `stadiums/*`, `match-form.js`, `stadium-default.svg`, test `StadiumFlowTest`.

### Najviše 30 igrača

Ograničenje je povećano sa 26 na 30 igrača po reprezentaciji, a pravilo je u `PlayerService` (`MAX_PLAYERS_PER_TEAM`).

- Servis odbija 31. igrača porukom "Reprezentacija može imati maksimalno 30 igrača.".
- Odbija ga i kad se zahtjev pošalje mimo interfejsa, a obrazac za novog igrača se ne otvara za pun spisak.
- Stranica reprezentacije prikazuje broj igrača u obliku 18/30. Kod punog spiska dugme za dodavanje je onemogućeno i prikazuje se poruka.
- Na listi igrača puna reprezentacija je onemogućena u izboru za dodavanje.
- Brisanje igrača oslobađa mjesto. Postojeće pravilo ostaje: igrač iz sastava ili događaja ne može se obrisati.

Baza nema ograničenje broja igrača, pa izmjena baze nije potrebna.

### Prelazak na Javu 11

Aplikacija je prebačena sa Jave 21 i Spring Boot 3.5 na Javu 11 i Spring Boot 2.7, kako bi radila u okruženju s JDK 11,
najnovijim Eclipseom, SQL Serverom 2022 i SSMS-om. Funkcionalnosti, izgled, baza i SQL skripte nisu mijenjani.
Verzija za Javu 21 sačuvana je u folderu `national-team-manager-java21`.

| Izmjena | Razlog |
|---|---|
| `jakarta.*` paketi postaju `javax.*` | Spring Boot 2.7 koristi Java EE (javax) pakete |
| `record` klase postaju obične klase s getterima | `record` postoji tek od Jave 16 |
| Upiti u `""""` blokovima postaju obični stringovi | Text blocks postoje tek od Jave 15 |
| `.toList()` postaje `.collect(Collectors.toList())` | `Stream.toList()` postoji tek od Jave 16 |
| `switch` sa strelicom i `instanceof` s varijablom zamijenjeni klasičnim oblikom | Te konstrukcije postoje tek od Jave 14 i 16 |
| `count(...) > 0` u upitima postaje `case when ... then true else false end` | Hibernate 5 ne dozvoljava logički izraz u SELECT-u |
| `SecurityConfig`, `I18nConfig`, `AppUserPrincipal`, `GlobalExceptionHandler` | API-ji Spring Security 5 i Spring Framework 5 |
| H2 u testovima bez načina `MSSQLServer` | Hibernate 5 koristi `LIMIT`, koji H2 2.1 u tom načinu ne prihvata |

`pom.xml` postavlja `maven.compiler.release=11`, pa kompajler odbija svaku metodu koja ne postoji u Javi 11, i kad se gradi novijim JDK-om.

### Logotipi takmičenja

Uvedena je uloga administratora (`Users.role`: `USER` ili `ADMIN`) i zajednička, odnosno zvanična takmičenja (`Competitions.ownerId = NULL`).

- Administrator pravi zvanična takmičenja i jedini dodaje, mijenja i briše logotipe. Ostali korisnici vide i biraju zvanična takmičenja, ali ih ne mogu mijenjati (stranica 403).
- Korisnik i dalje pravi lična takmičenja, koja vidi samo on; ona nemaju logo i prikazuju podrazumijevanu sliku.
- Logo se čuva u `uploads/competitions`, a u bazi je samo relativna putanja `logoPath`. Dozvoljeni su PNG, JPG, JPEG i SVG do 5 MB. `CompetitionLogoStorage` provjerava da ekstenzija odgovara sadržaju i odbija SVG sa skriptom, događajima (`onload`) ili vanjskim linkovima. SVG se servira uz `Content-Security-Policy: sandbox`.
- Izmjena takmičenja bez novog fajla zadržava postojeći logo. Novi fajl zamjenjuje stari, a stari fajl se briše s diska. Logo se briše posebnim dugmetom na detaljima ili oznakom u obrascu.
- Logo se prikazuje na karticama takmičenja, na detaljima, u listi utakmica, na detaljima utakmice i u obrascu utakmice. U obrascu ga JavaScript (`match-form.js`) mijenja čim se izabere takmičenje, bez ponovnog učitavanja.
- Svaki korisnik u zvaničnom takmičenju vidi samo svoje utakmice.

### Povratak na Javu 21

Profesor je dozvolio novije verzije, pa je aplikacija vraćena na Javu 21, Spring Boot 3.5 i Hibernate 6, sa svim
funkcionalnostima, uključujući logotipe takmičenja. Izmjene iz tabele iznad su vraćene (`jakarta.*` paketi, `record` klase,
text blocks, H2 u testovima ponovo u načinu `MSSQLServer`). Baza i SQL skripte nisu mijenjani: šema i podaci na SQL Serveru
su prije i poslije pokretanja identični. Verzija za Javu 11 sačuvana je u folderu `national-team-manager-java11`.

## Izmjene baze

Sve izmjene postojeće baze su nedestruktivne:

| Skripta | Šta radi |
|---|---|
| `05_tactics_coach_language_upgrade.sql` | Dodaje kolone za taktiku, uloge, prethodno stanje, selektora, zastavu i jezik, te formaciju 5-3-2 |
| `06_player_weight_optional.sql` | Čini `Players.weightKg` neobaveznom; postojeće vrijednosti ostaju |
| `07_optional_drop_player_weight.sql` | Opciono i nepovratno briše kolonu `weightKg`; nije pokrenuta |
| `08_stadiums.sql` | Dodaje tabelu `Stadiums`, filtrirani indeks `UX_Stadiums_one_main_per_team` i neobaveznu kolonu `Matches.stadiumId` |
| `09_competition_logos.sql` | Dodaje `Users.role` (podrazumijevano `USER`), čini `Competitions.ownerId` neobaveznim, dodaje `Competitions.logoPath` i pet zvaničnih takmičenja bez logotipa |

Kolona `weightKg` je zadržana jer brisanje nije potrebno da bi aplikacija radila, a nepovratno bi izbrisalo upisane težine svih igrača. Aplikacija je ne čita niti upisuje. Nova instalacija preko `02_create_schema.sql` je nema.

## Rezultati testiranja

Nakon povratka na Javu 21 (JDK 21.0.13, Spring Boot 3.5): svih 67 testova prolazi, 39 provjera cijelog toka i 23 provjere logotipa prolaze na SQL Serveru 2022, a šema baze je ista prije i poslije pokretanja. Ranije, na Javi 11: kod se kompajlira za Javu 11 (`release 11`), svih 60 testova prolazi, a 39 provjera cijelog toka prolazi na SQL Serveru 2022. Pokretanje je provjereno na JDK 17, jer JDK 11 nije bio instaliran na računaru za provjeru.

| Provjera | Rezultat |
|---|---|
| Automatski testovi (H2 samo u testovima) | 67 od 67, uključujući 7 testova logotipa |
| Logotipi na pravom SQL Serveru: upload, zamjena, zadržavanje, brisanje, format, veličina, prava korisnika, utakmice, engleski | 23 od 23 |
| Stadioni na pravom SQL Serveru: pregled, validacije, glavni stadion, fotografija, utakmice, vlasništvo, engleski | radi |
| Migracije pokrenute dvaput na postojećoj bazi | bez grešaka |
| Kontrolni zbir postojećih igrača i utakmica prije i poslije | isti |
| Nova instalacija: skripte `02` i `04` na privremenoj bazi | radi; jedina razlika šeme je zadržana kolona `weightKg` |
| Tok taktike: pregled, zamjena, prevlačenje, čuvanje, baza, vraćanje | radi |
| Oba jezika na 20 stranica, pamćenje nakon odjave i prijave | radi |
| Fotografija selektora: dodavanje, promjena, brisanje, pogrešan i prevelik fajl | radi |
| Zastave: Srbija, BiH, Hrvatska, Njemačka, Brazil i nepoznata država | radi |
| Vlastita zastava: upload, promjena, uklanjanje, odbijen SVG | radi |
| Igrač bez težine: dodavanje, uređivanje, prikaz | radi |
| Trajnost nakon stvarnog restarta aplikacije | radi |
| Tuđi podaci, uključujući taktiku i zastavu | 404 |
| Širina telefona 375 px | bez horizontalnog klizača i preklapanja |

Nije provjereno: pokretanje u Eclipseu.

## Demonstracija

Logotipi: kao administrator uredite zvanično takmičenje i dodajte logo, pa pokažite karticu. U novoj utakmici izaberite takmičenje i pokažite da se logo pojavi odmah. Zatim se prijavite kao običan korisnik, pokažite da logo vidi, ali da otvaranje `/competitions/{id}/edit` daje stranicu 403.

Stadioni: dodajte dva stadiona jednoj reprezentaciji i označite drugi kao glavni, pa pokažite da je prvi izgubio oznaku. U novoj utakmici izaberite domaćina i pokažite da se nude samo njegovi stadioni. Zatim pokušajte obrisati stadion s utakmicom i pokažite poruku.

Taktika, jezik i zastave:

1. Otvorite utakmicu, napravite sastav i kliknite "Taktika".
2. Prevucite igrača s klupe na teren i pokažite da se fotografija promijenila.
3. Probajte staviti igrača iz polja na mjesto golmana i pokažite poruku.
4. Promijenite formaciju u 5-3-2, podesite stil igre i izvođače, pa kliknite "Sačuvaj taktiku".
5. Osvježite stranicu i pokažite da je sve ostalo, a zatim pokažite red u tabeli `Lineups`.
6. Otvorite drugu utakmicu i pokažite da ima svoju taktiku.
7. Kliknite "Vrati prethodni sastav".
8. Kliknite "English" i pokažite da se tekstovi mijenjaju bez restarta, a imena ne.
9. Odjavite se i prijavite ponovo, pa pokažite da je jezik ostao.
10. U novoj reprezentaciji upišite "Njemačka" i pokažite kako se zastava pojavljuje dok kucate.
