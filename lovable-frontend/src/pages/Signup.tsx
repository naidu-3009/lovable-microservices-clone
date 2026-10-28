import { useEffect, useRef, useState } from "react";
import { ClosedEyeIcon } from "@/components/ClosedEyeIcon";
import { useNavigate, Link } from "react-router-dom";
import { AtSign, Check, Eye, Loader2, LockKeyhole, UserRound } from "lucide-react";
import { api, setAuthToken, setUserInfo } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { AuthLayout } from "@/components/AuthLayout";

export default function Signup() {
  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const passwordRef = useRef<HTMLInputElement>(null);
  const selectionRef = useRef<number | null>(null);
  const navigate = useNavigate();
  const { toast } = useToast();
  const meetsPasswordLength = password.length >= 8;

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
    if (!name.trim() || !username.trim() || !password) {
      toast({ title: "Missing details", description: "Complete all three fields to create your account.", variant: "destructive" });
      return;
    }
    if (!meetsPasswordLength) {
      toast({ title: "Choose a stronger password", description: "Use at least eight characters.", variant: "destructive" });
      return;
    }

    setIsLoading(true);
    try {
      const response = await api.signup({ name: name.trim(), username: username.trim(), password });
      setAuthToken(response.token);
      setUserInfo(response.user);
      toast({ title: "Account created", description: "Your workspace is ready." });
      navigate("/projects");
    } catch (error) {
      toast({ title: "Sign-up failed", description: error instanceof Error ? error.message : "Could not create your account. Try again.", variant: "destructive" });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout title="Make room for your idea." description="Create an account to turn a first brief into an application you can inspect and refine.">
      <form onSubmit={handleSubmit} className="auth-form">
        <div className="auth-field">
          <Label htmlFor="name">Full name</Label>
          <div className="auth-input-wrap">
            <UserRound className="auth-input-icon" aria-hidden="true" size={16} />
            <Input id="name" name="name" type="text" autoComplete="name" placeholder="Your full name" value={name} onChange={(event) => setName(event.target.value)} className="auth-input" disabled={isLoading} required maxLength={80} />
          </div>
        </div>
        <div className="auth-field">
          <Label htmlFor="username">Username</Label>
          <div className="auth-input-wrap">
            <AtSign className="auth-input-icon" aria-hidden="true" size={16} />
            <Input id="username" name="username" type="text" autoComplete="username" placeholder="choose-a-username" value={username} onChange={(event) => setUsername(event.target.value)} className="auth-input" disabled={isLoading} required maxLength={50} />
          </div>
        </div>
        <div className="auth-field">
          <Label htmlFor="password">Password</Label>
          <div className="auth-input-wrap">
            <LockKeyhole className="auth-input-icon" aria-hidden="true" size={16} />
            <Input ref={passwordRef} id="password" name="password" type={showPassword ? "text" : "password"} autoComplete="new-password" placeholder="At least 8 characters" value={password} onChange={(event) => setPassword(event.target.value)} className="auth-input auth-input--password" disabled={isLoading} required minLength={8} />
            <button type="button" className="password-visibility" onClick={togglePassword} aria-label={showPassword ? "Hide password" : "Show password"} aria-pressed={showPassword} disabled={isLoading}>
              {showPassword ? <ClosedEyeIcon width={17} height={17} /> : <Eye size={17} aria-hidden="true" />}
            </button>
          </div>
          <p className={`password-requirement ${meetsPasswordLength ? "is-valid" : ""}`} aria-live="polite">
            <span className="password-requirement-icon">{meetsPasswordLength ? <Check size={12} aria-hidden="true" /> : <span aria-hidden="true">8</span>}</span>
            At least 8 characters {meetsPasswordLength && <span className="password-met">— requirement met</span>}
          </p>
        </div>
        <Button type="submit" disabled={isLoading || !meetsPasswordLength || !name.trim() || !username.trim()} className="auth-submit">
          {isLoading ? <><Loader2 className="h-4 w-4 animate-spin" /> Creating account...</> : <>Create account <span className="auth-submit-icon" aria-hidden="true">↗</span></>}
        </Button>
      </form>
      <p className="auth-switch-copy">Already have an account? <Link to="/login">Sign in <span aria-hidden="true">↗</span></Link></p>
    </AuthLayout>
  );
}
