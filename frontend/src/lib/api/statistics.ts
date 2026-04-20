import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type {
  AccountTierStats,
  BalanceTierStats,
  LocationStats,
  PeriodUnit,
  TransactionPeriodStats,
} from "@/types/domain";

export const statisticsApi = {
  accountsByProductType: () =>
    unwrap<AccountTierStats[]>(
      api.get(ENDPOINTS.statistics.accountsByProductType),
    ),
  accountsByBalanceTier: () =>
    unwrap<BalanceTierStats[]>(
      api.get(ENDPOINTS.statistics.accountsByBalanceTier),
    ),
  transactionsByBalanceTier: () =>
    unwrap<BalanceTierStats[]>(
      api.get(ENDPOINTS.statistics.transactionsByBalanceTier),
    ),
  customersByLocation: () =>
    unwrap<LocationStats[]>(api.get(ENDPOINTS.statistics.customersByLocation)),
  transactionsSummary: (params: {
    period: PeriodUnit;
    accountId?: string;
    from: string;
    to: string;
  }) =>
    unwrap<TransactionPeriodStats[]>(
      api.get(ENDPOINTS.reports.summary, { params }),
    ),
};
