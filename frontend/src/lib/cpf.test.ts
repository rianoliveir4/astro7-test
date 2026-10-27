import { describe, expect, it } from "vitest";
import { formatCpf, isValidCpf, maskCpf } from "./cpf";

describe("formatCpf", () => {
  it("formats a complete CPF", () => {
    expect(formatCpf("52998224725")).toBe("529.982.247-25");
  });

  it("formats a partially typed CPF", () => {
    expect(formatCpf("5299")).toBe("529.9");
    expect(formatCpf("5299822")).toBe("529.982.2");
    expect(formatCpf("5299822472")).toBe("529.982.247-2");
  });

  it("ignores non-digit characters and extra digits", () => {
    expect(formatCpf("529.982.247-25999")).toBe("529.982.247-25");
    expect(formatCpf("abc529")).toBe("529");
  });
});

describe("isValidCpf", () => {
  it("accepts valid CPFs with or without formatting", () => {
    expect(isValidCpf("529.982.247-25")).toBe(true);
    expect(isValidCpf("11144477735")).toBe(true);
  });

  it("rejects CPFs with wrong check digits", () => {
    expect(isValidCpf("529.982.247-26")).toBe(false);
    expect(isValidCpf("111.222.333-44")).toBe(false);
  });

  it("rejects CPFs with all digits equal", () => {
    expect(isValidCpf("111.111.111-11")).toBe(false);
  });

  it("rejects CPFs with wrong length", () => {
    expect(isValidCpf("")).toBe(false);
    expect(isValidCpf("529.982.247")).toBe(false);
  });
});

describe("maskCpf", () => {
  it("hides the first and last digits", () => {
    expect(maskCpf("52998224725")).toBe("***.982.247-**");
  });
});
