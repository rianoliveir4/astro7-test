"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { CircleAlert, LoaderCircle, TriangleAlert } from "lucide-react";
import { useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ApiError, NetworkError, createAppointment } from "@/lib/api";
import { formatCpf, isValidCpf } from "@/lib/cpf";
import type { Appointment, PatientData } from "@/types/appointment";

const appointmentFormSchema = z.object({
  patientName: z
    .string()
    .trim()
    .min(1, "Informe o nome")
    .min(3, "Nome muito curto")
    .max(150, "Nome muito longo"),
  patientCpf: z.string().min(1, "Informe o CPF").refine(isValidCpf, "CPF inválido"),
});

type AppointmentFormValues = z.infer<typeof appointmentFormSchema>;

const appointmentFormFields = ["patientName", "patientCpf"] as const;

type FormAlert = {
  tone: "warning" | "error";
  message: string;
};

type AppointmentFormProps = {
  defaultPatient: PatientData;
  onAppointmentCreated: (createdAppointment: Appointment) => void;
  onActiveAppointmentFound: (activeAppointmentId: number) => void;
};

export function AppointmentForm({
  defaultPatient,
  onAppointmentCreated,
  onActiveAppointmentFound,
}: AppointmentFormProps) {
  const [formAlert, setFormAlert] = useState<FormAlert | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<AppointmentFormValues>({
    resolver: zodResolver(appointmentFormSchema),
    defaultValues: {
      patientName: defaultPatient.patientName,
      patientCpf: formatCpf(defaultPatient.patientCpf),
    },
  });

  const patientCpfField = register("patientCpf");

  async function submitAppointment(formValues: AppointmentFormValues) {
    setFormAlert(null);
    try {
      const createdAppointment = await createAppointment(formValues);
      onAppointmentCreated(createdAppointment);
      reset({ patientName: "", patientCpf: "" });
    } catch (submissionError) {
      handleSubmissionError(submissionError);
    }
  }

  function handleSubmissionError(submissionError: unknown) {
    if (submissionError instanceof ApiError) {
      const { activeAppointmentId, errors: fieldErrors } = submissionError.problemDetail;

      if (submissionError.status === 409 && activeAppointmentId) {
        onActiveAppointmentFound(activeAppointmentId);
        setFormAlert({ tone: "warning", message: "Este paciente já tem um atendimento em andamento." });
        return;
      }

      if (submissionError.status === 400 && fieldErrors && applyFieldErrors(fieldErrors)) {
        return;
      }
    }

    setFormAlert({
      tone: "error",
      message:
        submissionError instanceof NetworkError
          ? "Não foi possível conectar ao servidor."
          : "Não foi possível abrir o atendimento. Tente novamente.",
    });
  }

  function applyFieldErrors(fieldErrors: Record<string, string>) {
    const fieldsWithErrors = appointmentFormFields.filter((fieldName) => fieldErrors[fieldName]);
    fieldsWithErrors.forEach((fieldName) => setError(fieldName, { message: fieldErrors[fieldName] }));
    return fieldsWithErrors.length > 0;
  }

  return (
    <form
      onSubmit={handleSubmit(submitAppointment)}
      noValidate
      aria-labelledby="new-appointment-title"
      className="rounded-lg border border-zinc-200 bg-white p-5"
    >
      <div className="mb-4">
        <h2 id="new-appointment-title" className="text-sm font-semibold text-zinc-900">
          Novo atendimento
        </h2>
        <p className="mt-0.5 text-sm text-zinc-500">Informe os dados do paciente para gerar o protocolo.</p>
      </div>

      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_13rem_auto] md:items-start">
        <FormField label="Nome completo" fieldId="patientName" errorMessage={errors.patientName?.message}>
          <input
            id="patientName"
            type="text"
            autoComplete="name"
            aria-invalid={Boolean(errors.patientName)}
            aria-describedby={errors.patientName ? "patientName-error" : undefined}
            className={inputClassName(Boolean(errors.patientName))}
            {...register("patientName")}
          />
        </FormField>

        <FormField label="CPF" fieldId="patientCpf" errorMessage={errors.patientCpf?.message}>
          <input
            id="patientCpf"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            placeholder="000.000.000-00"
            maxLength={14}
            aria-invalid={Boolean(errors.patientCpf)}
            aria-describedby={errors.patientCpf ? "patientCpf-error" : undefined}
            className={`${inputClassName(Boolean(errors.patientCpf))} tabular-nums`}
            {...patientCpfField}
            onChange={(changeEvent) => {
              changeEvent.target.value = formatCpf(changeEvent.target.value);
              return patientCpfField.onChange(changeEvent);
            }}
          />
        </FormField>

        <button
          type="submit"
          disabled={isSubmitting}
          className="inline-flex h-9 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-medium text-white shadow-sm transition-colors hover:bg-teal-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:cursor-not-allowed disabled:opacity-70 md:mt-6"
        >
          {isSubmitting && <LoaderCircle className="size-4 animate-spin" aria-hidden />}
          {isSubmitting ? "Abrindo…" : "Abrir atendimento"}
        </button>
      </div>

      {formAlert && <FormAlertMessage formAlert={formAlert} />}
    </form>
  );
}

type FormFieldProps = {
  label: string;
  fieldId: string;
  errorMessage?: string;
  children: ReactNode;
};

function FormField({ label, fieldId, errorMessage, children }: FormFieldProps) {
  return (
    <div>
      <label htmlFor={fieldId} className="mb-1 block text-sm font-medium text-zinc-700">
        {label}
      </label>
      {children}
      {errorMessage && (
        <p id={`${fieldId}-error`} className="mt-1 text-xs text-red-700">
          {errorMessage}
        </p>
      )}
    </div>
  );
}

function FormAlertMessage({ formAlert }: { formAlert: FormAlert }) {
  const isWarning = formAlert.tone === "warning";
  const AlertIcon = isWarning ? TriangleAlert : CircleAlert;

  return (
    <p
      role="alert"
      className={`mt-4 flex items-center gap-2 rounded-md px-3 py-2 text-sm ${
        isWarning ? "bg-amber-50 text-amber-800" : "bg-red-50 text-red-800"
      }`}
    >
      <AlertIcon className="size-4 shrink-0" aria-hidden />
      {formAlert.message}
    </p>
  );
}

function inputClassName(hasError: boolean) {
  const baseClassName =
    "block h-9 w-full rounded-md border bg-white px-3 text-sm text-zinc-900 outline-none transition-colors placeholder:text-zinc-400 focus:ring-2";
  return hasError
    ? `${baseClassName} border-red-400 focus:border-red-500 focus:ring-red-500/20`
    : `${baseClassName} border-zinc-300 focus:border-teal-600 focus:ring-teal-600/15`;
}
