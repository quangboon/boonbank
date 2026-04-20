import type { LucideIcon } from "lucide-react";
import {
  BarChart3,
  Bell,
  CreditCard,
  FileText,
  Home,
  Repeat,
  Settings,
  Users,
  Wallet,
  ArrowLeftRight,
  UserCircle,
} from "lucide-react";

export type NavItem = {
  label: string;
  href: string;
  icon: LucideIcon;
};

export const adminNav: NavItem[] = [
  { label: "Tổng quan", href: "/admin/dashboard", icon: Home },
  { label: "Khách hàng", href: "/admin/customers", icon: Users },
  { label: "Tài khoản", href: "/admin/accounts", icon: Wallet },
  { label: "Giao dịch", href: "/admin/transactions", icon: ArrowLeftRight },
  { label: "Giao dịch định kỳ", href: "/admin/recurring", icon: Repeat },
  { label: "Cảnh báo", href: "/admin/alerts", icon: Bell },
  { label: "Báo cáo", href: "/admin/reports", icon: FileText },
  { label: "Thống kê", href: "/admin/statistics", icon: BarChart3 },
  { label: "Người dùng", href: "/admin/users", icon: Settings },
];

export const customerNav: NavItem[] = [
  { label: "Tài khoản của tôi", href: "/my/accounts", icon: Wallet },
  { label: "Giao dịch", href: "/my/transactions", icon: ArrowLeftRight },
  { label: "Chuyển khoản", href: "/my/transfer", icon: ArrowLeftRight },
  { label: "Giao dịch định kỳ", href: "/my/recurring", icon: Repeat },
  { label: "Sao kê", href: "/my/statements", icon: FileText },
  { label: "Hồ sơ", href: "/my/profile", icon: UserCircle },
];

export { CreditCard };
