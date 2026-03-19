# Script para lanzar múltiples clientes de tickets
# Uso: .\lanzarClientes.ps1 <numero_de_clientes>

$num = 5
if ($args.Count -gt 0) { $num = $args[0] }

Write-Host "Lanzando $num clientes..." -ForegroundColor Cyan

for ($i = 1; $i -le $num; $i++) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "java ClienteTickets 'Cliente-$i'"
}

Write-Host "¡Hecho!" -ForegroundColor Green
