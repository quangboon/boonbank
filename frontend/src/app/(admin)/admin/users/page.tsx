"use client";

import { useState } from "react";
import { KeyRound } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { CredentialsDialog } from "@/components/customers/credentials-dialog";
import {
  LoadingState,
  ErrorState,
  EmptyState,
} from "@/components/shared/data-state";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  useUsers,
  useEnableUser,
  useDisableUser,
  useResetUserPassword,
} from "@/lib/hooks/use-users";
import { useAuth } from "@/lib/auth/auth-context";

const PAGE_SIZE = 20;

export default function UsersPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useUsers({
    page,
    size: PAGE_SIZE,
    sort: "createdAt,desc",
  });
  const enable = useEnableUser();
  const disable = useDisableUser();
  const resetPw = useResetUserPassword();
  const { user: currentUser } = useAuth();
  const [resetResult, setResetResult] = useState<
    { username: string; tempPassword: string } | null
  >(null);

  return (
    <div className="space-y-4">
      <PageHeader
        title="Người dùng"
        description="Quản lý tài khoản đăng nhập. User tạo tự động khi thêm khách hàng; username là mã KH."
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">
            Danh sách {data ? `(${data.totalElements})` : ""}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <LoadingState />
          ) : isError ? (
            <ErrorState />
          ) : !data || data.content.length === 0 ? (
            <EmptyState />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Vai trò</TableHead>
                    <TableHead>Khách hàng</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Đăng nhập gần nhất</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((u) => (
                    <TableRow key={u.id}>
                      <TableCell className="font-mono text-xs">
                        {u.username}
                      </TableCell>
                      <TableCell className="space-x-1">
                        {u.roles.map((r) => (
                          <Badge key={r} variant="outline">
                            {r}
                          </Badge>
                        ))}
                      </TableCell>
                      <TableCell className="font-mono text-[11px] text-neutral-500">
                        {u.customerId ?? "—"}
                      </TableCell>
                      <TableCell>
                        {u.accountLocked ? (
                          <Badge variant="destructive">Locked</Badge>
                        ) : u.enabled ? (
                          <Badge>Active</Badge>
                        ) : (
                          <Badge variant="secondary">Disabled</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-xs text-neutral-500">
                        {u.lastLoginAt
                          ? new Date(u.lastLoginAt).toLocaleString("vi-VN")
                          : "—"}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button
                            size="sm"
                            variant="ghost"
                            title="Đặt lại mật khẩu"
                            disabled={resetPw.isPending}
                            onClick={() =>
                              resetPw.mutate(u.id, {
                                onSuccess: (d) =>
                                  setResetResult({
                                    username: u.username,
                                    tempPassword: d.tempPassword,
                                  }),
                              })
                            }
                          >
                            <KeyRound className="h-3.5 w-3.5" />
                          </Button>
                          {currentUser?.sub === u.id ? (
                            <span className="self-center text-xs text-neutral-400">
                              (bạn)
                            </span>
                          ) : u.enabled ? (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={disable.isPending}
                              onClick={() => disable.mutate(u.id)}
                            >
                              Tắt
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={enable.isPending}
                              onClick={() => enable.mutate(u.id)}
                            >
                              Bật
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <Pagination
                page={page}
                totalPages={data.totalPages}
                onPageChange={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>

      {resetResult ? (
        <CredentialsDialog
          open
          onOpenChange={(v) => {
            if (!v) setResetResult(null);
          }}
          username={resetResult.username}
          tempPassword={resetResult.tempPassword}
        />
      ) : null}
    </div>
  );
}

function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-4 flex items-center justify-end gap-2 text-sm">
      <span className="text-neutral-500">
        Trang {page + 1} / {totalPages}
      </span>
      <Button
        size="sm"
        variant="outline"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >
        Trước
      </Button>
      <Button
        size="sm"
        variant="outline"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Sau
      </Button>
    </div>
  );
}
