/*
    Početni testni podaci za demonstraciju.
    Prijava: korisničko ime demo, lozinka Demo1234! (u bazi je samo BCrypt hash).
    Datumi su relativni u odnosu na dan pokretanja, pa predstojeće utakmice ostaju u budućnosti.
    Svi igrači i selektori su izmišljeni.

    sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\04_seed_data.sql
*/
USE NationalTeamManager;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

IF EXISTS (SELECT 1 FROM dbo.Users WHERE username = N'demo')
BEGIN
    PRINT N'Demo podaci već postoje, skripta nije ništa promijenila.';
    RETURN;
END;

BEGIN TRANSACTION;

DECLARE @now DATETIME2(0) = SYSDATETIME();
DECLARE @today DATE = CAST(@now AS DATE);

INSERT INTO dbo.Users (fullName, username, email, passwordHash, createdAt)
VALUES (N'Demo Korisnik', N'demo', N'demo@example.com',
        N'$2a$10$WDmzLw9Y/YMVwSjctQBWgO25nuHvI2TdovlghnSd24Lv60AIu6r2C', @now);
DECLARE @owner BIGINT = SCOPE_IDENTITY();
-- Demo nalog je administrator, da se mogu isprobati logotipi zvaničnih takmičenja
UPDATE dbo.Users SET role = 'ADMIN' WHERE id = @owner;

-- Reprezentacije
INSERT INTO dbo.NationalTeams (ownerId, countryName, teamName, confederation, coach, foundedYear, description)
VALUES (@owner, N'Bosna i Hercegovina', N'Zmajevi', 'UEFA', N'Anel Selimović', 1992, N'Reprezentacija koja gradi igru na brzim krilima i visokom presingu.');
DECLARE @zmajevi BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.NationalTeams (ownerId, countryName, teamName, confederation, coach, foundedYear, description)
VALUES (@owner, N'Hrvatska', N'Vatreni', 'UEFA', N'Ivan Horvat', 1912, N'Iskusan tim s jakim veznim redom.');
DECLARE @vatreni BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.NationalTeams (ownerId, countryName, teamName, confederation, coach, foundedYear, description)
VALUES (@owner, N'Srbija', N'Orlovi', 'UEFA', N'Marko Stanković', 1919, N'Fizički snažna ekipa, opasna iz prekida.');
DECLARE @orlovi BIGINT = SCOPE_IDENTITY();

