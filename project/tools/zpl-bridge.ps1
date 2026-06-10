# ZPL TCP-to-USB Bridge
$Port = 9100

Write-Host ""
Write-Host "=== ZPL TCP Bridge ===" -ForegroundColor Cyan
Write-Host ""

# List printers
Write-Host "Loading printers..." -ForegroundColor Gray
$allPrinters = @()
try {
    $allPrinters = Get-Printer -ErrorAction Stop | Where-Object { $_.Type -eq 'Local' -or $_.Type -eq 4 }
} catch {
    try {
        $allPrinters = Get-WmiObject Win32_Printer -ErrorAction Stop | Where-Object { $_.Local -eq $true }
    } catch {
        Write-Host "Cannot get printer list: $_" -ForegroundColor Red
    }
}

if ($allPrinters.Count -eq 0) {
    Write-Host "No local printers found. Type printer name manually:" -ForegroundColor Yellow
    $printerName = Read-Host "Printer name"
} else {
    Write-Host ""
    for ($i = 0; $i -lt $allPrinters.Count; $i++) {
        Write-Host "  [$i] $($allPrinters[$i].Name)"
    }
    Write-Host ""
    $sel = Read-Host "Select printer number"
    $printerName = $allPrinters[[int]$sel].Name
}

Write-Host ""
Write-Host "Printer: $printerName" -ForegroundColor Green
Write-Host "Port:    $Port" -ForegroundColor Green
Write-Host "Waiting for print jobs..." -ForegroundColor Yellow
Write-Host ""

# TCP listener
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
$listener.Start()

while ($true) {
    $client = $listener.AcceptTcpClient()
    $stream = $client.GetStream()
    $buf = [byte[]]::new(8192)
    $ms = [System.IO.MemoryStream]::new()
    do {
        $n = $stream.Read($buf, 0, $buf.Length)
        if ($n -gt 0) { $ms.Write($buf, 0, $n) }
    } while ($n -gt 0)
    $zpl = [System.Text.Encoding]::UTF8.GetString($ms.ToArray())
    $ms.Close(); $stream.Close(); $client.Close()

    if ($zpl.Length -eq 0) { continue }

    # Save and print
    $tmp = "$env:TEMP\zpl_print.zpl"
    [System.IO.File]::WriteAllText($tmp, $zpl, [System.Text.Encoding]::UTF8)
    $ts = Get-Date -Format "HH:mm:ss"
    try {
        & print /D:"$printerName" $tmp 2>&1 | Out-Null
        Write-Host "[$ts] OK  $($zpl.Length) bytes" -ForegroundColor Green
    } catch {
        Write-Host "[$ts] ERR $_" -ForegroundColor Red
    }
    Remove-Item $tmp -ErrorAction SilentlyContinue
}
