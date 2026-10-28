import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowUpRight, CheckCircle2, CircleAlert, Loader2, RefreshCw } from "lucide-react";
import { api, isAuthenticated } from "@/lib/api";
import type { CurrentSubscriptionResponse } from "@/lib/types";
import { IdeaStitchBrand } from "@/components/IdeaStitchBrand";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Button } from "@/components/ui/button";

export default function CheckoutReturn({ success }: { success: boolean }) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(success && isAuthenticated());
  const [subscription, setSubscription] = useState<CurrentSubscriptionResponse | null>(null);
  const [error, setError] = useState("");

  const checkSubscription = useCallback(async () => {
    if (!isAuthenticated()) {
      setLoading(false);
      setError("Sign in to check the subscription linked to this account.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const current = await api.getCurrentSubscription();
      setSubscription(current);
    } catch (issue) {
      setError(issue instanceof Error ? issue.message : "The billing status could not be checked yet.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (success) void checkSubscription();
  }, [success, checkSubscription]);

  const activePlanName = subscription?.plan?.name;
  const activeStatus = subscription?.status?.toUpperCase();
  const confirmed = Boolean(activePlanName && activeStatus === "ACTIVE");

  return (
    <div className="checkout-return-shell industrial-app-shell">
      <header className="checkout-return-header">
        <IdeaStitchBrand to={isAuthenticated() ? "/projects" : "/"} tagline />
        <ThemeToggle />
      </header>
      <main className="checkout-return-main">
        <section className="checkout-return-panel">
          <div className={`checkout-return-icon ${success ? "is-success" : "is-cancelled"}`}>
            {success ? <CheckCircle2 size={25} /> : <CircleAlert size={25} />}
          </div>
          <p className="micro-label">BILLING / CHECKOUT</p>
          <h1>{success ? confirmed ? "Your plan is active." : "You're back from checkout." : "Checkout was canceled."}</h1>
          <p className="checkout-return-copy">
            {success
              ? confirmed
                ? `The account service reports your ${activePlanName} plan as active. You're ready to continue building.`
                : "Stripe returned you to IdeaStitch. Subscription activation is confirmed only when the account service reports the updated status."
              : "No successful subscription change is confirmed from this return. You can go back to your projects and continue building."}
          </p>
          {success && (
            <div className="checkout-status-detail" role={error ? "alert" : "status"} aria-live="polite">
              {loading ? <Loader2 className="animate-spin" size={17} /> : error ? <CircleAlert size={17} /> : <CheckCircle2 size={17} />}
              <span>{loading ? "Checking subscription status…" : error ? error : confirmed ? `Status: ${activeStatus}` : subscription ? `Current status: ${activeStatus || "not provided"}. The account service has not confirmed an active plan yet.` : "Subscription status has not been confirmed."}</span>
            </div>
          )}
          <div className="checkout-return-actions">
            {success && <Button onClick={() => void checkSubscription()} disabled={loading} variant="outline">{loading ? <Loader2 className="animate-spin" size={15} /> : <RefreshCw size={15} />} Check status</Button>}
            <Button className="billing-primary-action" onClick={() => navigate(isAuthenticated() ? "/projects" : "/login")}>
              {isAuthenticated() ? "Go to projects" : "Sign in to IdeaStitch"} <ArrowUpRight size={15} />
            </Button>
          </div>
          <Link to="/" className="checkout-return-home"><ArrowLeft size={14} /> Back to homepage</Link>
        </section>
        <footer className="billing-footer"><span>IDEASTITCH / SECURE CHECKOUT RETURN</span><Link to="/billing">Plan & billing</Link></footer>
      </main>
    </div>
  );
}
