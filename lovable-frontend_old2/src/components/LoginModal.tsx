import { useEffect, useRef, useState } from "react";
import { ClosedEyeIcon } from "@/components/ClosedEyeIcon";
import { useNavigate, Link } from "react-router-dom";
import { Eye, Loader2, LockKeyhole, UserRound } from "lucide-react";
import { api, setAuthToken, setUserInfo } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { AuthLayout } from "@/components/AuthLayout";

export function LoginModal() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const passwordRef = useRef<HTMLInputElement>(null);
  const selectionRef = useRef<number | null>(null);
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    if (selectionRef.current === null) return;
    const cursor = selectionRef.current;
    passwordRef.current?.focus();
    passwordRef.current?.setSelectionRange(cursor, cursor);
    selectionRef.current = null;
  }, [showPassword]);

  const togglePassword = () => {
    selectionRef.current = passwordRef.current?.selectionStart ?? password.length;
    setShowPassword((current) => !current);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!username.trim() || !password) {
      toast({ title: "Missing credentials", description: "Enter your username and password.", variant: "destructive" });
      return;
    }

    setIsLoading(true);
    try {
      const response = await api.login({ username: username.trim(), password });
      setAuthToken(response.token);
      if (response.user) setUserInfo(response.user);
      toast({ title: "Welcome back", description: "Your workspace is ready." });
      navigate("/projects");
    } catch (error) {
      toast({ title: "Sign-in failed", description: error instanceof Error ? error.message : "Check your credentials and try again.", variant: "destructive" });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout title="Welcome back." description="Sign in to return to your projects and continue building.">
      <form onSubmit={handleSubmit} className="auth-form">
        <div className="auth-field">
          <Label htmlFor="username">Username</Label>
          <div className="auth-input-wrap">
            <UserRound className="auth-input-icon" aria-hidden="true" size={16} />
            <Input id="username" name="username" type="text" autoComplete="username" placeholder="your-username" value={username} onChange={(event) => setUsername(event.target.value)} className="auth-input" disabled={isLoading} required />
          </div>
        </div>
        <div className="auth-field">
          <Label htmlFor="password">Password</Label>
          <div className="auth-input-wrap">
            <LockKeyhole className="auth-input-icon" aria-hidden="true" size={16} />
            <Input ref={passwordRef} id="password" name="password" type={showPassword ? "text" : "password"} autoComplete="current-password" placeholder="Enter your password" value={password} onChange={(event) => setPassword(event.target.value)} className="auth-input auth-input--password" disabled={isLoading} required />
            <button type="button" className="password-visibility" onClick={togglePassword} aria-label={showPassword ? "Hide password" : "Show password"} aria-pressed={showPassword} disabled={isLoading}>
              {showPassword ? <ClosedEyeIcon width={17} height={17} /> : <Eye size={17} aria-hidden="true" />}
            </button>
          </div>
        </div>
        <Button type="submit" disabled={isLoading} className="auth-submit">
          {isLoading ? <><Loader2 className="h-4 w-4 animate-spin" /> Signing in...</> : <>Sign in <span className="auth-submit-icon"><ArrowUpRightFallback /></span></>}
        </Button>
      </form>
      <p className="auth-switch-copy">New to IdeaStitch? <Link to="/signup">Create an account <span aria-hidden="true">↗</span></Link></p>
    </AuthLayout>
  );
}

function ArrowUpRightFallback() {
  return <span aria-hidden="true">↗</span>;
}
