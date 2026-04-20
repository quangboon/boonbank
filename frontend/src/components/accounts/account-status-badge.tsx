import { Badge } from "@/components/ui/badge";
import type { AccountStatus } from "@/types/domain";

const LABEL: Record<AccountStatus, string> = {
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Ngừng",
  FROZEN: "Đã khoá",
  CLOSED: "Đã đóng",
};

const TONE: Record<AccountStatus, string> = {
  ACTIVE: "bg-green-100 text-green-700 border-green-200",
  INACTIVE: "bg-neutral-100 text-neutral-700 border-neutral-200",
  FROZEN: "bg-amber-100 text-amber-700 border-amber-200",
  CLOSED: "bg-red-100 text-red-700 border-red-200",
};

export function AccountStatusBadge({ status }: { status: AccountStatus }) {
  return (
    <Badge className={`border ${TONE[status]} text-xs`} variant="secondary">
      {LABEL[status]}
    </Badge>
  );
}
