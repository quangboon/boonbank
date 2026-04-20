"use client";

import { useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader } from "@/components/shared/page-header";
import { KpiCard } from "@/components/dashboard/kpi-card";
import { BalanceTierChart } from "@/components/dashboard/balance-tier-chart";
import { LocationChart } from "@/components/dashboard/location-chart";
import { TransactionsTrendChart } from "@/components/dashboard/transactions-trend-chart";
import { useAccounts } from "@/lib/hooks/use-accounts";
import { useCustomers } from "@/lib/hooks/use-customers";
import { useOpenAlerts } from "@/lib/hooks/use-alerts";
import {
  useAccountsByBalanceTier,
  useCustomersByLocation,
  useTransactionsSummary,
} from "@/lib/hooks/use-statistics";
import { LoadingState } from "@/components/shared/data-state";

export default function AdminDashboardPage() {
  const { data: accountPage } = useAccounts({ size: 1 });
  const { data: customerPage } = useCustomers({ size: 1 });
  const { data: openAlerts } = useOpenAlerts();
  const { data: tierStats, isLoading: tierLoading } = useAccountsByBalanceTier();
  const { data: locStats, isLoading: locLoading } = useCustomersByLocation();

  const { from, to } = useMemo(() => {
    const now = new Date();
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    const start = new Date(now.getFullYear(), now.getMonth() - 5, 1);
    const fmt = (d: Date) => d.toISOString().slice(0, 10);
    return { from: fmt(start), to: fmt(end) };
  }, []);

  const { data: trend, isLoading: trendLoading } = useTransactionsSummary({
    period: "WEEK",
    from,
    to,
  });

  return (
    <div className="space-y-4">
      <PageHeader
        title="Tổng quan"
        description="Thống kê toàn hệ thống, tổng hợp mọi tài khoản."
      />

      <div className="grid gap-4 md:grid-cols-4">
        <KpiCard
          label="Tổng khách hàng"
          value={customerPage?.totalElements?.toLocaleString("vi-VN")}
        />
        <KpiCard
          label="Tổng tài khoản"
          value={accountPage?.totalElements?.toLocaleString("vi-VN")}
        />
        <KpiCard
          label="Cảnh báo đang mở"
          value={openAlerts?.length?.toLocaleString("vi-VN") ?? 0}
        />
        <KpiCard label="Kỳ thống kê" value="6 tuần gần nhất" hint="WEEK" />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Tài khoản theo mức số dư</CardTitle>
          </CardHeader>
          <CardContent>
            {tierLoading ? (
              <LoadingState />
            ) : (
              <BalanceTierChart data={tierStats ?? []} />
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Khách hàng theo địa điểm</CardTitle>
          </CardHeader>
          <CardContent>
            {locLoading ? (
              <LoadingState />
            ) : (
              <LocationChart data={locStats ?? []} />
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">
            Tổng giá trị giao dịch toàn hệ thống (theo tuần)
          </CardTitle>
        </CardHeader>
        <CardContent>
          {trendLoading ? (
            <LoadingState />
          ) : (
            <TransactionsTrendChart data={trend ?? []} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
