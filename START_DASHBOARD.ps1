# Lista de puertos que usa el proyecto
$puertos = @(9000, 12345, 12346, 12347, 8082, 8081, 8888)

Write-Host "Realizando limpieza nuclear de puertos..." -ForegroundColor Cyan
foreach ($p in $puertos) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue
        if ($conns) {
            foreach ($c in $conns) {
                $procId = $c.OwningProcess
                if ($procId -gt 4) { # Evita System (4) e Idle (0)
                    Write-Host "  > Cerrando proceso en puerto $p (PID: $procId)..."
                    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                }
            }
        }
    } catch {}
}

Write-Host "Esperando liberación de puertos..." -ForegroundColor Gray
Start-Sleep -Seconds 2

# Limpia archivos .class antiguos
Write-Host "Limpiando clases antiguas..." -ForegroundColor Yellow
Get-ChildItem -Path . -Include *.class -Recurse | Remove-Item -Force

# Recompila todo
Write-Host "Recompilando proyecto completo..." -ForegroundColor Green
Get-ChildItem -Filter *.java -Recurse | ForEach-Object { javac -encoding UTF-8 $_.FullName }

# Lanza el Dashboard
Write-Host "Iniciando Dashboard Master v7.1..." -ForegroundColor White -BackgroundColor Blue
cd 00_Dashboard
if (Test-Path "DashboardServer.class") {
    java DashboardServer
} else {
    Write-Host "❌ ERROR: No se pudo compilar DashboardServer.java" -ForegroundColor Red
}
