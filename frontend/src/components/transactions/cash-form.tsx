"use client";

import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { AmountInput } from "./amount-input";
import { AccountLookupField } from "@/components/accounts/account-lookup-field";
import { OwnedAccountSelect } from "@/components/accounts/owned-account-select";
import { useDeposit, useWithdraw } from "@/lib/hooks/use-transactions";
import { newIdempotencyKey } from "@/lib/utils/idempotency";

const schema = z.object({
  accountNumber: z.string().min(4),
  amount: z.string().refine((v) => Number(v) > 0, "Phải > 0"),
  location: z.string().max(128).optional().or(z.literal("")),
  description: z.string().max(500).optional().or(z.literal("")),
});

type Values = z.infer<typeof schema>;

export function CashForm({
  kind,
  defaultAccount,
  onSuccess,
  ownedOnly,
}: {
  kind: "withdraw" | "deposit";
  defaultAccount?: string;
  onSuccess?: () => void;
  ownedOnly?: boolean;
}) {
  const withdraw = useWithdraw();
  const deposit = useDeposit();
  const mut = kind === "withdraw" ? withdraw : deposit;
  const submitLabel = kind === "withdraw" ? "Rút tiền" : "Nạp tiền";
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
      accountNumber: defaultAccount ?? "",
      amount: "",
      location: "",
      description: "",
    },
  });

  const onSubmit = (v: Values) => {
    // Gen idempotency key PER CLICK. Retry auto (nếu có) dùng lại cùng key để
    // chống double-spend khi network timeout; click mới của user = attempt mới.
    const key = newIdempotencyKey();
    const body = {
      accountNumber: v.accountNumber,
      amount: v.amount,
      location: v.location || undefined,
      description: v.description || undefined,
    };
    mut.mutate(
      { req: body, key },
      {
        onSuccess: () => {
          reset({
            accountNumber: v.accountNumber,
            amount: "",
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
      <Field
        label="Số tài khoản"
        error={errors.accountNumber?.message}
      >
        {ownedOnly ? (
          <Controller
            control={control}
            name="accountNumber"
            render={({ field }) => (
              <OwnedAccountSelect
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
        ) : (
          <AccountLookupField
            value={watch("accountNumber")}
            {...register("accountNumber")}
          />
        )}
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
      <Field label="Địa điểm" error={errors.location?.message}>
        <Input {...register("location")} />
      </Field>
      <Field label="Ghi chú" error={errors.description?.message}>
        <Input {...register("description")} />
      </Field>
      <div className="md:col-span-2 flex justify-end gap-2 pt-2">
        <Button type="submit" disabled={mut.isPending}>
          {mut.isPending ? "Đang xử lý..." : submitLabel}
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
