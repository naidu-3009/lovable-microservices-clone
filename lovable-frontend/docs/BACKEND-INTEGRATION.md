# Required backend and deployment integration for IdeaStitch frontend

This document intentionally does not modify the backend source in this frontend ZIP. It gives the exact changes to apply to the supplied backend repository (`lovable-microservices-clone`). The frontend has been wired to the existing routes plus the public plan-catalog route described below.

## 1. Fix “Vite server unavailable” after a successful deployment

### Root cause

File:
`work/src/main/java/com/lovable_clone_microservices/workspace_service/service/impl/KubernetesDeploymentServiceImpl.java`

The current deploy path runs a command ending in `&`. `execCommand(...)` only waits 500 ms for such commands, then `claimAndStartNewPod(...)` registers `route:project-{id}.previews.ideastitch.in` in Redis and returns success. The Vite process can still be starting when the proxy forwards the first browser request. If the proxy cannot connect to port 5173, it returns the exact text `Vite server unavailable...`. The existing active-pod path also registers the route without checking whether Vite is responding.

### Exact implementation

In `KubernetesDeploymentServiceImpl.java`, add the following helper method inside the class:

```java
private void ensureViteReady(String podName) {
    String startAndWait = String.join("\n",
            "cd /app || exit 1",
            "if wget -q -T 2 -O /dev/null http://127.0.0.1:5173/; then exit 0; fi",
            "if [ -f /tmp/ideastitch-vite.pid ]; then",
            "  old_pid=\"$(cat /tmp/ideastitch-vite.pid)\"",
            "  kill \"$old_pid\" 2>/dev/null || true",
            "  rm -f /tmp/ideastitch-vite.pid",
            "fi",
            "nohup sh -c 'cd /app && exec ./node_modules/.bin/vite --host 0.0.0.0 --port 5173 --strictPort' > /app/dev.log 2>&1 < /dev/null &",
            "echo $! > /tmp/ideastitch-vite.pid",
            "for i in $(seq 1 60); do",
            "  if wget -q -T 2 -O /dev/null http://127.0.0.1:5173/; then exit 0; fi",
            "  vite_pid=\"$(cat /tmp/ideastitch-vite.pid 2>/dev/null)\"",
            "  if [ -z \"$vite_pid\" ] || ! kill -0 \"$vite_pid\" 2>/dev/null; then",
            "    cat /app/dev.log 2>/dev/null || true",
            "    exit 1",
            "  fi",
            "  sleep 1",
            "done",
            "cat /app/dev.log 2>/dev/null || true",
            "exit 1"
    );

    // This command does not end in '&', so execCommand waits for the probe
    // script to finish. It returns only when Vite responds or startup fails.
    execCommand(podName, RUNNER_CONTAINER, "sh", "-c", startAndWait);
}
```

Then in `claimAndStartNewPod(...)`, replace the existing `startCmd` declaration and `execCommand(... startCmd)` block:

```java
String startCmd = "nohup pnpm run dev --host 0.0.0.0 --port 5173 > /app/dev.log 2>&1 &";
log.info("Starting dev server for project {}...", projectId);
execCommand(podName, RUNNER_CONTAINER, "sh", "-c", startCmd);
```

with:

```java
log.info("Starting and checking Vite server for project {}...", projectId);
ensureViteReady(podName);
```

In the `existingPod != null` branch, insert this line immediately before `registerRoute(domain, existingPod);`:

```java
ensureViteReady(podName);
```

The helper checks an already healthy Vite server first; otherwise it starts Vite with a recorded PID and waits up to 60 seconds for HTTP readiness. A timeout or early Vite exit fails deployment instead of claiming the preview is ready. The runner image is Node Alpine, where BusyBox `wget` is available; if your runner image changes, make sure it has `wget` or replace the probe with an equivalent HTTP check.

Deploy the updated workspace-service image, roll out the deployment, and call `Run Preview` again. Check the runner's `/app/dev.log` if startup fails. Do not use a fixed sleep as a readiness substitute.

## 2. Add the Stripe-backed public plan catalog consumed by `/pricing`

The existing `Plan` table stores `stripePriceId` and feature quotas, but the current API does not return Stripe's amount/currency/billing interval. The frontend calls:

`GET https://api.ideastitch.in/api/v1/account/api/payment/plans`

The API gateway forwards that prefix to the account-service, where the controller path is `/api/payment/plans`.

### 2.1 Add a response DTO

Create:
`account/src/main/java/com/lovable_clone_microservices/account_service/dto/subscription/AvailablePlanResponse.java`

```java
package com.lovable_clone_microservices.account_service.dto.subscription;

public record AvailablePlanResponse(
        Long id,
        String name,
        Long unitAmountMinor,
        String currency,
        String interval,
        Long intervalCount,
        Integer maxProjects,
        Integer maxTokensPerDay,
        Integer maxPreviews,
        Boolean unlimitedAi
) {}
```

