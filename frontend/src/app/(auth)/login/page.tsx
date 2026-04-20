"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth/auth-context";
import { extractFieldErrors, extractMessage } from "@/lib/api/error-utils";
import { safeInternalPath } from "@/lib/utils/safe-redirect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const schema = z.object({
  username: z.string().min(3, "Tối thiểu 3 ký tự"),
  password: z.string().min(6, "Tối thiểu 6 ký tự"),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginPageInner />
    </Suspense>
  );
}

function LoginPageInner() {
  const router = useRouter();
  const params = useSearchParams();
  const { signIn } = useAuth();
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", password: "" },
  });

  const onSubmit = async (values: FormValues) => {
    setSubmitting(true);
    try {
      const user = await signIn(values.username, values.password);
      toast.success(`Chào ${user.username}`);
      const fallback =
        user.roles.includes("ADMIN") || user.roles.includes("FRAUD")
          ? "/admin/dashboard"
          : "/my/accounts";
      const target = safeInternalPath(params.get("next")) ?? fallback;
      router.replace(target);
    } catch (e) {
      const fieldErrors = extractFieldErrors(e);
      const unknownFieldMessages: string[] = [];
      for (const fe of fieldErrors) {
        if (fe.field === "username" || fe.field === "password") {
          setError(fe.field, { message: fe.message });
        } else {
          unknownFieldMessages.push(fe.message);
        }
      }
      toast.error(
        unknownFieldMessages.length > 0
          ? unknownFieldMessages.join("; ")
          : extractMessage(e, "Đăng nhập thất bại"),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-dvh items-center justify-center bg-neutral-50 p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">Đăng nhập</CardTitle>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-4"
            noValidate
          >
            <div className="space-y-1.5">
              <Label htmlFor="username">Tài khoản</Label>
              <Input
                id="username"
                autoComplete="username"
                autoFocus
                placeholder="admin"
                {...register("username")}
              />
              {errors.username && (
                <p className="text-xs text-red-600">{errors.username.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password">Mật khẩu</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                {...register("password")}
              />
              {errors.password && (
                <p className="text-xs text-red-600">{errors.password.message}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
            </Button>
          </form>

          <p className="mt-6 text-center text-xs text-neutral-500">
            Nội bộ ngân hàng
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
