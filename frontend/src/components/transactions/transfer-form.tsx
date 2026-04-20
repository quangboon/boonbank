"use client";

import { useMemo } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { AmountInput } from "./amount-input";
import { AccountLookupField } from "@/components/accounts/account-lookup-field";
import { OwnedAccountSelect } from "@/components/accounts/owned-account-select";
import { useTransfer } from "@/lib/hooks/use-transactions";
import { newIdempotencyKey } from "@/lib/utils/idempotency";

const schema = z
  .object({
    sourceAccountNumber: z.string().min(4, "Bắt buộc"),
    destinationAccountNumber: z.string().min(4, "Bắt buộc"),
    amount: z.string().refine((v) => Number(v) > 0, "Phải > 0"),
    currency: z.string().length(3, "3 ký tự"),
    location: z.string().max(128).optional().or(z.literal("")),
    description: z.string().max(500).optional().or(z.literal("")),
  })
  .refine((v) => v.sourceAccountNumber !== v.destinationAccountNumber, {
    path: ["destinationAccountNumber"],
    message: "Nguồn và đích phải khác nhau",
  });

type Values = z.infer<typeof schema>;

export function TransferForm({
  defaultSource,
  onSuccess,
  ownedOnly,
}: {
  defaultSource?: string;
  onSuccess?: () => void;
  ownedOnly?: boolean;
}) {
  const transfer = useTransfer();
  const {
    control,
    register,
    watch,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      sourceAccountNumber: defaultSource ?? "",
      destinationAccountNumber: "",
      amount: "",
      currency: "VND",
      location: "",
      description: "",
    },
  });

  const disabled = useMemo(() => transfer.isPending, [transfer.isPending]);

  const onSubmit = (v: Values) => {
    // Gen idempotency key PER CLICK. Retry auto (nếu có) dùng lại cùng key để
    // chống double-spend khi network timeout; click mới của user = attempt mới.
    const key = newIdempotencyKey();
    transfer.mutate(
      {
        req: {
          sourceAccountNumber: v.sourceAccountNumber,
          destinationAccountNumber: v.destinationAccountNumber,
          amount: v.amount,
          currency: v.currency,
          location: v.location || undefined,
          description: v.description || undefined,
        },
        key,
      },
      {
        onSuccess: () => {
          reset({
            sourceAccountNumber: v.sourceAccountNumber,
            destinationAccountNumber: "",
            amount: "",
            currency: v.currency,
            location: "",
            description: "",
          });
          onSuccess?.();
        },
      },
    );
  };

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="grid gap-4 md:grid-cols-2"
      noValidate
    >
      <Field label="Tài khoản nguồn" error={errors.sourceAccountNumber?.message}>
        {ownedOnly ? (
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
        ) : (
          <AccountLookupField
            value={watch("sourceAccountNumber")}
            {...register("sourceAccountNumber")}
          />
        )}
      </Field>
      <Field label="Tài khoản đích" error={errors.destinationAccountNumber?.message}>
        <AccountLookupField
          value={watch("destinationAccountNumber")}
          {...register("destinationAccountNumber")}
        />
      </Field>
      <Field label="Số tiền" error={errors.amount?.message}>
        <Controller
          control={control}
          name="amount"
          render={({ field }) => (
            <AmountInput value={field.value} onChange={field.onChange} />
          )}
        />
      </Field>
      <Field label="Tiền tệ" error={errors.currency?.message}>
        <Input {...register("currency")} />
      </Field>
      <Field label="Địa điểm" error={errors.location?.message}>
        <Input {...register("location")} placeholder="HCM / HN / ..." />
      </Field>
      <Field label="Ghi chú" error={errors.description?.message}>
        <Input {...register("description")} />
      </Field>
      <div className="md:col-span-2 flex justify-end gap-2 pt-2">
        <Button type="submit" disabled={disabled}>
          {disabled ? "Đang xử lý..." : "Chuyển khoản"}
        </Button>
      </div>
    </form>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
      {error ? <p className="text-xs text-red-600">{error}</p> : null}
    </div>
  );
}
