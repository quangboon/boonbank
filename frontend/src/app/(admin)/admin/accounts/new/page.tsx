"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import { CustomerPicker } from "@/components/customers/customer-picker";
import { useCreateAccount } from "@/lib/hooks/use-accounts";
import { ACCOUNT_TYPE_LABEL } from "@/lib/enums";
import type { AccountType } from "@/types/domain";

const ACCOUNT_TYPES: AccountType[] = ["SAVINGS", "CHECKING", "LOAN", "CREDIT"];

const schema = z.object({
  customerId: z.uuid("Customer ID phải là UUID hợp lệ"),
  accountType: z.enum(["SAVINGS", "CHECKING", "LOAN", "CREDIT"]),
  currency: z.string().length(3, "Mã tiền tệ 3 ký tự (VD: VND)"),
});

type Values = z.infer<typeof schema>;

export default function NewAccountPage() {
  const router = useRouter();
  const create = useCreateAccount();
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { customerId: "", accountType: "SAVINGS", currency: "VND" },
  });

  const onSubmit = (v: Values) => {
    create.mutate(v, { onSuccess: () => router.push("/admin/accounts") });
  };

  return (
    <div className="space-y-4">
      <PageHeader title="Mở tài khoản mới" />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Thông tin tài khoản</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="grid gap-4 md:grid-cols-2"
            noValidate
          >
            <div className="md:col-span-2 space-y-1.5">
              <Label>Khách hàng</Label>
              <CustomerPicker
                value={watch("customerId")}
                onChange={(v) =>
                  setValue("customerId", v, { shouldValidate: true })
                }
              />
              {errors.customerId ? (
                <p className="text-xs text-red-600">{errors.customerId.message}</p>
              ) : null}
            </div>
            <div className="space-y-1.5">
              <Label>Loại tài khoản</Label>
              <Select
                value={watch("accountType")}
                onValueChange={(v) => setValue("accountType", v as AccountType)}
              >
                <SelectTrigger>
                  <SelectValue>
                    {(v) =>
                      v
                        ? (ACCOUNT_TYPE_LABEL[v as AccountType] ?? String(v))
                        : "Chọn loại"
                    }
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {ACCOUNT_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {ACCOUNT_TYPE_LABEL[t]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Tiền tệ</Label>
              <Input {...register("currency")} />
              {errors.currency ? (
                <p className="text-xs text-red-600">{errors.currency.message}</p>
              ) : null}
            </div>
            <div className="md:col-span-2 flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push("/admin/accounts")}
              >
                Huỷ
              </Button>
              <Button type="submit" disabled={create.isPending}>
                {create.isPending ? "Đang mở..." : "Mở tài khoản"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
