import { ReactNode } from "react";
import { ArrowUpRight, Braces, Code2, Eye, Workflow } from "lucide-react";
import { IdeaStitchBrand } from "@/components/IdeaStitchBrand";
import { ThemeToggle } from "@/components/ThemeToggle";

interface AuthLayoutProps {
  children: ReactNode;
  title: string;
  description: string;
}

export function AuthLayout({ children, title, description }: AuthLayoutProps) {
  return (
    <main className="auth-shell">
      <section className="auth-story" aria-label="About IdeaStitch">
        <div className="auth-story-header">
          <IdeaStitchBrand tagline />
          <span className="auth-document-id">ACCESS DECK / 01</span>
        </div>
        <div className="auth-story-content">
          <p className="micro-label"><span className="signal-dot" /> IDEASTITCH / BUILD SYSTEM</p>
          <h2>From a clear idea<br /><span>to a working product.</span></h2>
          <p className="auth-story-copy">Your project, source files, and preview belong in the same workspace. Start with a brief, then iterate on the result.</p>
          <div className="auth-sequence" aria-label="IdeaStitch workflow">
            <div><span className="auth-sequence-icon"><Workflow size={16} /></span><span>Describe</span></div>
            <span className="auth-sequence-line" />
            <div><span className="auth-sequence-icon"><Braces size={16} /></span><span>Generate</span></div>
            <span className="auth-sequence-line" />
            <div><span className="auth-sequence-icon"><Eye size={16} /></span><span>Inspect</span></div>
          </div>
          <div className="auth-workspace-preview" aria-hidden="true">
            <div className="auth-preview-code">
              <div className="auth-preview-window-bar"><span /><span /><span /><b>src / App.tsx</b></div>
              <div className="auth-code-lines"><i /><i /><i /><i /><i /></div>
            </div>
            <div className="auth-preview-canvas">
              <div className="auth-preview-canvas-top"><span /><span /><span /></div>
              <div className="auth-preview-hero-line" />
              <div className="auth-preview-short-line" />
              <div className="auth-preview-cards"><i /><i /><i /></div>
              <div className="auth-preview-chart"><i /><i /><i /><i /><i /><i /><i /></div>
            </div>
          </div>
        </div>
        <div className="auth-story-foot"><span><Code2 size={14} /> CODE / PREVIEW / ITERATION</span><span>IDEASTITCH.IN</span></div>
        <div className="auth-story-geometry" aria-hidden="true"><span /><span /><span /><span /></div>
      </section>

      <section className="auth-form-stage">
        <div className="auth-stage-header">
          <span className="auth-document-id">SECURE ACCESS <span className="micro-divider">/</span> ACCOUNT</span>
          <ThemeToggle />
        </div>
        <div className="auth-form-content">
          <p className="micro-label"><span className="signal-dot" /> WELCOME</p>
          <h1>{title}</h1>
          <p className="auth-form-description">{description}</p>
          <div className="auth-form-deck">
            <div className="auth-form-deck-head"><span>IDENTITY CHECK</span><span>FORM / 01</span></div>
            {children}
          </div>
          <p className="auth-privacy-note"><span className="auth-privacy-dot" /> Your workspace remains tied to your account.</p>
        </div>
        <div className="auth-mobile-foot"><IdeaStitchBrand compact /><span>ACCESS / 01</span><ArrowUpRight size={15} /></div>
      </section>
    </main>
  );
}
