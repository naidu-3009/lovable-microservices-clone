import type { SVGProps } from "react";

/** A distinct closed-eye glyph for the visible-password state (not a slash overlay). */
export function ClosedEyeIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
      <path d="M3 12.2c2.25 3.25 5.25 4.9 9 4.9s6.75-1.65 9-4.9" />
      <path d="M6.6 9.3 5.4 7.7M12 8.1V5.9M17.4 9.3l1.2-1.6" />
    </svg>
  );
}
