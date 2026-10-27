import { Stethoscope } from "lucide-react";
import { AppointmentDashboard } from "@/components/appointment-dashboard";

export default function HomePage() {
  return (
    <>
      <header className="border-b border-zinc-200 bg-white">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-2.5 px-4 sm:px-6">
          <span className="flex size-7 items-center justify-center rounded-md bg-teal-700 text-white">
            <Stethoscope className="size-4" aria-hidden />
          </span>
          <h1 className="text-sm font-semibold text-zinc-900">Atendimentos Astro</h1>
        </div>
      </header>
      <main className="flex-1">
        <AppointmentDashboard />
      </main>
    </>
  );
}
