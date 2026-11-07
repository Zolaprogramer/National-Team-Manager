/*
    Aplikacija više ne koristi težinu igrača. Kolona weightKg postaje neobavezna, kako bi novi igrači mogli biti
    sačuvani bez nje, a postojeće vrijednosti ostaju netaknute. Skripta se može pokrenuti više puta.
    Nova instalacija ovo ne treba, jer 02_create_schema.sql nema ovu kolonu.

    sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\06_player_weight_optional.sql
*/
USE NationalTeamManager;
GO

IF COL_LENGTH(N'dbo.Players', N'weightKg') IS NOT NULL
    ALTER TABLE dbo.Players ALTER COLUMN weightKg INT NULL;
GO
