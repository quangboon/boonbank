"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader } from "@/components/shared/page-header";
import { BalanceTierChart } from "@/components/dashboard/balance-tier-chart";
import { LocationChart } from "@/components/dashboard/location-chart";
import {
  useAccountsByBalanceTier,
  useCustomersByLocation,
} from "@/lib/hooks/use-statistics";
import { LoadingState } from "@/components/shared/data-state";

export default function StatisticsPage() {
  const tier = useAccountsByBalanceTier();
  const loc = useCustomersByLocation();

  return (
    <div className="space-y-4">
      <PageHeader title="Thống kê" />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Tài khoản theo mức số dư</CardTitle>
          </CardHeader>
          <CardContent>
            {tier.isLoading ? (
              <LoadingState />
            ) : (
              <BalanceTierChart data={tier.data ?? []} />
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Khách hàng theo địa điểm</CardTitle>
          </CardHeader>
          <CardContent>
            {loc.isLoading ? (
              <LoadingState />
            ) : (
              <LocationChart data={loc.data ?? []} />
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