`unitAmountMinor` is Stripe's integer amount in the currency's smallest unit. For the existing INR plans, `199900` formats as ₹1,999.00 and `99900` as ₹999.00. The currency, interval, and amount below must be read from the actual Stripe Price object; do not hardcode these values in the endpoint.

### 2.2 Add the public endpoint to the existing controller

File:
`account/src/main/java/com/lovable_clone_microservices/account_service/controller/BillingController.java`

Add these imports:

```java
import com.lovable_clone_microservices.account_service.dto.subscription.AvailablePlanResponse;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.repository.PlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Locale;
```

Add this constructor-injected field alongside `subscriptionService` and `paymentProcessor`:

```java
private final PlanRepository planRepository;
```

Add this method inside `BillingController`:

```java
@GetMapping("/api/payment/plans")
public ResponseEntity<List<AvailablePlanResponse>> getAvailablePlans() {
    List<AvailablePlanResponse> result = new ArrayList<>();

    for (Plan plan : planRepository.findAll()) {
        if (!Boolean.TRUE.equals(plan.getActive())
                || plan.getStripePriceId() == null
                || plan.getStripePriceId().isBlank()) {
            continue;
        }

        try {
            Price price = Price.retrieve(plan.getStripePriceId());
            if (!Boolean.TRUE.equals(price.getActive())
                    || price.getRecurring() == null
                    || price.getUnitAmount() == null
                    || price.getCurrency() == null) {
                continue;
            }

            long intervalCount = price.getRecurring().getIntervalCount() == null
                    ? 1L
                    : price.getRecurring().getIntervalCount();

            result.add(new AvailablePlanResponse(
                    plan.getId(),
                    plan.getName(),
                    price.getUnitAmount(),
                    price.getCurrency().toUpperCase(Locale.ROOT),
                    price.getRecurring().getInterval(),
                    intervalCount,
                    plan.getMaxProjects(),
                    plan.getMaxTokensPerDay(),
                    plan.getMaxPreviews(),
                    plan.getUnlimitedAi()
            ));
        } catch (StripeException e) {
            log.error("Unable to read Stripe price for planId={}", plan.getId(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The Stripe plan catalog is temporarily unavailable",
                    e
            );
        }
    }

    return ResponseEntity.ok(result);
}
```

The controller already imports `HttpStatus`, `ResponseEntity`, `List`, `Map`, and `HashMap`; do not duplicate those imports.

### 2.3 Permit only this catalog endpoint without JWT

File:
`account/src/main/java/com/lovable_clone_microservices/account_service/security/AccountSecurityconfig.java`

Replace:

```java
.requestMatchers("/auth/**", "/webhooks/**", "/actuator/**", "/internal/**").permitAll()
```

with:

```java
.requestMatchers(
        "/auth/**",
        "/webhooks/**",
        "/actuator/**",
        "/internal/**",
        "/api/payment/plans"
).permitAll()
```

Checkout (`POST /api/payment/checkout`), portal (`POST /api/payment/portal`) and current-subscription (`GET /api/me/subscription`) must remain authenticated.

### 2.4 Add backend validation before checkout

File:
`account/src/main/java/com/lovable_clone_microservices/account_service/services/implementation/StripePaymentProcessor.java`

Immediately after the existing `Plan plan = ...` lookup in `createCheckoutSessionUrl(...)`, add:

```java
if (!Boolean.TRUE.equals(plan.getActive())
        || plan.getStripePriceId() == null
        || plan.getStripePriceId().isBlank()) {
    throw new BadRequestException("That subscription plan is not available.");
}
```

Do not rely on the browser to enforce plan IDs or prevent purchases of inactive plans. The API must be the authority. Also verify the current user's subscription state on the server so an active, past-due, or incomplete subscription is managed through the Stripe billing portal instead of creating a second subscription checkout session.

## 3. Make Stripe webhook processing idempotent

The current `BillingController` verifies `Stripe-Signature`, but it does not persist `event.getId()`. Stripe retries deliveries, so the same event can run more than once. Add a database uniqueness constraint and process the event plus subscription changes in one transaction.

### 3.1 Create the event table in the production database

Run this migration against the same database used by account-service:

```sql
CREATE TABLE processed_stripe_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
```

Use your normal migration workflow if the project is managed by Flyway/Liquibase. Do not drop or recreate existing subscription tables.

### 3.2 Add the entity

Create:
`account/src/main/java/com/lovable_clone_microservices/account_service/entity/ProcessedStripeEvent.java`

```java
package com.lovable_clone_microservices.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_stripe_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedStripeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedStripeEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }
}
```

### 3.3 Add the repository

Create:
`account/src/main/java/com/lovable_clone_microservices/account_service/repository/ProcessedStripeEventRepository.java`

