"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Download, Search } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import { AccountPicker } from "@/components/accounts/account-picker";
import {
  TransactionStatusBadge,
  TransactionTypeBadge,
} from "@/components/transactions/transaction-type-badge";
import { downloadBlob } from "@/lib/utils/download-blob";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { useQuery } from "@tanstack/react-query";
import { transactionsApi } from "@/lib/api/transactions";
import { formatDate, formatMoney } from "@/lib/utils/format";

export default function MyStatementsPage() {
  return (
    <Suspense fallback={null}>
      <Inner />
    </Suspense>
  );
}

function defaultFrom() {
  const d = new Date();
  d.setMonth(d.getMonth() - 3);
  return d.toISOString().slice(0, 10);
}

function defaultTo() {
  return new Date().toISOString().slice(0, 10);
}

type Applied = { accountId: string; from: string; to: string };

function Inner() {
  const params = useSearchParams();
  const [accountId, setAccountId] = useState<string | undefined>(
    params.get("account") ?? undefined,
  );
  const [from, setFrom] = useState<string>(defaultFrom);
  const [to, setTo] = useState<string>(defaultTo);
  const [downloading, setDownloading] = useState(false);
  const [applied, setApplied] = useState<Applied | null>(null);

  const canSearch = Boolean(accountId && from && to);
  const canDownload = canSearch && !downloading;

  // Chỉ fetch khi user bấm Xem (applied != null). Inline useQuery để có `enabled`
  // — hook useTransactions chung không expose cờ này.
  const {
    data: preview,
    isLoading: previewLoading,
    isError: previewError,
    isFetching: previewFetching,
  } = useQuery({
    queryKey: ["transactions", "statement-preview", applied],
    queryFn: () =>
      transactionsApi.search({
        accountId: applied!.accountId,
        // from = 00:00 local ngày đầu; to = 23:59:59.999 local ngày cuối (inclusive).
        // Tránh bug UTC-midnight cắt mất tx cuối ngày theo giờ VN.
        from: new Date(`${applied!.from}T00:00:00`).toISOString(),
        to: new Date(`${applied!.to}T23:59:59.999`).toISOString(),
        page: 0,
        size: 50,
      }),
    enabled: applied !== null,
  });

  const onSearch = () => {
    if (!accountId || !from || !to) return;
    setApplied({ accountId, from, to });
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

  const rows = preview?.content ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Sao kê"
        description="Xem danh sách giao dịch trong khoảng thời gian và tải về PDF / Excel."
      />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Điều kiện</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-3">
            <div className="space-y-1.5 md:col-span-1">
              <Label>Tài khoản</Label>
              <AccountPicker value={accountId} onChange={setAccountId} />
            </div>
            <div className="space-y-1.5">
              <Label>Từ ngày</Label>
              <Input
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Đến ngày</Label>
              <Input
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button
              onClick={onSearch}
              disabled={!canSearch || previewFetching}
              className="gap-1"
            >
              <Search className="h-4 w-4" />
              {previewFetching ? "Đang tải..." : "Xem"}
            </Button>
            <Button
              onClick={downloadPdf}
              disabled={!canDownload}
              variant="outline"
              className="gap-1"
            >
              <Download className="h-4 w-4" /> Tải PDF
            </Button>
            <Button
              onClick={downloadXlsx}
              disabled={!canDownload}
              variant="outline"
              className="gap-1"
            >
              <Download className="h-4 w-4" /> Tải Excel
            </Button>
          </div>
        </CardContent>
      </Card>

      {applied ? (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">
              Giao dịch{" "}
              {preview ? (
                <span className="text-neutral-500">
                  ({preview.totalElements} — hiển thị {rows.length} dòng đầu)
                </span>
              ) : null}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {previewLoading ? (
              <LoadingState />
            ) : previewError ? (
              <ErrorState />
            ) : rows.length === 0 ? (
              <EmptyState
                title="Không có giao dịch"
                description="Khoảng thời gian này không có giao dịch."
              />
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Thời gian</TableHead>
                    <TableHead>Loại</TableHead>
                    <TableHead>Nguồn / Đích</TableHead>
                    <TableHead className="text-right">Số tiền</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Ghi chú</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((t) => (
                    <TableRow key={t.id}>
                      <TableCell className="text-xs">
                        {formatDate(t.createdAt)}
                      </TableCell>
                      <TableCell>
                        <TransactionTypeBadge type={t.type} />
                      </TableCell>
                      <TableCell className="font-mono text-[11px] text-neutral-600">
                        {t.sourceAccountNumber ?? "—"}
                        {t.destinationAccountNumber
                          ? ` → ${t.destinationAccountNumber}`
                          : ""}
                      </TableCell>
                      <TableCell className="text-right">
                        {formatMoney(t.amount, t.currency)}
                      </TableCell>
                      <TableCell>
                        <TransactionStatusBadge status={t.status} />
                      </TableCell>
                      <TableCell className="text-xs text-neutral-500">
                        {t.description ?? "—"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            {preview && preview.totalElements > rows.length ? (
              <p className="mt-3 text-xs text-neutral-500">
                Còn {preview.totalElements - rows.length} giao dịch nữa — tải
                PDF/Excel để xem toàn bộ.
              </p>
            ) : null}
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
