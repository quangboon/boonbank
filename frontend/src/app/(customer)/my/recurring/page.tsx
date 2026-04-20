"use client";

import { useState } from "react";
import { Pause, Pencil, Play, Plus, Trash2 } from "lucide-react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import {
  useCreateRecurring,
  useDeleteRecurring,
  useRecurringList,
  useToggleRecurring,
  useUpdateRecurring,
} from "@/lib/hooks/use-recurring";
import { formatDate, formatMoney } from "@/lib/utils/format";
import {
  composeCron,
  humanizeCron,
  parseCron,
  DEFAULT_SCHEDULE,
  type Schedule,
} from "@/lib/utils/cron";
import type { RecurringTransaction } from "@/lib/api/recurring";
import { ScheduleInput } from "@/components/recurring/schedule-input";
import { AccountLookupField } from "@/components/accounts/account-lookup-field";
import { OwnedAccountSelect } from "@/components/accounts/owned-account-select";
import { AmountInput } from "@/components/transactions/amount-input";

const schema = z.object({
  sourceAccountNumber: z.string().min(4),
  destinationAccountNumber: z.string().min(4),
  amount: z.string().refine((v) => Number(v) > 0, "Phải > 0"),
});

type Values = z.infer<typeof schema>;

export default function MyRecurringPage() {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<RecurringTransaction | null>(null);
  const [customCronWarning, setCustomCronWarning] = useState<string | null>(null);
  const [schedule, setSchedule] = useState<Schedule>(DEFAULT_SCHEDULE);
  const { data, isLoading, isError } = useRecurringList({ size: 50 });
  const create = useCreateRecurring();
  const update = useUpdateRecurring();
  const toggle = useToggleRecurring();
  const del = useDeleteRecurring();

  const {
    control,
    register,
    watch,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      sourceAccountNumber: "",
      destinationAccountNumber: "",
      amount: "",
    },
  });

  const rows = data?.content ?? [];
  const isEdit = editing !== null;

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
    setCustomCronWarning(null);
    setSchedule(DEFAULT_SCHEDULE);
    reset();
  };

  const startEdit = (r: RecurringTransaction) => {
    const parsed = parseCron(r.cronExpression);
    setEditing(r);
    setShowForm(true);
    reset({
      sourceAccountNumber: r.sourceAccountNumber,
      destinationAccountNumber: r.destinationAccountNumber,
      amount: String(r.amount),
    });
    if (parsed) {
      setSchedule(parsed);
      setCustomCronWarning(null);
    } else {
      setSchedule(DEFAULT_SCHEDULE);
      setCustomCronWarning(
        `Lịch hiện tại là "${r.cronExpression}" — dạng tuỳ chỉnh, không biểu diễn được trong form. Lưu sẽ ghi đè bằng lịch mới.`,
      );
    }
  };

  const onSubmit = (v: Values) => {
    if (isEdit && editing) {
      update.mutate(
        {
          id: editing.id,
          req: {
            amount: v.amount,
            cronExpression: composeCron(schedule),
          },
        },
        { onSuccess: closeForm },
      );
    } else {
      create.mutate(
        { ...v, cronExpression: composeCron(schedule) },
        { onSuccess: closeForm },
      );
    }
  };

  const submitting = create.isPending || update.isPending;

  return (
    <div className="space-y-4">
      <PageHeader
        title="Giao dịch định kỳ"
        actions={
          <Button
            onClick={() => (showForm ? closeForm() : setShowForm(true))}
            className="gap-1"
          >
            <Plus className="h-4 w-4" /> {showForm ? "Đóng" : "Thêm mới"}
          </Button>
        }
      />

      {showForm ? (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">
              {isEdit ? "Chỉnh sửa lịch định kỳ" : "Thiết lập mới"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form
              onSubmit={handleSubmit(onSubmit)}
              className="grid gap-4 md:grid-cols-2"
              noValidate
            >
              <div className="space-y-1.5">
                <Label>Nguồn {isEdit ? "(không đổi được)" : ""}</Label>
                {isEdit ? (
                  <Input
                    value={watch("sourceAccountNumber")}
                    disabled
                    className="font-mono text-xs"
                  />
                ) : (
                  <Controller
                    control={control}
                    name="sourceAccountNumber"
                    render={({ field }) => (
                      <OwnedAccountSelect
                        value={field.value}
                        onChange={field.onChange}
                      />
                    )}
                  />
                )}
                {errors.sourceAccountNumber && (
                  <p className="text-xs text-red-600">
                    {errors.sourceAccountNumber.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Đích {isEdit ? "(không đổi được)" : ""}</Label>
                {isEdit ? (
                  <Input
                    value={watch("destinationAccountNumber")}
                    disabled
                    className="font-mono text-xs"
                  />
                ) : (
                  <AccountLookupField
                    value={watch("destinationAccountNumber")}
                    {...register("destinationAccountNumber")}
                  />
                )}
                {errors.destinationAccountNumber && (
                  <p className="text-xs text-red-600">
                    {errors.destinationAccountNumber.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Số tiền (VND)</Label>
                <Controller
                  control={control}
                  name="amount"
                  render={({ field }) => (
                    <AmountInput
                      value={field.value}
                      onChange={field.onChange}
                    />
                  )}
                />
                {errors.amount && (
                  <p className="text-xs text-red-600">{errors.amount.message}</p>
                )}
              </div>
              <div className="md:col-span-2 space-y-1.5">
                <Label>Lịch chạy</Label>
                {customCronWarning ? (
                  <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700">
                    {customCronWarning}
                  </p>
                ) : null}
                <ScheduleInput value={schedule} onChange={setSchedule} />
              </div>
              <div className="md:col-span-2 flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={closeForm}>
                  Huỷ
                </Button>
                <Button type="submit" disabled={submitting}>
                  {submitting
                    ? "Đang lưu..."
                    : isEdit
                      ? "Lưu thay đổi"
                      : "Tạo"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      ) : null}

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState />
      ) : rows.length === 0 ? (
        <EmptyState
          title="Chưa có giao dịch định kỳ"
          description="Bấm 'Thêm mới' để thiết lập."
        />
      ) : (
        <div className="rounded-lg border border-neutral-200 bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nguồn → Đích</TableHead>
                <TableHead className="text-right">Số tiền</TableHead>
                <TableHead>Lịch chạy</TableHead>
                <TableHead>Chạy tiếp</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-mono text-xs">
                    {r.sourceAccountNumber} → {r.destinationAccountNumber}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatMoney(r.amount)}
                  </TableCell>
                  <TableCell className="text-xs">
                    {humanizeCron(r.cronExpression)}
                  </TableCell>
                  <TableCell className="text-xs">
                    {formatDate(r.nextRunAt ?? undefined)}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="secondary"
                      className={
                        r.enabled
                          ? "border border-green-200 bg-green-100 text-green-700 text-xs"
                          : "border border-neutral-200 bg-neutral-100 text-neutral-700 text-xs"
                      }
                    >
                      {r.enabled ? "Đang chạy" : "Tạm dừng"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => startEdit(r)}
                        aria-label="Sửa"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() =>
                          toggle.mutate({ id: r.id, enabled: !r.enabled })
                        }
                        aria-label={r.enabled ? "Tạm dừng" : "Kích hoạt"}
                      >
                        {r.enabled ? (
                          <Pause className="h-3.5 w-3.5" />
                        ) : (
                          <Play className="h-3.5 w-3.5" />
                        )}
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-red-600"
                        onClick={() => del.mutate(r.id)}
                        aria-label="Xoá"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
