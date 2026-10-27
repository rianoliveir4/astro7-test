import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppointmentForm } from "@/components/appointment-form";
import { ApiError, NetworkError, createAppointment } from "@/lib/api";
import type { Appointment } from "@/types/appointment";

vi.mock("@/lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/api")>()),
  createAppointment: vi.fn(),
}));

const createAppointmentMock = vi.mocked(createAppointment);

const createdAppointment: Appointment = {
  id: 1,
  patientName: "Ana Souza",
  patientCpf: "52998224725",
  status: "PENDING",
  protocol: null,
  attemptCount: 0,
  nextAttemptAt: null,
  createdAt: "2026-10-26T10:00:00",
  updatedAt: "2026-10-26T10:00:00",
};

function renderAppointmentForm() {
  const onAppointmentCreated = vi.fn();
  const onActiveAppointmentFound = vi.fn();
  render(
    <AppointmentForm
      defaultPatient={{ patientName: "", patientCpf: "" }}
      onAppointmentCreated={onAppointmentCreated}
      onActiveAppointmentFound={onActiveAppointmentFound}
    />,
  );
  return { onAppointmentCreated, onActiveAppointmentFound };
}

async function fillAndSubmitForm(patientName: string, patientCpf: string) {
  const user = userEvent.setup();
  if (patientName) {
    await user.type(screen.getByLabelText("Nome completo"), patientName);
  }
  if (patientCpf) {
    await user.type(screen.getByLabelText("CPF"), patientCpf);
  }
  await user.click(screen.getByRole("button", { name: "Abrir atendimento" }));
}

describe("AppointmentForm", () => {
  beforeEach(() => {
    createAppointmentMock.mockReset();
  });

  it("shows validation errors without calling the API", async () => {
    renderAppointmentForm();

    await fillAndSubmitForm("", "11122233344");

    expect(await screen.findByText("Informe o nome")).toBeInTheDocument();
    expect(screen.getByText("CPF inválido")).toBeInTheDocument();
    expect(createAppointmentMock).not.toHaveBeenCalled();
  });

  it("applies the CPF mask while typing", async () => {
    renderAppointmentForm();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText("CPF"), "52998224725");

    expect(screen.getByLabelText("CPF")).toHaveValue("529.982.247-25");
  });

  it("submits valid data and notifies the created appointment", async () => {
    createAppointmentMock.mockResolvedValue(createdAppointment);
    const { onAppointmentCreated } = renderAppointmentForm();

    await fillAndSubmitForm("Ana Souza", "52998224725");

    expect(createAppointmentMock).toHaveBeenCalledWith({
      patientName: "Ana Souza",
      patientCpf: "529.982.247-25",
    });
    expect(onAppointmentCreated).toHaveBeenCalledWith(createdAppointment);
    expect(screen.getByLabelText("Nome completo")).toHaveValue("");
  });

  it("shows backend field errors returned with status 400", async () => {
    createAppointmentMock.mockRejectedValue(
      new ApiError({ status: 400, errors: { patientCpf: "CPF já cadastrado de forma inválida" } }),
    );
    renderAppointmentForm();

    await fillAndSubmitForm("Ana Souza", "52998224725");

    expect(await screen.findByText("CPF já cadastrado de forma inválida")).toBeInTheDocument();
  });

  it("warns and reports the active appointment when status is 409", async () => {
    createAppointmentMock.mockRejectedValue(new ApiError({ status: 409, activeAppointmentId: 7 }));
    const { onActiveAppointmentFound } = renderAppointmentForm();

    await fillAndSubmitForm("Ana Souza", "52998224725");

    expect(await screen.findByRole("alert")).toHaveTextContent("já tem um atendimento em andamento");
    expect(onActiveAppointmentFound).toHaveBeenCalledWith(7);
  });

  it("shows a connection alert when the server is unreachable", async () => {
    createAppointmentMock.mockRejectedValue(new NetworkError());
    renderAppointmentForm();

    await fillAndSubmitForm("Ana Souza", "52998224725");

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível conectar ao servidor");
  });
});
