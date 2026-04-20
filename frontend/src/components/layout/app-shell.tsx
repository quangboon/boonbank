"use client";

import type { ReactNode } from "react";
import { Sidebar } from "./sidebar";
import { Header } from "./header";
import type { NavItem } from "./sidebar-nav";

export function AppShell({
  nav,
  title,
  children,
}: {
  nav: NavItem[];
  title?: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-dvh bg-neutral-50">
      <Sidebar items={nav} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header title={title} />
        <main className="flex-1 p-4 md:p-6">{children}</main>
      </div>
    </div>
  );
}
