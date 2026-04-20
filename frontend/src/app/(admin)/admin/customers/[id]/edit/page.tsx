"use client";

import { useParams, useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CustomerForm } from "@/components/customers/customer-form";
import { useCustomer, useUpdateCustomer } from "@/lib/hooks/use-customers";
import { PageHeader } from "@/components/shared/page-header";
import { LoadingState, ErrorState } from "@/components/shared/data-state";
import { mapServerFieldErrors } from "@/lib/api/error-utils";

export default function EditCustomerPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { data, isLoading, isError } = useCustomer(params.id);
  const update = useUpdateCustomer();

  return (
    <div className="space-y-4">
      <PageHeader title="Sửa khách hàng" description={data?.customerCode} />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Thông tin khách hàng</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <LoadingState />
          ) : isError || !data ? (
            <ErrorState />
          ) : (
            <CustomerForm
              defaults={data}
              isEdit
              submitLabel="Lưu thay đổi"
              submitting={update.isPending}
              onCancel={() => router.push("/admin/customers")}
              onSubmit={(v, { setError }) => {
                update.mutate(
                  {
                    id: data.id,
                    req: {
                      fullName: v.fullName,
                      email: v.email,
                      phone: v.phone,
                      address: v.address || undefined,
                      location: v.location || undefined,
                      customerTypeCode: v.customerTypeCode || undefined,
                    },
                  },
                  {
                    onSuccess: () => router.push("/admin/customers"),
                    onError: (e) =>
                      mapServerFieldErrors(e, setError, {
                        "013": "idNumber",
                      }),
                  },
                );
              }}
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
