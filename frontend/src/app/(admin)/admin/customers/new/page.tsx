"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CustomerForm } from "@/components/customers/customer-form";
import { CredentialsDialog } from "@/components/customers/credentials-dialog";
import { useCreateCustomer } from "@/lib/hooks/use-customers";
import { PageHeader } from "@/components/shared/page-header";
import { mapServerFieldErrors } from "@/lib/api/error-utils";

export default function NewCustomerPage() {
  const router = useRouter();
  const create = useCreateCustomer();
  const [credentials, setCredentials] = useState<{
    username: string;
    tempPassword: string;
  } | null>(null);

  return (
    <div className="space-y-4">
      <PageHeader title="Thêm khách hàng" />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Thông tin khách hàng</CardTitle>
        </CardHeader>
        <CardContent>
          <CustomerForm
            submitLabel="Tạo mới"
            submitting={create.isPending}
            onCancel={() => router.push("/admin/customers")}
            onSubmit={(v, { setError }) => {
              create.mutate(
                {
                  fullName: v.fullName,
                  idNumber: v.idNumber,
                  email: v.email,
                  phone: v.phone,
                  address: v.address || undefined,
                  location: v.location || undefined,
                  dateOfBirth: v.dateOfBirth || undefined,
                  customerTypeCode: v.customerTypeCode || undefined,
                },
                {
                  onSuccess: (data) => setCredentials(data.credentials),
                  onError: (e) =>
                    mapServerFieldErrors(e, setError, {
                      "013": "idNumber",
                    }),
                },
              );
            }}
          />
        </CardContent>
      </Card>

      {credentials ? (
        <CredentialsDialog
          open
          onOpenChange={(v) => {
            if (!v) setCredentials(null);
          }}
          username={credentials.username}
          tempPassword={credentials.tempPassword}
          onClose={() => router.push("/admin/customers")}
        />
      ) : null}
    </div>
  );
}