-- Igrači: dresovi 1 i 12 su golmani, 2-5 i 13 odbrana, 6-8 i 14 vezni red, 9-11, 15 i 16 napad
DECLARE @squads TABLE (squad INT, jersey INT, firstName NVARCHAR(50), lastName NVARCHAR(50), birthDate DATE, club NVARCHAR(100));
INSERT INTO @squads VALUES
(1, 1, N'Amar', N'Hadžić', '1996-03-14', N'FK Sarajevo'),
(1, 2, N'Tarik', N'Kovačević', '1998-07-02', N'FK Željezničar'),
(1, 3, N'Emir', N'Delić', '1995-11-23', N'HŠK Zrinjski'),
(1, 4, N'Nedim', N'Begić', '1997-01-30', N'FK Velež'),
(1, 5, N'Haris', N'Mehić', '1999-05-18', N'FK Borac'),
(1, 6, N'Adnan', N'Šabić', '1994-09-09', N'NK Široki Brijeg'),
(1, 7, N'Kenan', N'Zukić', '2000-02-12', N'FK Sarajevo'),
(1, 8, N'Dino', N'Ćosić', '1998-12-05', N'FK Željezničar'),
(1, 9, N'Vedad', N'Mujić', '1996-06-27', N'HŠK Zrinjski'),
(1, 10, N'Eldar', N'Omerović', '1997-10-16', N'FK Velež'),
(1, 11, N'Sanel', N'Karić', '2001-04-08', N'FK Borac'),
(1, 12, N'Jasmin', N'Halilović', '1999-08-21', N'NK Čelik'),
(1, 13, N'Mirza', N'Softić', '2000-11-03', N'FK Tuzla City'),
(1, 14, N'Almir', N'Džafić', '2002-01-15', N'FK Sloboda'),
(1, 15, N'Armin', N'Rizvić', '2001-07-29', N'NK Čelik'),
(1, 16, N'Benjamin', N'Avdić', '2003-03-11', N'FK Sarajevo'),
(2, 1, N'Luka', N'Marić', '1995-02-17', N'GNK Dinamo'),
(2, 2, N'Ante', N'Šarić', '1997-06-06', N'HNK Hajduk'),
(2, 3, N'Josip', N'Jurić', '1996-10-25', N'HNK Rijeka'),
(2, 4, N'Mateo', N'Tomić', '1998-04-13', N'NK Osijek'),
(2, 5, N'Filip', N'Babić', '1999-09-01', N'GNK Dinamo'),
(2, 6, N'Marko', N'Vuković', '1994-12-19', N'HNK Hajduk'),
(2, 7, N'Ivan', N'Perić', '1997-03-28', N'HNK Rijeka'),
(2, 8, N'Nikola', N'Knežević', '2000-08-10', N'NK Osijek'),
(2, 9, N'Dario', N'Lončar', '1996-05-22', N'NK Lokomotiva'),
(2, 10, N'Tomislav', N'Barić', '1998-01-07', N'GNK Dinamo'),
(2, 11, N'Petar', N'Rukavina', '2001-11-14', N'HNK Hajduk'),
(2, 12, N'Domagoj', N'Grgić', '2000-06-30', N'NK Istra'),
(2, 13, N'Bruno', N'Pavić', '2002-02-24', N'NK Varaždin'),
(2, 14, N'Karlo', N'Matić', '2001-05-09', N'NK Gorica'),
(2, 15, N'Leon', N'Bošnjak', '2003-09-17', N'NK Lokomotiva'),
(2, 16, N'Roko', N'Vidović', '2002-12-02', N'HNK Rijeka'),
(3, 1, N'Stefan', N'Petrović', '1996-04-04', N'FK Crvena zvezda'),
(3, 2, N'Nemanja', N'Lukić', '1997-08-15', N'FK Partizan'),
(3, 3, N'Aleksa', N'Ilić', '1995-01-26', N'FK Vojvodina'),
(3, 4, N'Miloš', N'Đurić', '1998-11-11', N'FK Čukarički'),
(3, 5, N'Luka', N'Nikolić', '1999-03-03', N'FK Crvena zvezda'),
(3, 6, N'Dušan', N'Janković', '1994-07-20', N'FK Partizan'),
(3, 7, N'Vuk', N'Marković', '2000-10-08', N'FK Vojvodina'),
(3, 8, N'Andrija', N'Ristić', '1997-12-31', N'TSC'),
(3, 9, N'Strahinja', N'Pavlović', '1996-02-14', N'FK Crvena zvezda'),
(3, 10, N'Filip', N'Stojanović', '1998-06-19', N'FK Partizan'),
(3, 11, N'Lazar', N'Savić', '2001-09-25', N'FK Radnički'),
(3, 12, N'Bogdan', N'Popović', '1999-05-05', N'FK Spartak'),
(3, 13, N'Marko', N'Živković', '2001-01-22', N'FK Napredak'),
(3, 14, N'Ognjen', N'Todorović', '2002-04-12', N'FK Vojvodina'),
(3, 15, N'Uroš', N'Mitrović', '2003-07-07', N'TSC'),
(3, 16, N'Nikola', N'Ćirić', '2002-10-28', N'FK Radnički');

INSERT INTO dbo.Players (teamId, firstName, lastName, birthDate, nationality, playerPosition, jerseyNumber, heightCm, club)
SELECT CASE s.squad WHEN 1 THEN @zmajevi WHEN 2 THEN @vatreni ELSE @orlovi END,
       s.firstName, s.lastName, s.birthDate,
       CASE s.squad WHEN 1 THEN N'Bosna i Hercegovina' WHEN 2 THEN N'Hrvatska' ELSE N'Srbija' END,
       CASE WHEN s.jersey IN (1, 12) THEN 'GOALKEEPER'
            WHEN s.jersey BETWEEN 2 AND 5 OR s.jersey = 13 THEN 'DEFENDER'
            WHEN s.jersey BETWEEN 6 AND 8 OR s.jersey = 14 THEN 'MIDFIELDER'
            ELSE 'FORWARD' END,
       s.jersey, 172 + (s.jersey * 3) % 22, s.club
FROM @squads s;

-- Stadioni
INSERT INTO dbo.Stadiums (teamId, name, city, country, capacity, builtYear, surface, address, description, mainStadium)
VALUES (@zmajevi, N'Stadion Asim Ferhatović Hase', N'Sarajevo', N'Bosna i Hercegovina', 34500, 1947, 'NATURAL_GRASS', N'Patriotske lige 35', N'Glavni stadion reprezentacije.', 1),
       (@zmajevi, N'Stadion Bilino polje', N'Zenica', N'Bosna i Hercegovina', 15600, 1972, 'NATURAL_GRASS', N'Aska Borića bb', NULL, 0),
       (@vatreni, N'Stadion Maksimir', N'Zagreb', N'Hrvatska', 35000, 1912, 'NATURAL_GRASS', N'Maksimirska cesta 128', NULL, 1),
       (@orlovi, N'Stadion Rajko Mitić', N'Beograd', N'Srbija', 51800, 1963, 'NATURAL_GRASS', N'Ljutice Bogdana 1a', NULL, 1);

