const CPF_LENGTH = 11;

export function extractDigits(value: string) {
  return value.replace(/\D/g, "");
}

export function formatCpf(value: string) {
  return extractDigits(value)
    .slice(0, CPF_LENGTH)
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

export function maskCpf(value: string) {
  const cpfDigits = extractDigits(value);
  return `***.${cpfDigits.slice(3, 6)}.${cpfDigits.slice(6, 9)}-**`;
}

export function isValidCpf(value: string) {
  const cpfDigits = extractDigits(value);

  if (cpfDigits.length !== CPF_LENGTH || /^(\d)\1+$/.test(cpfDigits)) {
    return false;
  }

  const firstCheckDigit = calculateCheckDigit(cpfDigits.slice(0, 9));
  const secondCheckDigit = calculateCheckDigit(cpfDigits.slice(0, 10));

  return firstCheckDigit === Number(cpfDigits[9]) && secondCheckDigit === Number(cpfDigits[10]);
}

function calculateCheckDigit(baseDigits: string) {
  const weightedSum = [...baseDigits].reduce(
    (sum, digit, position) => sum + Number(digit) * (baseDigits.length + 1 - position),
    0,
  );
  const remainder = (weightedSum * 10) % 11;
  return remainder === 10 ? 0 : remainder;
}