```java
package com.lovable_clone_microservices.account_service.repository;

import com.lovable_clone_microservices.account_service.entity.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedStripeEventRepository
        extends JpaRepository<ProcessedStripeEvent, Long> {
    boolean existsByEventId(String eventId);
}
```

### 3.4 Add a transactional event processor

Create:
`account/src/main/java/com/lovable_clone_microservices/account_service/services/implementation/StripeWebhookEventService.java`

```java
package com.lovable_clone_microservices.account_service.services.implementation;

import com.lovable_clone_microservices.account_service.entity.ProcessedStripeEvent;
import com.lovable_clone_microservices.account_service.repository.ProcessedStripeEventRepository;
import com.lovable_clone_microservices.account_service.services.PaymentProcessor;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeWebhookEventService {
    private final ProcessedStripeEventRepository repository;
    private final PaymentProcessor paymentProcessor;

    @Transactional
    public void process(
            String eventId,
            String eventType,
            StripeObject stripeObject,
            Map<String, String> metadata
    ) {
        if (repository.existsByEventId(eventId)) {
            return;
        }

        // Flush now so concurrent duplicate deliveries collide on the unique key.
        // If the processor fails, this row and the same-DB subscription writes roll back.
        repository.saveAndFlush(new ProcessedStripeEvent(eventId, eventType));
        paymentProcessor.handleWebhookEvent(eventType, stripeObject, metadata);
    }
}
```

### 3.5 Route controller webhooks through the idempotent service

In `BillingController.java`, add imports:

```java
import com.lovable_clone_microservices.account_service.repository.ProcessedStripeEventRepository;
import com.lovable_clone_microservices.account_service.services.implementation.StripeWebhookEventService;
import org.springframework.dao.DataIntegrityViolationException;
```

Add constructor-injected fields:

```java
private final StripeWebhookEventService stripeWebhookEventService;
private final ProcessedStripeEventRepository processedStripeEventRepository;
```

In `handlePaymentWebhooks(...)`, replace the current line:

```java
paymentProcessor.handleWebhookEvent(event.getType(), stripeObject, metadata);
return ResponseEntity.ok().build();
```

with:

```java
try {
    stripeWebhookEventService.process(event.getId(), event.getType(), stripeObject, metadata);
    return ResponseEntity.ok().build();
} catch (DataIntegrityViolationException concurrentDuplicate) {
    // A simultaneous delivery may have inserted and completed this same event.
    // Only acknowledge it if the committed unique record now exists.
    if (processedStripeEventRepository.existsByEventId(event.getId())) {
        return ResponseEntity.ok().build();
    }
    throw concurrentDuplicate;
}
```

Also replace the signature-verification catch with a 400 response, because a signature-invalid request must not be processed:

```java
} catch (SignatureVerificationException e) {
    log.warn("Rejected Stripe webhook with an invalid signature");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Stripe signature");
}
```

If webhook object deserialization returns `null`, do not return HTTP 200 (that would permanently discard an unprocessed event). Return HTTP 500 so Stripe retries it. Never mark an event processed if its business operation fails. The unique event record is committed in the same database transaction as the processor's local JPA writes; Stripe API calls themselves are external and cannot be rolled back.

### 3.6 Fix subscription state retrieval/update

File:
`account/src/main/java/com/lovable_clone_microservices/account_service/services/implementation/SubscriptionServiceImpl.java`

In `getCurrentSubscription()`, include `SubscriptionStatus.INCOMPLETE` in the queried status set:

```java
var currentSubscription = subscriptionRespository.findByUserIdAndStatusIn(userId, Set.of(
        SubscriptionStatus.ACTIVE,
        SubscriptionStatus.PAST_DUE,
        SubscriptionStatus.TRAILING,
        SubscriptionStatus.INCOMPLETE
)).orElse(new Subscription());
```

This allows the frontend to see a pending/incomplete checkout and prevents presenting it as if the account had no subscription. Keep CANCELED subscriptions out of this active/current set.

In `updateSubscription(...)`, correct the current status condition:

```java
if (status != null && status != subscription.getStatus()) {
    subscription.setStatus(status);
    hasUpdated = true;
}
```

The supplied code currently uses `||`, which can set a null status. `&&` is required so only a known, changed Stripe status overwrites the stored value.

Keep the existing webhook event handlers for `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.paid`, and `invoice.payment_failed`. If you enable delayed/asynchronous payment methods in Checkout, also handle `checkout.session.async_payment_succeeded` and `checkout.session.async_payment_failed` according to the supported Stripe methods; otherwise restrict accepted methods to ones that confirm through your current subscription/invoice webhook path.

## 4. Stripe and environment configuration

