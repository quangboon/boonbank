"use client";

import { useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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
import { useAlerts } from "@/lib/hooks/use-alerts";
import { formatDate } from "@/lib/utils/format";
import type { AlertSeverity } from "@/types/domain";
import { ALERT_SEVERITY_LABEL } from "@/lib/enums";

const SEVERITIES: AlertSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

const SEV_TONE: Record<AlertSeverity, string> = {
  LOW: "border-neutral-200 bg-neutral-100 text-neutral-700",
  MEDIUM: "border-amber-200 bg-amber-100 text-amber-700",
  HIGH: "border-orange-200 bg-orange-100 text-orange-700",
  CRITICAL: "border-red-200 bg-red-100 text-red-700",
};

export default function AlertsPage() {
  const [severity, setSeverity] = useState<AlertSeverity | "all">("all");
  const [resolved, setResolved] = useState<"all" | "open" | "done">("open");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useAlerts({
    severity: severity === "all" ? undefined : severity,
    resolved: resolved === "all" ? undefined : resolved === "done",
    page,
    size: 20,
  });

  const rows = data?.content ?? [];
  const hasNext = data?.hasNext ?? false;
  const total = data?.totalElements ?? 0;

  return (
    <div className="space-y-4">
      <PageHeader title="Cảnh báo gian lận" description={`${total} cảnh báo`} />
      <div className="flex flex-wrap items-end gap-2">
        <div className="space-y-1">
          <span className="text-xs text-neutral-500">Mức độ</span>
          <Select value={severity} onValueChange={(v) => { setSeverity(v as AlertSeverity | "all"); setPage(0); }}>
            <SelectTrigger className="w-40">
              <SelectValue>
                {(v) =>
                  v === "all" || !v
                    ? "Tất cả"
                    : (ALERT_SEVERITY_LABEL[v as AlertSeverity] ?? String(v))
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả</SelectItem>
              {SEVERITIES.map((s) => (
                <SelectItem key={s} value={s}>
                  {ALERT_SEVERITY_LABEL[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <span className="text-xs text-neutral-500">Trạng thái</span>
          <Select value={resolved} onValueChange={(v) => { setResolved(v as typeof resolved); setPage(0); }}>
            <SelectTrigger className="w-40">
              <SelectValue>
                {(v) =>
                  v === "done"
                    ? "Đã xử lý"
                    : v === "all"
                      ? "Tất cả"
                      : "Đang mở"
                }
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="open">Đang mở</SelectItem>
              <SelectItem value="done">Đã xử lý</SelectItem>
              <SelectItem value="all">Tất cả</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState />
      ) : rows.length === 0 ? (
        <EmptyState title="Không có cảnh báo" />
      ) : (
        <div className="rounded-lg border border-neutral-200 bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mức độ</TableHead>
                <TableHead>Rule</TableHead>
                <TableHead>Thông điệp</TableHead>
                <TableHead>Giao dịch</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Thời gian</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((a) => (
                <TableRow key={a.id}>
                  <TableCell>
                    <Badge
                      variant="secondary"
                      className={`border text-xs ${SEV_TONE[a.severity]}`}
                    >
                      {a.severity}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs">{a.ruleCode}</TableCell>
                  <TableCell>{a.message}</TableCell>
                  <TableCell className="font-mono text-xs">
                    {a.transactionId ? a.transactionId.slice(0, 8) : "—"}
                  </TableCell>
                  <TableCell>
                    {a.resolved ? (
                      <Badge variant="secondary" className="text-xs">
                        Đã xử lý
                      </Badge>
                    ) : (
                      <Badge
                        variant="secondary"
                        className="border border-red-200 bg-red-50 text-xs text-red-700"
                      >
                        Đang mở
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-xs">
                    {formatDate(a.createdAt)}
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
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Trước</Button>
          <Button variant="outline" size="sm" disabled={!hasNext} onClick={() => setPage((p) => p + 1)}>Sau</Button>
        </div>
      </div>
    </div>
  );
}
