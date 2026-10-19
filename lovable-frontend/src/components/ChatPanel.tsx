import { useState, useRef, useEffect } from "react";
import { Send, Loader2, Bot, Copy, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { format } from "date-fns";
import { useStreamParser } from "../hooks/use-stream-parser";
import { ChatEventRenderer } from './ChatEventRenderer';
import { ChatEvent } from "@/lib/types";

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  isStreaming?: boolean;
  createdAt?: string;
  events?: ChatEvent[]; // Structured events from the database
  editedFiles?: string[];
}

interface ChatPanelProps {
  messages: ChatMessage[];
  onSendMessage: (message: string) => void;
  isStreaming: boolean;
  isLoading?: boolean;
  readOnly?: boolean;
}

export function ChatPanel({ messages, onSendMessage, isStreaming, isLoading, readOnly }: ChatPanelProps) {
  const [input, setInput] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isStreaming) return;

    onSendMessage(input.trim());
    setInput("");

    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    const textarea = e.target;
    textarea.style.height = "auto";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`;
  };

  return (
    <div className="flex h-full flex-col bg-panel">
      <div className="flex h-12 shrink-0 items-center justify-between border-b border-border/70 px-4">
        <div><p className="text-sm font-semibold">Companion</p><p className="text-[11px] text-muted-foreground">Plan, build, refine</p></div>
        {isStreaming && <span className="flex items-center gap-1.5 text-[11px] font-medium text-primary"><Loader2 className="h-3 w-3 animate-spin" />Working</span>}
      </div>
      {/* Messages */}
      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
          </div>
        ) : messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center p-8">
            <div className="grid h-14 w-14 place-items-center rounded-xl border border-primary/20 bg-primary/10 mb-4">
              <Bot className="w-7 h-7 text-primary" />
            </div>
            <h3 className="text-base font-semibold mb-1">What are we building?</h3>
            <p className="text-sm text-muted-foreground max-w-xs">
              Describe what you want to build or modify
            </p>
          </div>
        ) : (
          <div className="flex flex-col">
            {messages.map((message) => (
              <MessageItem key={message.id} message={message}
                isStreaming={isStreaming && message.isStreaming} />
            ))}
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="shrink-0 border-t border-border/70 bg-card p-3">
        <form onSubmit={handleSubmit} className="relative">
          <Textarea
            ref={textareaRef}
            value={input}
            onChange={handleTextareaChange}
            onKeyDown={handleKeyDown}
            placeholder={readOnly ? "You have view-only access to this project" : "Describe what you want to build..."}
            className="min-h-[52px] max-h-[200px] resize-none border-border/70 bg-muted/40 pr-12 text-sm shadow-inner"
            disabled={isStreaming || readOnly}
            rows={1}
          />
          <Button
            type="submit"
            size="icon"
            disabled={!input.trim() || isStreaming || readOnly}
            className="absolute right-2 bottom-2 h-8 w-8 rounded-lg"
          >
            {isStreaming ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </Button>
        </form>

        <div className="flex items-center justify-between mt-2 px-1">
          <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
            <Sparkles className="h-3 w-3 text-primary" /><span>Enter to send · Shift + Enter for a new line</span>
          </div>
          {isStreaming && (
            <span className="text-xs text-muted-foreground flex items-center gap-1 font-medium">
              <Loader2 className="w-3 h-3 animate-spin text-primary" />
              Thinking...
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

// Inner Component to handle logic per message
function MessageItem({ message, isStreaming }: { message: ChatMessage, isStreaming: boolean }) {
  // Use the stream parser to turn raw XML text into Event objects live
  // 1. Parse content live if we are streaming OR if we don't have DB events yet
  const liveEvents = useStreamParser(message.content || "");

  // 2. Logic: If we have DB events, use them. Otherwise, use the parsed content.
  const eventsToRender = (message.events && message.events.length > 0)
    ? message.events
    : liveEvents;

  return (
    <div className={`border-b border-border/40 p-4 ${message.role === 'user' ? 'bg-muted/20' : 'bg-panel'}`}>
      <div className="max-w-4xl mx-auto">
        {message.role === "user" ? (
          <div className="flex flex-col items-end gap-2">
            <div className="max-w-[90%] rounded-2xl rounded-tr-sm border border-primary/25 bg-primary/10 px-3.5 py-2.5 text-sm">
              <p className="text-foreground leading-relaxed whitespace-pre-wrap">{message.content}</p>
            </div>
            {message.createdAt && (
              <span className="text-[10px] text-muted-foreground px-1 uppercase tracking-tight">
                {format(new Date(message.createdAt), "HH:mm")}
              </span>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {/* Render granular events (Thought, Tool, Message, File) */}
            <div className="flex flex-col gap-3">
              {eventsToRender.map((event, idx) => {
                const isLast = idx === eventsToRender.length - 1;
                return (
                  <ChatEventRenderer
                    key={idx}
                    event={event}
                    // It is "loading" only if:
                    // 1. The message is currently streaming
                    // 2. AND this is the last event in the list
                    isLoading={isStreaming && isLast}
                  />
                );
              })}
            </div>

            {/* Action buttons for assistant message */}
            {!message.isStreaming && message.content && eventsToRender.length > 0 && (
              <div className="flex items-center gap-1 pt-2">
                <Button variant="ghost" size="icon" aria-label="Copy assistant response" onClick={() => void navigator.clipboard.writeText(message.content)} className="h-8 w-8 text-muted-foreground hover:text-primary">
                  <Copy className="w-3.5 h-3.5" />
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
