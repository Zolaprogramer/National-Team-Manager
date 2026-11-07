/*
    Nadogradnja postojeće baze za taktičku tablu, fotografiju selektora, zastave i izbor jezika.
    Postojeći podaci ostaju netaknuti: skripta samo dodaje kolone i ograničenja, a može se pokrenuti više puta.
    Nova instalacija ovo ne treba, jer 02_create_schema.sql već sadrži iste kolone.

    sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\05_tactics_coach_language_upgrade.sql
*/
USE NationalTeamManager;
GO

-- Nova formacija 5-3-2
IF OBJECT_ID(N'dbo.CK_Lineups_formation', N'C') IS NOT NULL
    ALTER TABLE dbo.Lineups DROP CONSTRAINT CK_Lineups_formation;
GO
ALTER TABLE dbo.Lineups ADD CONSTRAINT CK_Lineups_formation
    CHECK (formation IN ('F_4_3_3', 'F_4_4_2', 'F_3_5_2', 'F_4_2_3_1', 'F_3_4_3', 'F_5_3_2'));
GO

-- Taktičke postavke; postojeći sastavi dobijaju neutralne vrijednosti
IF COL_LENGTH(N'dbo.Lineups', N'playStyle') IS NULL
    ALTER TABLE dbo.Lineups ADD playStyle NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_playStyle DEFAULT 'POSSESSION'
        CONSTRAINT CK_Lineups_playStyle CHECK (playStyle IN ('POSSESSION', 'COUNTER_ATTACK', 'HIGH_PRESS', 'DEFENSIVE', 'DIRECT'));
IF COL_LENGTH(N'dbo.Lineups', N'mentality') IS NULL
    ALTER TABLE dbo.Lineups ADD mentality NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_mentality DEFAULT 'BALANCED'
        CONSTRAINT CK_Lineups_mentality CHECK (mentality IN ('VERY_DEFENSIVE', 'DEFENSIVE', 'BALANCED', 'ATTACKING', 'VERY_ATTACKING'));
IF COL_LENGTH(N'dbo.Lineups', N'playWidth') IS NULL
    ALTER TABLE dbo.Lineups ADD playWidth NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_playWidth DEFAULT 'NORMAL'
        CONSTRAINT CK_Lineups_playWidth CHECK (playWidth IN ('NARROW', 'NORMAL', 'WIDE'));
IF COL_LENGTH(N'dbo.Lineups', N'playTempo') IS NULL
    ALTER TABLE dbo.Lineups ADD playTempo NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_playTempo DEFAULT 'NORMAL'
        CONSTRAINT CK_Lineups_playTempo CHECK (playTempo IN ('SLOW', 'NORMAL', 'FAST'));
IF COL_LENGTH(N'dbo.Lineups', N'pressing') IS NULL
    ALTER TABLE dbo.Lineups ADD pressing NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_pressing DEFAULT 'MEDIUM'
        CONSTRAINT CK_Lineups_pressing CHECK (pressing IN ('LOW', 'MEDIUM', 'HIGH'));
IF COL_LENGTH(N'dbo.Lineups', N'defensiveLine') IS NULL
    ALTER TABLE dbo.Lineups ADD defensiveLine NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_defensiveLine DEFAULT 'NORMAL'
        CONSTRAINT CK_Lineups_defensiveLine CHECK (defensiveLine IN ('LOW', 'NORMAL', 'HIGH'));
IF COL_LENGTH(N'dbo.Lineups', N'passingStyle') IS NULL
    ALTER TABLE dbo.Lineups ADD passingStyle NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_passingStyle DEFAULT 'MIXED'
        CONSTRAINT CK_Lineups_passingStyle CHECK (passingStyle IN ('SHORT', 'MIXED', 'LONG'));
IF COL_LENGTH(N'dbo.Lineups', N'attackDirection') IS NULL
    ALTER TABLE dbo.Lineups ADD attackDirection NVARCHAR(20) NOT NULL CONSTRAINT DF_Lineups_attackDirection DEFAULT 'THROUGH_MIDDLE'
        CONSTRAINT CK_Lineups_attackDirection CHECK (attackDirection IN ('THROUGH_MIDDLE', 'THROUGH_WINGS'));
GO

-- Izvođači prekida i prethodno stanje za vraćanje sastava
IF COL_LENGTH(N'dbo.Lineups', N'penaltyTakerId') IS NULL
    ALTER TABLE dbo.Lineups ADD penaltyTakerId BIGINT NULL
        CONSTRAINT FK_Lineups_PenaltyTaker FOREIGN KEY REFERENCES dbo.Players (id);
IF COL_LENGTH(N'dbo.Lineups', N'freeKickTakerId') IS NULL
    ALTER TABLE dbo.Lineups ADD freeKickTakerId BIGINT NULL
        CONSTRAINT FK_Lineups_FreeKickTaker FOREIGN KEY REFERENCES dbo.Players (id);
IF COL_LENGTH(N'dbo.Lineups', N'cornerTakerId') IS NULL
    ALTER TABLE dbo.Lineups ADD cornerTakerId BIGINT NULL
        CONSTRAINT FK_Lineups_CornerTaker FOREIGN KEY REFERENCES dbo.Players (id);
IF COL_LENGTH(N'dbo.Lineups', N'previousState') IS NULL
    ALTER TABLE dbo.Lineups ADD previousState NVARCHAR(4000) NULL;
GO

-- Individualna uloga igrača u sastavu
IF COL_LENGTH(N'dbo.LineupPlayers', N'duty') IS NULL
    ALTER TABLE dbo.LineupPlayers ADD duty NVARCHAR(20) NOT NULL CONSTRAINT DF_LineupPlayers_duty DEFAULT 'BALANCED'
        CONSTRAINT CK_LineupPlayers_duty CHECK (duty IN ('OFFENSIVE', 'BALANCED', 'DEFENSIVE'));
GO

-- Fotografija selektora
IF COL_LENGTH(N'dbo.NationalTeams', N'coachPhotoFileName') IS NULL
    ALTER TABLE dbo.NationalTeams ADD coachPhotoFileName NVARCHAR(100) NULL;
GO

-- Zastava reprezentacije
IF COL_LENGTH(N'dbo.NationalTeams', N'flagFileName') IS NULL
    ALTER TABLE dbo.NationalTeams ADD flagFileName NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NationalTeams', N'flagRemoved') IS NULL
    ALTER TABLE dbo.NationalTeams ADD flagRemoved BIT NOT NULL CONSTRAINT DF_NationalTeams_flagRemoved DEFAULT 0;
GO

-- Izabrani jezik korisnika
IF COL_LENGTH(N'dbo.Users', N'preferredLanguage') IS NULL
    ALTER TABLE dbo.Users ADD preferredLanguage NVARCHAR(5) NOT NULL CONSTRAINT DF_Users_preferredLanguage DEFAULT 'sr'
        CONSTRAINT CK_Users_preferredLanguage CHECK (preferredLanguage IN ('sr', 'en'));
GO
