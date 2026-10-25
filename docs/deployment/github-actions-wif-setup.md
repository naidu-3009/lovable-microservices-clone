# GitHub Actions — Google Cloud Authentication

The backend deployment workflows authenticate to Google Cloud using
Workload Identity Federation (WIF), avoiding a long-lived service-account
key file.

## Required GitHub Actions secrets

| Secret | Purpose |
|---|---|
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | WIF provider resource name |
| `GCP_SERVICE_ACCOUNT` | Service account used by GitHub Actions |
| `GCP_CLUSTER` | Target GKE cluster name |
| `GCP_ZONE` | Target GKE region or location |
| `DOCKERHUB_USERNAME` | Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token |

## WIF configuration

The Google Cloud setup uses a workload identity pool and provider to
federate GitHub Actions identities with a Google Cloud service account.

Configure attribute mappings and conditions to restrict access to the
intended GitHub repository and workflow identity.

Grant the service account only the permissions required to publish images
and deploy the application to GKE.

## Security

- Store credentials in GitHub Actions secrets.
- Do not commit populated environment files or local configuration exports.
- Prefer Workload Identity Federation over long-lived service-account keys.
- Rotate any genuine credentials that have been exposed.