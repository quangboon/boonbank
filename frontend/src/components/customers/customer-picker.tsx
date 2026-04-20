"use client";

import { useCustomers } from "@/lib/hooks/use-customers";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function CustomerPicker({
  value,
  onChange,
  placeholder = "Chọn khách hàng",
  className,
}: {
  value: string | undefined;
  onChange: (id: string) => void;
  placeholder?: string;
  className?: string;
}) {
  const { data, isLoading } = useCustomers({ size: 100 });
  const customers = data?.content ?? [];

  return (
    <Select
      value={value ?? ""}
      onValueChange={(v) => {
        if (v) onChange(v);
      }}
      disabled={isLoading}
    >
      <SelectTrigger className={className ?? "w-full"}>
        <SelectValue placeholder={placeholder}>
          {(v) => {
            if (!v) return placeholder;
            const c = customers.find((x) => x.id === v);
            return c ? `${c.fullName} · ${c.customerCode}` : placeholder;
          }}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {customers.map((c) => (
          <SelectItem key={c.id} value={c.id}>
            {c.fullName} · {c.customerCode}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
