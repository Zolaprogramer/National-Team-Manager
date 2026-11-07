/*
    Nadogradnja postojeće baze za logotipe takmičenja i ulogu administratora.
    Postojeći podaci ostaju netaknuti, a skripta se može pokrenuti više puta.

    - Users.role: USER ili ADMIN (svi postojeći korisnici dobijaju USER)
    - Competitions.ownerId postaje neobavezan: takmičenje bez vlasnika je zajedničko (zvanično)
    - Competitions.logoPath: relativna putanja logotipa u folderu uploads (npr. competitions/abc.png)
    - zvanična takmičenja iz zadatka, bez logotipa; logo i tačne datume postavlja administrator

    sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\09_competition_logos.sql

    Administrator se određuje jednom naredbom, na primjer:
    UPDATE dbo.Users SET role = 'ADMIN' WHERE username = N'demo';
*/
USE NationalTeamManager;
GO

IF COL_LENGTH(N'dbo.Users', N'role') IS NULL
    ALTER TABLE dbo.Users ADD role NVARCHAR(10) NOT NULL CONSTRAINT DF_Users_role DEFAULT 'USER'
        CONSTRAINT CK_Users_role CHECK (role IN ('USER', 'ADMIN'));
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Competitions') AND name = N'ownerId' AND is_nullable = 0)
    ALTER TABLE dbo.Competitions ALTER COLUMN ownerId BIGINT NULL;
GO

IF COL_LENGTH(N'dbo.Competitions', N'logoPath') IS NULL
    ALTER TABLE dbo.Competitions ADD logoPath NVARCHAR(255) NULL;
GO

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
