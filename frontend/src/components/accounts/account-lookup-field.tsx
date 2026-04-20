"use client";

import { forwardRef } from "react";
import { Loader2, AlertTriangle, CheckCircle2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { useAccountLookup } from "@/lib/hooks/use-accounts";
import { useDebounce } from "@/lib/hooks/use-debounce";

/**
 * Input số TK + tự lookup tên chủ TK hiển thị bên dưới (NAPAS/VietQR style).
 * Debounce 500ms để không spam API khi user gõ.
 * UX:
 * - < 10 ký tự: không fetch, không hiển thị.
 * - Đang fetch: loader spinner + "Đang tra cứu...".
 * - Thành công: tên + currency; badge warning nếu TK không ACTIVE.
 * - 404: "Số TK không tồn tại" inline màu đỏ.
 */
type Props = React.InputHTMLAttributes<HTMLInputElement> & {
  value: string;
};

export const AccountLookupField = forwardRef<HTMLInputElement, Props>(
  function AccountLookupField({ value, ...rest }, ref) {
    const debounced = useDebounce(value ?? "", 500);
    const { data, isFetching, isError } = useAccountLookup(debounced);

    const tooShort = debounced.length > 0 && debounced.length < 10;

    return (
      <div className="space-y-1">
        <Input ref={ref} value={value} {...rest} />
        <div className="min-h-[1.25rem] text-xs">
          {isFetching ? (
            <span className="inline-flex items-center gap-1 text-neutral-500">
              <Loader2 className="h-3 w-3 animate-spin" />
              Đang tra cứu...
            </span>
          ) : tooShort ? (
            <span className="text-neutral-400">Nhập tối thiểu 10 ký tự</span>
          ) : isError ? (
            <span className="inline-flex items-center gap-1 text-red-600">
              <AlertTriangle className="h-3 w-3" />
              Số TK không tồn tại
            </span>
          ) : data ? (
            <span className="inline-flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1 font-medium text-emerald-700">
                <CheckCircle2 className="h-3 w-3" />
                {data.holderName}
              </span>
              <span className="text-neutral-500">({data.currency})</span>
              {data.status !== "ACTIVE" ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-medium text-amber-700">
                  <AlertTriangle className="h-3 w-3" />
                  {data.status === "FROZEN" ? "TK đang bị khoá" : data.status}
                </span>
              ) : null}
            </span>
          ) : null}
        </div>
      </div>
    );
  },
);
