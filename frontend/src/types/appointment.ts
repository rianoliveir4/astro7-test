export type AppointmentStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export type Appointment = {
  id: number;
  patientName: string;
  patientCpf: string;
  status: AppointmentStatus;
  protocol: string | null;
  attemptCount: number;
  nextAttemptAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PatientData = {
  patientName: string;
  patientCpf: string;
};

export type ProblemDetail = {
  status: number;
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
  activeAppointmentId?: number;
};
