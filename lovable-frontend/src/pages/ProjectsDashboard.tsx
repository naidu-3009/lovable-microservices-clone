import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowUpRight, Braces, Download, FolderPlus, Loader2, LogOut, MoreHorizontal, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { api, getUserInfo, removeAuthToken, removeUserInfo } from "@/lib/api";
import { ProjectSummaryResponse } from "@/lib/types";
import { generateGradient } from "@/lib/utils";
import { useToast } from "@/hooks/use-toast";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";

const formatDate = (value: string) => new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", year: "numeric" }).format(new Date(value));

export function ProjectsDashboard() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [projects, setProjects] = useState<ProjectSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [dialog, setDialog] = useState<"create" | "rename" | null>(null);
  const [name, setName] = useState("");
  const [selected, setSelected] = useState<ProjectSummaryResponse | null>(null);
  const [saving, setSaving] = useState(false);

  const loadProjects = async () => {
    setLoading(true);
    try { setProjects(await api.getProjects()); }
    catch { toast({ title: "Couldn’t load projects", description: "Please refresh and try again.", variant: "destructive" }); }
    finally { setLoading(false); }
  };
  useEffect(() => { void loadProjects(); }, []);
  const filtered = useMemo(() => projects.filter((project) => project.name.toLowerCase().includes(query.trim().toLowerCase())), [projects, query]);
  const openCreate = () => { setName(""); setSelected(null); setDialog("create"); };
  const openRename = (project: ProjectSummaryResponse) => { setSelected(project); setName(project.name); setDialog("rename"); };
  const saveProject = async () => {
    const trimmed = name.trim(); if (!trimmed) return;
    setSaving(true);
    try {
      if (dialog === "create") { const created = await api.createProject(trimmed); setProjects((current) => [created, ...current]); toast({ title: "Project created", description: "Your workspace is ready." }); }
      else if (selected) { const updated = await api.updateProject(String(selected.id), trimmed); setProjects((current) => current.map((project) => project.projectId === selected.id ? { ...project, name: updated.name } : project)); toast({ title: "Project renamed" }); }
      setDialog(null);
    } catch { toast({ title: "Couldn’t save project", description: "Please try again.", variant: "destructive" }); }
    finally { setSaving(false); }
  };
  const deleteProject = async (project: ProjectSummaryResponse) => {
    if (!window.confirm(`Delete “${project.name}”? This cannot be undone.`)) return;
    try { await api.deleteProject(String(project.projectId)); setProjects((current) => current.filter((item) => item.id !== project.projectId)); toast({ title: "Project deleted" }); }
    catch { toast({ title: "Couldn’t delete project", variant: "destructive" }); }
  };
  const downloadProject = async (project: ProjectSummaryResponse) => {
    try { const blob = await api.downloadProjectZip(String(project.projectId)); const url = URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = `${project.name}.zip`; link.click(); URL.revokeObjectURL(url); }
    catch { toast({ title: "Couldn’t download project", variant: "destructive" }); }
  };
  const logout = () => { removeAuthToken(); removeUserInfo(); navigate("/login"); };
  const user = getUserInfo();
  return <div className="app-shell">
    <header className="sticky top-0 z-20 border-b border-border/70 bg-background/85 backdrop-blur-xl"><div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between px-4 sm:px-7">
      <button className="flex items-center gap-3 text-left" onClick={() => navigate("/projects")} aria-label="Project Companion home"><span className="grid h-9 w-9 place-items-center rounded-xl border border-primary/30 bg-primary/10 text-primary"><Braces className="h-5 w-5" /></span><span><span className="block text-sm font-semibold tracking-tight">Project Companion</span><span className="block text-[11px] text-muted-foreground">Your AI builder</span></span></button>
      <div className="flex items-center gap-2"><Button className="hidden gap-2 sm:inline-flex" size="sm" onClick={openCreate}><Plus className="h-4 w-4" />New project</Button><DropdownMenu><DropdownMenuTrigger asChild><button className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" aria-label="Open account menu"><Avatar className="h-9 w-9 border border-border"><AvatarFallback className="bg-muted text-xs font-semibold text-foreground">{user?.name?.slice(0, 1).toUpperCase() || "U"}</AvatarFallback></Avatar></button></DropdownMenuTrigger><DropdownMenuContent align="end" className="w-56"><div className="px-2 py-2"><p className="truncate text-sm font-medium">{user?.name || "Account"}</p><p className="truncate text-xs text-muted-foreground">{user?.username}</p></div><DropdownMenuSeparator /><DropdownMenuItem onClick={logout}><LogOut className="mr-2 h-4 w-4" />Sign out</DropdownMenuItem></DropdownMenuContent></DropdownMenu></div>
    </div></header>
    <main className="mx-auto max-w-[1440px] px-4 py-10 sm:px-7 lg:py-14">
      <div className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end"><div><p className="eyebrow mb-3">Workspace</p><h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Build with clarity.</h1><p className="mt-3 max-w-xl text-sm leading-6 text-muted-foreground">Create a project, describe the outcome, then inspect the generated code and preview in one focused workspace.</p></div><Button className="gap-2 sm:hidden" onClick={openCreate}><Plus className="h-4 w-4" />New project</Button></div>
      <div className="mt-10 flex items-center gap-3"><div className="relative w-full max-w-md"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" /><Input className="h-10 border-border/70 bg-card pl-9" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search projects" aria-label="Search projects" /></div><span className="hidden text-xs text-muted-foreground sm:block">{projects.length} project{projects.length === 1 ? "" : "s"}</span></div>
      {loading ? <div className="grid grid-cols-1 gap-4 pt-8 sm:grid-cols-2 xl:grid-cols-3">{[1, 2, 3].map((item) => <div key={item} className="h-56 animate-pulse rounded-xl border border-border/70 bg-card" />)}</div> : filtered.length === 0 ? <EmptyProjects searching={Boolean(query)} onCreate={openCreate} /> : <div className="grid grid-cols-1 gap-4 pt-8 sm:grid-cols-2 xl:grid-cols-3">{filtered.map((project) => <ProjectCard key={project.projectId} project={project} onOpen={() => navigate(`/projects/${project.projectId}`)} onRename={() => openRename(project)} onDownload={() => void downloadProject(project)} onDelete={() => void deleteProject(project)} />)}</div>}
    </main>
    <ProjectDialog mode={dialog} name={name} saving={saving} onNameChange={setName} onClose={() => setDialog(null)} onSave={() => void saveProject()} />
  </div>;
}

