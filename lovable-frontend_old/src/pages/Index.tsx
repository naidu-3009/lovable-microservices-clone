import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowDownRight, ArrowRight, ArrowUpRight, Braces, Check, Code2, Eye, FileCode2, Layers3, Sparkles, Workflow } from "lucide-react";
import { IdeaStitchBrand, IdeaStitchMark } from "@/components/IdeaStitchBrand";
import { ThemeToggle } from "@/components/ThemeToggle";
import { isAuthenticated } from "@/lib/api";
import { PENDING_PROMPT_KEY } from "@/lib/initial-prompt";

const promptExamples = [
  "A dashboard to track my spending and savings",
  "A booking page for a neighborhood fitness studio",
  "A clean portfolio for a product designer",
];

const buildSteps = [
  { number: "01", title: "Describe the outcome", copy: "Start with the problem, the people using it, and what success should look like.", icon: Sparkles },
  { number: "02", title: "Shape the first version", copy: "IdeaStitch creates a starter workspace and streams code changes as they are made.", icon: Workflow },
  { number: "03", title: "Inspect, refine, export", copy: "Move between the live preview and source files, ask for revisions, and download the project.", icon: Code2 },
];

const capabilities = [
  { number: "A1", title: "Prompt-led changes", copy: "Explain what should change in plain language and keep the conversation alongside the project." },
  { number: "A2", title: "Real source files", copy: "Inspect the generated file tree and read the code behind the interface." },
  { number: "A3", title: "Preview in context", copy: "Review the running application while the project stays connected to its workspace." },
  { number: "A4", title: "A project you can take", copy: "Rename and manage projects, share workspace access, or export files as a ZIP archive." },
];

