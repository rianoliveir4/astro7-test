import { LoaderCircle } from "lucide-react";
import type { Appointment, AppointmentStatus as AppointmentStatusValue } from "@/types/appointment";

export const statusAppearance: Record<
  AppointmentStatusValue,
  { label: string; pluralLabel: string; dotClassName: string; badgeClassName: string }
> = {
  PENDING: {
    label: "Aguardando processamento",
    pluralLabel: "Aguardando",
    dotClassName: "bg-amber-500",
    badgeClassName: "bg-amber-50 text-amber-800",
  },
  PROCESSING: {
    label: "Processando",
    pluralLabel: "Processando",
    dotClassName: "bg-sky-500",
    badgeClassName: "bg-sky-50 text-sky-800",
  },
  COMPLETED: {
    label: "Concluído",
    pluralLabel: "Concluídos",
    dotClassName: "bg-emerald-500",
    badgeClassName: "bg-emerald-50 text-emerald-800",
  },
  FAILED: {
    label: "Falhou",
    pluralLabel: "Falhas",
    dotClassName: "bg-red-500",
    badgeClassName: "bg-red-50 text-red-800",
  },
};

export function AppointmentStatus({ appointment }: { appointment: Appointment }) {
  const { label, dotClassName, badgeClassName } = statusAppearance[appointment.status];
  const isRetrying = appointment.status === "PENDING" && appointment.attemptCount > 0;

  return (
    <span className="inline-flex flex-col gap-1 max-md:items-end">
      <span
        className={`inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap ${badgeClassName}`}
      >
        {appointment.status === "PROCESSING" ? (
          <LoaderCircle className="size-3 animate-spin" aria-hidden />
        ) : (
          <span className={`size-1.5 rounded-full ${dotClassName}`} aria-hidden />
        )}
        {label}
      </span>
      {isRetrying && (
        <span className="text-xs text-zinc-500">Tentativa {appointment.attemptCount + 1}</span>
      )}
    </span>
  );
}
