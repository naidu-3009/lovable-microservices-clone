import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowUpRight, Check, CreditCard, Loader2, RefreshCw, ShieldCheck } from "lucide-react";
import { api, isAuthenticated } from "@/lib/api";
import type { AvailablePlan, CurrentSubscriptionResponse } from "@/lib/types";
import { IdeaStitchBrand } from "@/components/IdeaStitchBrand";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";

const visibleFallbackPlans: AvailablePlan[] = [
  { id: -1, name: "Pro Plan", unitAmountMinor: 99_900, currency: "INR", interval: "month", intervalCount: 1 },
  { id: -2, name: "Max Plan", unitAmountMinor: 199_900, currency: "INR", interval: "month", intervalCount: 1 },
];

function displayPlanName(name: string) {
  return name.replace(/\s+plan$/i, "").trim() || name;
}

function formatPrice(amountMinor: number, currency: string) {
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: currency.toUpperCase(),
      maximumFractionDigits: 2,
    }).format(amountMinor / 100);
  } catch {
    return `${currency.toUpperCase()} ${(amountMinor / 100).toFixed(2)}`;
  }
}

function intervalText(interval: string, intervalCount: number) {
  const count = Math.max(1, intervalCount || 1);
  if (interval === "month") return count === 1 ? "per month" : `every ${count} months`;
  if (interval === "year") return count === 1 ? "per year" : `every ${count} years`;
  if (interval === "week") return count === 1 ? "per week" : `every ${count} weeks`;
  if (interval === "day") return count === 1 ? "per day" : `every ${count} days`;
  return `every ${count} ${interval}${count === 1 ? "" : "s"}`;
}

function planFeatures(plan: AvailablePlan): string[] {
  const features: string[] = [];
  if (plan.maxProjects != null) features.push(`${plan.maxProjects.toLocaleString("en-IN")} project${plan.maxProjects === 1 ? "" : "s"}`);
  if (plan.unlimitedAi === true) features.push("Unlimited AI generation");
  else if (plan.maxTokensPerDay != null) features.push(`${plan.maxTokensPerDay.toLocaleString("en-IN")} AI tokens per day`);
  if (plan.maxPreviews != null) features.push(`${plan.maxPreviews.toLocaleString("en-IN")} preview${plan.maxPreviews === 1 ? "" : "s"}`);
  return features;
}

