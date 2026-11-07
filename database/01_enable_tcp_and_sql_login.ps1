# Jednokratno podesavanje lokalnog SQL Servera za JDBC konekciju:
#   - ukljucuje TCP/IP protokol na portu 1433
#   - ukljucuje SQL Server Authentication (Mixed Mode)
#   - restartuje SQL Server servis
#
# Pokrenuti iz PowerShell-a otvorenog kao Administrator:
#   powershell -ExecutionPolicy Bypass -File .\database\01_enable_tcp_and_sql_login.ps1
#
# Za drugu instancu: -InstanceName MSSQLSERVER (podrazumijevana je SQLEXPRESS)

param(
    [string]$InstanceName = "SQLEXPRESS",
    [int]$Port = 1433
)

$ErrorActionPreference = "Stop"

$principal = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error "Skriptu pokrenite iz PowerShell-a otvorenog kao Administrator."
}

$instanceId = (Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL").$InstanceName
if (-not $instanceId) {
    Write-Error "SQL Server instanca '$InstanceName' nije pronadjena."
}

$server = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$instanceId\MSSQLServer"
$tcp = "$server\SuperSocketNetLib\Tcp"

Set-ItemProperty -Path $tcp -Name Enabled -Value 1
Set-ItemProperty -Path "$tcp\IPAll" -Name TcpPort -Value "$Port"
Set-ItemProperty -Path "$tcp\IPAll" -Name TcpDynamicPorts -Value ""
Set-ItemProperty -Path $server -Name LoginMode -Value 2

$serviceName = if ($InstanceName -eq "MSSQLSERVER") { "MSSQLSERVER" } else { "MSSQL`$$InstanceName" }
Restart-Service -Name $serviceName -Force

Write-Host "Gotovo: TCP/IP je ukljucen na portu $Port, a SQL prijava je dozvoljena."
