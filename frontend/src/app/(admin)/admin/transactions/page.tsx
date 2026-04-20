"use client";

import Link from "next/link";
import { useState } from "react";
import { Plus } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import { useTransactions } from "@/lib/hooks/use-transactions";
import { formatDate, formatMoney } from "@/lib/utils/format";
import {
  TransactionStatusBadge,
  TransactionTypeBadge,
} from "@/components/transactions/transaction-type-badge";
import type { TransactionStatus, TransactionType } from "@/types/domain";
import {
  TRANSACTION_STATUS_LABEL,
  TRANSACTION_TYPE_LABEL,
} from "@/lib/enums";
import { cn } from "@/lib/utils";

const TYPES: TransactionType[] = ["TRANSFER", "WITHDRAW", "DEPOSIT"];
const STATUSES: TransactionStatus[] = [
  "PENDING",
  "COMPLETED",
  "FAILED",
  "REVERSED",
];

export default function TransactionsListPage() {
  const [type, setType] = useState<TransactionType | "all">("all");
  const [status, setStatus] = useState<TransactionStatus | "all">("all");
  const [from, setFrom] = useState<string>("");
  const [to, setTo] = useState<string>("");
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading, isError } = useTransactions({
    type: type === "all" ? undefined : type,
    status: status === "all" ? undefined : status,
    from: from ? new Date(from).toISOString() : undefined,
    to: to ? new Date(to).toISOString() : undefined,
    page,
    size,
  });

  const rows = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const hasNext = data?.hasNext ?? false;

  return (
    <div className="space-y-4">
      <PageHeader
        title="Giao dịch"
        description={`Tổng ${total} giao dịch`}
        actions={
          <Link
            href="/admin/transactions/new"
            className={cn(buttonVariants(), "gap-1")}
          >
            <Plus className="h-4 w-4" /> Tạo giao dịch
          </Link>
        }
      />

      <div className="flex flex-wrap items-end gap-2">
        <div className="space-y-1">
          <span className="text-xs text-neutral-500">Loại</span>
          <Select value={type} onValueChange={(v) => { setType(v as TransactionType | "all"); setPage(0); }}>
            <SelectTrigger className="w-36">
              <SelectValue>
                {(v) =>
                  v === "all" || !v
                    ? "Mọi loại"
                    : (TRANSACTION_TYPE_LABEL[v as TransactionType] ?? String(v))
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Mọi loại</SelectItem>
              {TYPES.map((t) => (
                <SelectItem key={t} value={t}>
                  {TRANSACTION_TYPE_LABEL[t]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <span className="text-xs text-neutral-500">Trạng thái</span>
          <Select value={status} onValueChange={(v) => { setStatus(v as TransactionStatus | "all"); setPage(0); }}>
            <SelectTrigger className="w-40">
              <SelectValue>
                {(v) =>
                  v === "all" || !v
                    ? "Mọi trạng thái"
                    : (TRANSACTION_STATUS_LABEL[v as TransactionStatus] ?? String(v))
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Mọi trạng thái</SelectItem>
              {STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {TRANSACTION_STATUS_LABEL[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <span className="text-xs text-neutral-500">Từ ngày</span>
          <Input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} className="w-44" />
        </div>
        <div className="space-y-1">
          <span className="text-xs text-neutral-500">Đến ngày</span>
          <Input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} className="w-44" />
        </div>
      </div>

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState />
      ) : rows.length === 0 ? (
        <EmptyState title="Không có giao dịch" />
      ) : (
        <div className="rounded-lg border border-neutral-200 bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mã GD</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Nguồn</TableHead>
                <TableHead>Đích</TableHead>
                <TableHead className="text-right">Số tiền</TableHead>
                <TableHead className="text-right">Phí</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Thời gian</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="font-mono text-xs">
                    {t.id.slice(0, 8)}
                  </TableCell>
                  <TableCell>
                    <TransactionTypeBadge type={t.type} />
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {t.sourceAccountNumber ?? "—"}
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {t.destinationAccountNumber ?? "—"}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatMoney(t.amount, t.currency)}
                  </TableCell>
                  <TableCell className="text-right text-neutral-500">
                    {formatMoney(t.fee, t.currency)}
                  </TableCell>
                  <TableCell>
                    <TransactionStatusBadge status={t.status} />
                  </TableCell>
                  <TableCell className="text-xs">
                    {formatDate(t.createdAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <div className="flex items-center justify-between text-xs text-neutral-500">
        <span>Trang {page + 1}</span>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Trước
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={!hasNext}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </Button>
        </div>
      </div>
    </div>
  );
}
