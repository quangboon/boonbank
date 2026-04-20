import { Badge } from "@/components/ui/badge";
import type { TransactionStatus, TransactionType } from "@/types/domain";

const TYPE_LABEL: Record<TransactionType, string> = {
  TRANSFER: "Chuyển khoản",
  WITHDRAW: "Rút tiền",
  DEPOSIT: "Nạp tiền",
};

const STATUS_LABEL: Record<TransactionStatus, string> = {
  PENDING: "Đang xử lý",
  COMPLETED: "Thành công",
  FAILED: "Thất bại",
  REVERSED: "Đã hoàn",
};

const STATUS_TONE: Record<TransactionStatus, string> = {
  PENDING: "bg-amber-100 text-amber-700 border-amber-200",
  COMPLETED: "bg-green-100 text-green-700 border-green-200",
  FAILED: "bg-red-100 text-red-700 border-red-200",
  REVERSED: "bg-neutral-100 text-neutral-700 border-neutral-200",
};

export function TransactionTypeBadge({ type }: { type: TransactionType }) {
  return (
    <Badge variant="secondary" className="text-xs">
      {TYPE_LABEL[type]}
    </Badge>
  );
}

export function TransactionStatusBadge({
  status,
}: {
  status: TransactionStatus;
}) {
  return (
    <Badge
      variant="secondary"
      className={`border text-xs ${STATUS_TONE[status]}`}
    >
      {STATUS_LABEL[status]}
    </Badge>
  );
}
