"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { findAppointmentsByIds } from "@/lib/api";
import type { Appointment } from "@/types/appointment";

const POLLING_INTERVAL_MS = 2000;

export function isAppointmentActive(appointment: Appointment) {
  return appointment.status === "PENDING" || appointment.status === "PROCESSING";
}

export function useTrackedAppointments(appointmentIds: number[]) {
  return useQuery({
    queryKey: ["appointments", appointmentIds],
    queryFn: () => findAppointmentsByIds(appointmentIds),
    enabled: appointmentIds.length > 0,
    placeholderData: keepPreviousData,
    refetchInterval: (query) => {
      const hasActiveAppointment = query.state.data?.some(isAppointmentActive) ?? false;
      return hasActiveAppointment || query.state.status === "error" ? POLLING_INTERVAL_MS : false;
    },
  });
}
