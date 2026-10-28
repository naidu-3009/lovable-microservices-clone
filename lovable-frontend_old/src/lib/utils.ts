import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Helper for deterministic gradient generation
export const generateGradient = (_name: string) => ({
  // Use semantic theme tokens instead of random high-saturation gradients so
  // project tiles stay calm and consistent with the Industrial design system.
  background: `radial-gradient(circle at 18% 16%, rgb(241 91 34 / 0.20), transparent 37%), linear-gradient(135deg, var(--dp-color-well), var(--dp-color-raised))`,
  backgroundSize: "cover",
});
