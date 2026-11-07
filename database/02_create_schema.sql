/*
    Kreira bazu NationalTeamManager i sve tabele. Skripta se može sigurno pokrenuti više puta.

    Svi strani ključevi koriste podrazumijevano ON DELETE NO ACTION. SQL Server odbija višestruke
    kaskadne putanje (npr. NationalTeams -> Players -> LineupPlayers i NationalTeams -> Lineups -> LineupPlayers),
    pa redoslijed brisanja povezanih podataka određuje servisni sloj aplikacije.
*/
IF DB_ID(N'NationalTeamManager') IS NULL
    CREATE DATABASE NationalTeamManager;
GO

USE NationalTeamManager;
GO

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
CREATE TABLE dbo.Users (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    fullName     NVARCHAR(100) NOT NULL,
    username     NVARCHAR(50)  NOT NULL,
    email        NVARCHAR(150) NOT NULL,
    passwordHash NVARCHAR(72)  NOT NULL,
    createdAt    DATETIME2(6)  NOT NULL,
    preferredLanguage NVARCHAR(5) NOT NULL CONSTRAINT DF_Users_preferredLanguage DEFAULT 'sr',
    role         NVARCHAR(10)  NOT NULL CONSTRAINT DF_Users_role DEFAULT 'USER',
    CONSTRAINT PK_Users PRIMARY KEY (id),
    CONSTRAINT CK_Users_preferredLanguage CHECK (preferredLanguage IN ('sr', 'en')),
    CONSTRAINT CK_Users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT UQ_Users_username UNIQUE (username),
    CONSTRAINT UQ_Users_email UNIQUE (email)
);
GO

IF OBJECT_ID(N'dbo.NationalTeams', N'U') IS NULL
CREATE TABLE dbo.NationalTeams (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    ownerId       BIGINT         NOT NULL,
    countryName   NVARCHAR(100)  NOT NULL,
    teamName      NVARCHAR(100)  NOT NULL,
    confederation NVARCHAR(20)   NOT NULL,
    coach         NVARCHAR(100)  NOT NULL,
    coachPhotoFileName NVARCHAR(100) NULL,
    flagFileName  NVARCHAR(100)  NULL,
    flagRemoved   BIT            NOT NULL CONSTRAINT DF_NationalTeams_flagRemoved DEFAULT 0,
    foundedYear   INT            NOT NULL,
    description   NVARCHAR(1000) NULL,
    CONSTRAINT PK_NationalTeams PRIMARY KEY (id),
    CONSTRAINT FK_NationalTeams_Users FOREIGN KEY (ownerId) REFERENCES dbo.Users (id),
    CONSTRAINT UQ_NationalTeams_owner_name UNIQUE (ownerId, teamName),
    CONSTRAINT CK_NationalTeams_confederation CHECK (confederation IN ('UEFA', 'CONMEBOL', 'CONCACAF', 'CAF', 'AFC', 'OFC'))
);
GO

