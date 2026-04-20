"use client";

import { useForm, type UseFormSetError } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect } from "react";
import type { Customer } from "@/types/domain";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import { CUSTOMER_TYPES, CUSTOMER_TYPE_LABEL } from "@/lib/enums";

const schema = z.object({
  fullName: z.string().min(2).max(200),
  idNumber: z.string().min(5).max(30),
  email: z.email().max(150),
  phone: z.string().min(5).max(20),
  address: z.string().max(255).optional().or(z.literal("")),
  location: z.string().max(100).optional().or(z.literal("")),
  dateOfBirth: z.string().optional().or(z.literal("")),
  customerTypeCode: z.string().optional().or(z.literal("")),
});

export type CustomerFormValues = z.infer<typeof schema>;

export type CustomerFormHelpers = {
  setError: UseFormSetError<CustomerFormValues>;
};

const EMPTY: CustomerFormValues = {
  fullName: "",
  idNumber: "",
  email: "",
  phone: "",
  address: "",
  location: "",
  dateOfBirth: "",
  customerTypeCode: "",
};

export function CustomerForm({
  defaults,
  submitting,
  submitLabel,
  onCancel,
  onSubmit,
  isEdit = false,
}: {
  defaults?: Customer | null;
  submitting?: boolean;
  submitLabel: string;
  onCancel?: () => void;
  onSubmit: (v: CustomerFormValues, helpers: CustomerFormHelpers) => void;
  isEdit?: boolean;
}) {
  const form = useForm<CustomerFormValues>({
    resolver: zodResolver(schema),
    defaultValues: EMPTY,
  });
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = form;

  useEffect(() => {
    if (defaults) {
      reset({
        fullName: defaults.fullName ?? "",
        idNumber: defaults.idNumber ?? "",
        email: defaults.email ?? "",
        phone: defaults.phone ?? "",
        address: defaults.address ?? "",
        location: defaults.location ?? "",
        dateOfBirth: defaults.dateOfBirth ?? "",
        customerTypeCode: defaults.customerTypeCode ?? "",
      });
    }
  }, [defaults, reset]);

  return (
    <form
      onSubmit={handleSubmit((v) => onSubmit(v, { setError }))}
      className="grid gap-4 md:grid-cols-2"
      noValidate
    >
      <Field label="Họ và tên" error={errors.fullName?.message}>
        <Input {...register("fullName")} />
      </Field>
      <Field label="CCCD/CMND" error={errors.idNumber?.message}>
        <Input {...register("idNumber")} disabled={isEdit} />
      </Field>
      <Field label="Email" error={errors.email?.message}>
        <Input type="email" {...register("email")} />
      </Field>
      <Field label="Số điện thoại" error={errors.phone?.message}>
        <Input {...register("phone")} />
      </Field>
      <Field label="Địa chỉ" error={errors.address?.message}>
        <Input {...register("address")} />
      </Field>
      <Field label="Thành phố / Địa điểm" error={errors.location?.message}>
        <Input {...register("location")} />
      </Field>
      <Field label="Ngày sinh" error={errors.dateOfBirth?.message}>
        <Input type="date" {...register("dateOfBirth")} disabled={isEdit} />
      </Field>
      <Field label="Loại khách hàng" error={errors.customerTypeCode?.message}>
        <Select
          value={watch("customerTypeCode") ?? ""}
          onValueChange={(v) => {
            if (v) setValue("customerTypeCode", v, { shouldValidate: true });
          }}
        >
          <SelectTrigger>
            <SelectValue placeholder="Chọn loại">
              {(v) =>
                v ? (CUSTOMER_TYPE_LABEL[v as string] ?? String(v)) : "Chọn loại"
              }
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {CUSTOMER_TYPES.map((t) => (
              <SelectItem key={t.code} value={t.code}>
                {t.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>

      <div className="md:col-span-2 flex justify-end gap-2 pt-2">
        {onCancel ? (
          <Button type="button" variant="outline" onClick={onCancel}>
            Huỷ
          </Button>
        ) : null}
        <Button type="submit" disabled={submitting}>
          {submitting ? "Đang lưu..." : submitLabel}
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
