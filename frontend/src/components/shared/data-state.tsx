import type { ReactNode } from "react";
import { AlertCircle, Inbox, Loader2 } from "lucide-react";

export function LoadingState({ label = "Đang tải..." }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 rounded-lg border border-neutral-200 bg-white p-6 text-sm text-neutral-500">
      <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({
  title = "Chưa có dữ liệu",
  description,
  action,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-neutral-300 bg-white p-10 text-center">
      <Inbox className="h-8 w-8 text-neutral-300" aria-hidden />
      <div>
        <p className="text-sm font-medium">{title}</p>
        {description ? (
          <p className="mt-1 text-xs text-neutral-500">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

export function ErrorState({ message }: { message?: string }) {
  return (
    <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      <AlertCircle className="h-4 w-4" aria-hidden />
      <span>{message ?? "Không tải được dữ liệu."}</span>
    </div>
  );
}
