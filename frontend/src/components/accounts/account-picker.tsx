"use client";

import { useAccounts } from "@/lib/hooks/use-accounts";
import { ACCOUNT_TYPE_LABEL } from "@/lib/enums";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const ALL_VALUE = "__all__";

export function AccountPicker({
  value,
  onChange,
  allowAll = false,
  allLabel = "Tất cả tài khoản",
  placeholder = "Chọn tài khoản",
  className,
}: {
  value: string | undefined;
  onChange: (id: string | undefined) => void;
  allowAll?: boolean;
  allLabel?: string;
  placeholder?: string;
  className?: string;
}) {
  const { data, isLoading } = useAccounts({ size: 100 });
  const accounts = data?.content ?? [];

  const current = value ?? (allowAll ? ALL_VALUE : "");

  return (
    <Select
      value={current}
      onValueChange={(v) =>
        onChange(!v || v === ALL_VALUE ? undefined : v)
      }
      disabled={isLoading}
    >
      <SelectTrigger className={className ?? "w-full"}>
        <SelectValue placeholder={placeholder}>
          {(v) => {
            if (!v || v === ALL_VALUE) return allowAll ? allLabel : placeholder;
            const acc = accounts.find((a) => a.id === v);
            return acc
              ? `${acc.accountNumber} · ${ACCOUNT_TYPE_LABEL[acc.accountType]}`
              : placeholder;
          }}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {allowAll ? <SelectItem value={ALL_VALUE}>{allLabel}</SelectItem> : null}
        {accounts.map((a) => (
          <SelectItem key={a.id} value={a.id}>
            {a.accountNumber} · {ACCOUNT_TYPE_LABEL[a.accountType]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
