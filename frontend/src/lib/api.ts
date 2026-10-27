import type { Appointment, PatientData, ProblemDetail } from "@/types/appointment";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly problemDetail: ProblemDetail;

  constructor(problemDetail: ProblemDetail) {
    super(problemDetail.detail ?? "Erro inesperado");
    this.name = "ApiError";
    this.problemDetail = problemDetail;
  }

  get status() {
    return this.problemDetail.status;
  }
}

export class NetworkError extends Error {
  constructor() {
    super("Não foi possível conectar ao servidor");
    this.name = "NetworkError";
  }
}

async function request<ResponseBody>(path: string, requestInit?: RequestInit): Promise<ResponseBody> {
  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, requestInit);
  } catch {
    throw new NetworkError();
  }

  if (!response.ok) {
    throw new ApiError(await readProblemDetail(response));
  }

  return response.json() as Promise<ResponseBody>;
}

async function readProblemDetail(response: Response): Promise<ProblemDetail> {
  try {
    return await response.json();
  } catch {
    return { status: response.status, detail: "Erro inesperado no servidor" };
  }
}

export function createAppointment(patientData: PatientData) {
  return request<Appointment>("/api/appointments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(patientData),
  });
}

export function findAppointmentsByIds(appointmentIds: number[]) {
  return request<Appointment[]>(`/api/appointments?ids=${appointmentIds.join(",")}`);
}
