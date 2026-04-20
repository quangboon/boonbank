"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader } from "@/components/shared/page-header";
import { TransferForm } from "@/components/transactions/transfer-form";

export default function MyTransferPage() {
  return (
    <Suspense fallback={null}>
      <Inner />
    </Suspense>
  );
}

function Inner() {
  const params = useSearchParams();
  const defaultSource = params.get("from") ?? undefined;

  return (
    <div className="space-y-4">
      <PageHeader title="Chuyển khoản" />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Thông tin giao dịch</CardTitle>
        </CardHeader>
        <CardContent>
          <TransferForm defaultSource={defaultSource} ownedOnly />
        </CardContent>
      </Card>
    </div>
  );
}
