"use client";

import { forwardRef } from "react";
import { Input } from "@/components/ui/input";

type Props = {
  value: string;
  onChange: (raw: string) => void;
  placeholder?: string;
  disabled?: boolean;
  id?: string;
};

const fmt = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 });

function toDisplay(raw: string) {
  if (!raw) return "";
  const n = Number(raw);
  if (Number.isNaN(n)) return raw;
  return fmt.format(n);
}

function fromDisplay(raw: string): string {
  const cleaned = raw.replace(/[.\s]/g, "").replace(/,/g, ".");
  if (cleaned === "") return "";
  const n = Number(cleaned);
  if (Number.isNaN(n)) return "";
  return cleaned;
}

export const AmountInput = forwardRef<HTMLInputElement, Props>(function AmountInput(
  { value, onChange, placeholder, disabled, id },
  ref,
) {
  return (
    <Input
      ref={ref}
      id={id}
      inputMode="decimal"
      disabled={disabled}
      placeholder={placeholder ?? "0"}
      value={toDisplay(value)}
      onChange={(e) => onChange(fromDisplay(e.target.value))}
    />
  );
});
