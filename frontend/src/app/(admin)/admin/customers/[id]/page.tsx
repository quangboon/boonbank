"use client";

import Link from "next/link";
import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Ban, KeyRound, Lock, LockOpen, Pencil, Trash2 } from "lucide-react";
import { CredentialsDialog } from "@/components/customers/credentials-dialog";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button, buttonVariants } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/shared/page-header";
import {
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
import { AccountStatusBadge } from "@/components/accounts/account-status-badge";
import { ReasonDialog } from "@/components/accounts/reason-dialog";
import { useCustomer, useDeleteCustomer } from "@/lib/hooks/use-customers";
import {
  useAccounts,
  useCloseAccount,
  useFreezeAccount,
  useUnfreezeAccount,
} from "@/lib/hooks/use-accounts";
import {
  useUsersByCustomer,
  useEnableUser,
  useDisableUser,
  useResetUserPassword,
} from "@/lib/hooks/use-users";
import { formatDate, formatMoney } from "@/lib/utils/format";
import { cn } from "@/lib/utils";
import { CUSTOMER_TYPE_LABEL } from "@/lib/enums";

export default function CustomerDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const customerQ = useCustomer(params.id);
  const accountsQ = useAccounts({ customerId: params.id, size: 50 });
  const usersQ = useUsersByCustomer(params.id);

  const freeze = useFreezeAccount();
  const unfreeze = useUnfreezeAccount();
  const close = useCloseAccount();
  const enableUser = useEnableUser();
  const disableUser = useDisableUser();
  const resetPw = useResetUserPassword();
  const deleteCustomer = useDeleteCustomer();
  const [resetResult, setResetResult] = useState<
    { username: string; tempPassword: string } | null
  >(null);

  if (customerQ.isLoading) return <LoadingState />;
  if (customerQ.isError || !customerQ.data) return <ErrorState />;

  const customer = customerQ.data;
  const accounts = accountsQ.data?.content ?? [];
  const users = usersQ.data ?? [];
  const hasOpenAccount = accounts.some((a) => a.status !== "CLOSED");

  return (
    <div className="space-y-4">
      <PageHeader
        title={customer.fullName}
        description={`Mã KH: ${customer.customerCode}`}
        actions={
          <div className="flex gap-2">
            <Link
              href={`/admin/customers/${customer.id}/edit`}
              className={cn(buttonVariants({ variant: "outline" }), "gap-1")}
            >
              <Pencil className="h-4 w-4" /> Sửa KYC
            </Link>
            <AlertDialog>
              <AlertDialogTrigger
                render={
                  <Button
                    variant="destructive"
                    className="gap-1"
                    disabled={hasOpenAccount || deleteCustomer.isPending}
                    title={
                      hasOpenAccount
                        ? "Còn tài khoản chưa đóng — đóng hết trước khi xoá"
                        : undefined
                    }
                  >
                    <Trash2 className="h-4 w-4" /> Xoá KH
                  </Button>
                }
              />
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Xoá khách hàng?</AlertDialogTitle>
                  <AlertDialogDescription>
                    Hồ sơ KYC của <b>{customer.fullName}</b> sẽ bị xoá (soft).
                    User đăng nhập gắn với KH sẽ bị tắt tự động.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Huỷ</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={() =>
                      deleteCustomer.mutate(customer.id, {
                        onSuccess: () => router.push("/admin/customers"),
                      })
                    }
                  >
                    Xoá
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Hồ sơ KYC</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <Info label="Mã KH" value={customer.customerCode} mono />
            <Info label="Họ và tên" value={customer.fullName} />
            <Info label="CCCD/CMND" value={customer.idNumber} mono />
            <Info label="Email" value={customer.email} />
            <Info label="SĐT" value={customer.phone} />
            <Info label="Địa chỉ" value={customer.address ?? "—"} />
            <Info label="Địa điểm" value={customer.location ?? "—"} />
            <Info label="Ngày sinh" value={customer.dateOfBirth ?? "—"} />
            <Info
              label="Loại KH"
              value={
                CUSTOMER_TYPE_LABEL[customer.customerTypeCode ?? ""] ??
                customer.customerTypeCode ??
                "—"
              }
            />
            <Info label="Ngày tạo" value={formatDate(customer.createdAt)} />
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-sm">
              Tài khoản ({accounts.length})
            </CardTitle>
          </CardHeader>
          <CardContent>
            {accounts.length === 0 ? (
              <p className="text-sm text-neutral-500">Chưa có tài khoản.</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Số TK</TableHead>
                    <TableHead>Loại</TableHead>
                    <TableHead>Số dư</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {accounts.map((a) => {
                    const canFreeze = a.status === "ACTIVE";
                    const canUnfreeze = a.status === "FROZEN";
                    const canClose =
                      a.status === "ACTIVE" || a.status === "FROZEN";
                    return (
                      <TableRow key={a.id}>
                        <TableCell>
                          <Link
                            href={`/admin/accounts/${a.id}`}
                            className="font-mono text-xs hover:underline"
                          >
                            {a.accountNumber}
                          </Link>
                        </TableCell>
                        <TableCell className="text-xs">
                          {a.accountType}
                        </TableCell>
                        <TableCell className="text-xs">
                          {formatMoney(a.balance, a.currency)}
                        </TableCell>
                        <TableCell>
                          <AccountStatusBadge status={a.status} />
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-1">
                            <ReasonDialog
                              title="Khoá tài khoản?"
                              confirmLabel="Khoá"
                              disabled={!canFreeze || freeze.isPending}
                              onConfirm={(reason) =>
                                freeze.mutate({ id: a.id, reason })
                              }
                              trigger={
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  disabled={!canFreeze}
                                  aria-label="Khoá"
                                >
                                  <Lock className="h-3.5 w-3.5" />
                                </Button>
                              }
                            />
                            <ReasonDialog
                              title="Mở khoá tài khoản?"
                              confirmLabel="Mở khoá"
                              disabled={!canUnfreeze || unfreeze.isPending}
                              onConfirm={(reason) =>
                                unfreeze.mutate({ id: a.id, reason })
                              }
                              trigger={
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  disabled={!canUnfreeze}
                                  aria-label="Mở khoá"
                                >
                                  <LockOpen className="h-3.5 w-3.5" />
                                </Button>
                              }
                            />
                            <ReasonDialog
                              title="Đóng tài khoản?"
                              description="Số dư phải bằng 0. Hành động không thể hoàn tác."
                              confirmLabel="Đóng"
                              disabled={!canClose || close.isPending}
                              onConfirm={(reason) =>
                                close.mutate({ id: a.id, reason })
                              }
                              trigger={
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  disabled={!canClose}
                                  aria-label="Đóng"
                                >
                                  <Ban className="h-3.5 w-3.5" />
                                </Button>
                              }
                            />
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle className="text-sm">
              User đăng nhập ({users.length})
            </CardTitle>
          </CardHeader>
          <CardContent>
            {users.length === 0 ? (
              <p className="text-sm text-neutral-500">
                Chưa có user đăng nhập gắn với KH này.
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Vai trò</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Đăng nhập gần nhất</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((u) => (
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
                          {u.enabled ? (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={disableUser.isPending}
                              onClick={() => disableUser.mutate(u.id)}
                            >
                              Tắt
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={enableUser.isPending}
                              onClick={() => enableUser.mutate(u.id)}
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
            )}
          </CardContent>
        </Card>
      </div>

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

function Info({
  label,
  value,
  mono,
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <div className="flex justify-between gap-4 border-b border-neutral-100 py-1 last:border-0">
      <span className="text-xs text-neutral-500">{label}</span>
      <span className={mono ? "font-mono text-xs" : "text-xs"}>{value}</span>
    </div>
  );
}
