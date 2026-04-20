"use client";

import Link from "next/link";
import { useState } from "react";
import { Eye, Plus } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button, buttonVariants } from "@/components/ui/button";
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import { useAccounts } from "@/lib/hooks/use-accounts";
import { formatMoney, formatDate } from "@/lib/utils/format";
import { AccountStatusBadge } from "@/components/accounts/account-status-badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { AccountStatus, AccountType } from "@/types/domain";
import { ACCOUNT_STATUS_LABEL, ACCOUNT_TYPE_LABEL } from "@/lib/enums";
import { cn } from "@/lib/utils";

const ACCOUNT_TYPES: AccountType[] = ["SAVINGS", "CHECKING", "LOAN", "CREDIT"];
const ACCOUNT_STATUSES: AccountStatus[] = [
  "ACTIVE",
  "INACTIVE",
  "FROZEN",
  "CLOSED",
];

export default function AccountsListPage() {
  const [status, setStatus] = useState<AccountStatus | "all">("all");
  const [type, setType] = useState<AccountType | "all">("all");
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading, isError } = useAccounts({
    status: status === "all" ? undefined : status,
    accountType: type === "all" ? undefined : type,
    page,
    size,
  });

  const rows = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const hasNext = data?.hasNext ?? false;

  return (
    <div className="space-y-4">
      <PageHeader
        title="Tài khoản"
        description={`Tổng ${total} tài khoản`}
        actions={
          <Link
            href="/admin/accounts/new"
            className={cn(buttonVariants(), "gap-1")}
          >
            <Plus className="h-4 w-4" /> Mở tài khoản
          </Link>
        }
      />

      <div className="flex flex-wrap gap-2">
        <Select value={status} onValueChange={(v) => { setStatus(v as AccountStatus | "all"); setPage(0); }}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Trạng thái">
              {(v) =>
                v === "all" || !v
                  ? "Mọi trạng thái"
                  : (ACCOUNT_STATUS_LABEL[v as AccountStatus] ?? String(v))
              }
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Mọi trạng thái</SelectItem>
            {ACCOUNT_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {ACCOUNT_STATUS_LABEL[s]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={type} onValueChange={(v) => { setType(v as AccountType | "all"); setPage(0); }}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Loại">
              {(v) =>
                v === "all" || !v
                  ? "Mọi loại"
                  : (ACCOUNT_TYPE_LABEL[v as AccountType] ?? String(v))
              }
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Mọi loại</SelectItem>
            {ACCOUNT_TYPES.map((t) => (
              <SelectItem key={t} value={t}>
                {ACCOUNT_TYPE_LABEL[t]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState />
      ) : rows.length === 0 ? (
        <EmptyState title="Không có tài khoản" />
      ) : (
        <div className="rounded-lg border border-neutral-200 bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Số tài khoản</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="text-right">Số dư</TableHead>
                <TableHead className="text-right">Hạn mức GD</TableHead>
                <TableHead>Tiền tệ</TableHead>
                <TableHead>Mở ngày</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((a) => (
                <TableRow key={a.id}>
                  <TableCell className="font-mono text-xs">
                    {a.accountNumber}
                  </TableCell>
                  <TableCell>{ACCOUNT_TYPE_LABEL[a.accountType]}</TableCell>
                  <TableCell>
                    <AccountStatusBadge status={a.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    {formatMoney(a.balance, a.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatMoney(a.transactionLimit, a.currency)}
                  </TableCell>
                  <TableCell>{a.currency}</TableCell>
                  <TableCell>{formatDate(a.openedAt)}</TableCell>
                  <TableCell className="text-right">
                    <Link
                      href={`/admin/accounts/${a.id}`}
                      className={cn(
                        buttonVariants({ variant: "ghost", size: "icon-sm" }),
                      )}
                      aria-label="Chi tiết"
                    >
                      <Eye className="h-3.5 w-3.5" />
                    </Link>
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
