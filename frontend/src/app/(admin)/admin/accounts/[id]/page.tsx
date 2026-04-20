"use client";

import { useParams, useRouter } from "next/navigation";
import { Ban, Lock, LockOpen, Trash2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { PageHeader } from "@/components/shared/page-header";
import {
  ErrorState,
  LoadingState,
  EmptyState,
} from "@/components/shared/data-state";
import {
  useAccount,
  useAccountStatusHistory,
  useCloseAccount,
  useDeleteAccount,
  useFreezeAccount,
  useUnfreezeAccount,
} from "@/lib/hooks/use-accounts";
import { formatDate, formatMoney } from "@/lib/utils/format";
import { AccountStatusBadge } from "@/components/accounts/account-status-badge";
import { ReasonDialog } from "@/components/accounts/reason-dialog";
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

export default function AccountDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { data: account, isLoading, isError } = useAccount(params.id);
  const { data: history } = useAccountStatusHistory(params.id);
  const freeze = useFreezeAccount();
  const unfreeze = useUnfreezeAccount();
  const close = useCloseAccount();
  const remove = useDeleteAccount();

  if (isLoading) return <LoadingState />;
  if (isError || !account) return <ErrorState />;

  const canFreeze = account.status === "ACTIVE";
  const canUnfreeze = account.status === "FROZEN";
  const canClose =
    account.status === "ACTIVE" || account.status === "FROZEN";
  const canDelete = account.status === "CLOSED";

  return (
    <div className="space-y-4">
      <PageHeader
        title={account.accountNumber}
        description="Chi tiết tài khoản"
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-sm">Thông tin</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 text-sm md:grid-cols-2">
            <Info label="Số tài khoản" value={account.accountNumber} mono />
            <Info label="Loại" value={account.accountType} />
            <Info
              label="Trạng thái"
              value={<AccountStatusBadge status={account.status} />}
            />
            <Info
              label="Số dư"
              value={formatMoney(account.balance, account.currency)}
            />
            <Info
              label="Hạn mức giao dịch"
              value={formatMoney(account.transactionLimit, account.currency)}
            />
            <Info label="Tiền tệ" value={account.currency} />
            <Info label="Mở ngày" value={formatDate(account.openedAt)} />
            <Info label="Khách hàng" value={account.customerId} mono />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Thao tác</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <ReasonDialog
              title="Khoá tài khoản?"
              description="Tài khoản sẽ không thể thực hiện giao dịch cho đến khi mở khoá."
              confirmLabel="Khoá"
              disabled={!canFreeze || freeze.isPending}
              onConfirm={(reason) => freeze.mutate({ id: account.id, reason })}
              trigger={
                <Button variant="outline" disabled={!canFreeze} className="gap-1">
                  <Lock className="h-4 w-4" /> Khoá
                </Button>
              }
            />
            <ReasonDialog
              title="Mở khoá tài khoản?"
              description="Tài khoản sẽ giao dịch được trở lại."
              confirmLabel="Mở khoá"
              disabled={!canUnfreeze || unfreeze.isPending}
              onConfirm={(reason) =>
                unfreeze.mutate({ id: account.id, reason })
              }
              trigger={
                <Button
                  variant="outline"
                  disabled={!canUnfreeze}
                  className="gap-1"
                >
                  <LockOpen className="h-4 w-4" /> Mở khoá
                </Button>
              }
            />
            <ReasonDialog
              title="Đóng tài khoản?"
              description="Hành động này không thể hoàn tác. Số dư phải bằng 0."
              confirmLabel="Đóng"
              disabled={!canClose || close.isPending}
              onConfirm={(reason) => close.mutate({ id: account.id, reason })}
              trigger={
                <Button variant="outline" disabled={!canClose} className="gap-1">
                  <Ban className="h-4 w-4" /> Đóng
                </Button>
              }
            />
            <AlertDialog>
              <AlertDialogTrigger
                render={
                  <Button variant="destructive" className="gap-1" disabled={!canDelete}>
                    <Trash2 className="h-4 w-4" /> Xoá
                  </Button>
                }
              />
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Xoá tài khoản?</AlertDialogTitle>
                  <AlertDialogDescription>
                    Hành động này không thể hoàn tác. Chỉ có thể xoá tài khoản
                    đã đóng.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Huỷ</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={() =>
                      remove.mutate(account.id, {
                        onSuccess: () => router.push("/admin/accounts"),
                      })
                    }
                  >
                    Xoá
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Lịch sử trạng thái</CardTitle>
        </CardHeader>
        <CardContent>
          {!history || history.length === 0 ? (
            <EmptyState title="Chưa có lịch sử thay đổi" />
          ) : (
            <ol className="space-y-3">
              {history.map((h, idx) => (
                <li key={idx} className="flex gap-3 text-sm">
                  <div className="w-32 shrink-0 text-xs text-neutral-500">
                    {formatDate(h.createdAt)}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">
                        {h.fromStatus ?? "—"} → {h.toStatus}
                      </span>
                    </div>
                    <div className="text-xs text-neutral-500">
                      {h.reason} · bởi {h.createdBy}
                    </div>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </CardContent>
      </Card>
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
    <div>
      <div className="text-xs text-neutral-500">{label}</div>
      <div className={mono ? "font-mono text-xs" : ""}>{value}</div>
    </div>
  );
}
