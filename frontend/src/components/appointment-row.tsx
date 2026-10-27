"use client";

import { Check, Copy } from "lucide-react";
import { useState } from "react";
import { AppointmentStatus } from "@/components/appointment-status";
import { maskCpf } from "@/lib/cpf";
import type { Appointment, PatientData } from "@/types/appointment";

const COPY_FEEDBACK_DURATION_MS = 2000;

export const appointmentGridColumns =
  "md:grid-cols-[minmax(0,1.4fr)_8.5rem_4.5rem_12.5rem_minmax(0,2fr)_10rem]";

type AppointmentRowProps = {
  appointment: Appointment;
  isHighlighted: boolean;
  onStartNewAppointment: (patientData: PatientData) => void;
  onRemove: (appointmentId: number) => void;
};

export function AppointmentRow({
  appointment,
  isHighlighted,
  onStartNewAppointment,
  onRemove,
}: AppointmentRowProps) {
  const isFinished = appointment.status === "COMPLETED" || appointment.status === "FAILED";

  return (
    <div
      className={`grid grid-cols-[minmax(0,1fr)_auto] gap-x-4 gap-y-1.5 px-4 py-3 transition-colors md:items-center md:gap-y-0 ${appointmentGridColumns} ${
        isHighlighted
          ? "bg-amber-50/70 shadow-[inset_3px_0_0_var(--color-amber-400)]"
          : "hover:bg-zinc-50"
      }`}
    >
      <span className="truncate text-sm font-medium text-zinc-900 max-md:col-start-1 max-md:row-start-1">
        {appointment.patientName}
      </span>

      <span className="text-sm tabular-nums text-zinc-500 max-md:col-start-1 max-md:row-start-2">
        {maskCpf(appointment.patientCpf)}
      </span>

      <span className="text-sm tabular-nums text-zinc-500 max-md:col-start-2 max-md:row-start-2 max-md:text-right">
        {formatTime(appointment.createdAt)}
      </span>

      <span className="max-md:col-start-2 max-md:row-start-1 max-md:text-right">
        <AppointmentStatus appointment={appointment} />
      </span>

      <div className="min-w-0 max-md:col-span-2 max-md:row-start-3 max-md:empty:hidden">
        {appointment.status === "COMPLETED" && appointment.protocol && (
          <ProtocolValue protocol={appointment.protocol} />
        )}
        {appointment.status === "FAILED" && (
          <span className="text-sm text-zinc-500">Não foi possível gerar o protocolo</span>
        )}
      </div>

      <div className="flex gap-4 text-sm max-md:col-span-2 max-md:row-start-4 max-md:empty:hidden md:justify-end">
        {isFinished && (
          <>
            <button
              type="button"
              onClick={() =>
                onStartNewAppointment({
                  patientName: appointment.patientName,
                  patientCpf: appointment.patientCpf,
                })
              }
              className="font-medium text-teal-700 underline-offset-4 hover:text-teal-800 hover:underline"
            >
              Novo atendimento
            </button>
            <button
              type="button"
              onClick={() => onRemove(appointment.id)}
              className="text-zinc-500 underline-offset-4 hover:text-zinc-900 hover:underline"
            >
              Remover
            </button>
          </>
        )}
      </div>
    </div>
  );
}

function ProtocolValue({ protocol }: { protocol: string }) {
  const [wasCopied, setWasCopied] = useState(false);

  async function copyProtocol() {
    try {
      await navigator.clipboard.writeText(protocol);
      setWasCopied(true);
      setTimeout(() => setWasCopied(false), COPY_FEEDBACK_DURATION_MS);
    } catch {
      setWasCopied(false);
    }
  }

  return (
    <span className="inline-flex max-w-full animate-reveal items-center gap-1 rounded-md bg-zinc-100 py-0.5 pr-0.5 pl-2">
      <code className="truncate font-mono text-xs text-zinc-800" title={protocol}>
        {protocol}
      </code>
      <button
        type="button"
        onClick={copyProtocol}
        aria-label={wasCopied ? "Protocolo copiado" : "Copiar protocolo"}
        title={wasCopied ? "Copiado" : "Copiar"}
        className="shrink-0 rounded p-1 text-zinc-500 transition-colors hover:bg-white hover:text-zinc-900"
      >
        {wasCopied ? (
          <Check className="size-3.5 text-emerald-600" aria-hidden />
        ) : (
          <Copy className="size-3.5" aria-hidden />
        )}
      </button>
    </span>
  );
}

function formatTime(isoDateTime: string) {
  return new Date(isoDateTime).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}
