$ErrorActionPreference = 'Stop'
$Timeout = '300s'
$Root = $PSScriptRoot
$K8sDir = Join-Path $Root 'k8s'
$ExpectedContext = 'gke_lovable-microservices-clone_asia-south2_lovable-microservices-cluster'

function Invoke-Kubectl {
    param([Parameter(Mandatory)][string[]]$KubectlArgs)
    & kubectl @KubectlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl failed: kubectl $($KubectlArgs -join ' ')"
    }
}

function Wait-Deployment {
    param([Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][string]$Namespace)
    Write-Host "`nWaiting for deployment/$Name in namespace $Namespace..." -ForegroundColor Cyan
    Invoke-Kubectl @('rollout', 'status', "deployment/$Name", '-n', $Namespace, "--timeout=$Timeout")
}

function Wait-StatefulSet {
    param([Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][string]$Namespace)
    Write-Host "`nWaiting for statefulset/$Name in namespace $Namespace..." -ForegroundColor Cyan
    Invoke-Kubectl @('rollout', 'status', "statefulset/$Name", '-n', $Namespace, "--timeout=$Timeout")
}

function Get-K8sYamlFilesByKind {
    param([Parameter(Mandatory)][string]$Kind)
    if (-not (Test-Path $K8sDir)) {
        Write-Warning "Directory '$K8sDir' not found; cannot auto-discover $Kind YAMLs."
        return @()
    }

    $files = foreach ($file in Get-ChildItem -Path $K8sDir -Recurse -File) {
        if ($file.Extension -notin @('.yaml', '.yml')) { continue }
        if ($file.FullName -match '[\\/](\.git|node_modules|target|dist)[\\/]') { continue }
        if ($file.Name -match '(?i)(backup|bkp|\.bak|_bkp)') { continue }

        # Only apply files whose Kubernetes documents contain this kind exclusively.
        # This avoids re-applying a broad manifest file containing unrelated resources.
        $kindLines = Select-String -Path $file.FullName -Pattern '^\s*kind:\s*([A-Za-z]+)\s*$' -ErrorAction SilentlyContinue
        $fileKinds = @($kindLines | ForEach-Object {
            [regex]::Match($_.Line, '^\s*kind:\s*([A-Za-z]+)').Groups[1].Value
        } | Sort-Object -Unique)
        if ($fileKinds.Count -eq 1 -and $fileKinds[0] -eq $Kind) {
            $file.FullName
        }
    }
    return @($files | Sort-Object -Unique)
}

function Apply-YamlFiles {
    param([string[]]$Files)
    foreach ($file in $Files) {
        Write-Host "Applying $file" -ForegroundColor DarkCyan
        Invoke-Kubectl @('apply', '-f', $file)
    }
}

Write-Host "=== 0. Verify production cluster ===" -ForegroundColor Yellow
$currentContext = (& kubectl config current-context | Out-String).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Could not read the current kubectl context.' }
Write-Host "Current context: $currentContext"
if ($currentContext -ne $ExpectedContext) {
    throw "Wrong kubectl context. Expected '$ExpectedContext'. Switch contexts and rerun."
}
$confirm = Read-Host 'Type YES to start IdeaStitch on the production GKE cluster'
if ($confirm -cne 'YES') { throw 'Cancelled by user.' }

Write-Host "`n=== 1. Verify required namespaces already exist ===" -ForegroundColor Yellow
Invoke-Kubectl @('get', 'namespace', 'ingress-nginx', 'lovable-core', 'lovable-previews')

Write-Host "`n=== 2. Start Ingress NGINX first ===" -ForegroundColor Yellow
Invoke-Kubectl @('scale', 'deployment/ingress-nginx-controller', '--replicas=1', '-n', 'ingress-nginx')

# The standard ingress-nginx admission-create and admission-patch Jobs are one-time install jobs.
# If they still exist, verify they completed; do not delete/recreate them on every wake-up.
Write-Host "Checking existing Ingress NGINX Jobs (completed admission Jobs are normally reused)..." -ForegroundColor Cyan
$jobJsonLines = & kubectl get jobs -n ingress-nginx -o json 2>$null
$jobExitCode = $LASTEXITCODE
if ($jobExitCode -ne 0) {
    throw 'Could not list Jobs in ingress-nginx namespace.'
}
$jobList = ($jobJsonLines -join "`n") | ConvertFrom-Json
$ingressJobs = @($jobList.items)
if ($ingressJobs.Count -eq 0) {
    Write-Warning 'No Jobs found in ingress-nginx. Continuing; do not recreate admission Jobs unless the webhook secret/configuration is actually missing.'
} else {
    foreach ($job in $ingressJobs) {
        $jobName = $job.metadata.name
        $complete = @($job.status.conditions | Where-Object { $_.type -eq 'Complete' -and $_.status -eq 'True' }).Count -gt 0
        if ($complete) {
            Write-Host "Job/$jobName already completed; no rerun needed." -ForegroundColor Green
        } else {
            Write-Host "Waiting for Job/$jobName to complete..." -ForegroundColor Cyan
            Invoke-Kubectl @('wait', '--for=condition=complete', "job/$jobName", '-n', 'ingress-nginx', "--timeout=$Timeout")
        }
    }
}
Wait-Deployment 'ingress-nginx-controller' 'ingress-nginx'

