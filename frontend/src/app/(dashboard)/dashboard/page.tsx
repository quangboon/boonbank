"use client"

import { useQuery } from "@tanstack/react-query"
import { Users, Wallet, ArrowLeftRight, ArrowDownLeft, ArrowUpRight, Banknote } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { getBalanceTiers, getCustomersByLocation } from "@/lib/api/statistics"
import { getMyAccounts } from "@/lib/api/accounts"
import { getTransactions } from "@/lib/api/transactions"
import { useAuthStore } from "@/lib/stores/auth-store"
import type { Transaction } from "@/types"
import { fmtVnd, fmtDate } from "@/lib/format-currency-and-date"

function StatCard({ label, value, icon: Icon, color }: {
  label: string; value: string; icon: typeof Wallet; color: string
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between text-sm font-medium text-slate-600">
          {label}
          <Icon className={`size-4 ${color}`} />
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-bold text-slate-900">{value}</p>
      </CardContent>
    </Card>
  )
}

function CardSkeleton() {
  return (
    <Card>
      <CardHeader><Skeleton className="h-4 w-28" /></CardHeader>
      <CardContent><Skeleton className="h-8 w-20" /></CardContent>
    </Card>
  )
}

function TableSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div className="flex flex-col gap-2 px-4 pb-4">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-9 w-full" />
      ))}
    </div>
  )
}

// ---------- CUSTOMER DASHBOARD ----------
function CustomerDashboard() {
  const { data: accounts, isLoading: acctLoading } = useQuery({
    queryKey: ["my-accounts"],
    queryFn: getMyAccounts,
  })

  const { data: txns, isLoading: txnLoading } = useQuery({
    queryKey: ["transactions", 0],
    queryFn: () => getTransactions(0),
  })

  const activeAccts = accounts?.content.filter(a => a.status === "ACTIVE") ?? []
  const totalBalance = activeAccts.reduce((sum, a) => sum + a.balance, 0)
  const myAcctIds = new Set(activeAccts.map(a => a.id))

  const recentTxns = txns?.content ?? []
  let totalIn = 0
  let totalOut = 0
  for (const t of recentTxns) {
    if (t.toAccountId && myAcctIds.has(t.toAccountId)) totalIn += t.amount
    if (t.fromAccountId && myAcctIds.has(t.fromAccountId)) totalOut += t.amount + t.fee
  }

  const loading = acctLoading || txnLoading

  return (
    <>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {loading ? (
          <>
            <CardSkeleton /><CardSkeleton /><CardSkeleton /><CardSkeleton />
          </>
        ) : (
          <>
            <StatCard label="Total Balance" value={fmtVnd(totalBalance)} icon={Banknote} color="text-emerald-600" />
            <StatCard label="Accounts" value={String(activeAccts.length)} icon={Wallet} color="text-blue-600" />
            <StatCard label="Money In" value={fmtVnd(totalIn)} icon={ArrowDownLeft} color="text-green-600" />
            <StatCard label="Money Out" value={fmtVnd(totalOut)} icon={ArrowUpRight} color="text-red-500" />
          </>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {/* My accounts */}
        <Card>
          <CardHeader><CardTitle>My Accounts</CardTitle></CardHeader>
          {acctLoading ? <TableSkeleton /> : (
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-4">Account Number</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right pr-4">Balance</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(accounts?.content ?? []).map(a => (
                    <TableRow key={a.id}>
                      <TableCell className="pl-4 font-mono text-sm">{a.accountNumber}</TableCell>
                      <TableCell>
                        <Badge variant={a.status === "ACTIVE" ? "default" : "destructive"}>{a.status}</Badge>
                      </TableCell>
                      <TableCell className="text-right pr-4 font-medium">{fmtVnd(a.balance)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          )}
        </Card>

        {/* Recent transactions */}
        <Card>
          <CardHeader><CardTitle>Recent Transactions</CardTitle></CardHeader>
          {txnLoading ? <TableSkeleton /> : (
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-4">Type</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead className="text-right pr-4">Date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {recentTxns.slice(0, 10).map(t => (
                    <TableRow key={t.id}>
                      <TableCell className="pl-4">
                        <Badge variant={t.type === "DEPOSIT" ? "default" : t.type === "WITHDRAWAL" ? "destructive" : "secondary"}>
                          {t.type}
                        </Badge>
                      </TableCell>
                      <TableCell className="font-medium">{fmtVnd(t.amount)}</TableCell>
                      <TableCell className="text-right pr-4 text-slate-500 text-sm">{fmtDate(t.createdAt)}</TableCell>
                    </TableRow>
                  ))}
                  {!recentTxns.length && (
                    <TableRow>
                      <TableCell colSpan={3} className="text-center text-slate-400 py-6">No transactions</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          )}
        </Card>
      </div>
    </>
  )
}

// ---------- ADMIN DASHBOARD ----------
function AdminDashboard() {
  const { data: tiers, isLoading: tiersLoading } = useQuery({
    queryKey: ["balance-tiers"],
    queryFn: getBalanceTiers,
  })

  const { data: locations, isLoading: locationsLoading } = useQuery({
    queryKey: ["customers-by-location"],
    queryFn: getCustomersByLocation,
  })

  return (
    <>
      <div className="grid grid-cols-2 gap-4">
        <Card>
          <CardHeader><CardTitle>Balance Tiers</CardTitle></CardHeader>
          {tiersLoading ? <TableSkeleton /> : (
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-4">Tier</TableHead>
                    <TableHead>Accounts</TableHead>
                    <TableHead className="text-right pr-4">Transactions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {tiers?.map(tier => (
                    <TableRow key={tier.tier}>
                      <TableCell className="pl-4 font-medium">{tier.tier}</TableCell>
                      <TableCell>{tier.accountCount.toLocaleString()}</TableCell>
                      <TableCell className="text-right pr-4">{tier.transactionCount.toLocaleString()}</TableCell>
                    </TableRow>
                  ))}
                  {!tiers?.length && (
                    <TableRow>
                      <TableCell colSpan={3} className="text-center text-slate-400 py-6">No data</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          )}
        </Card>

        <Card>
          <CardHeader><CardTitle>Customers by Location</CardTitle></CardHeader>
          {locationsLoading ? <TableSkeleton /> : (
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="pl-4">Location</TableHead>
                    <TableHead className="text-right pr-4">Customers</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {locations?.map(loc => (
                    <TableRow key={loc.location}>
                      <TableCell className="pl-4 font-medium">{loc.location}</TableCell>
                      <TableCell className="text-right pr-4">{loc.customerCount.toLocaleString()}</TableCell>
                    </TableRow>
                  ))}
                  {!locations?.length && (
                    <TableRow>
                      <TableCell colSpan={2} className="text-center text-slate-400 py-6">No data</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          )}
        </Card>
      </div>
    </>
  )
}

// ---------- MAIN ----------
export default function DashboardPage() {
  const role = useAuthStore(s => s.role)
  const isAdmin = role === "ADMIN"

  return (
    <div className="p-6 flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          {isAdmin ? "Overview of banking operations" : "Your financial overview"}
        </p>
      </div>

      {isAdmin ? <AdminDashboard /> : <CustomerDashboard />}
    </div>
  )
}
