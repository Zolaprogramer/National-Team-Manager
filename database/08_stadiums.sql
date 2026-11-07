/*
    Nadogradnja postojeće baze za modul stadiona. Postojeći podaci ostaju netaknuti, a skripta se može pokrenuti više puta.
    Utakmice koje već imaju upisan naziv stadiona zadržavaju ga, a stadionId kod njih ostaje prazan.
    Nova instalacija ovo ne treba, jer 02_create_schema.sql već sadrži iste objekte.

    sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\08_stadiums.sql
*/
SET QUOTED_IDENTIFIER ON;
GO

USE NationalTeamManager;
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
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_Stadiums_one_main_per_team' AND object_id = OBJECT_ID(N'dbo.Stadiums'))
    CREATE UNIQUE INDEX UX_Stadiums_one_main_per_team ON dbo.Stadiums (teamId) WHERE mainStadium = 1;
GO

IF COL_LENGTH(N'dbo.Matches', N'stadiumId') IS NULL
    ALTER TABLE dbo.Matches ADD stadiumId BIGINT NULL
        CONSTRAINT FK_Matches_Stadiums FOREIGN KEY REFERENCES dbo.Stadiums (id);
GO
