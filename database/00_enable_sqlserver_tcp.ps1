# Bật giao thức TCP/IP cho SQL Server để JDBC driver kết nối được (chạy PowerShell với quyền Administrator).
$ErrorActionPreference = 'Stop'

$tcp = 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQLServer\SuperSocketNetLib\Tcp'

if (-not (Test-Path $tcp)) {
    Write-Host "[LOI] Khong tim thay khoa registry: $tcp" -ForegroundColor Red
    Write-Host "      Kiem tra lai ten instance trong SQL Server Configuration Manager." -ForegroundColor Red
    exit 1
}

Write-Host "1. Bat giao thuc TCP/IP ..." -ForegroundColor Cyan
Set-ItemProperty -Path $tcp -Name 'Enabled'         -Value 1 -Type DWord
Set-ItemProperty -Path $tcp -Name 'ListenOnAllIPs'  -Value 1 -Type DWord

Write-Host "2. Ghim cong tinh 1433 cho IPAll ..." -ForegroundColor Cyan
Set-ItemProperty -Path "$tcp\IPAll" -Name 'TcpPort'         -Value '1433' -Type String
Set-ItemProperty -Path "$tcp\IPAll" -Name 'TcpDynamicPorts' -Value ''     -Type String

Get-ChildItem -Path $tcp | Where-Object { $_.PSChildName -match '^IP\d+$' } | ForEach-Object {
    Set-ItemProperty -Path $_.PSPath -Name 'Enabled' -Value 1 -Type DWord -ErrorAction SilentlyContinue
}

Write-Host "3. Khoi dong lai service MSSQLSERVER ..." -ForegroundColor Cyan
Restart-Service -Name 'MSSQLSERVER' -Force
Start-Sleep -Seconds 5

Write-Host "4. Kiem tra ket qua:" -ForegroundColor Cyan
$svc = Get-Service MSSQLSERVER
Write-Host "   Service MSSQLSERVER: $($svc.Status)"

$listen = netstat -ano | Select-String ':1433\s' | Select-String 'LISTENING'
if ($listen) {
    Write-Host "   [OK] SQL Server dang lang nghe tren cong 1433:" -ForegroundColor Green
    $listen | ForEach-Object { Write-Host "        $_" }
} else {
    Write-Host "   [CANH BAO] Chua thay cong 1433 LISTENING." -ForegroundColor Yellow
    Write-Host "   Xem ERRORLOG de biet chi tiet:" -ForegroundColor Yellow
    Write-Host '   Select-String -Path "C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Log\ERRORLOG" -Pattern "listening|TDSSNIClient"'
}

Write-Host ""
Write-Host "Xong. Co the dong cua so nay." -ForegroundColor Green