IF OBJECT_ID(N'dbo.Players', N'U') IS NULL
CREATE TABLE dbo.Players (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    teamId         BIGINT        NOT NULL,
    firstName      NVARCHAR(50)  NOT NULL,
    lastName       NVARCHAR(50)  NOT NULL,
    birthDate      DATE          NOT NULL,
    nationality    NVARCHAR(100) NOT NULL,
    playerPosition NVARCHAR(20)  NOT NULL,
    jerseyNumber   INT           NOT NULL,
    heightCm       INT           NOT NULL,
    club           NVARCHAR(100) NULL,
    photoFileName  NVARCHAR(100) NULL,
    CONSTRAINT PK_Players PRIMARY KEY (id),
    CONSTRAINT FK_Players_NationalTeams FOREIGN KEY (teamId) REFERENCES dbo.NationalTeams (id),
    CONSTRAINT UQ_Players_team_jersey UNIQUE (teamId, jerseyNumber),
    CONSTRAINT CK_Players_jersey CHECK (jerseyNumber BETWEEN 1 AND 99),
    CONSTRAINT CK_Players_position CHECK (playerPosition IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'))
);
GO

IF OBJECT_ID(N'dbo.Competitions', N'U') IS NULL
CREATE TABLE dbo.Competitions (
    id        BIGINT IDENTITY(1,1) NOT NULL,
    ownerId   BIGINT        NULL,
    name      NVARCHAR(100) NOT NULL,
    season    NVARCHAR(20)  NOT NULL,
    type      NVARCHAR(30)  NOT NULL,
    startDate DATE          NOT NULL,
    endDate   DATE          NOT NULL,
    logoPath  NVARCHAR(255) NULL,
    CONSTRAINT PK_Competitions PRIMARY KEY (id),
    CONSTRAINT FK_Competitions_Users FOREIGN KEY (ownerId) REFERENCES dbo.Users (id),
    CONSTRAINT CK_Competitions_dates CHECK (endDate >= startDate),
    CONSTRAINT CK_Competitions_type CHECK (type IN ('WORLD_CUP', 'CONTINENTAL_CHAMPIONSHIP', 'QUALIFIERS', 'NATIONS_LEAGUE', 'FRIENDLY_TOURNAMENT'))
);
GO

IF OBJECT_ID(N'dbo.Stadiums', N'U') IS NULL
CREATE TABLE dbo.Stadiums (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    teamId        BIGINT         NOT NULL,
    name          NVARCHAR(100)  NOT NULL,
    city          NVARCHAR(100)  NOT NULL,
    country       NVARCHAR(100)  NOT NULL,
    capacity      INT            NOT NULL,
    builtYear     INT            NOT NULL,
    surface       NVARCHAR(20)   NOT NULL,
    address       NVARCHAR(200)  NOT NULL,
    description   NVARCHAR(1000) NULL,
    photoFileName NVARCHAR(100)  NULL,
    mainStadium   BIT            NOT NULL CONSTRAINT DF_Stadiums_mainStadium DEFAULT 0,
    CONSTRAINT PK_Stadiums PRIMARY KEY (id),
    CONSTRAINT FK_Stadiums_NationalTeams FOREIGN KEY (teamId) REFERENCES dbo.NationalTeams (id),
    CONSTRAINT UQ_Stadiums_team_name UNIQUE (teamId, name),
    CONSTRAINT CK_Stadiums_capacity CHECK (capacity > 0),
    CONSTRAINT CK_Stadiums_surface CHECK (surface IN ('NATURAL_GRASS', 'ARTIFICIAL_TURF'))
);
GO

-- Filtrirani indeks: reprezentacija može imati najviše jedan glavni stadion
SET QUOTED_IDENTIFIER ON;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_Stadiums_one_main_per_team' AND object_id = OBJECT_ID(N'dbo.Stadiums'))
    CREATE UNIQUE INDEX UX_Stadiums_one_main_per_team ON dbo.Stadiums (teamId) WHERE mainStadium = 1;
GO

IF OBJECT_ID(N'dbo.Matches', N'U') IS NULL
CREATE TABLE dbo.Matches (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    homeTeamId    BIGINT        NOT NULL,
    awayTeamId    BIGINT        NOT NULL,
    competitionId BIGINT        NULL,
    stadiumId     BIGINT        NULL,
    kickoff       DATETIME2(6)  NOT NULL,
    stadium       NVARCHAR(100) NOT NULL,
    status        NVARCHAR(20)  NOT NULL,
    homeGoals     INT           NULL,
    awayGoals     INT           NULL,
    CONSTRAINT PK_Matches PRIMARY KEY (id),
    CONSTRAINT FK_Matches_HomeTeam FOREIGN KEY (homeTeamId) REFERENCES dbo.NationalTeams (id),
    CONSTRAINT FK_Matches_AwayTeam FOREIGN KEY (awayTeamId) REFERENCES dbo.NationalTeams (id),
    CONSTRAINT FK_Matches_Competitions FOREIGN KEY (competitionId) REFERENCES dbo.Competitions (id),
    CONSTRAINT FK_Matches_Stadiums FOREIGN KEY (stadiumId) REFERENCES dbo.Stadiums (id),
    CONSTRAINT CK_Matches_different_teams CHECK (homeTeamId <> awayTeamId),
    CONSTRAINT CK_Matches_goals CHECK ((homeGoals IS NULL OR homeGoals >= 0) AND (awayGoals IS NULL OR awayGoals >= 0)),
    CONSTRAINT CK_Matches_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED'))
);
GO

IF OBJECT_ID(N'dbo.Lineups', N'U') IS NULL
CREATE TABLE dbo.Lineups (
    id        BIGINT IDENTITY(1,1) NOT NULL,
    matchId   BIGINT        NOT NULL,
    teamId    BIGINT        NOT NULL,
    captainId BIGINT        NOT NULL,
    formation NVARCHAR(20)  NOT NULL,
    tactic    NVARCHAR(500) NULL,
    playStyle       NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_playStyle DEFAULT 'POSSESSION',
    mentality       NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_mentality DEFAULT 'BALANCED',
    playWidth       NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_playWidth DEFAULT 'NORMAL',
    playTempo       NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_playTempo DEFAULT 'NORMAL',
    pressing        NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_pressing DEFAULT 'MEDIUM',
    defensiveLine   NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_defensiveLine DEFAULT 'NORMAL',
    passingStyle    NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_passingStyle DEFAULT 'MIXED',
    attackDirection NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_attackDirection DEFAULT 'THROUGH_MIDDLE',
    penaltyTakerId  BIGINT NULL,
    freeKickTakerId BIGINT NULL,
    cornerTakerId   BIGINT NULL,
    previousState   NVARCHAR(4000) NULL,
    CONSTRAINT PK_Lineups PRIMARY KEY (id),
    CONSTRAINT FK_Lineups_Matches FOREIGN KEY (matchId) REFERENCES dbo.Matches (id),
    CONSTRAINT FK_Lineups_NationalTeams FOREIGN KEY (teamId) REFERENCES dbo.NationalTeams (id),
    CONSTRAINT FK_Lineups_Captain FOREIGN KEY (captainId) REFERENCES dbo.Players (id),
    CONSTRAINT FK_Lineups_PenaltyTaker FOREIGN KEY (penaltyTakerId) REFERENCES dbo.Players (id),
    CONSTRAINT FK_Lineups_FreeKickTaker FOREIGN KEY (freeKickTakerId) REFERENCES dbo.Players (id),
    CONSTRAINT FK_Lineups_CornerTaker FOREIGN KEY (cornerTakerId) REFERENCES dbo.Players (id),
    CONSTRAINT UQ_Lineups_match_team UNIQUE (matchId, teamId),
    CONSTRAINT CK_Lineups_formation CHECK (formation IN ('F_4_3_3', 'F_4_4_2', 'F_3_5_2', 'F_4_2_3_1', 'F_3_4_3', 'F_5_3_2')),
    CONSTRAINT CK_Lineups_playStyle CHECK (playStyle IN ('POSSESSION', 'COUNTER_ATTACK', 'HIGH_PRESS', 'DEFENSIVE', 'DIRECT')),
    CONSTRAINT CK_Lineups_mentality CHECK (mentality IN ('VERY_DEFENSIVE', 'DEFENSIVE', 'BALANCED', 'ATTACKING', 'VERY_ATTACKING')),
    CONSTRAINT CK_Lineups_playWidth CHECK (playWidth IN ('NARROW', 'NORMAL', 'WIDE')),
    CONSTRAINT CK_Lineups_playTempo CHECK (playTempo IN ('SLOW', 'NORMAL', 'FAST')),
    CONSTRAINT CK_Lineups_pressing CHECK (pressing IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT CK_Lineups_defensiveLine CHECK (defensiveLine IN ('LOW', 'NORMAL', 'HIGH')),
    CONSTRAINT CK_Lineups_passingStyle CHECK (passingStyle IN ('SHORT', 'MIXED', 'LONG')),
    CONSTRAINT CK_Lineups_attackDirection CHECK (attackDirection IN ('THROUGH_MIDDLE', 'THROUGH_WINGS'))
);
GO

IF OBJECT_ID(N'dbo.LineupPlayers', N'U') IS NULL
CREATE TABLE dbo.LineupPlayers (
    id        BIGINT IDENTITY(1,1) NOT NULL,
    lineupId  BIGINT       NOT NULL,
    playerId  BIGINT       NOT NULL,
    role      NVARCHAR(20) NOT NULL,
    slotIndex INT          NULL,
    duty      NVARCHAR(20) NOT NULL CONSTRAINT DF_LineupPlayers_duty DEFAULT 'BALANCED',
    CONSTRAINT PK_LineupPlayers PRIMARY KEY (id),
    CONSTRAINT FK_LineupPlayers_Lineups FOREIGN KEY (lineupId) REFERENCES dbo.Lineups (id),
    CONSTRAINT FK_LineupPlayers_Players FOREIGN KEY (playerId) REFERENCES dbo.Players (id),
    CONSTRAINT UQ_LineupPlayers_lineup_player UNIQUE (lineupId, playerId),
    CONSTRAINT CK_LineupPlayers_role CHECK (role IN ('STARTER', 'SUBSTITUTE')),
    CONSTRAINT CK_LineupPlayers_duty CHECK (duty IN ('OFFENSIVE', 'BALANCED', 'DEFENSIVE')),
    CONSTRAINT CK_LineupPlayers_slot CHECK (
        (role = 'STARTER' AND slotIndex BETWEEN 0 AND 10) OR (role = 'SUBSTITUTE' AND slotIndex IS NULL))
);
GO

IF OBJECT_ID(N'dbo.MatchEvents', N'U') IS NULL
CREATE TABLE dbo.MatchEvents (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    matchId     BIGINT        NOT NULL,
    playerId    BIGINT        NOT NULL,
    eventMinute INT           NOT NULL,
    type        NVARCHAR(20)  NOT NULL,
    description NVARCHAR(255) NULL,
    CONSTRAINT PK_MatchEvents PRIMARY KEY (id),
    CONSTRAINT FK_MatchEvents_Matches FOREIGN KEY (matchId) REFERENCES dbo.Matches (id),
    CONSTRAINT FK_MatchEvents_Players FOREIGN KEY (playerId) REFERENCES dbo.Players (id),
    CONSTRAINT CK_MatchEvents_minute CHECK (eventMinute BETWEEN 1 AND 130),
    CONSTRAINT CK_MatchEvents_type CHECK (type IN ('GOAL', 'ASSIST', 'YELLOW_CARD', 'RED_CARD', 'SUBSTITUTION'))
);
GO

-- Zvanična takmičenja (bez vlasnika); logo i tačne datume postavlja administrator
INSERT INTO dbo.Competitions (ownerId, name, season, type, startDate, endDate)
SELECT NULL, c.name, c.season, c.type, c.startDate, c.endDate
FROM (VALUES
    (N'FIFA World Cup', N'2026', 'WORLD_CUP', '2026-06-11', '2026-07-19'),
    (N'UEFA European Championship (EURO)', N'2028', 'CONTINENTAL_CHAMPIONSHIP', '2028-06-09', '2028-07-09'),
    (N'UEFA Nations League', N'2026/27', 'NATIONS_LEAGUE', '2026-09-01', '2027-06-30'),
    (N'UEFA World Cup Qualifiers', N'2025/26', 'QUALIFIERS', '2025-03-01', '2026-03-31'),
    (N'UEFA European Qualifiers', N'2027/28', 'QUALIFIERS', '2027-03-01', '2028-03-31')
) AS c (name, season, type, startDate, endDate)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Competitions x WHERE x.ownerId IS NULL AND x.name = c.name);
GO
