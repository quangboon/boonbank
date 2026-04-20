"use client";

import type { ReactNode } from "react";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";
import { AppShell } from "@/components/layout/app-shell";
import { adminNav } from "@/components/layout/sidebar-nav";

export default function AdminLayout({ children }: { children: ReactNode }) {
  const { user, isAuthed, isAdmin } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthed && !isAdmin && !user?.roles.includes("FRAUD")) {
      router.replace("/forbidden");
    }
  }, [isAuthed, isAdmin, user, router]);

  if (!isAuthed) return null;

  return <AppShell nav={adminNav}>{children}</AppShell>;
}
