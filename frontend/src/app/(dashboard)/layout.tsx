"use client"

import { useEffect, useState } from "react"
import { useRouter, usePathname } from "next/navigation"
import Link from "next/link"
import {
  LayoutDashboard,
  Users,
  Wallet,
  ArrowLeftRight,
  Clock,
  ShieldAlert,
  BarChart3,
  FileDown,
  LogOut,
  Building2,
} from "lucide-react"
import { useAuthStore } from "@/lib/stores/auth-store"
import { cn } from "@/lib/utils"

type NavLink = { href: string; label: string; icon: typeof LayoutDashboard; adminOnly?: boolean }

const navLinks: NavLink[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/customers", label: "Customers", icon: Users, adminOnly: true },
  { href: "/accounts", label: "Accounts", icon: Wallet },
  { href: "/transactions", label: "Transactions", icon: ArrowLeftRight },
  { href: "/scheduled", label: "Scheduled", icon: Clock },
  { href: "/fraud-alerts", label: "Fraud Alerts", icon: ShieldAlert, adminOnly: true },
  { href: "/analytics", label: "Analytics", icon: BarChart3, adminOnly: true },
  { href: "/reports", label: "Reports", icon: FileDown, adminOnly: true },
]

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const { token, role, logout, username } = useAuthStore()
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    setHydrated(true)
  }, [])

  useEffect(() => {
    if (hydrated && !token) {
      router.replace("/login")
    }
  }, [hydrated, token, router])

  if (!hydrated || !token) return null

  function handleLogout() {
    logout()
    router.replace("/login")
  }

  return (
    <div className="flex h-screen bg-slate-50">
      {/* Sidebar */}
      <aside className="flex w-60 flex-col bg-slate-900 text-slate-100">
        <div className="flex items-center gap-2.5 px-5 py-5 border-b border-slate-700/50">
          <Building2 className="size-5 text-blue-400 shrink-0" />
          <span className="font-semibold text-white tracking-tight">Boon Bank</span>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <ul className="flex flex-col gap-0.5">
            {navLinks.filter(l => !l.adminOnly || role === 'ADMIN').map(({ href, label, icon: Icon }) => {
              const active = pathname === href || pathname.startsWith(href + "/")
              return (
                <li key={href}>
                  <Link
                    href={href}
                    className={cn(
                      "flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                      active
                        ? "bg-slate-700 text-white"
                        : "text-slate-400 hover:bg-slate-800 hover:text-slate-100"
                    )}
                  >
                    <Icon className="size-4 shrink-0" />
                    {label}
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>

        <div className="border-t border-slate-700/50 px-3 py-3">
          <div className="mb-1 px-3 py-1">
            <p className="text-xs text-slate-500">Logged in as</p>
            <p className="text-sm font-medium text-slate-300 truncate">{username}</p>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium text-slate-400 hover:bg-slate-800 hover:text-slate-100 transition-colors"
          >
            <LogOut className="size-4 shrink-0" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main */}
      <main className="flex flex-1 flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto">
          {children}
        </div>
      </main>
    </div>
  )
}
