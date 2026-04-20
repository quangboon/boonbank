"use client";

import { Pause, Play, Trash2 } from "lucide-react";
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
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import {
  useDeleteRecurring,
  useRecurringList,
  useToggleRecurring,
} from "@/lib/hooks/use-recurring";
import { formatDate, formatMoney } from "@/lib/utils/format";
import { humanizeCron } from "@/lib/utils/cron";

export default function AdminRecurringPage() {
  const { data, isLoading, isError } = useRecurringList({ size: 50 });
  const toggle = useToggleRecurring();
  const del = useDeleteRecurring();

  const rows = data?.content ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Giao dịch định kỳ"
        description="Admin xem toàn bộ các lịch chạy định kỳ của khách hàng."
      />

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState />
      ) : rows.length === 0 ? (
        <EmptyState title="Chưa có giao dịch định kỳ" />
      ) : (
        <div className="rounded-lg border border-neutral-200 bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nguồn → Đích</TableHead>
                <TableHead className="text-right">Số tiền</TableHead>
                <TableHead>Lịch chạy</TableHead>
                <TableHead>Chạy tiếp</TableHead>
                <TableHead>Chạy gần nhất</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-mono text-xs">
                    {r.sourceAccountNumber} → {r.destinationAccountNumber}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatMoney(r.amount)}
                  </TableCell>
                  <TableCell className="text-xs">
                    <div>{humanizeCron(r.cronExpression)}</div>
                    <div className="font-mono text-[10px] text-neutral-400">
                      {r.cronExpression}
                    </div>
                  </TableCell>
                  <TableCell className="text-xs">
                    {formatDate(r.nextRunAt ?? undefined)}
                  </TableCell>
                  <TableCell className="text-xs">
                    {formatDate(r.lastRunAt ?? undefined)}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="secondary"
                      className={
                        r.enabled
                          ? "border border-green-200 bg-green-100 text-green-700 text-xs"
                          : "border border-neutral-200 bg-neutral-100 text-neutral-700 text-xs"
                      }
                    >
                      {r.enabled ? "Đang chạy" : "Tạm dừng"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() =>
                          toggle.mutate({ id: r.id, enabled: !r.enabled })
                        }
                        aria-label={r.enabled ? "Tạm dừng" : "Kích hoạt"}
                      >
                        {r.enabled ? (
                          <Pause className="h-3.5 w-3.5" />
                        ) : (
                          <Play className="h-3.5 w-3.5" />
                        )}
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-red-600"
                        onClick={() => del.mutate(r.id)}
                        aria-label="Xoá"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
