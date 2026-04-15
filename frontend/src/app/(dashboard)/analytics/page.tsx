'use client'

import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getTxnStats } from '@/lib/api/analytics'
import type { AnalyticsPeriod } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { fmtVnd } from '@/lib/format-currency-and-date'
import {
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer
} from 'recharts'

function fmtDate(d: Date) {
  return d.toISOString().slice(0, 10)
}

export default function AnalyticsPage() {
  const today = fmtDate(new Date())
  const weekAgo = fmtDate(new Date(Date.now() - 7 * 86400_000))

  const [period, setPeriod] = useState<AnalyticsPeriod>('WEEK')
  const [from, setFrom] = useState(weekAgo)
  const [to, setTo] = useState(today)
  const [enabled, setEnabled] = useState(false)

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['analytics', period, from, to],
    queryFn: () => getTxnStats(period, from || undefined, to || undefined),
    enabled,
  })

  const handleSearch = () => {
    setEnabled(true)
    refetch()
  }

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Analytics</h1>

      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <Label>Period</Label>
          <Select value={period} onValueChange={v => setPeriod(v as AnalyticsPeriod)}>
            <SelectTrigger className="w-36"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="WEEK">Week</SelectItem>
              <SelectItem value="MONTH">Month</SelectItem>
              <SelectItem value="YEAR">Year</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label>From</Label>
          <Input type="date" value={from} onChange={e => setFrom(e.target.value)} className="w-40" />
        </div>
        <div className="space-y-1">
          <Label>To</Label>
          <Input type="date" value={to} onChange={e => setTo(e.target.value)} className="w-40" />
        </div>
        <Button onClick={handleSearch}>Search</Button>
      </div>

      {isLoading && <p className="text-muted-foreground">Loading...</p>}
      {error && <p className="text-red-500">{(error as Error).message}</p>}

      {data && data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Period</TableHead>
              <TableHead>Transactions</TableHead>
              <TableHead>Avg Amount</TableHead>
              <TableHead>Max Amount</TableHead>
              <TableHead>Min Amount</TableHead>
              <TableHead>Total Fees</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((row, i) => (
              <TableRow key={i}>
                <TableCell className="font-medium">{row.period}</TableCell>
                <TableCell>{row.txnCount.toLocaleString()}</TableCell>
                <TableCell>{fmtVnd(row.avgAmount)}</TableCell>
                <TableCell>{fmtVnd(row.maxAmount)}</TableCell>
                <TableCell>{fmtVnd(row.minAmount)}</TableCell>
                <TableCell>{fmtVnd(row.totalFees)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {data && data.length > 0 && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="rounded-lg border p-4">
            <h3 className="mb-3 text-sm font-medium text-muted-foreground">Transaction Count</h3>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" tick={{ fontSize: 12 }} />
                <YAxis />
                <Tooltip />
                <Bar dataKey="txnCount" fill="#3b82f6" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="rounded-lg border p-4">
            <h3 className="mb-3 text-sm font-medium text-muted-foreground">Amount Trends</h3>
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" tick={{ fontSize: 12 }} />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="avgAmount" stroke="#3b82f6" name="Avg" />
                <Line type="monotone" dataKey="maxAmount" stroke="#ef4444" name="Max" />
                <Line type="monotone" dataKey="minAmount" stroke="#22c55e" name="Min" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="rounded-lg border p-4 lg:col-span-2">
            <h3 className="mb-3 text-sm font-medium text-muted-foreground">Total Fees</h3>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" tick={{ fontSize: 12 }} />
                <YAxis />
                <Tooltip />
                <Bar dataKey="totalFees" fill="#f59e0b" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {data && data.length === 0 && (
        <p className="text-muted-foreground">No data for the selected range.</p>
      )}
    </div>
  )
}
