# GKE Cost Management Notes

> Migrated from the previous root-level GKE notes.
> Review commands against the current cluster before executing them.

```powershell
kubectl scale deployment/account-service deployment/api-gtw deployment/config-service deployment/intelligence-service deployment/lovable-frontend deployment/lovable-micro-proxy deployment/workspace-service --replicas=0 -n lovable-core
kubectl scale deployment/runner-pool --replicas=0 -n lovable-previews
kubectl scale deployment/ingress-nginx-controller --replicas=0 -n ingress-nginx
kubectl scale statefulset/kafka statefulset/minio statefulset/pgvector statefulset/redis --replicas=0 -n lovable-core
```
