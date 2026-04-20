"use client";

import Link from "next/link";
import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Input } from "@/components/ui/input";
import { Button, buttonVariants } from "@/components/ui/button";
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import { useCustomers, useDeleteCustomer } from "@/lib/hooks/use-customers";
import { CUSTOMER_TYPE_LABEL } from "@/lib/enums";
import { formatDate } from "@/lib/utils/format";
import { cn } from "@/lib/utils";

export default function CustomersListPage() {
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const size = 20;
  const { data, isLoading, isError, error } = useCustomers({ keyword, page, size });
  const del = useDeleteCustomer();

  const rows = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const hasNext = data?.hasNext ?? false;

  return (
    <div className="space-y-4">
      <PageHeader
        title="Khách hàng"
        description={`Tổng ${total} khách hàng`}
        actions={
          <Link
            href="/admin/customers/new"
            className={cn(buttonVariants(), "gap-1")}
          >
            <Plus className="h-4 w-4" /> Thêm mới
          </Link>
        }
      />

      <div className="flex gap-2">
        <Input
          placeholder="Tìm theo tên / email / CCCD"
          value={keyword}
          onChange={(e) => {
            setKeyword(e.target.value);
            setPage(0);
          }}
          className="max-w-sm"
        />
      </div>

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState message={String(error)} />
      ) : rows.length === 0 ? (
        <EmptyState title="Không có khách hàng" />
      ) : (
        <div className="rounded-lg border border-neutral-200 bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mã KH</TableHead>
                <TableHead>Họ và tên</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>SĐT</TableHead>
                <TableHead>Địa điểm</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Ngày tạo</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-mono text-xs">
                    <Link
                      href={`/admin/customers/${c.id}`}
                      className="hover:underline"
                    >
                      {c.customerCode}
                    </Link>
                  </TableCell>
                  <TableCell className="font-medium">
                    <Link
                      href={`/admin/customers/${c.id}`}
                      className="hover:underline"
                    >
                      {c.fullName}
                    </Link>
                  </TableCell>
                  <TableCell>{c.email}</TableCell>
                  <TableCell>{c.phone}</TableCell>
                  <TableCell>{c.location ?? "—"}</TableCell>
                  <TableCell>{CUSTOMER_TYPE_LABEL[c.customerTypeCode ?? ""] ?? c.customerTypeCode ?? "—"}</TableCell>
                  <TableCell>{formatDate(c.createdAt)}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Link
                        href={`/admin/customers/${c.id}/edit`}
                        className={cn(
                          buttonVariants({ variant: "ghost", size: "icon-sm" }),
                        )}
                        aria-label="Sửa"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </Link>
                      <AlertDialog>
                        <AlertDialogTrigger
                          className={buttonVariants({
                            variant: "ghost",
                            size: "icon-sm",
                            className: "text-red-600",
                          })}
                          aria-label="Xoá"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>Xoá khách hàng?</AlertDialogTitle>
                            <AlertDialogDescription>
                              Hành động này không thể hoàn tác. Khách hàng{" "}
                              <b>{c.fullName}</b> sẽ bị xoá.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Huỷ</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={() => del.mutate(c.id)}
                              disabled={del.isPending}
                            >
                              Xoá
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
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
