/*
    Kreira SQL prijavu kojom se aplikacija spaja na bazu. Lozinka se prosljeđuje pri pokretanju i ne čuva se u skripti:

    sqlcmd -S localhost\SQLEXPRESS -E -C -i database\03_create_login.sql -v AppPassword="VASA_LOZINKA"

    Istu lozinku upišite u application-local.properties.
*/
USE master;
GO

IF SUSER_ID(N'ntm_app') IS NULL
    CREATE LOGIN ntm_app WITH PASSWORD = N'$(AppPassword)', DEFAULT_DATABASE = NationalTeamManager, CHECK_POLICY = ON;
ELSE
    ALTER LOGIN ntm_app WITH PASSWORD = N'$(AppPassword)';
GO

USE NationalTeamManager;
GO

IF USER_ID(N'ntm_app') IS NULL
    CREATE USER ntm_app FOR LOGIN ntm_app;
GO

ALTER ROLE db_datareader ADD MEMBER ntm_app;
ALTER ROLE db_datawriter ADD MEMBER ntm_app;
ALTER ROLE db_ddladmin ADD MEMBER ntm_app;
GO
