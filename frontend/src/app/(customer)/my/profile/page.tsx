"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/shared/page-header";
import { useAuth } from "@/lib/auth/auth-context";
import { formatDate } from "@/lib/utils/format";

export default function MyProfilePage() {
  const { user } = useAuth();
  if (!user) return null;
  const expiresAt = new Date(user.exp * 1000).toISOString();

  return (
    <div className="space-y-4">
      <PageHeader
        title="Hồ sơ"
        description="Thông tin phiên đăng nhập hiện tại."
      />
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Thông tin</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 text-sm md:grid-cols-2">
          <Info label="Tên đăng nhập" value={user.username} />
          <Info
            label="Vai trò"
            value={
              <div className="flex gap-1">
                {user.roles.map((r) => (
                  <Badge key={r} variant="secondary" className="text-xs">
                    {r}
                  </Badge>
                ))}
              </div>
            }
          />
          <Info label="Phiên hết hạn" value={formatDate(expiresAt)} />
          <Info
            label="Mã KH (nếu có)"
            value={user.customerId ?? "—"}
            mono
          />
        </CardContent>
      </Card>
      <p className="text-xs text-neutral-500">
        Đổi mật khẩu sẽ được bổ sung ở iteration sau (BE endpoint chưa sẵn).
      </p>
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
