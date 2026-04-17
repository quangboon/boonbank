'use client'

import { useState, useEffect, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getTransactions, searchTransactions, execute } from '@/lib/api/transactions'
import type { TxnSearchParams } from '@/lib/api/transactions'
import { getMyAccounts, lookupAccount } from '@/lib/api/accounts'
import { useAuthStore } from '@/lib/stores/auth-store'
import type { TransactionRequest, TransactionType } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { fmtVnd, fmtDate } from '@/lib/format-currency-and-date'

const typeVariant: Record<TransactionType, 'default' | 'destructive' | 'secondary'> = {
  DEPOSIT: 'default',
  WITHDRAWAL: 'destructive',
  TRANSFER: 'secondary',
}

const emptyForm = (): TransactionRequest => ({
  type: 'TRANSFER',
  amount: 0,
  description: '',
  toAccountNumber: '',
})

export default function TransactionsPage() {
  const [page, setPage] = useState(0)
  const [form, setForm] = useState<TransactionRequest>(emptyForm())
  const [tab, setTab] = useState<'history' | 'new'>('history')
  const [recipientName, setRecipientName] = useState<string | null>(null)
  const [lookupError, setLookupError] = useState<string | null>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(null)
  const [filters, setFilters] = useState<TxnSearchParams>({})
  const hasFilters = !!(filters.type || filters.amountMin != null || filters.amountMax != null || filters.from || filters.to)

  const role = useAuthStore(s => s.role)
  const isAdmin = role === 'ADMIN'

  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({
    queryKey: hasFilters ? ['txn-search', filters, page] : ['transactions', page],
    queryFn: () => hasFilters ? searchTransactions({ ...filters, page }) : getTransactions(page),
  })

  const { data: myAccounts } = useQuery({
    queryKey: ['my-accounts'],
    queryFn: getMyAccounts,
  })

  const submit = useMutation({
    mutationFn: execute,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['transactions'] })
      qc.invalidateQueries({ queryKey: ['my-accounts'] })
      setForm(emptyForm())
      setRecipientName(null)
      setTab('history')
    },
  })

  const activeAccounts = myAccounts?.content.filter(a => a.status === 'ACTIVE') ?? []

  // debounced lookup khi nhap so tai khoan
  useEffect(() => {
    const num = form.toAccountNumber?.trim()
    if (!num || num.length < 5) {
      setRecipientName(null)
      setLookupError(null)
      return
    }

    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(async () => {
      try {
        const acct = await lookupAccount(num)
        setRecipientName(acct.customerName)
        setLookupError(null)
      } catch {
        setRecipientName(null)
        setLookupError('Account not found')
      }
    }, 500)

    return () => { if (debounceRef.current) clearTimeout(debounceRef.current) }
  }, [form.toAccountNumber, activeAccounts])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    submit.mutate(form)
  }

  const txnTypes: TransactionType[] = isAdmin
    ? ['DEPOSIT', 'WITHDRAWAL', 'TRANSFER']
    : ['TRANSFER']

  const showFrom = form.type === 'WITHDRAWAL' || form.type === 'TRANSFER'
  const showTo = form.type === 'DEPOSIT' || form.type === 'TRANSFER'

  const handleFromChange = (v: string) => {
    setForm(f => ({
      ...f,
      fromAccountId: Number(v),
    }))
  }

  return (
    <div className="space-y-4 p-6">
      <h1 className="text-2xl font-semibold">Transactions</h1>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="history">History</TabsTrigger>
          <TabsTrigger value="new">New Transaction</TabsTrigger>
        </TabsList>

        <TabsContent value="history" className="pt-4">
          <div className="flex flex-wrap items-end gap-3 pb-4">
            <div className="space-y-1">
              <Label className="text-xs">Type</Label>
              <Select
                value={filters.type ?? 'ALL'}
                onValueChange={v => setFilters(f => ({ ...f, type: v === 'ALL' ? undefined : v as TransactionType }))}
              >
                <SelectTrigger className="w-[140px]"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All</SelectItem>
                  <SelectItem value="DEPOSIT">Deposit</SelectItem>
                  <SelectItem value="WITHDRAWAL">Withdrawal</SelectItem>
                  <SelectItem value="TRANSFER">Transfer</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Min Amount</Label>
              <Input className="w-[130px]" type="number" placeholder="0"
                value={filters.amountMin ?? ''}
                onChange={e => setFilters(f => ({ ...f, amountMin: e.target.value ? Number(e.target.value) : undefined }))}
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Max Amount</Label>
              <Input className="w-[130px]" type="number" placeholder="∞"
                value={filters.amountMax ?? ''}
                onChange={e => setFilters(f => ({ ...f, amountMax: e.target.value ? Number(e.target.value) : undefined }))}
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">From</Label>
              <Input className="w-[150px]" type="date"
                value={filters.from ?? ''}
                onChange={e => setFilters(f => ({ ...f, from: e.target.value || undefined }))}
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">To</Label>
              <Input className="w-[150px]" type="date"
                value={filters.to ?? ''}
                onChange={e => setFilters(f => ({ ...f, to: e.target.value || undefined }))}
              />
            </div>
            <Button size="sm" onClick={() => setPage(0)}>Search</Button>
            {hasFilters && (
              <Button size="sm" variant="outline" onClick={() => { setFilters({}); setPage(0) }}>Clear</Button>
            )}
          </div>

          {isLoading && <p className="text-muted-foreground">Loading...</p>}
          {error && <p className="text-red-500">{(error as Error).message}</p>}
          {data && (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>ID</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead>Fee</TableHead>
                    <TableHead>From</TableHead>
                    <TableHead>To</TableHead>
                    <TableHead>Date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map(t => (
                    <TableRow key={t.id}>
                      <TableCell className="font-mono text-xs text-muted-foreground">#{t.id}</TableCell>
                      <TableCell>
                        <Badge variant={typeVariant[t.type]}>{t.type}</Badge>
                      </TableCell>
                      <TableCell>{fmtVnd(t.amount)}</TableCell>
                      <TableCell>{fmtVnd(t.fee)}</TableCell>
                      <TableCell>{t.fromAccountId ?? '—'}</TableCell>
                      <TableCell>{t.toAccountId ?? '—'}</TableCell>
                      <TableCell>{fmtDate(t.createdAt)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-center gap-3 pt-2 text-sm">
                <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(p => p - 1)}>
                  Previous
                </Button>
                <span className="text-muted-foreground">Page {data.number + 1} of {data.totalPages}</span>
                <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(p => p + 1)}>
                  Next
                </Button>
              </div>
            </>
          )}
        </TabsContent>

        <TabsContent value="new" className="pt-4">
          <form onSubmit={handleSubmit} className="max-w-md space-y-4">
            {/* txn type */}
            <div className="space-y-1">
              <Label>Type</Label>
              {txnTypes.length === 1 ? (
                <Input value={txnTypes[0]} disabled />
              ) : (
                <Select
                  value={form.type}
                  onValueChange={v => setForm(f => ({ ...f, type: v as TransactionType, fromAccountId: undefined, toAccountId: undefined, toAccountNumber: '' }))}
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {txnTypes.map(t => (
                      <SelectItem key={t} value={t}>{t}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            {/* from account */}
            {showFrom && (
              <div className="space-y-1">
                <Label>From Account</Label>
                {activeAccounts.length > 0 ? (
                  <Select
                    value={form.fromAccountId?.toString() ?? ''}
                    onValueChange={handleFromChange}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select account">
                        {form.fromAccountId
                          ? (() => { const a = activeAccounts.find(a => a.id === form.fromAccountId); return a ? `${a.accountNumber} — ${fmtVnd(a.balance)}` : 'Select account' })()
                          : 'Select account'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {activeAccounts.map(a => (
                        <SelectItem key={a.id} value={a.id.toString()}>
                          {a.accountNumber} — {fmtVnd(a.balance)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : (
                  <p className="text-sm text-muted-foreground">No active accounts</p>
                )}
              </div>
            )}

            {/* to account */}
            {showTo && (
              <div className="space-y-1">
                <Label>To Account Number</Label>
                <Input
                  placeholder="Enter recipient account number"
                  value={form.toAccountNumber ?? ''}
                  onChange={e => {
                    setForm(f => ({ ...f, toAccountNumber: e.target.value, toAccountId: undefined }))
                    setRecipientName(null)
                    setLookupError(null)
                  }}
                  required
                />
                {recipientName && (
                  <p className="text-sm text-green-600">{recipientName}</p>
                )}
                {lookupError && (
                  <p className="text-sm text-red-500">{lookupError}</p>
                )}
              </div>
            )}

            {/* amount */}
            <div className="space-y-1">
              <Label>Amount (VND)</Label>
              <Input
                type="number"
                value={form.amount || ''}
                onChange={e => setForm(f => ({ ...f, amount: Number(e.target.value) }))}
                min="0.01"
                step="0.01"
                onKeyDown={e => { if (e.key === '-' || e.key === 'e') e.preventDefault() }}
                required
              />
            </div>

            {/* description */}
            <div className="space-y-1">
              <Label>Description</Label>
              <Input
                value={form.description ?? ''}
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              />
            </div>

            {submit.error && <p className="text-sm text-red-500">{(submit.error as Error).message}</p>}
            {submit.isSuccess && <p className="text-sm text-green-600">Transaction completed.</p>}

            <Button type="submit" disabled={submit.isPending || !recipientName || (showFrom && !isAdmin && activeAccounts.length === 0)}>
              {submit.isPending ? 'Processing...' : 'Execute'}
            </Button>
          </form>
        </TabsContent>
      </Tabs>
    </div>
  )
}