function ProjectCard({ project, onOpen, onRename, onDownload, onDelete }: { project: ProjectSummaryResponse; onOpen: () => void; onRename: () => void; onDownload: () => void; onDelete: () => void }) {
  return <article className="surface group overflow-hidden rounded-xl transition duration-200 hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-[0_20px_50px_hsl(222_28%_3%_/_0.34)]"><button className="block w-full text-left" onClick={onOpen}><div className="relative h-28 overflow-hidden" style={project.thumbnailUrl ? undefined : generateGradient(project.name)}>{project.thumbnailUrl && <img src={project.thumbnailUrl} alt="" className="h-full w-full object-cover" />}<div className="absolute inset-0 bg-gradient-to-t from-background/60 to-transparent" /><div className="absolute bottom-3 left-4 rounded-md border border-white/10 bg-background/60 px-2 py-1 text-[11px] font-medium text-foreground backdrop-blur">{project.role || "PROJECT"}</div></div><div className="p-4 pb-3"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h2 className="truncate font-semibold tracking-tight">{project.name}</h2><p className="mt-1 text-xs text-muted-foreground">Created {formatDate(project.createdAt)}</p></div><ArrowUpRight className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground transition group-hover:text-primary" /></div></div></button><div className="flex items-center justify-between border-t border-border/60 px-4 py-2"><span className="text-[11px] uppercase tracking-wider text-muted-foreground">AI workspace</span><DropdownMenu><DropdownMenuTrigger asChild><button className="icon-button" aria-label={`Actions for ${project.name}`}><MoreHorizontal className="h-4 w-4" /></button></DropdownMenuTrigger><DropdownMenuContent align="end"><DropdownMenuItem onClick={onRename}><Pencil className="mr-2 h-4 w-4" />Rename</DropdownMenuItem><DropdownMenuItem onClick={onDownload}><Download className="mr-2 h-4 w-4" />Download</DropdownMenuItem><DropdownMenuSeparator /><DropdownMenuItem className="text-destructive focus:text-destructive" onClick={onDelete}><Trash2 className="mr-2 h-4 w-4" />Delete</DropdownMenuItem></DropdownMenuContent></DropdownMenu></div></article>;
}
function EmptyProjects({ searching, onCreate }: { searching: boolean; onCreate: () => void }) { return <div className="surface mt-8 flex min-h-[310px] flex-col items-center justify-center rounded-xl px-6 text-center"><span className="grid h-12 w-12 place-items-center rounded-xl border border-border bg-muted text-primary"><FolderPlus className="h-5 w-5" /></span><h2 className="mt-5 font-semibold">{searching ? "No matching projects" : "Your workspace is ready"}</h2><p className="mt-2 max-w-sm text-sm leading-6 text-muted-foreground">{searching ? "Try a different name or clear the search." : "Create your first project and tell your companion what you want to make."}</p>{!searching && <Button className="mt-6 gap-2" onClick={onCreate}><Plus className="h-4 w-4" />Create project</Button>}</div>; }
function ProjectDialog({ mode, name, saving, onNameChange, onClose, onSave }: { mode: "create" | "rename" | null; name: string; saving: boolean; onNameChange: (value: string) => void; onClose: () => void; onSave: () => void }) { const creating = mode === "create"; return <Dialog open={mode !== null} onOpenChange={(open) => !open && onClose()}><DialogContent className="border-border/80 bg-card sm:max-w-md"><DialogHeader><DialogTitle>{creating ? "Create a project" : "Rename project"}</DialogTitle><DialogDescription>{creating ? "Give your new workspace a clear, memorable name." : "Choose a name that makes this workspace easy to find."}</DialogDescription></DialogHeader><div className="py-3"><label className="mb-2 block text-sm font-medium" htmlFor="project-name">Project name</label><Input id="project-name" autoFocus value={name} onChange={(event) => onNameChange(event.target.value)} onKeyDown={(event) => event.key === "Enter" && onSave()} placeholder="e.g. Customer portal" /></div><DialogFooter><Button variant="ghost" onClick={onClose}>Cancel</Button><Button disabled={!name.trim() || saving} onClick={onSave}>{saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}{creating ? "Create project" : "Save changes"}</Button></DialogFooter></DialogContent></Dialog>; }