-- Takmičenja
INSERT INTO dbo.Competitions (ownerId, name, season, type, startDate, endDate)
VALUES (@owner, N'Balkanski kup', N'2026/27', 'FRIENDLY_TOURNAMENT', DATEADD(DAY, -60, @today), DATEADD(DAY, 120, @today));
DECLARE @kup BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.Competitions (ownerId, name, season, type, startDate, endDate)
VALUES (@owner, N'Kvalifikacije za Evropsko prvenstvo', N'2027', 'QUALIFIERS', DATEADD(DAY, 20, @today), DATEADD(DAY, 300, @today));
DECLARE @kvalifikacije BIGINT = SCOPE_IDENTITY();

-- Utakmice
INSERT INTO dbo.Matches (homeTeamId, awayTeamId, competitionId, kickoff, stadium, status, homeGoals, awayGoals)
VALUES (@zmajevi, @vatreni, @kup, DATEADD(DAY, -30, @now), N'Stadion Asim Ferhatović Hase', 'FINISHED', 2, 1);
DECLARE @m1 BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.Matches (homeTeamId, awayTeamId, competitionId, kickoff, stadium, status, homeGoals, awayGoals)
VALUES (@vatreni, @orlovi, NULL, DATEADD(DAY, -20, @now), N'Stadion Maksimir', 'FINISHED', 1, 1);
DECLARE @m2 BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.Matches (homeTeamId, awayTeamId, competitionId, kickoff, stadium, status, homeGoals, awayGoals)
VALUES (@orlovi, @zmajevi, @kup, DATEADD(DAY, -10, @now), N'Stadion Rajko Mitić', 'FINISHED', 0, 2);
DECLARE @m3 BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.Matches (homeTeamId, awayTeamId, competitionId, kickoff, stadium, status)
VALUES (@zmajevi, @orlovi, @kup, DATEADD(DAY, 10, @now), N'Stadion Bilino polje', 'SCHEDULED');
DECLARE @m4 BIGINT = SCOPE_IDENTITY();

-- Utakmice povezane sa stadionom domaćina; jedna se igra i na stadionu koji nije glavni
UPDATE m SET stadiumId = s.id
FROM dbo.Matches m JOIN dbo.Stadiums s ON s.teamId = m.homeTeamId AND s.name = m.stadium
WHERE m.homeTeamId IN (@zmajevi, @vatreni, @orlovi);

INSERT INTO dbo.Matches (homeTeamId, awayTeamId, competitionId, kickoff, stadium, status)
VALUES (@orlovi, @vatreni, @kvalifikacije, DATEADD(DAY, 30, @now), N'Stadion Karađorđe', 'SCHEDULED');

INSERT INTO dbo.Matches (homeTeamId, awayTeamId, competitionId, kickoff, stadium, status)
VALUES (@vatreni, @zmajevi, NULL, DATEADD(DAY, 45, @now), N'Stadion Poljud', 'CANCELLED');

-- Sastavi: prvih 11 dresova je u početnoj postavi (pozicija = dres - 1), a dresovi 12-16 su na klupi
DECLARE @lineups TABLE (rowNo INT IDENTITY(1,1), matchId BIGINT, teamId BIGINT, formation NVARCHAR(20), captainJersey INT, tactic NVARCHAR(500));
INSERT INTO @lineups (matchId, teamId, formation, captainJersey, tactic) VALUES
(@m1, @zmajevi, 'F_4_3_3', 6, N'Visok presing i brza tranzicija preko krila.'),
(@m1, @vatreni, 'F_4_4_2', 6, N'Kontrola sredine terena i igra iz drugog vala.'),
(@m2, @vatreni, 'F_4_2_3_1', 6, NULL),
(@m2, @orlovi, 'F_3_5_2', 6, N'Tri štopera i opasni prekidi.'),
(@m3, @orlovi, 'F_3_4_3', 6, NULL),
(@m3, @zmajevi, 'F_4_3_3', 6, N'Duboka odbrana i kontranapadi.'),
(@m4, @zmajevi, 'F_4_4_2', 10, N'Dva napadača i centaršutevi s krila.');

