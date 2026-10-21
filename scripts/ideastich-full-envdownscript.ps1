$ErrorActionPreference = "Stop"

$ExpectedContext = "gke_lovable-microservices-clone_asia-south2_lovable-microservices-cluster"

Write-Host "Checking Kubernetes context..." -ForegroundColor Cyan
$currentContext = (kubectl config current-context).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Could not read the current Kubernetes context. Check that kubectl is installed and configured."
}
if ($currentContext -ne $ExpectedContext) {
    throw "Wrong Kubernetes context: '$currentContext'. Expected '$ExpectedContext'. Nothing was scaled down."
}

Write-Host "Target cluster: $currentContext" -ForegroundColor Yellow
Write-Host "This scales workloads down; it does NOT delete PVCs, namespaces, or persistent disks." -ForegroundColor Yellow
$confirm = Read-Host "Type STOP to continue"
if ($confirm -cne "STOP") {
    Write-Host "Cancelled. No workloads were changed." -ForegroundColor Yellow
    exit 0
}

# Suspend CronJobs so they do not start new Jobs during shutdown.
foreach ($ns in @("lovable-core", "lovable-previews")) {
    $cronJobs = kubectl get cronjobs -n $ns -o name 2>$null
    if ($LASTEXITCODE -eq 0) {
        foreach ($cron in $cronJobs) {
            if ($cron) {
                kubectl patch $cron -n $ns --type merge -p '{"spec":{"suspend":true}}'
                if ($LASTEXITCODE -ne 0) {
                    throw "Failed to suspend $cron in namespace $ns."
                }
            }
        }
    }
}

kubectl scale deployment --all --replicas=0 -n lovable-core
if ($LASTEXITCODE -ne 0) { throw "Failed to scale Deployments in lovable-core." }

kubectl scale statefulset --all --replicas=0 -n lovable-core
if ($LASTEXITCODE -ne 0) { throw "Failed to scale StatefulSets in lovable-core." }

kubectl scale deployment --all --replicas=0 -n lovable-previews
if ($LASTEXITCODE -ne 0) { throw "Failed to scale Deployments in lovable-previews." }

# The runner-pool pods can be orphaned, so scaling Deployments may not remove them.
Write-Host "`nRemoving leftover runner-pool pods in lovable-previews..." -ForegroundColor Cyan
$runnerPods = @(kubectl get pods -n lovable-previews -o name 2>$null | Where-Object { $_ -match '^pod/runner-pool-' })
if ($runnerPods.Count -gt 0) {
    kubectl delete $runnerPods -n lovable-previews --wait=true --timeout=120s
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: some runner-pool pods did not terminate within 120 seconds. They were requested for deletion; inspect them with 'kubectl get pods -n lovable-previews'." -ForegroundColor DarkYellow
    }
} else {
    Write-Host "No leftover runner-pool pods found." -ForegroundColor DarkYellow
}

$ingressExists = kubectl get namespace ingress-nginx --ignore-not-found -o name
if ($ingressExists) {
    kubectl scale deployment --all --replicas=0 -n ingress-nginx
    if ($LASTEXITCODE -ne 0) { throw "Failed to scale Deployments in ingress-nginx." }
} else {
    Write-Host "Namespace ingress-nginx not found; skipping ingress controller." -ForegroundColor DarkYellow
}

Write-Host "`nShutdown requested. Checking remaining workloads..." -ForegroundColor Cyan
kubectl get deployments,statefulsets -n lovable-core
kubectl get deployments,statefulsets -n lovable-previews
if ($ingressExists) { kubectl get deployments,statefulsets -n ingress-nginx }

$remainingRunnerPods = @(kubectl get pods -n lovable-previews -o name 2>$null | Where-Object { $_ -match '^pod/runner-pool-' })
if ($remainingRunnerPods.Count -gt 0) {
    Write-Host "Warning: runner-pool pods still exist (possibly terminating):" -ForegroundColor DarkYellow
    kubectl get $remainingRunnerPods -n lovable-previews
} else {
    Write-Host "Verified: no runner-pool pods remain." -ForegroundColor Green
}

Write-Host "`nChecking persistent volume claims (these should remain present)..." -ForegroundColor Cyan
kubectl get pvc -n lovable-core
kubectl get pvc -n lovable-previews

Write-Host "`nDone. No PVCs, namespaces, or persistent disks were deleted." -ForegroundColor Green
Write-Host "Note: GKE, persistent disks, and load balancer resources may still incur charges."
