import { Braces, Sparkles } from "lucide-react";
import { ReactNode } from "react";

interface AuthLayoutProps {
  children: ReactNode;
  title: string;
  description: string;
}

export function AuthLayout({ children, title, description }: AuthLayoutProps) {
  return (
    <main className="relative grid min-h-screen overflow-hidden bg-background lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative hidden overflow-hidden border-r border-border/60 p-12 lg:flex lg:flex-col">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,hsl(var(--primary)/0.16),transparent_28rem),radial-gradient(circle_at_75%_75%,hsl(210_80%_58%/0.10),transparent_25rem)]" />
        <div className="relative flex items-center gap-3 text-sm font-semibold tracking-tight">
          <div className="grid h-9 w-9 place-items-center rounded-xl border border-primary/30 bg-primary/10 text-primary shadow-[0_0_24px_hsl(var(--primary)/0.14)]"><Braces className="h-5 w-5" /></div>
          Project Companion
        </div>
        <div className="relative my-auto max-w-md">
          <p className="eyebrow mb-5">AI application builder</p>
          <h2 className="text-4xl font-semibold leading-tight tracking-tight text-foreground">From a clear idea to a working product.</h2>
          <p className="mt-5 max-w-sm text-base leading-7 text-muted-foreground">Collaborate with an AI that can understand your project, change the code, and help you inspect each result.</p>
          <div className="mt-10 flex gap-3 rounded-xl border border-border/70 bg-card/70 p-4 text-sm text-muted-foreground shadow-2xl">
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
            <span>Describe the outcome. Project Companion handles the iteration.</span>
          </div>
        </div>
      </section>
      <section className="relative flex items-center justify-center px-5 py-10 sm:px-8">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_90%_0%,hsl(var(--primary)/0.10),transparent_22rem)] lg:hidden" />
        <div className="relative w-full max-w-[25rem]">
          <div className="mb-10 flex items-center gap-3 lg:hidden">
            <div className="grid h-9 w-9 place-items-center rounded-xl border border-primary/30 bg-primary/10 text-primary"><Braces className="h-5 w-5" /></div>
            <span className="font-semibold tracking-tight">Project Companion</span>
          </div>
          <p className="eyebrow mb-3">Welcome</p>
          <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">{description}</p>
          <div className="surface mt-8 rounded-2xl p-6 sm:p-7">{children}</div>
        </div>
      </section>
    </main>
  );
}