Write-Host "`n=== 3. Start all StatefulSets ===" -ForegroundColor Yellow
foreach ($name in @('kafka', 'minio', 'pgvector', 'redis')) {
    Invoke-Kubectl @('scale', "statefulset/$name", '--replicas=1', '-n', 'lovable-core')
}
foreach ($name in @('pgvector', 'minio', 'redis', 'kafka')) {
    Wait-StatefulSet $name 'lovable-core'
}

Write-Host "`n=== 4. Start Config Server and wait until ready ===" -ForegroundColor Yellow
Invoke-Kubectl @('scale', 'deployment/config-service', '--replicas=1', '-n', 'lovable-core')
Wait-Deployment 'config-service' 'lovable-core'

Write-Host "`n=== 5. Start API Gateway and wait until ready ===" -ForegroundColor Yellow
Invoke-Kubectl @('scale', 'deployment/api-gtw', '--replicas=1', '-n', 'lovable-core')
Wait-Deployment 'api-gtw' 'lovable-core'

Write-Host "`n=== 6. Start remaining backend services in order ===" -ForegroundColor Yellow
foreach ($name in @('account-service', 'workspace-service', 'intelligence-service', 'lovable-micro-proxy')) {
    Invoke-Kubectl @('scale', "deployment/$name", '--replicas=1', '-n', 'lovable-core')
    Wait-Deployment $name 'lovable-core'
}

Write-Host "`n=== 7. Start preview runner pool ===" -ForegroundColor Yellow
Invoke-Kubectl @('scale', 'deployment/runner-pool', '--replicas=2', '-n', 'lovable-previews')
Wait-Deployment 'runner-pool' 'lovable-previews'

Write-Host "`n=== 8. Start frontend last ===" -ForegroundColor Yellow
Invoke-Kubectl @('scale', 'deployment/lovable-frontend', '--replicas=1', '-n', 'lovable-core')
Wait-Deployment 'lovable-frontend' 'lovable-core'

Write-Host "`n=== 9. Reapply all NetworkPolicy YAMLs under k8s/ ===" -ForegroundColor Yellow
$networkPolicyFiles = Get-K8sYamlFilesByKind 'NetworkPolicy'
if ($networkPolicyFiles.Count -gt 0) {
    Apply-YamlFiles $networkPolicyFiles
} else {
    Write-Warning 'No NetworkPolicy YAMLs found under k8s/.'
}

Write-Host "`n=== 10. Reapply all Namespace YAMLs under k8s/ ===" -ForegroundColor Yellow
$namespaceFiles = Get-K8sYamlFilesByKind 'Namespace'
if ($namespaceFiles.Count -gt 0) {
    Apply-YamlFiles $namespaceFiles
} else {
    Write-Warning 'No Namespace YAMLs found under k8s/.'
}

Write-Host "`n=== 11. Final workload status ===" -ForegroundColor Yellow
Invoke-Kubectl @('get', 'deployments,statefulsets', '-n', 'lovable-core')
Invoke-Kubectl @('get', 'deployments,pods', '-n', 'lovable-previews', '-o', 'wide')
Invoke-Kubectl @('get', 'svc', 'ingress-nginx-controller', '-n', 'ingress-nginx', '-o', 'wide')

Write-Host "`n=== 12. Wait for external Ingress IP ===" -ForegroundColor Green
$ip = ''
for ($attempt = 1; $attempt -le 20 -and -not $ip; $attempt++) {
    $ipArgs = @('get', 'svc', 'ingress-nginx-controller', '-n', 'ingress-nginx', '-o', 'jsonpath={.status.loadBalancer.ingress[0].ip}')
    $ip = (& kubectl @ipArgs 2>$null | Out-String).Trim()
    $ipExitCode = $LASTEXITCODE
    if ($ipExitCode -ne 0) { throw 'Failed to read the Ingress LoadBalancer Service.' }
    if (-not $ip -and $attempt -lt 20) {
        Write-Host "External IP not assigned yet; checking again in 15 seconds ($attempt/20)..."
        Start-Sleep -Seconds 15
    }
}

if ($ip) {
    Write-Host "`nExternal Ingress IP: $ip" -ForegroundColor Green
    Write-Host 'Compare this with the GoDaddy A records for @, api, and *.previews.' -ForegroundColor Yellow
} else {
    Write-Warning 'No external IP appeared within 5 minutes. Run: kubectl get svc ingress-nginx-controller -n ingress-nginx -o wide'
}

Write-Host "`nStartup sequence finished. Verify the website, API login, AI generation, and preview separately." -ForegroundColor Green
