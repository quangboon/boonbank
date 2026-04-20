"use client";

import Link from "next/link";
import { ArrowLeftRight, FileText } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { PageHeader } from "@/components/shared/page-header";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "@/components/shared/data-state";
import { AccountStatusBadge } from "@/components/accounts/account-status-badge";
import { useAccounts } from "@/lib/hooks/use-accounts";
import { ACCOUNT_TYPE_LABEL } from "@/lib/enums";
import { formatMoney, formatDate } from "@/lib/utils/format";
import { cn } from "@/lib/utils";

export default function MyAccountsPage() {
  const { data, isLoading, isError } = useAccounts({ size: 50 });
  const accounts = data?.content ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        title="Tài khoản của tôi"
        description="BE đã giới hạn kết quả theo chủ sở hữu hiện tại."
      />

      {isLoading ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState />
      ) : accounts.length === 0 ? (
        <EmptyState title="Chưa có tài khoản" />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {accounts.map((a) => (
            <Card key={a.id}>
              <CardContent className="space-y-3 p-5">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="font-mono text-xs text-neutral-500">
                      {a.accountNumber}
                    </div>
                    <div className="mt-0.5 text-sm font-medium">
                      {ACCOUNT_TYPE_LABEL[a.accountType]}
                    </div>
                  </div>
                  <AccountStatusBadge status={a.status} />
                </div>
                <div>
                  <div className="text-xs text-neutral-500">Số dư</div>
                  <div className="text-2xl font-semibold">
                    {formatMoney(a.balance, a.currency)}
                  </div>
                </div>
                <div className="text-xs text-neutral-500">
                  Mở ngày {formatDate(a.openedAt)}
                </div>
                <div className="flex gap-2 pt-1">
                  <Link
                    href={`/my/transfer?from=${encodeURIComponent(a.accountNumber)}`}
                    className={cn(
                      buttonVariants({ variant: "outline", size: "sm" }),
                      "gap-1",
                    )}
                  >
                    <ArrowLeftRight className="h-3.5 w-3.5" /> Chuyển
                  </Link>
                  <Link
                    href={`/my/statements?account=${encodeURIComponent(a.id)}`}
                    className={cn(
                      buttonVariants({ variant: "outline", size: "sm" }),
                      "gap-1",
                    )}
                  >
                    <FileText className="h-3.5 w-3.5" /> Sao kê
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
