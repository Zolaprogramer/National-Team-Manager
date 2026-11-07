# Analiza i dizajn

## Slojevi aplikacije

Zahtjev prolazi ovim putem:

`preglednik -> Spring Security filteri -> Controller -> Service -> Repository -> SQL Server`

- **Controller** prima zahtjev, pokreće validaciju obrasca i bira stranicu. Ne sadrži poslovna pravila.
- **Service** sadrži poslovna pravila i granice transakcija (`@Transactional`).
- **Repository** pristupa bazi preko Spring Data JPA.
- **Entity** predstavlja tabelu i štiti vlastito stanje metodama kao što su `updateDetails` i `addGoalFor`.
- **DTO** prenosi podatke između prikaza i servisa, pa korisnik obrascem ne može postaviti polja poput ID-a ili vlasnika.

## Entiteti i veze

| Veza | Kardinalnost | Strani ključ | Napomena |
|---|---|---|---|
| Users - NationalTeams | 1:N | `NationalTeams.ownerId` | vlasnik reprezentacije |
| Users - Competitions | 0..1:N | `Competitions.ownerId` | vlasnik ličnog takmičenja; NULL znači zvanično takmičenje |
| NationalTeams - Players | 1:N | `Players.teamId` | igrač pripada tačno jednoj reprezentaciji |
| NationalTeams - Matches (domaćin) | 1:N | `Matches.homeTeamId` | |
| NationalTeams - Matches (gost) | 1:N | `Matches.awayTeamId` | |
| Competitions - Matches | 0..1:N | `Matches.competitionId` | NULL znači prijateljska utakmica |
| NationalTeams - Stadiums | 1:N | `Stadiums.teamId` | najviše jedan glavni stadion po reprezentaciji |
| Stadiums - Matches | 0..1:N | `Matches.stadiumId` | NULL znači da je naziv stadiona samo upisan |
| Matches - Lineups | 1:0..2 | `Lineups.matchId` | jedinstveno (matchId, teamId) |
| NationalTeams - Lineups | 1:N | `Lineups.teamId` | |
| Players - Lineups (kapiten) | 1:N | `Lineups.captainId` | jedan kapiten po sastavu |
| Lineups - LineupPlayers | 1:N | `LineupPlayers.lineupId` | prvih 11 i rezerve |
| Players - LineupPlayers | 1:N | `LineupPlayers.playerId` | jedinstveno (lineupId, playerId) |
| Matches - MatchEvents | 1:N | `MatchEvents.matchId` | |
| Players - MatchEvents | 1:N | `MatchEvents.playerId` | |

`LineupPlayers` je vezna tabela za vezu M:N između sastava i igrača. Modelirana je kao poseban entitet jer veza ima svoje
podatke: ulogu (`role`) i poziciju u formaciji (`slotIndex`).

Vlasnik utakmice se ne čuva posebno, nego se izvodi iz domaće reprezentacije. Servis garantuje da obje reprezentacije imaju
istog vlasnika, pa bi dodatna kolona bila samo redundantan podatak koji može postati neusklađen.

`TacticalSettings` je `@Embeddable`: postavke su kolone tabele `Lineups`, jer svaki sastav ima tačno jednu taktiku i posebna
tabela ne bi donijela ništa. Izvođači prekida su, kao i kapiten, strani ključevi u `Lineups`. Individualna uloga je kolona
`LineupPlayers.duty`, jer zavisi i od igrača i od sastava.

Uloga korisnika je kolona `Users.role` (`USER` ili `ADMIN`), a ne posebna tabela, jer aplikacija ima samo dvije fiksne uloge.
Logo takmičenja je fajl na disku. U bazi je samo relativna putanja `Competitions.logoPath`, pa baza ostaje mala, a fajl se lako
zamijeni ili obriše.

Zastava se ne čuva kao poseban entitet. `NationalTeams` ima samo naziv vlastite slike (`flagFileName`) i oznaku da je zastava
uklonjena (`flagRemoved`), a zastava države se izračunava iz naziva države.

## Problemi u poslovnoj logici i rješenja

