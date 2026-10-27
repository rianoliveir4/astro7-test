import { statusAppearance } from "@/components/appointment-status";
import type { Appointment, AppointmentStatus } from "@/types/appointment";

const summaryStatuses: AppointmentStatus[] = ["PENDING", "PROCESSING", "COMPLETED", "FAILED"];

export function AppointmentSummary({ appointments }: { appointments: Appointment[] }) {
  return (
    <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-lg border border-zinc-200 bg-zinc-200 md:grid-cols-4">
      {summaryStatuses.map((status) => {
        const { pluralLabel, dotClassName } = statusAppearance[status];
        const appointmentCount = appointments.filter((appointment) => appointment.status === status).length;

        return (
          <div key={status} className="bg-white px-4 py-3">
            <dt className="flex items-center gap-1.5 text-xs text-zinc-500">
              <span className={`size-1.5 rounded-full ${dotClassName}`} aria-hidden />
              {pluralLabel}
            </dt>
            <dd className="mt-1 text-2xl font-semibold tabular-nums text-zinc-900">{appointmentCount}</dd>
          </div>
        );
      })}
    </dl>
  );
}
