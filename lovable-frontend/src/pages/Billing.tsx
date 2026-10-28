import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowUpRight, CalendarClock, CheckCircle2, CreditCard, Loader2, RefreshCw, ShieldCheck } from "lucide-react";
import { api, isAuthenticated } from "@/lib/api";
import type { CurrentSubscriptionResponse } from "@/lib/types";
import { IdeaStitchBrand } from "@/components/IdeaStitchBrand";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";

function displayDate(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return new Intl.DateTimeFormat(undefined, { month: "long", day: "numeric", year: "numeric" }).format(date);
}

export default function Billing() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [subscription, setSubscription] = useState<CurrentSubscriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [openingPortal, setOpeningPortal] = useState(false);
  const [loadError, setLoadError] = useState("");

  const loadSubscription = useCallback(async (refresh = false) => {
    setLoadError("");
    refresh ? setRefreshing(true) : setLoading(true);
    try {
      const result = await api.getCurrentSubscription();
      setSubscription(result);
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : "Could not load billing details.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login", { replace: true });
      return;
    }
    void loadSubscription();
  }, [navigate, loadSubscription]);

  const openPortal = async () => {
    setOpeningPortal(true);
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
      setOpeningPortal(false);
    }
  };

  const plan = subscription?.plan;
  const periodEnd = displayDate(subscription?.periodEnd);

  return (
    <div className="billing-shell industrial-app-shell">
      <header className="billing-header">
        <div className="billing-header-inner">
          <IdeaStitchBrand to="/projects" tagline />
          <div className="billing-header-actions">
            <ThemeToggle />
            <Button variant="outline" onClick={() => navigate("/projects")}><ArrowLeft size={15} /> Projects</Button>
          </div>
        </div>
      </header>

      <main className="billing-main">
        <div className="billing-page-heading">
          <p className="micro-label"><span className="signal-dot" /> ACCOUNT / BILLING</p>
          <h1>Plan, without the paperwork.</h1>
          <p>Review the subscription status reported by your account and manage billing securely through the existing payment provider.</p>
        </div>

        {loading ? (
          <section className="billing-state" role="status" aria-live="polite">
            <Loader2 className="animate-spin" size={22} />
            <div><strong>Loading account details</strong><span>Checking the current subscription with IdeaStitch.</span></div>
          </section>
        ) : loadError ? (
          <section className="billing-state billing-state--error" role="alert">
            <div><strong>We couldn’t load billing details.</strong><span>{loadError}</span></div>
            <Button variant="outline" onClick={() => void loadSubscription(true)} disabled={refreshing}>
              {refreshing ? <Loader2 size={15} className="animate-spin" /> : <RefreshCw size={15} />} Try again
            </Button>
          </section>
        ) : (
          <div className="billing-content-grid">
            <section className="billing-plan-panel">
              <div className="billing-panel-heading"><span className="billing-panel-icon"><CreditCard size={19} /></span><span className="micro-label">CURRENT SUBSCRIPTION</span></div>
              {plan?.name ? (
                <>
                  <div className="billing-plan-title-row"><h2>{plan.name}</h2><span className={`billing-status billing-status--${(subscription?.status || "unknown").toLowerCase()}`}>{subscription?.status || "Status unavailable"}</span></div>
                  {plan.price != null && String(plan.price).trim() !== "" && <p className="billing-price">{String(plan.price)} <span>as returned by your account</span></p>}
                  <div className="billing-facts">
                    <div><span>Project allowance</span><strong>{plan.maxProjects == null ? "Not provided" : plan.maxProjects}</strong></div>
                    <div><span>AI usage</span><strong>{plan.unlimitedAi ? "Unlimited" : plan.maxTokensPerDay == null ? "Not provided" : `${plan.maxTokensPerDay.toLocaleString()} tokens / day`}</strong></div>
                    <div><span>Period end</span><strong>{periodEnd || "Not provided"}</strong></div>
                  </div>
                </>
              ) : (
                <div className="billing-no-plan">
                  <h2>No active subscription returned</h2>
                  <p>The account service did not return a current plan. No plan name, price, or allowance is being assumed here.</p>
                </div>
              )}
              <div className="billing-actions">
                <Button onClick={() => void openPortal()} disabled={openingPortal} className="billing-primary-action">
                  {openingPortal ? <Loader2 size={15} className="animate-spin" /> : <ArrowUpRight size={15} />}
                  Open billing portal
                </Button>
                <Button variant="outline" onClick={() => void loadSubscription(true)} disabled={refreshing}>
                  {refreshing ? <Loader2 size={15} className="animate-spin" /> : <RefreshCw size={15} />} Refresh status
                </Button>
              </div>
              <p className="billing-note"><ShieldCheck size={14} /> Payment details and cancellation options are handled by the payment provider, not stored in this screen.</p>
            </section>

            <aside className="billing-side-panel">
              <p className="micro-label">HOW BILLING WORKS</p>
              <div className="billing-side-item"><span><CheckCircle2 size={16} /></span><div><strong>Account-backed status</strong><p>Plan name, limits, status, and period end appear only when the account API returns them.</p></div></div>
              <div className="billing-side-item"><span><CalendarClock size={16} /></span><div><strong>Period information</strong><p>{periodEnd ? `The current period end reported by your account is ${periodEnd}.` : "A billing period end has not been returned for this subscription."}</p></div></div>
              <div className="billing-side-item"><span><CreditCard size={16} /></span><div><strong>Secure management</strong><p>Use the billing portal to review or update payment information when your account is eligible.</p></div></div>
              <div className="billing-help"><span>Need to get back to building?</span><Link to="/projects">Return to projects <ArrowUpRight size={14} /></Link></div>
            </aside>
          </div>
        )}
        <footer className="billing-footer"><span>IDEASTITCH / ACCOUNT SYSTEM</span><Link to="/">Back to homepage</Link></footer>
      </main>
    </div>
  );
}
