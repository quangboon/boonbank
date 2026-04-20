import { Hammer } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function ComingSoon({
  title,
  phase,
  description,
}: {
  title: string;
  phase: string;
  description?: string;
}) {
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold">{title}</h2>
        <p className="text-sm text-neutral-500">
          Tính năng sẽ có ở {phase}.
        </p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-sm">
            <Hammer className="h-4 w-4 text-neutral-400" /> Đang phát triển
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-neutral-500">
          {description ??
            "Trang này là placeholder để tránh 404 khi duyệt sidebar. Logic thực tế sẽ được triển khai theo plan."}
        </CardContent>
      </Card>
    </div>
  );
}
