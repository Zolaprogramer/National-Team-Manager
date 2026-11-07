/*
    OPCIONO I NEPOVRATNO: trajno briše kolonu weightKg i sve upisane težine igrača.
    Aplikacija radi i bez ove skripte, pa je pokrenite samo ako te podatke sigurno više ne trebate.

    sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\07_optional_drop_player_weight.sql
*/
USE NationalTeamManager;
GO

IF COL_LENGTH(N'dbo.Players', N'weightKg') IS NOT NULL
    ALTER TABLE dbo.Players DROP COLUMN weightKg;
GO
