"use client";

import { useState } from "react";
import { Download } from "lucide-react";
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
import { AccountPicker } from "@/components/accounts/account-picker";
import { downloadBlob } from "@/lib/utils/download-blob";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { useTransactionsSummary } from "@/lib/hooks/use-statistics";
import { TransactionsTrendChart } from "@/components/dashboard/transactions-trend-chart";
import { PERIOD_UNIT_LABEL } from "@/lib/enums";
import type { PeriodUnit } from "@/types/domain";

const PERIODS: PeriodUnit[] = ["WEEK", "QUARTER", "YEAR"];

export default function ReportsPage() {
  const [accountId, setAccountId] = useState<string | undefined>(undefined);
  const [from, setFrom] = useState<string>(() => {
    const d = new Date();
    d.setMonth(d.getMonth() - 3);
    return d.toISOString().slice(0, 10);
  });
  const [to, setTo] = useState<string>(() =>
    new Date().toISOString().slice(0, 10),
  );
  const [period, setPeriod] = useState<PeriodUnit>("WEEK");
  const [downloading, setDownloading] = useState(false);

  const { data: summary } = useTransactionsSummary({
    period,
    from,
    to,
    accountId,
  });

  const downloadXlsx = async () => {
    if (!accountId) return;
    setDownloading(true);
    try {
      await downloadBlob(
        ENDPOINTS.reports.excel(accountId),
        `transactions-${accountId}.xlsx`,
        { from, to },
      );
    } finally {
      setDownloading(false);
    }
  };

  const downloadPdf = async () => {
    if (!accountId) return;
    setDownloading(true);
    try {
      await downloadBlob(
        ENDPOINTS.reports.pdf(accountId),
        `statement-${accountId}.pdf`,
        { from, to },
      );
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="space-y-4">
      <PageHeader
        title="Báo cáo tài chính"
        description="Chọn 'Tất cả tài khoản' để xem tổng hệ thống, hoặc chọn 1 tài khoản để tải sao kê Excel / PDF."
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Tham số</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <div className="space-y-1.5 md:col-span-2">
              <Label>Tài khoản</Label>
              <AccountPicker
                value={accountId}
                onChange={setAccountId}
                allowAll
              />
            </div>
            <div className="space-y-1.5">
              <Label>Từ ngày</Label>
              <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label>Đến ngày</Label>
              <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label>Kỳ</Label>
              <Select value={period} onValueChange={(v) => setPeriod(v as PeriodUnit)}>
                <SelectTrigger>
                  <SelectValue>
                    {(v) =>
                      v
                        ? (PERIOD_UNIT_LABEL[v as PeriodUnit] ?? String(v))
                        : "Chọn kỳ"
                    }
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {PERIODS.map((p) => (
                    <SelectItem key={p} value={p}>
                      {PERIOD_UNIT_LABEL[p]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <Button onClick={downloadXlsx} disabled={!accountId || downloading} className="gap-1">
              <Download className="h-4 w-4" /> Excel
            </Button>
            <Button onClick={downloadPdf} disabled={!accountId || downloading} variant="outline" className="gap-1">
              <Download className="h-4 w-4" /> PDF
            </Button>
            {!accountId ? (
              <span className="text-xs text-neutral-500">
                Chọn 1 tài khoản cụ thể để bật nút xuất file.
              </span>
            ) : null}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Xu hướng giao dịch</CardTitle>
        </CardHeader>
        <CardContent>
          <TransactionsTrendChart data={summary ?? []} />
        </CardContent>
      </Card>
    </div>
  );
}
