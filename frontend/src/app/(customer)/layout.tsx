"use client";

import type { ReactNode } from "react";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";
import { AppShell } from "@/components/layout/app-shell";
import { customerNav } from "@/components/layout/sidebar-nav";

export default function CustomerLayout({ children }: { children: ReactNode }) {
  const { isAuthed, isCustomer } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthed && !isCustomer) {
      router.replace("/forbidden");
    }
  }, [isAuthed, isCustomer, router]);

  if (!isAuthed) return null;

  return <AppShell nav={customerNav}>{children}</AppShell>;
}
