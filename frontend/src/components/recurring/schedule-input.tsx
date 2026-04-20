"use client";

import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { Schedule, Frequency } from "@/lib/utils/cron";

const FREQ_LABEL: Record<Frequency, string> = {
  DAILY: "Hàng ngày",
  WEEKLY: "Hàng tuần",
  MONTHLY: "Hàng tháng",
};

const DOW_LABEL: Record<number, string> = {
  1: "Thứ hai",
  2: "Thứ ba",
  3: "Thứ tư",
  4: "Thứ năm",
  5: "Thứ sáu",
  6: "Thứ bảy",
  7: "Chủ nhật",
};

/**
 * Form control structured cho recurring schedule. User chọn:
 * - Tần suất (DAILY/WEEKLY/MONTHLY)
 * - Thứ (chỉ WEEKLY) / Ngày trong tháng (chỉ MONTHLY, 1-28)
 * - Giờ HH:MM
 * Component parent chịu trách nhiệm compose cron string trước khi submit.
 */
export function ScheduleInput({
  value,
  onChange,
}: {
  value: Schedule;
  onChange: (next: Schedule) => void;
}) {
  const set = <K extends keyof Schedule>(k: K, v: Schedule[K]) =>
    onChange({ ...value, [k]: v });

  return (
    <div className="grid gap-3 md:grid-cols-2">
      <div className="space-y-1.5">
        <Label>Tần suất</Label>
        <Select
          value={value.frequency}
          onValueChange={(v) => {
            const f = v as Frequency;
            onChange({
              ...value,
              frequency: f,
              dayOfWeek: f === "WEEKLY" ? (value.dayOfWeek ?? 1) : undefined,
              dayOfMonth: f === "MONTHLY" ? (value.dayOfMonth ?? 1) : undefined,
            });
          }}
        >
          <SelectTrigger>
            <SelectValue>
              {(v) => (v ? FREQ_LABEL[v as Frequency] : "Chọn")}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {(Object.keys(FREQ_LABEL) as Frequency[]).map((f) => (
              <SelectItem key={f} value={f}>
                {FREQ_LABEL[f]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {value.frequency === "WEEKLY" ? (
        <div className="space-y-1.5">
          <Label>Vào thứ</Label>
          <Select
            value={String(value.dayOfWeek ?? 1)}
            onValueChange={(v) =>
              set("dayOfWeek", Number(v) as Schedule["dayOfWeek"])
            }
          >
            <SelectTrigger>
              <SelectValue>
                {(v) => (v ? DOW_LABEL[Number(v)] : "Chọn")}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {[1, 2, 3, 4, 5, 6, 7].map((d) => (
                <SelectItem key={d} value={String(d)}>
                  {DOW_LABEL[d]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      ) : null}

      {value.frequency === "MONTHLY" ? (
        <div className="space-y-1.5">
          <Label>Vào ngày</Label>
          <Select
            value={String(value.dayOfMonth ?? 1)}
            onValueChange={(v) => set("dayOfMonth", Number(v))}
          >
            <SelectTrigger>
              <SelectValue>
                {(v) => (v ? `Ngày ${v}` : "Chọn")}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {Array.from({ length: 28 }, (_, i) => i + 1).map((d) => (
                <SelectItem key={d} value={String(d)}>
                  Ngày {d}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-[10px] text-neutral-400">
            Giới hạn 1-28 để mọi tháng đều chạy đúng ngày.
          </p>
        </div>
      ) : null}

      <div className="space-y-1.5">
        <Label>Giờ chạy</Label>
        <div className="flex items-center gap-2">
          <Input
            type="number"
            min={0}
            max={23}
            className="w-20"
            value={value.hour}
            onChange={(e) =>
              set("hour", clamp(Number(e.target.value), 0, 23))
            }
          />
          <span className="text-neutral-500">:</span>
          <Input
            type="number"
            min={0}
            max={59}
            className="w-20"
            value={value.minute}
            onChange={(e) =>
              set("minute", clamp(Number(e.target.value), 0, 59))
            }
          />
        </div>
      </div>
    </div>
  );
}

function clamp(n: number, min: number, max: number) {
  if (Number.isNaN(n)) return min;
  return Math.max(min, Math.min(max, n));
}
