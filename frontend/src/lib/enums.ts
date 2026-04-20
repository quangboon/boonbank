import type {
  AccountStatus,
  AccountType,
  AlertSeverity,
  PeriodUnit,
  TransactionStatus,
  TransactionType,
} from "@/types/domain";

export const ACCOUNT_STATUS_LABEL: Record<AccountStatus, string> = {
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Ngừng",
  FROZEN: "Đã khoá",
  CLOSED: "Đã đóng",
};

export const ACCOUNT_TYPE_LABEL: Record<AccountType, string> = {
  SAVINGS: "Tiết kiệm",
  CHECKING: "Thanh toán",
  LOAN: "Vay",
  CREDIT: "Tín dụng",
};

export const TRANSACTION_TYPE_LABEL: Record<TransactionType, string> = {
  TRANSFER: "Chuyển khoản",
  WITHDRAW: "Rút tiền",
  DEPOSIT: "Nạp tiền",
};

export const TRANSACTION_STATUS_LABEL: Record<TransactionStatus, string> = {
  PENDING: "Đang xử lý",
  COMPLETED: "Thành công",
  FAILED: "Thất bại",
  REVERSED: "Đã hoàn",
};

export const ALERT_SEVERITY_LABEL: Record<AlertSeverity, string> = {
  LOW: "Thấp",
  MEDIUM: "Trung bình",
  HIGH: "Cao",
  CRITICAL: "Nghiêm trọng",
};

export const PERIOD_UNIT_LABEL: Record<PeriodUnit, string> = {
  WEEK: "Theo tuần",
  QUARTER: "Theo quý",
  YEAR: "Theo năm",
};

export const CUSTOMER_TYPES: { code: string; label: string }[] = [
  { code: "INDIVIDUAL", label: "Cá nhân" },
  { code: "CORPORATE", label: "Doanh nghiệp" },
  { code: "VIP", label: "VIP" },
];

export const CUSTOMER_TYPE_LABEL: Record<string, string> = Object.fromEntries(
  CUSTOMER_TYPES.map((t) => [t.code, t.label]),
);
