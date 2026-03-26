# Lista de puertos que usa el proyecto
# Se cambia 8080 por 8082 para evitar conflictos con Oracle (TNSLSNR)
$puertos = @(9000, 12345, 12346, 12347, 8082, 8081, 8888)

Write-Host "Realizando limpieza nuclear de puertos..." -ForegroundColor Cyan
foreach ($p in $puertos) {
    try {
        $conn = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue
        if ($conn) {
            $procId = $conn.OwningProcess
            # No intentamos matar el proceso 0 (System Idle) ni procesos de sistema protegidos
            if ($procId -gt 0) {
                Write-Host "  > Cerrando proceso en puerto $p (PID: $procId)..."
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
    } catch {}
}
Start-Sleep -Seconds 1

# Limpia archivos .class antiguos
Write-Host "Limpiando clases antiguas..." -ForegroundColor Yellow
Get-ChildItem -Path . -Include *.class -Recurse | Remove-Item -Force

# Recompila todo
Write-Host "Recompilando proyecto completo..." -ForegroundColor Green
Get-ChildItem -Filter *.java -Recurse | ForEach-Object { javac -encoding UTF-8 $_.FullName }

# Lanza el Dashboard
Write-Host "Iniciando Dashboard Master v7.1..." -ForegroundColor White -BackgroundColor Blue
cd 00_Dashboard
# Quitamos el -D si daba problemas, o lo ponemos entre comillas
java -cp . DashboardServer
