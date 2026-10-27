"use client";

import { useCallback, useMemo, useSyncExternalStore } from "react";

const STORAGE_KEY = "tracked-appointment-ids";
const MAX_TRACKED_APPOINTMENTS = 50;
const EMPTY_STORED_VALUE = "[]";

const storageListeners = new Set<() => void>();

function subscribeToStorage(listener: () => void) {
  storageListeners.add(listener);
  window.addEventListener("storage", listener);
  return () => {
    storageListeners.delete(listener);
    window.removeEventListener("storage", listener);
  };
}

function readStoredValue() {
  try {
    return window.localStorage.getItem(STORAGE_KEY) ?? EMPTY_STORED_VALUE;
  } catch {
    return EMPTY_STORED_VALUE;
  }
}

function parseAppointmentIds(storedValue: string): number[] {
  try {
    const parsedValue: unknown = JSON.parse(storedValue);
    return Array.isArray(parsedValue) ? parsedValue.filter(Number.isInteger) : [];
  } catch {
    return [];
  }
}

function writeAppointmentIds(appointmentIds: number[]) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(appointmentIds));
  } catch {
    return;
  }
  storageListeners.forEach((listener) => listener());
}

export function useTrackedAppointmentIds() {
  const storedValue = useSyncExternalStore(subscribeToStorage, readStoredValue, () => EMPTY_STORED_VALUE);
  const trackedAppointmentIds = useMemo(() => parseAppointmentIds(storedValue), [storedValue]);

  const trackAppointment = useCallback((appointmentId: number) => {
    const currentIds = parseAppointmentIds(readStoredValue());
    const updatedIds = [appointmentId, ...currentIds.filter((trackedId) => trackedId !== appointmentId)];
    writeAppointmentIds(updatedIds.slice(0, MAX_TRACKED_APPOINTMENTS));
  }, []);

  const untrackAppointment = useCallback((appointmentId: number) => {
    const currentIds = parseAppointmentIds(readStoredValue());
    writeAppointmentIds(currentIds.filter((trackedId) => trackedId !== appointmentId));
  }, []);

  return { trackedAppointmentIds, trackAppointment, untrackAppointment };
}
