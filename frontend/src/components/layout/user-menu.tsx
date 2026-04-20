"use client";

import { useRouter } from "next/navigation";
import { LogOut, ShieldCheck, User as UserIcon } from "lucide-react";
import { useAuth } from "@/lib/auth/auth-context";
import { buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";

export function UserMenu() {
  const { user, signOut } = useAuth();
  const router = useRouter();

  if (!user) return null;

  const roleLabel = user.roles.includes("ADMIN")
    ? "Quản trị viên"
    : user.roles.includes("FRAUD")
      ? "Chuyên viên gian lận"
      : "Khách hàng";

  const onSignOut = async () => {
    await signOut();
    router.replace("/login");
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className={buttonVariants({ variant: "ghost", className: "gap-2 px-2" })}
        aria-label="Menu người dùng"
      >
        <UserIcon className="h-4 w-4" />
        <span className="text-sm">{user.username}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-neutral-500" />
            <div>
              <div className="text-sm font-medium">{user.username}</div>
              <Badge variant="secondary" className="mt-1 text-[10px]">
                {roleLabel}
              </Badge>
            </div>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={onSignOut} className="text-red-600">
          <LogOut className="mr-2 h-4 w-4" /> Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
