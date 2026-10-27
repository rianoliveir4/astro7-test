"use client";

import { useState } from "react";
import { AppointmentForm } from "@/components/appointment-form";
import { AppointmentList } from "@/components/appointment-list";
import { AppointmentSummary } from "@/components/appointment-summary";
import { useTrackedAppointmentIds } from "@/hooks/use-tracked-appointment-ids";
import { isAppointmentActive, useTrackedAppointments } from "@/hooks/use-tracked-appointments";
import type { Appointment, PatientData } from "@/types/appointment";

const EMPTY_PATIENT: PatientData = { patientName: "", patientCpf: "" };

export function AppointmentDashboard() {
  const { trackedAppointmentIds, trackAppointment, untrackAppointment } = useTrackedAppointmentIds();
  const {
    data: fetchedAppointments = [],
    isLoading,
    isError,
  } = useTrackedAppointments(trackedAppointmentIds);
  const [highlightedAppointmentId, setHighlightedAppointmentId] = useState<number | null>(null);
  const [formSetup, setFormSetup] = useState({ defaultPatient: EMPTY_PATIENT, formVersion: 0 });

  const trackedAppointments = trackedAppointmentIds.flatMap((appointmentId) =>
    fetchedAppointments.filter((appointment) => appointment.id === appointmentId),
  );

  function handleAppointmentCreated(createdAppointment: Appointment) {
    trackAppointment(createdAppointment.id);
    setHighlightedAppointmentId(null);
  }

  function handleActiveAppointmentFound(activeAppointmentId: number) {
    trackAppointment(activeAppointmentId);
    setHighlightedAppointmentId(activeAppointmentId);
  }

  function handleStartNewAppointment(patientData: PatientData) {
    setFormSetup((currentFormSetup) => ({
      defaultPatient: patientData,
      formVersion: currentFormSetup.formVersion + 1,
    }));
    setHighlightedAppointmentId(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function handleRemoveAppointment(appointmentId: number) {
    untrackAppointment(appointmentId);
    if (highlightedAppointmentId === appointmentId) {
      setHighlightedAppointmentId(null);
    }
  }

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 px-4 py-6 sm:px-6">
      <AppointmentForm
        key={formSetup.formVersion}
        defaultPatient={formSetup.defaultPatient}
        onAppointmentCreated={handleAppointmentCreated}
        onActiveAppointmentFound={handleActiveAppointmentFound}
      />

      <AppointmentSummary appointments={trackedAppointments} />

      <AppointmentList
        appointments={trackedAppointments}
        isLoading={isLoading}
        isAutoRefreshing={trackedAppointments.some(isAppointmentActive)}
        hasConnectionError={isError}
        highlightedAppointmentId={highlightedAppointmentId}
        onStartNewAppointment={handleStartNewAppointment}
        onRemove={handleRemoveAppointment}
      />
    </div>
  );
}
