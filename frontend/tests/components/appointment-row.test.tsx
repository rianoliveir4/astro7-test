import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AppointmentRow } from "@/components/appointment-row";
import type { Appointment } from "@/types/appointment";

const pendingAppointment: Appointment = {
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

function renderAppointmentRow(appointment: Appointment) {
  const onStartNewAppointment = vi.fn();
  const onRemove = vi.fn();
  render(
    <AppointmentRow
      appointment={appointment}
      isHighlighted={false}
      onStartNewAppointment={onStartNewAppointment}
      onRemove={onRemove}
    />,
  );
  return { onStartNewAppointment, onRemove };
}

describe("AppointmentRow", () => {
  it("shows pending appointments with masked CPF and no actions", () => {
    renderAppointmentRow(pendingAppointment);

    expect(screen.getByText("Aguardando processamento")).toBeInTheDocument();
    expect(screen.getByText("***.982.247-**")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("shows the next attempt when a previous attempt failed", () => {
    renderAppointmentRow({ ...pendingAppointment, attemptCount: 2 });

    expect(screen.getByText("Aguardando processamento")).toBeInTheDocument();
    expect(screen.getByText("Tentativa 3")).toBeInTheDocument();
  });

  it("shows processing appointments", () => {
    renderAppointmentRow({ ...pendingAppointment, status: "PROCESSING" });

    expect(screen.getByText("Processando")).toBeInTheDocument();
  });

  it("shows the protocol and copies it when completed", async () => {
    const user = userEvent.setup();
    const writeTextMock = vi.spyOn(navigator.clipboard, "writeText");
    renderAppointmentRow({
      ...pendingAppointment,
      status: "COMPLETED",
      protocol: "8d51195e-06e3-4b18-b324-0358ddb7231b",
    });

    expect(screen.getByText("Concluído")).toBeInTheDocument();
    expect(screen.getByText("8d51195e-06e3-4b18-b324-0358ddb7231b")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Copiar protocolo" }));

    expect(writeTextMock).toHaveBeenCalledWith("8d51195e-06e3-4b18-b324-0358ddb7231b");
    expect(await screen.findByRole("button", { name: "Protocolo copiado" })).toBeInTheDocument();
  });

  it("offers a new appointment and removal when failed", async () => {
    const user = userEvent.setup();
    const { onStartNewAppointment, onRemove } = renderAppointmentRow({
      ...pendingAppointment,
      status: "FAILED",
      attemptCount: 5,
    });

    expect(screen.getByText("Falhou")).toBeInTheDocument();
    expect(screen.getByText("Não foi possível gerar o protocolo")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Novo atendimento" }));
    await user.click(screen.getByRole("button", { name: "Remover" }));

    expect(onStartNewAppointment).toHaveBeenCalledWith({
      patientName: "Ana Souza",
      patientCpf: "52998224725",
    });
    expect(onRemove).toHaveBeenCalledWith(1);
  });
});
