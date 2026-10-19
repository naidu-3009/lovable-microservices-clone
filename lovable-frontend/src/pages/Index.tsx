import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { isAuthenticated } from "@/lib/api";
import { Loader2 } from "lucide-react";

const Index = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // Redirect based on auth status
    if (isAuthenticated()) {
      // If authenticated, redirect to projects dashboard
      navigate("/projects");
    } else {
      navigate("/login");
    }
  }, [navigate]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center">
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-1/3 left-1/3 w-[500px] h-[500px] bg-primary/5 rounded-full blur-3xl" />
        <div className="absolute bottom-1/3 right-1/3 w-[500px] h-[500px] bg-secondary/5 rounded-full blur-3xl" />
      </div>

      <div className="relative flex flex-col items-center">
        <img src="/ideastitch-icon.png" alt="IdeaStitch logo" className="mb-5 h-20 w-20 rounded-2xl object-cover glow-effect" />
        <h1 className="text-3xl font-bold gradient-text">IdeaStitch</h1>
        <p className="mt-2 mb-4 text-sm text-muted-foreground">Weave ideas into reality</p>
        <div className="flex items-center gap-2 text-muted-foreground">
          <Loader2 className="w-4 h-4 animate-spin" />
          <span>Loading...</span>
        </div>
      </div>
    </div>
  );
};

export default Index;
