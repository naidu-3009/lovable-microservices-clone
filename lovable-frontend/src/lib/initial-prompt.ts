export const PENDING_PROMPT_KEY = "ideastitch.pending-prompt";

/**
 * A deterministic, no-network fallback title for the current single-starter flow.
 * Server-generated title and template metadata can replace this in the planned
 * generation-workflow backend change without changing the UI contract.
 */
export function projectNameFromPrompt(prompt: string): string {
  const cleaned = prompt
    .trim()
    .replace(/^(?:please\s+)?(?:help\s+me\s+)?(?:build|create|make|design|develop|generate|write)\s+(?:me\s+)?/i, "")
    .replace(/^(?:a|an|the)\s+/i, "")
    .split(/[.!?;\n]/, 1)[0]
    .split(/\b(?:with|for|that|which|including|using|so\s+that|to)\b/i, 1)[0]
    .replace(/[^\p{L}\p{N}\s-]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();

  const words = cleaned.split(" ").filter(Boolean).slice(0, 5);
  const title = words.map((word) => word.charAt(0).toLocaleUpperCase() + word.slice(1)).join(" ");
  return (title || "My First Project").slice(0, 48).trim();
}
