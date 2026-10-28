import { ThemeToggle } from "@/components/ThemeToggle";
import { useLocation } from "react-router-dom";
import { useEffect } from "react";

const NotFound = () => {
  const location = useLocation();

  useEffect(() => {
    console.error("404 Error: User attempted to access non-existent route:", location.pathname);
  }, [location.pathname]);

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-background px-5">
      <div className="absolute right-5 top-5"><ThemeToggle /></div>
      <div className="surface max-w-md rounded-lg p-10 text-center">
        <p className="eyebrow mb-3">ROUTE NOT FOUND / 404</p>
        <h1 className="mb-4 text-4xl font-bold">404</h1>
        <p className="mb-4 text-xl text-muted-foreground">Oops! Page not found</p>
        <a href="/" className="key-button key-button--accent mt-3 inline-flex">
          Return to Home ↗
        </a>
      </div>
    </div>
  );
};

export default NotFound;
