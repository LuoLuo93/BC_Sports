# BC Sports Admin Health Check Script
# Usage: .\check-service.ps1
# Run this script with Windows Task Scheduler to auto-restart if service is down

$url = "http://localhost:8080/actuator/health"
$logFile = ".\monitor.log"

function Write-Log {
    param($message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp - $message" | Out-File -FilePath $logFile -Append
    Write-Host "$timestamp - $message"
}

function Start-Service {
    Write-Log "Starting BC Sports Admin service..."

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
        Write-Log "Service started successfully! PID: $($javaProcess.Id)"
    } else {
        Write-Log "ERROR: Service failed to start!"
    }
}

# Check if Java process is running
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue

if (-not $javaProcess) {
    Write-Log "Java process not found. Starting service..."
    Start-Service
} else {
    # Check health endpoint
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            Write-Log "Service is healthy (PID: $($javaProcess.Id))"
        } else {
            Write-Log "Service returned status code: $($response.StatusCode). Restarting..."
            Start-Service
        }
    } catch {
        Write-Log "Health check failed: $($_.Exception.Message). Restarting..."
        Start-Service
    }
}
