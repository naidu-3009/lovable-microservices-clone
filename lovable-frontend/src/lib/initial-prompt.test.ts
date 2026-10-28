import { describe, expect, it } from "vitest";
import { projectNameFromPrompt } from "@/lib/initial-prompt";

describe("projectNameFromPrompt", () => {
  it("removes build intent and supporting details", () => {
    expect(projectNameFromPrompt("Build a simple expense tracker with monthly charts and budgets"))
      .toBe("Simple Expense Tracker");
  });

  it("keeps concise names when the prompt starts with a description", () => {
    expect(projectNameFromPrompt("A clean portfolio for a product designer"))
      .toBe("Clean Portfolio");
  });

  it("falls back when a prompt has no useful title words", () => {
    expect(projectNameFromPrompt("   ")).toBe("My First Project");
  });

  it("caps the generated title length", () => {
    expect(projectNameFromPrompt("Create exceptionallylongword extraordinarilylongword substantiallylongword tremendouslylongword phenomenallylongword"))
      .toHaveLength(48);
  });
});
