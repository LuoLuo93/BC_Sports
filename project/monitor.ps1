# BC Sports Admin Monitor Script
# Usage: .\monitor.ps1
# This script monitors the Java service and auto-restarts if it crashes

$checkInterval = 60  # Check every 60 seconds
$url = "http://localhost:8080/actuator/health"
$maxFailures = 3     # Restart after 3 consecutive failures
$failureCount = 0

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC Sports Admin - Service Monitor" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Check interval: $checkInterval seconds" -ForegroundColor Gray
Write-Host "Health check URL: $url" -ForegroundColor Gray
Write-Host "Max failures before restart: $maxFailures" -ForegroundColor Gray
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

function Start-Service {
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Starting service..." -ForegroundColor Yellow

    # Stop existing Java processes
    $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
    if ($javaProcesses) {
        Stop-Process -Name "java" -Force
        Start-Sleep -Seconds 2
    }

    # Start new service
    $scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
    Set-Location $scriptPath

    $javaArgs = @(
        "-Djava.security.properties=./tls-override.security",
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Duser.language=zh",
        "-Duser.region=CN",
        "-jar",
        "target/bc-sports-admin-1.0.0.jar",
        "--spring.profiles.active=prod"
    )

    Start-Process -FilePath "java" -ArgumentList $javaArgs -WindowStyle Hidden
    Start-Sleep -Seconds 15

    $javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
    if ($javaProcess) {
        Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Service started successfully! PID: $($javaProcess.Id)" -ForegroundColor Green
    } else {
        Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] ERROR: Service failed to start!" -ForegroundColor Red
    }
}

function Test-ServiceHealth {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            return $true
        }
    } catch {
        return $false
    }
    return $false
}

# Initial check
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
if (-not $javaProcess) {
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] No Java process found. Starting service..." -ForegroundColor Yellow
    Start-Service
}

# Main monitoring loop
while ($true) {
    $javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue

    if (-not $javaProcess) {
        Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Java process not found! Restarting..." -ForegroundColor Red
        Start-Service
        $failureCount = 0
    } else {
        $isHealthy = Test-ServiceHealth

        if ($isHealthy) {
            Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Service is healthy (PID: $($javaProcess.Id))" -ForegroundColor Green
            $failureCount = 0
        } else {
            $failureCount++
            Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Health check failed ($failureCount/$maxFailures)" -ForegroundColor Yellow

            if ($failureCount -ge $maxFailures) {
                Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Max failures reached! Restarting service..." -ForegroundColor Red
                Start-Service
                $failureCount = 0
            }
        }
    }

    Start-Sleep -Seconds $checkInterval
}