1. In Stripe Dashboard, confirm the Pro and Max products each have an active **recurring** Price. In the account database, each active `Plan.stripePriceId` must be the corresponding Stripe `price_...` ID, not a product ID. Keep the two plan names in the account `Plan` table as `Pro Plan` and `Max Plan` (the UI displays them as `Pro` and `Max`). Based on the supplied screenshot, the expected current prices are ₹999/month and ₹1,999/month; the endpoint reads the actual Stripe price rather than treating those screenshot values as authoritative forever.
2. Set account-service's existing Stripe API key and `stripe.webhook.secret` from Kubernetes Secrets/configuration, not a committed YAML or `.env` file. Use the webhook signing secret for the deployed endpoint, not the Stripe API secret key.
3. Register Stripe's webhook URL at the public endpoint routed by the API Gateway: `https://api.ideastitch.in/api/v1/account/webhooks/payment` **only if** that exact path is forwarded unchanged to account-service `/webhooks/payment` in your gateway configuration. Verify this with a test request and the gateway route config before saving it in Stripe Dashboard.
4. Subscribe the Stripe endpoint to `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.paid`, and `invoice.payment_failed`; add async Checkout events only if your configured payment methods require them.
5. Set account-service `app.frontend.url` to `https://ideastitch.in` so Checkout redirects to `/success.html?session_id=...` or `/cancel.html` on the main frontend.
6. Build/deploy the frontend with `VITE_API_URL=https://api.ideastitch.in`. Vite variables are baked in at build time; changing a Kubernetes environment variable on a static Nginx container alone does not rewrite the already-built JavaScript.
7. The frontend Dockerfile in this archive uses Nginx to serve the Vite build. Ensure `k8s/services` points to the rebuilt frontend image; a static SPA should not run `vite dev` in production.

## 5. Apply the branded generated-app starter

The supplied generated starter ZIP in this frontend archive contains IdeaStitch title/description/theme/favicon assets and removes the stock Vite/DaisyUI/React logos. The source confirms the exact initializer constants in `work/src/main/java/com/lovable_clone_microservices/workspace_service/service/impl/ProjectTemplateServiceImpl.java`: bucket `starter-projects`, prefix `react-vite-tailwind-daisyui-starter-main/`. Extract the nested ZIP and mirror the **contents** of its `react-vite-tailwind-daisyui-starter/` directory into that prefix (do not upload the ZIP itself and do not retain the inner folder name as another path level). If your existing `mc` alias is `myminio`, the upload command is:

```bash
unzip deployment-assets/react-vite-tailwind-daisyui-starter-branded.zip -d /tmp/ideastitch-branded-starter
mc mirror --overwrite /tmp/ideastitch-branded-starter/react-vite-tailwind-daisyui-starter/ myminio/starter-projects/react-vite-tailwind-daisyui-starter-main/
```

Use your actual configured `mc` alias if it differs. Verify with `mc ls myminio/starter-projects/react-vite-tailwind-daisyui-starter-main/` that `index.html`, `public/ideastitch-mark.svg`, `public/favicon.ico`, and `public/ideastitch-icon.png` are directly beneath the prefix. Update the default starter template source too, if you keep it in Git, so a later template upload does not reintroduce Vite branding. This affects new projects created after the upload; it does not overwrite an existing user project's intentionally changed `index.html`.

## 6. End-to-end rollout checklist

- Build account-service and workspace-service after applying the changes; run the existing unit/integration tests.
- Apply the database migration before the account-service deployment.
- Deploy account-service and workspace-service, verify their readiness, then build and deploy this frontend archive.
- `GET https://api.ideastitch.in/api/v1/account/api/payment/plans` should return the active Pro and Max plan IDs plus Stripe amount, currency and recurring interval without a JWT.
- With no token, `/projects`, `/projects/{id}` and `/billing` must redirect to `/login`. With an expired JWT, authenticated API calls return 401, clear `auth_token` and `user_info`, and redirect to `/login?reason=session-expired`.
- New checkout must use the Stripe price ID mapped by the selected database Plan. A return to `/success.html` must not activate anything without the webhook updating the account database.
- In Stripe test mode, verify success, cancel, failed invoice, cancellation, plan change, duplicate webhook delivery, and a webhook processor failure followed by Stripe retry. Confirm DB status/period and My Subscriptions reflect each result.
- Run Preview for a new project and an existing project. The deploy request should not return success until HTTP on runner port 5173 responds. Refresh the preview without an artificial second reload.
- Verify `ideastitch.in`, `www.ideastitch.in`, and `project-*.previews.ideastitch.in` load IdeaStitch branding where the project uses the starter's metadata.

## What is not verified from this archive

I could not confirm a real Stripe test payment, live webhook delivery, Kubernetes rollout, or a production build from this environment. The npm dependency install was blocked by unavailable registry/cache access; the front-end TS/TSX syntax was transpiled with zero syntax diagnostics, but a full `npm run build`, tests, and end-to-end cloud checks remain deployment tasks.
