const moneyFmt = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 });
const dateFmt = new Intl.DateTimeFormat("vi-VN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});
const dateOnlyFmt = new Intl.DateTimeFormat("vi-VN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

export function formatMoney(value: string | number | undefined, currency = "VND") {
  if (value === undefined || value === null || value === "") return "—";
  const n = typeof value === "string" ? Number(value) : value;
  if (Number.isNaN(n)) return String(value);
  return `${moneyFmt.format(n)} ${currency}`;
}

export function formatDate(iso: string | undefined) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return dateFmt.format(d);
}

export function formatDateOnly(iso: string | undefined) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return dateOnlyFmt.format(d);
}

export function parseAmountInput(raw: string): string | null {
  const cleaned = raw.replace(/[.\s]/g, "").replace(/,/g, ".");
  if (cleaned === "") return null;
  const n = Number(cleaned);
  if (Number.isNaN(n) || n < 0) return null;
  return cleaned;
}
