import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { clearAuthSession, getAuthToken, getTokenExpiryMs, isAuthenticated } from "@/lib/api";
import Index from "./pages/Index";
import { LoginModal } from "./components/LoginModal";
import { ProjectView } from "./pages/ProjectView";
import { ProjectsDashboard } from "./pages/ProjectsDashboard";
import Signup from "./pages/Signup";
import NotFound from "./pages/NotFound";
import Billing from "./pages/Billing";
import CheckoutReturn from "./pages/CheckoutReturn";
import Pricing from "./pages/Pricing";
import { ThemeProvider } from "@/components/ThemeProvider";

const queryClient = new QueryClient();

function RequireAuth() {
  const location = useLocation();
  const token = getAuthToken();
  const expiry = getTokenExpiryMs(token);
  const expired = Boolean(token && expiry !== null && (expiry === 0 || expiry <= Date.now() + 5_000));

  if (!isAuthenticated()) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          reason: expired ? "session-expired" : "sign-in-required",
          from: `${location.pathname}${location.search}`,
        }}
      />
    );
  }
  return <Outlet />;
}

/** Log the user out even if a protected page stays open until the JWT expires. */
function SessionExpiryMonitor() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === "auth_token" && event.newValue === null) {
        clearAuthSession();
        navigate("/login?reason=session-expired", { replace: true });
      }
    };
    window.addEventListener("storage", onStorage);

    const token = getAuthToken();
    if (!token) return () => window.removeEventListener("storage", onStorage);
    const expiry = getTokenExpiryMs(token);
    if (expiry === null || expiry === 0 || expiry <= Date.now()) {
      clearAuthSession();
      navigate("/login", { replace: true, state: { reason: "session-expired" } });
      return () => window.removeEventListener("storage", onStorage);
    }
    const timer = window.setTimeout(() => {
      clearAuthSession();
      navigate("/login", { replace: true, state: { reason: "session-expired" } });
    }, Math.min(expiry - Date.now(), 2_147_000_000));
    return () => {
      window.clearTimeout(timer);
      window.removeEventListener("storage", onStorage);
    };
  }, [location.key, location.pathname, navigate]);

  return null;
}

const App = () => (
  <ThemeProvider>
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BrowserRouter>
          <SessionExpiryMonitor />
          <Routes>
            <Route path="/" element={<Index />} />
            <Route path="/login" element={<LoginModal />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/pricing" element={<Pricing />} />
            <Route element={<RequireAuth />}>
              <Route path="/projects" element={<ProjectsDashboard />} />
              <Route path="/projects/:projectId" element={<ProjectView />} />
              <Route path="/billing" element={<Billing />} />
            </Route>
            <Route path="/success.html" element={<CheckoutReturn success />} />
            <Route path="/cancel.html" element={<CheckoutReturn success={false} />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </TooltipProvider>
    </QueryClientProvider>
  </ThemeProvider>
);

export default App;