INSERT INTO dbo.Lineups (matchId, teamId, captainId, formation, tactic)
SELECT l.matchId, l.teamId, p.id, l.formation, l.tactic
FROM @lineups l
JOIN dbo.Players p ON p.teamId = l.teamId AND p.jerseyNumber = l.captainJersey
ORDER BY l.rowNo;

INSERT INTO dbo.LineupPlayers (lineupId, playerId, role, slotIndex)
SELECT lu.id, p.id,
       CASE WHEN p.jerseyNumber <= 11 THEN 'STARTER' ELSE 'SUBSTITUTE' END,
       CASE WHEN p.jerseyNumber <= 11 THEN p.jerseyNumber - 1 END
FROM dbo.Lineups lu
JOIN dbo.Players p ON p.teamId = lu.teamId
WHERE lu.matchId IN (@m1, @m2, @m3, @m4);

-- Različite taktike na različitim utakmicama
UPDATE lu
SET playStyle = 'HIGH_PRESS', mentality = 'ATTACKING', playWidth = 'WIDE', playTempo = 'FAST', pressing = 'HIGH',
    defensiveLine = 'HIGH', passingStyle = 'SHORT', attackDirection = 'THROUGH_WINGS',
    penaltyTakerId = (SELECT id FROM dbo.Players WHERE teamId = lu.teamId AND jerseyNumber = 9),
    freeKickTakerId = (SELECT id FROM dbo.Players WHERE teamId = lu.teamId AND jerseyNumber = 10),
    cornerTakerId = (SELECT id FROM dbo.Players WHERE teamId = lu.teamId AND jerseyNumber = 7)
FROM dbo.Lineups lu
WHERE lu.matchId = @m1 AND lu.teamId = @zmajevi;

UPDATE lu
SET playStyle = 'COUNTER_ATTACK', mentality = 'DEFENSIVE', pressing = 'LOW', defensiveLine = 'LOW',
    passingStyle = 'LONG',
    penaltyTakerId = (SELECT id FROM dbo.Players WHERE teamId = lu.teamId AND jerseyNumber = 10)
FROM dbo.Lineups lu
WHERE lu.matchId = @m3 AND lu.teamId = @zmajevi;

-- Događaji; golovi odgovaraju rezultatima utakmica
DECLARE @events TABLE (matchId BIGINT, teamId BIGINT, jersey INT, eventMinute INT, type NVARCHAR(20), description NVARCHAR(255));
INSERT INTO @events VALUES
(@m1, @zmajevi, 9, 17, 'GOAL', N'Udarac s ivice šesnaesterca'),
(@m1, @zmajevi, 7, 17, 'ASSIST', NULL),
(@m1, @vatreni, 6, 34, 'YELLOW_CARD', N'Prekršaj na sredini terena'),
(@m1, @vatreni, 10, 52, 'GOAL', N'Glavom nakon kornera'),
(@m1, @zmajevi, 14, 63, 'SUBSTITUTION', N'Ulazi umjesto Dine Ćosića'),
(@m1, @zmajevi, 9, 81, 'GOAL', N'Iz kontranapada'),
(@m1, @zmajevi, 14, 81, 'ASSIST', NULL),
(@m2, @orlovi, 9, 28, 'GOAL', NULL),
(@m2, @orlovi, 3, 44, 'YELLOW_CARD', NULL),
(@m2, @vatreni, 11, 70, 'GOAL', N'Precizan udarac u donji ugao'),
(@m2, @vatreni, 15, 75, 'SUBSTITUTION', N'Ulazi umjesto Darija Lončara'),
(@m3, @zmajevi, 10, 12, 'GOAL', N'Sa bijele tačke'),
(@m3, @orlovi, 4, 11, 'YELLOW_CARD', N'Prekršaj u kaznenom prostoru'),
(@m3, @orlovi, 4, 58, 'YELLOW_CARD', N'Drugi žuti karton'),
(@m3, @orlovi, 4, 58, 'RED_CARD', N'Isključenje nakon drugog žutog'),
(@m3, @zmajevi, 9, 76, 'GOAL', NULL),
(@m3, @zmajevi, 8, 76, 'ASSIST', NULL);

INSERT INTO dbo.MatchEvents (matchId, playerId, eventMinute, type, description)
SELECT e.matchId, p.id, e.eventMinute, e.type, e.description
FROM @events e
JOIN dbo.Players p ON p.teamId = e.teamId AND p.jerseyNumber = e.jersey;

COMMIT TRANSACTION;
PRINT N'Demo podaci su uspješno dodani.';
GO
