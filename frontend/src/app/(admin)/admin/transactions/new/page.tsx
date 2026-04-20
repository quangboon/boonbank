"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { PageHeader } from "@/components/shared/page-header";
import { TransferForm } from "@/components/transactions/transfer-form";
import { CashForm } from "@/components/transactions/cash-form";

export default function NewTransactionPage() {
  return (
    <div className="space-y-4">
      <PageHeader title="Tạo giao dịch" />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Loại giao dịch</CardTitle>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="transfer">
            <TabsList>
              <TabsTrigger value="transfer">Chuyển khoản</TabsTrigger>
              <TabsTrigger value="withdraw">Rút tiền</TabsTrigger>
              <TabsTrigger value="deposit">Nạp tiền</TabsTrigger>
            </TabsList>
            <TabsContent value="transfer" className="pt-4">
              <TransferForm />
            </TabsContent>
            <TabsContent value="withdraw" className="pt-4">
              <CashForm kind="withdraw" />
            </TabsContent>
            <TabsContent value="deposit" className="pt-4">
              <CashForm kind="deposit" />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