| Problem | Rješenje |
|---|---|
| Sve reprezentacije pripadaju jednom korisniku, pa je svaka odlučena utakmica i pobjeda i poraz. | Ukupne pobjede i porazi su uvijek jednaki, što stranica i objašnjava. Stvarni učinak se vidi u tabeli po reprezentacijama. |
| Rezultat i evidentirani golovi mogu postati neusklađeni. | Gol automatski povećava rezultat, a brisanje gola ga smanjuje. Ručni rezultat smije biti veći od broja golova (strijelac ne mora biti upisan), ali ne i manji. |
| Asistencija bez gola. | Reprezentacija ne može imati više asistencija nego golova, a gol se ne može obrisati dok bi asistencija ostala bez njega. |
| Višestruke kaskadne putanje. | SQL Server odbija takve `ON DELETE CASCADE` ključeve, pa baza koristi `NO ACTION`, a servis određuje redoslijed brisanja. |
| Brisanje bi moglo promijeniti historiju. | Reprezentacija s utakmicama i igrač u sastavu ili događaju se ne brišu. Brisanje utakmice briše njene sastave i događaje. Brisanje takmičenja pretvara utakmice u prijateljske. |
| Promjena reprezentacija na utakmici koja već ima sastave. | Zabranjena je dok utakmica ima sastave ili događaje. |
| Povratak završene utakmice u status "zakazana". | Zabranjen je dok postoje događaji. |
| Reprezentacija u dvije utakmice istovremeno. | Reprezentacija može imati jednu neotkazanu utakmicu dnevno. |
| Datum i status si protivrječe. | Zakazana utakmica mora biti u budućnosti, a utakmica u toku ili završena ne smije biti. Datum mora biti unutar perioda takmičenja. |
| Ponovno čuvanje sastava s istim igračima. | Hibernate izvršava INSERT prije DELETE, što bi narušilo jedinstveni ključ, pa se stari redovi brišu pozivom `flush` prije dodavanja novih. |
| Izvođač prekida izlazi iz početnih 11. | Uloga se uklanja, a igrači koji ostaju zadržavaju svoje uloge i kad se sastav mijenja običnim obrascem. |
| Vraćanje prethodnog sastava. | Stanje prije čuvanja je JSON u `Lineups.previousState`. Vraćanje prolazi kroz ista pravila, pa neispravno stanje ne može ući u bazu. |
| Poslovne poruke na više jezika. | Izuzetak nosi ključ poruke i argumente, a tekst se bira tek pri prikazu. Servisi tako ne zavise od jezika. |
| Težina više nije potrebna. | Kolona `weightKg` je samo postala neobavezna, jer bi njeno brisanje nepovratno izbrisalo postojeće podatke. |
| Dva glavna stadiona. | Servis prvo skida staru oznaku i odmah je upisuje, a filtrirani jedinstveni indeks to štiti i u bazi. |
| Stadion koji ne pripada domaćinu. | Provjerava se na serveru pri svakom čuvanju utakmice, pa i kad se promijeni domaćin. |
| Brisanje stadiona s utakmicama. | Zabranjeno je, kao i prebacivanje na drugu reprezentaciju, da utakmica ne ostane bez ispravnog stadiona. |
| Tuđi podaci. | Svaki upit filtrira po vlasniku. Tuđi ID daje 404, pa se ne može ni saznati da podatak postoji. |
| Slova č, ć, š, đ i ž. | Kolone su NVARCHAR, a Hibernate šalje nacionalizovane stringove. |
| Lažne fotografije. | Tip se provjerava po prvim bajtovima fajla, naziv generiše aplikacija, a fotografiju može dobiti samo vlasnik. |
| Dva zahtjeva istovremeno prelaze granicu od 30 igrača. | Poznato ograničenje: granica se provjerava u servisu, bez zaključavanja, što je dovoljno za aplikaciju s jednim korisnikom po nalogu. Jedinstvenost broja dresa dodatno štiti baza. |

## Gdje se šta validira

1. **Bean Validation na obrascu**: obavezna polja, rasponi i format (`@NotBlank`, `@Min`, `@Past`, vlastite anotacije).
2. **Servis**: pravila koja traže podatke iz baze (jedinstven dres, najviše 30 igrača, sastav).
3. **Baza**: jedinstveni ključevi, strani ključevi i CHECK ograničenja kao posljednja linija odbrane.

JavaScript u obrascu sastava samo olakšava unos. Server sve provjerava nezavisno od njega.
