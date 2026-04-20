"use client";

import type { ReactNode } from "react";
import { UserMenu } from "./user-menu";

export function Header({ title, actions }: { title?: string; actions?: ReactNode }) {
  return (
    <header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-neutral-200 bg-white px-4">
      <h1 className="text-sm font-medium text-neutral-700">{title ?? ""}</h1>
      <div className="flex items-center gap-2">
        {actions}
        <UserMenu />
      </div>
    </header>
  );
}
