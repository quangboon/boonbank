"use client";

import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function ForbiddenPage() {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-neutral-50 p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>403 — Không có quyền truy cập</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-neutral-600">
            Tài khoản của bạn không được phép mở trang này.
          </p>
          <div className="flex gap-2">
            <Link href="/" className={buttonVariants({ variant: "outline" })}>
              Về trang chủ
            </Link>
            <Link href="/login" className={buttonVariants({ variant: "default" })}>
              Đăng nhập lại
            </Link>
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