export default function Index() {
  const [prompt, setPrompt] = useState("");
  const [promptError, setPromptError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated()) navigate("/projects", { replace: true });
  }, [navigate]);

  const startBuilding = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = prompt.trim();
    if (!trimmed) {
      setPromptError("Describe what you want to make first.");
      return;
    }

    try {
      window.sessionStorage.setItem(PENDING_PROMPT_KEY, trimmed);
    } catch {
      setPromptError("Your browser could not save this prompt. Please enable session storage and try again.");
      return;
    }
    navigate(isAuthenticated() ? "/projects" : "/signup");
  };

  return (
    <div className="industrial-home">
      <header className="site-header">
        <IdeaStitchBrand tagline />
        <nav className="site-nav" aria-label="Main navigation">
          <a href="#how-it-works" className="site-nav-link">How it works</a>
          <a href="#capabilities" className="site-nav-link">Capabilities</a>
          <a href="#builder" className="site-nav-link">The workflow</a>
        </nav>
        <div className="site-header-actions">
          <ThemeToggle />
          <Link to="/login" className="key-button key-button--quiet">Sign in</Link>
          <a href="#builder" className="key-button key-button--accent site-header-start">Start building <ArrowUpRight size={15} /></a>
        </div>
      </header>

      <main>
        <section className="hero-bay" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="micro-label"><span className="signal-dot" /> IDEA TO INTERFACE <span className="micro-divider">/</span> BUILD SYSTEM 01</p>
            <h1 id="hero-title">Make the idea.<br /><span>Then make it real.</span></h1>
            <p className="hero-description">An AI application builder for turning a clear brief into a working interface — with the code, preview, and iteration loop kept together.</p>

            <form className="prompt-instrument" onSubmit={startBuilding} id="builder">
              <div className="prompt-instrument-top">
                <span className="instrument-label"><Sparkles size={14} /> START WITH A PROMPT</span>
                <span className="instrument-index">IS / 001</span>
              </div>
              <label htmlFor="homepage-prompt" className="sr-only">Describe the application you want to build</label>
              <textarea
                id="homepage-prompt"
                value={prompt}
                onChange={(event) => { setPrompt(event.target.value); setPromptError(""); }}
                placeholder="Build a simple expense tracker with monthly charts…"
                rows={3}
                maxLength={4000}
                aria-describedby={promptError ? "homepage-prompt-error" : "homepage-prompt-hint"}
              />
              <div className="prompt-instrument-bottom">
                <span className="prompt-hint" id="homepage-prompt-hint">A clear first brief is enough.</span>
                <button type="submit" className="key-button key-button--accent prompt-submit">Start building <ArrowUpRight size={16} /></button>
              </div>
              {promptError && <p className="field-error" id="homepage-prompt-error" role="alert">{promptError}</p>}
            </form>

            <div className="prompt-examples" aria-label="Prompt examples">
              <span className="micro-label">TRY A STARTING POINT</span>
              {promptExamples.map((example) => (
                <button key={example} type="button" className="prompt-example" onClick={() => { setPrompt(example); setPromptError(""); }}>
                  <span>{example}</span><ArrowUpRight size={13} />
                </button>
              ))}
            </div>
          </div>

          <div className="hero-instrument-wrap" aria-label="Illustrative IdeaStitch workspace preview">
            <div className="instrument-caption"><span>WORKSPACE / EXAMPLE</span><span>FIG. 01</span></div>
            <div className="hero-instrument">
              <div className="instrument-topline">
                <span className="instrument-dot" /><span>IDEASTITCH WORKSPACE</span><span className="instrument-topline-spacer" /><span>PREVIEW MODE</span>
              </div>
              <div className="mock-window-header">
                <div className="mock-window-lights"><i /><i /><i /></div>
                <span>spendwise / overview</span>
                <span className="mock-window-state"><span /> concept</span>
              </div>
              <div className="mock-window-body">
                <aside className="mock-sidebar" aria-hidden="true">
                  <IdeaStitchMark size="sm" />
                  <span className="mock-side-line mock-side-line--active" />
                  <span className="mock-side-line" />
                  <span className="mock-side-line" />
                  <span className="mock-side-line mock-side-line--short" />
                  <span className="mock-sidebar-bottom" />
                </aside>
                <div className="mock-app-content">
                  <div className="mock-app-heading"><div><span className="mock-eyebrow">MONDAY, OCTOBER 26</span><strong>Your money, in focus.</strong></div><span className="mock-avatar">S</span></div>
                  <div className="mock-metrics">
                    <div><span>AVAILABLE BALANCE</span><strong>₹42,680</strong><small><i /> Current month</small></div>
                    <div><span>THIS MONTH</span><strong>₹18,240</strong><small>Across 6 categories</small></div>
                  </div>
                  <div className="mock-chart-card">
                    <div className="mock-chart-heading"><strong>Spending overview</strong><span>Last 7 days <ArrowDownRight size={12} /></span></div>
                    <div className="mock-chart" aria-hidden="true"><div className="mock-chart-grid" /><svg viewBox="0 0 460 112" preserveAspectRatio="none"><path d="M0 86 C28 80 38 55 72 63 S117 91 148 65 S195 20 229 42 S270 79 306 52 S347 20 377 38 S423 26 460 8" fill="none" stroke="var(--dp-color-accent)" strokeWidth="3" strokeLinecap="round" /><path d="M0 86 C28 80 38 55 72 63 S117 91 148 65 S195 20 229 42 S270 79 306 52 S347 20 377 38 S423 26 460 8 L460 112 L0 112 Z" fill="var(--dp-chart-fill)" /></svg></div>
                    <div className="mock-chart-axis"><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span><span>SAT</span><span>SUN</span></div>
                  </div>
                  <div className="mock-lower-row"><div><span className="mock-lower-title">Recent activity</span><span className="mock-activity"><i /> Groceries <b>− ₹1,240</b></span><span className="mock-activity"><i /> Transport <b>− ₹480</b></span></div><div className="mock-code-note"><Braces size={15} /><span>Source files</span><strong>src / App.tsx</strong><small>Ready to inspect</small></div></div>
                </div>
              </div>
              <div className="mock-window-foot"><span><span className="signal-dot" /> ILLUSTRATIVE PREVIEW</span><span>APPLICATION / CONCEPT</span></div>
            </div>
            <div className="instrument-footnote"><span>01 — A prompt becomes a workspace</span><span>SCHEMATIC, NOT A LIVE USER PROJECT</span></div>
          </div>
        </section>

        <section className="workflow-section" id="how-it-works" aria-labelledby="workflow-title">
          <div className="section-heading-row">
            <div><p className="micro-label">A CLEAR BUILD LOOP</p><h2 id="workflow-title">From first brief to<br /><span>something you can inspect.</span></h2></div>
            <p className="section-intro">Move from intent to implementation without losing sight of the source. Each step stays connected to the same project.</p>
          </div>
          <div className="workflow-steps">
            {buildSteps.map(({ number, title, copy, icon: Icon }) => (
              <article className="workflow-step" key={number}>
                <div className="step-top"><span className="step-number">{number}</span><Icon size={19} strokeWidth={1.8} /></div>
                <h3>{title}</h3><p>{copy}</p><span className="step-rule" />
              </article>
            ))}
          </div>
        </section>

        <section className="capabilities-deck" id="capabilities" aria-labelledby="capabilities-title">
          <div className="capabilities-heading">
            <div><p className="micro-label"><span className="signal-dot" /> THE WORKBENCH</p><h2 id="capabilities-title">Built for the whole<br />iteration, not just the first screen.</h2></div>
            <p>IdeaStitch keeps the parts of building an application close enough to work together.</p>
          </div>
          <div className="capabilities-grid">
            {capabilities.map((item) => (
              <article className="capability-cell" key={item.number}>
                <span className="capability-number">{item.number}</span><h3>{item.title}</h3><p>{item.copy}</p><Check className="capability-check" size={15} aria-hidden="true" />
              </article>
            ))}
          </div>
          <div className="capabilities-foot"><span><Layers3 size={14} /> ONE PROJECT / CONNECTED TOOLS</span><span>CODE <span className="capability-separator">/</span> PREVIEW <span className="capability-separator">/</span> ITERATION</span></div>
        </section>

        <section className="closing-bay">
          <div className="closing-copy"><p className="micro-label">READY WHEN YOU ARE</p><h2>Give your next idea<br /><span>a place to start.</span></h2><p>Describe the outcome. Use the generated workspace to inspect, change, and refine what comes next.</p></div>
          <div className="closing-actions"><button type="button" className="key-button key-button--accent key-button--large" onClick={() => { document.getElementById("homepage-prompt")?.focus(); window.scrollTo({ top: 0, behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" }); }}>Start with a prompt <ArrowRight size={17} /></button><Link to="/signup" className="key-button key-button--quiet key-button--large">Create an account <ArrowUpRight size={16} /></Link><span className="closing-note">No fabricated demos. Start with your own brief.</span></div>
        </section>
      </main>

      <footer className="site-footer">
        <div className="footer-main"><div className="footer-brand"><IdeaStitchBrand tagline /><p>Weave a thought into something you can test, inspect, and improve.</p></div><div className="footer-column"><span>PRODUCT</span><a href="#how-it-works">How it works</a><a href="#capabilities">Capabilities</a><a href="#builder">Start building</a></div><div className="footer-column"><span>ACCOUNT</span><Link to="/login">Sign in</Link><Link to="/signup">Create account</Link><Link to="/projects">Projects</Link></div><div className="footer-column"><span>WORKSPACE</span><span className="footer-detail"><FileCode2 size={14} /> Source files</span><span className="footer-detail"><Eye size={14} /> Live preview</span><span className="footer-detail"><Code2 size={14} /> Code inspection</span></div></div>
        <div className="footer-bottom"><span>© {new Date().getFullYear()} IdeaStitch</span><span>IDEASTITCH / IDEAS, MADE TANGIBLE</span><a href="#hero-title">Back to top ↑</a></div>
      </footer>
    </div>
  );
}
