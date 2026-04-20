"use client";

import { useMemo } from "react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAccounts } from "@/lib/hooks/use-accounts";
import { ACCOUNT_TYPE_LABEL } from "@/lib/enums";
import { formatMoney } from "@/lib/utils/format";

/**
 * Dropdown chọn TK của customer đang đăng nhập.
 * BE tự scope theo chủ sở hữu (xem my/accounts page).
 * Loại CLOSED khỏi danh sách — TK đã đóng không còn dùng để giao dịch.
 */
export function OwnedAccountSelect({
  value,
  onChange,
  placeholder = "Chọn tài khoản",
  disabled,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  disabled?: boolean;
}) {
  const { data, isLoading, isError } = useAccounts({ size: 50 });

  const options = useMemo(
    () => (data?.content ?? []).filter((a) => a.status !== "CLOSED"),
    [data],
  );

  const byNumber = useMemo(
    () => new Map(options.map((a) => [a.accountNumber, a])),
    [options],
  );

  const trigger = (
    <SelectTrigger className="w-full" disabled={disabled || isLoading}>
      <SelectValue>
        {(v) => {
          if (!v) return placeholder;
          const a = byNumber.get(v as string);
          if (!a) return v as string;
          return `${a.accountNumber} — ${ACCOUNT_TYPE_LABEL[a.accountType]}`;
        }}
      </SelectValue>
    </SelectTrigger>
  );

  return (
    <div className="space-y-1">
      <Select
        value={value || ""}
        onValueChange={(v) => onChange(v ?? "")}
        disabled={disabled}
      >
        {trigger}
        <SelectContent>
          {options.map((a) => (
            <SelectItem key={a.id} value={a.accountNumber}>
              <span className="font-mono text-xs">{a.accountNumber}</span>
              <span className="text-neutral-500">
                {ACCOUNT_TYPE_LABEL[a.accountType]}
              </span>
              <span className="ml-auto text-xs text-neutral-600">
                {formatMoney(a.balance, a.currency)}
              </span>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <div className="min-h-[1.25rem] text-xs">
        {isLoading ? (
          <span className="text-neutral-400">Đang tải tài khoản...</span>
        ) : isError ? (
          <span className="text-red-600">Không tải được tài khoản</span>
        ) : options.length === 0 ? (
          <span className="text-neutral-400">Chưa có TK khả dụng</span>
        ) : null}
      </div>
    </div>
  );
}
