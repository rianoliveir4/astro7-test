import { LoaderCircle, WifiOff } from "lucide-react";
import { AppointmentRow, appointmentGridColumns } from "@/components/appointment-row";
import type { Appointment, PatientData } from "@/types/appointment";

type AppointmentListProps = {
  appointments: Appointment[];
  isLoading: boolean;
  isAutoRefreshing: boolean;
  hasConnectionError: boolean;
  highlightedAppointmentId: number | null;
  onStartNewAppointment: (patientData: PatientData) => void;
  onRemove: (appointmentId: number) => void;
};

export function AppointmentList({
  appointments,
  isLoading,
  isAutoRefreshing,
  hasConnectionError,
  highlightedAppointmentId,
  onStartNewAppointment,
  onRemove,
}: AppointmentListProps) {
  return (
    <section aria-labelledby="appointments-title" className="overflow-hidden rounded-lg border border-zinc-200 bg-white">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 border-b border-zinc-200 px-4 py-3">
        <h2 id="appointments-title" className="text-sm font-semibold text-zinc-900">
          Em acompanhamento
        </h2>
        <RefreshIndicator hasConnectionError={hasConnectionError} isAutoRefreshing={isAutoRefreshing} />
      </div>

      <div
        className={`hidden gap-x-4 border-b border-zinc-200 bg-zinc-50 px-4 py-2 text-xs font-medium text-zinc-500 md:grid ${appointmentGridColumns}`}
      >
        <span>Paciente</span>
        <span>CPF</span>
        <span>Aberto às</span>
        <span>Status</span>
        <span>Protocolo</span>
        <span className="sr-only">Ações</span>
      </div>

      {isLoading && appointments.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-zinc-500">Carregando…</p>
      ) : appointments.length === 0 ? (
        <div className="px-4 py-12 text-center">
          <p className="text-sm font-medium text-zinc-900">Nenhum atendimento aberto ainda</p>
          <p className="mt-1 text-sm text-zinc-500">Os atendimentos abertos acima aparecem aqui.</p>
        </div>
      ) : (
        <ul className="divide-y divide-zinc-100">
          {appointments.map((appointment) => (
            <li key={appointment.id}>
              <AppointmentRow
                appointment={appointment}
                isHighlighted={appointment.id === highlightedAppointmentId}
                onStartNewAppointment={onStartNewAppointment}
                onRemove={onRemove}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function RefreshIndicator({
  hasConnectionError,
  isAutoRefreshing,
}: {
  hasConnectionError: boolean;
  isAutoRefreshing: boolean;
}) {
  if (hasConnectionError) {
    return (
      <p role="status" className="inline-flex items-center gap-1.5 text-xs text-amber-700">
        <WifiOff className="size-3.5" aria-hidden />
        Sem conexão. Tentando novamente…
      </p>
    );
  }

  if (isAutoRefreshing) {
    return (
      <p className="inline-flex items-center gap-1.5 text-xs text-zinc-500">
        <LoaderCircle className="size-3.5 animate-spin" aria-hidden />
        Atualizando automaticamente
      </p>
    );
  }

  return null;
}
