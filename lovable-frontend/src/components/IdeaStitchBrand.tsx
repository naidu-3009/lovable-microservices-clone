import type { HTMLAttributes } from "react";
import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";

interface BrandMarkProps extends HTMLAttributes<HTMLSpanElement> {
  size?: "sm" | "md" | "lg";
}

export function IdeaStitchMark({ size = "md", className, ...props }: BrandMarkProps) {
  return (
    <span className={cn("idea-mark", `idea-mark--${size}`, className)} {...props}>
      <svg viewBox="0 0 48 48" fill="none" role="img" aria-label="IdeaStitch symbol">
        <rect x="1" y="1" width="46" height="46" rx="10" fill="var(--dp-color-raised)" stroke="var(--dp-color-well)" />
        <path d="M12 15.5 23.8 25 36 12.5" stroke="var(--dp-color-ink)" strokeWidth="3.3" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M12 32.5 23.8 23 36 35.5" stroke="var(--dp-color-ink)" strokeWidth="3.3" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M24 23.9 31.1 16.7" stroke="var(--dp-color-accent)" strokeWidth="3.3" strokeLinecap="round" />
        <circle cx="12" cy="15.5" r="3.1" fill="var(--dp-color-accent)" />
        <circle cx="36" cy="35.5" r="3.1" fill="var(--dp-color-accent)" />
        <circle cx="24" cy="24" r="3.6" fill="var(--dp-color-accent)" stroke="var(--dp-color-raised)" strokeWidth="1.2" />
      </svg>
    </span>
  );
}

interface BrandProps {
  compact?: boolean;
  className?: string;
  tagline?: boolean;
  to?: string;
}

export function IdeaStitchBrand({ compact = false, className, tagline = false, to = "/" }: BrandProps) {
  return (
    <Link to={to} className={cn("idea-brand", compact && "idea-brand--compact", className)} aria-label={to === "/projects" ? "IdeaStitch projects" : "IdeaStitch homepage"}>
      <IdeaStitchMark size={compact ? "sm" : "md"} />
      <span className="idea-brand-copy">
        <span className="idea-brand-name">IdeaStitch<span className="idea-brand-period">.</span></span>
        {tagline && <span className="idea-brand-tagline">Ideas, made tangible.</span>}
      </span>
    </Link>
  );
}