export default function Pricing() {
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const [plans, setPlans] = useState<AvailablePlan[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");
  const [subscription, setSubscription] = useState<CurrentSubscriptionResponse | null>(null);
  const [checkoutPlanId, setCheckoutPlanId] = useState<number | null>(null);
  const [portalLoading, setPortalLoading] = useState(false);

  const hasLiveCatalog = !catalogError && plans.length > 0 && plans.every((plan) => plan.id > 0);
  const currentStatus = subscription?.status?.toUpperCase() || "";
  const hasManagedSubscription = Boolean(
    subscription?.plan?.id && ["ACTIVE", "TRAILING", "PAST_DUE", "INCOMPLETE", "UNPAID"].includes(currentStatus),
  );

  useEffect(() => {
    let alive = true;
    setCatalogLoading(true);
    setCatalogError("");
    void api.getAvailablePlans().then((availablePlans) => {
      if (!alive) return;
      const activePlans = availablePlans
        .filter((plan) => Number.isFinite(plan.id) && plan.id > 0 && plan.name)
        .sort((a, b) => {
          const rank = (name: string) => /\bpro\b/i.test(name) ? 0 : /\bmax\b/i.test(name) ? 1 : 2;
          return rank(a.name) - rank(b.name) || a.name.localeCompare(b.name);
        });
      if (activePlans.length === 0) throw new Error("No active subscription plans are configured.");
      setPlans(activePlans);
    }).catch((error: unknown) => {
      if (!alive) return;
      setPlans(visibleFallbackPlans);
      setCatalogError(error instanceof Error ? error.message : "The subscription catalog is unavailable.");
    }).finally(() => {
      if (alive) setCatalogLoading(false);
    });

    if (isAuthenticated()) {
      void api.getCurrentSubscription().then((result) => {
        if (alive) setSubscription(result);
      }).catch(() => {
        // Central auth handling redirects on expired JWTs. Other failures should not prevent plan browsing.
      });
    }
    return () => { alive = false; };
  }, [location.key]);

  const activePlanId = subscription?.plan?.id;
  const pageMessage = useMemo(() => {
    if (catalogLoading) return "Reading the active plan catalog from IdeaStitch.";
    if (catalogError) return "Plans are temporarily unavailable. Refresh to retry; checkout remains disabled until the account service confirms the Stripe-linked plan IDs.";
    return "Prices, billing intervals, and feature limits come from your active IdeaStitch account plan configuration.";
  }, [catalogLoading, catalogError]);

  const manageSubscription = async () => {
    setPortalLoading(true);
    try {
      const { portalUrl } = await api.openBillingPortal();
      window.location.assign(portalUrl);
    } catch (error) {
      toast({
        title: "Billing portal unavailable",
        description: error instanceof Error ? error.message : "The billing portal could not be opened for this account.",
        variant: "destructive",
      });
    } finally {
      setPortalLoading(false);
    }
  };

  const choosePlan = async (plan: AvailablePlan) => {
    if (!isAuthenticated()) {
      navigate("/login", { state: { from: "/pricing" } });
      return;
    }
    if (hasManagedSubscription) {
      await manageSubscription();
      return;
    }
    if (!hasLiveCatalog || plan.id < 1) {
      toast({
        title: "Checkout is not ready",
        description: "The account service must return the active plan IDs linked to Stripe before a checkout session can be created.",
        variant: "destructive",
      });
      return;
    }
    setCheckoutPlanId(plan.id);
    try {
      const { checkOutUrl } = await api.createCheckoutSession(plan.id);
      window.location.assign(checkOutUrl);
    } catch (error) {
      toast({
        title: "Could not start checkout",
        description: error instanceof Error ? error.message : "The checkout session could not be created. Please try again.",
        variant: "destructive",
      });
    } finally {
      setCheckoutPlanId(null);
    }
  };

  const refreshPlans = () => {
    setCatalogLoading(true);
    setCatalogError("");
    void api.getAvailablePlans().then((availablePlans) => {
      const activePlans = availablePlans
        .filter((plan) => Number.isFinite(plan.id) && plan.id > 0 && plan.name)
        .sort((a, b) => {
          const rank = (name: string) => /\bpro\b/i.test(name) ? 0 : /\bmax\b/i.test(name) ? 1 : 2;
          return rank(a.name) - rank(b.name) || a.name.localeCompare(b.name);
        });
      if (!activePlans.length) throw new Error("No active subscription plans are configured.");
      setPlans(activePlans);
    }).catch((error: unknown) => {
      setPlans(visibleFallbackPlans);
      setCatalogError(error instanceof Error ? error.message : "The subscription catalog is unavailable.");
    }).finally(() => setCatalogLoading(false));
  };

  return (
    <div className="pricing-shell industrial-app-shell">
      <header className="billing-header">
        <div className="billing-header-inner">
          <IdeaStitchBrand to="/" tagline />
          <div className="billing-header-actions">
            <ThemeToggle />
            <Button variant="outline" onClick={() => navigate(isAuthenticated() ? "/projects" : "/")}>
              <ArrowLeft size={15} /> {isAuthenticated() ? "Projects" : "Homepage"}
            </Button>
          </div>
        </div>
      </header>

      <main className="pricing-main">
        <div className="pricing-heading">
          <p className="micro-label"><span className="signal-dot" /> IDEASTITCH / PLANS</p>
          <h1>Choose the room your ideas need.</h1>
          <p>One workspace for prompts, source files, live previews, and iteration. Compare the active plans configured for your account.</p>
        </div>

        <div className={`pricing-catalog-message${catalogError ? " is-error" : ""}`} role={catalogError ? "alert" : "status"} aria-live="polite">
          {catalogLoading ? <Loader2 className="animate-spin" size={16} /> : catalogError ? <RefreshCw size={16} /> : <ShieldCheck size={16} />}
          <span>{pageMessage}</span>
          {catalogError && <Button variant="outline" size="sm" onClick={refreshPlans} disabled={catalogLoading}>Refresh plans</Button>}
        </div>

        <div className="pricing-grid">
          {plans.map((plan, index) => {
            const features = planFeatures(plan);
            const fallback = plan.id < 1 || Boolean(catalogError);
            const isCurrent = Boolean(activePlanId && activePlanId === plan.id);
            return (
              <section className={`pricing-plan-card${index === 1 ? " pricing-plan-card--featured" : ""}`} key={`${plan.name}-${plan.id}`}>
                <div className="pricing-card-topline">
                  <span className="micro-label">PLAN / {String(index + 1).padStart(2, "0")}</span>
                  {isCurrent && <span className="pricing-current-label">CURRENT PLAN</span>}
                  {!isCurrent && index === 1 && <span className="pricing-recommended-label">MORE ROOM</span>}
                </div>
                <h2>{displayPlanName(plan.name)}</h2>
                <div className="pricing-price-line">
                  <strong>{formatPrice(plan.unitAmountMinor, plan.currency)}</strong>
                  <span>{intervalText(plan.interval, plan.intervalCount)}</span>
                </div>
                <p className="pricing-plan-summary">
                  {displayPlanName(plan.name) === "Pro"
                    ? "A focused plan for building and refining your projects."
                    : displayPlanName(plan.name) === "Max"
                      ? "More room to create, iterate, and manage projects."
                      : "A plan for your IdeaStitch workspace."}
                </p>
                <div className="pricing-feature-list">
                  {features.length > 0 ? features.map((feature) => (
                    <div key={feature}><Check size={15} aria-hidden="true" /><span>{feature}</span></div>
                  )) : (
                    <div className="pricing-feature-muted"><CreditCard size={15} aria-hidden="true" /><span>Detailed feature limits are managed by the account plan configuration.</span></div>
                  )}
                  <div><Check size={15} aria-hidden="true" /><span>AI-assisted code and live previews</span></div>
                  <div><Check size={15} aria-hidden="true" /><span>Inspect source files and export a ZIP</span></div>
                </div>
                <Button
                  className="pricing-plan-action"
                  onClick={() => void choosePlan(plan)}
                  disabled={catalogLoading || checkoutPlanId !== null || (fallback && !hasManagedSubscription)}
                >
                  {checkoutPlanId === plan.id || (portalLoading && hasManagedSubscription)
                    ? <Loader2 size={15} className="animate-spin" />
                    : hasManagedSubscription ? <CreditCard size={15} /> : <ArrowUpRight size={15} />}
                  {hasManagedSubscription ? (isCurrent ? "Manage current plan" : "Change plan in Stripe") : fallback ? "Checkout unavailable" : "Choose this plan"}
                </Button>
                <p className="pricing-plan-footnote">{hasManagedSubscription ? "Plan changes and cancellation are handled securely in Stripe." : "Secure checkout is opened by the IdeaStitch account service."}</p>
              </section>
            );
          })}
        </div>

        <div className="pricing-security-note"><ShieldCheck size={16} /><p>Subscription status is confirmed by IdeaStitch's account service and Stripe webhooks—not by a browser redirect alone.</p></div>
        <footer className="billing-footer"><span>IDEASTITCH / PRICING</span><Link to="/">Back to homepage <ArrowUpRight size={13} /></Link></footer>
      </main>
    </div>
  );
}
