'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getAccounts, createAccount, changeStatus, getStatusHistory } from '@/lib/api/accounts'
import type { AccountRequest, AccountStatus } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Plus } from 'lucide-react'
import { useAuthStore } from '@/lib/stores/auth-store'
import { fmtVnd, fmtDate } from '@/lib/format-currency-and-date'

const statusVariant: Record<AccountStatus, 'default' | 'destructive' | 'secondary' | 'outline'> = {
  ACTIVE: 'default',
  LOCKED: 'destructive',
  CLOSED: 'secondary',
}

export default function AccountsPage() {
  const { role } = useAuthStore()
  const isAdmin = role === 'ADMIN'
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [statusOpen, setStatusOpen] = useState<number | null>(null)
  const [form, setForm] = useState<AccountRequest & { accountNumber?: string; initialBalance?: number }>({
    customerId: 0,
    transactionLimit: 10000000,
  })
  const [statusForm, setStatusForm] = useState({ status: 'ACTIVE' as AccountStatus, reason: '' })
  const [historyId, setHistoryId] = useState<number | null>(null)

  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({
    queryKey: ['accounts', page],
    queryFn: () => getAccounts(page),
  })

  const create = useMutation({
    mutationFn: createAccount,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts'] })
      setCreateOpen(false)
      setForm({ customerId: 0, transactionLimit: 10000000 })
    },
  })

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ['status-history', historyId],
    queryFn: () => getStatusHistory(historyId!),
    enabled: !!historyId,
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, status, reason }: { id: number; status: AccountStatus; reason: string }) =>
      changeStatus(id, status, reason, 'admin'),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts'] })
      setStatusOpen(null)
      setStatusForm({ status: 'ACTIVE', reason: '' })
    },
  })

  return (
    <div className="space-y-4 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Accounts</h1>
        {isAdmin && <Dialog open={createOpen} onOpenChange={setCreateOpen}>
          <DialogTrigger className="inline-flex h-9 items-center justify-center gap-1 rounded-md bg-primary px-3 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4" />Create Account
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New Account</DialogTitle>
            </DialogHeader>
            <form
              onSubmit={e => {
                e.preventDefault()
                create.mutate({ customerId: form.customerId, transactionLimit: form.transactionLimit })
              }}
              className="space-y-3 pt-2"
            >
              <div className="space-y-1">
                <Label>Customer ID</Label>
                <Input
                  type="number"
                  value={form.customerId || ''}
                  onChange={e => setForm(f => ({ ...f, customerId: Number(e.target.value) }))}
                  required
                />
              </div>
              <div className="space-y-1">
                <Label>Transaction Limit (VND)</Label>
                <Input
                  type="number"
                  value={form.transactionLimit || ''}
                  onChange={e => setForm(f => ({ ...f, transactionLimit: Number(e.target.value) }))}
                />
              </div>
              {create.error && <p className="text-sm text-red-500">{(create.error as Error).message}</p>}
              <Button type="submit" className="w-full" disabled={create.isPending}>
                {create.isPending ? 'Creating...' : 'Create'}
              </Button>
            </form>
          </DialogContent>
        </Dialog>}
      </div>

      {isLoading && <p className="text-muted-foreground">Loading...</p>}
      {error && <p className="text-red-500">{(error as Error).message}</p>}

      {data && (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Account No.</TableHead>
                <TableHead>Customer ID</TableHead>
                <TableHead>Balance</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Limit</TableHead>
                <TableHead>Opened</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map(acc => (
                <TableRow key={acc.id}>
                  <TableCell className="font-mono text-sm">{acc.accountNumber}</TableCell>
                  <TableCell>{acc.customerId}</TableCell>
                  <TableCell>{fmtVnd(acc.balance)}</TableCell>
                  <TableCell>
                    <Badge variant={statusVariant[acc.status]}>{acc.status}</Badge>
                  </TableCell>
                  <TableCell>{fmtVnd(acc.transactionLimit)}</TableCell>
                  <TableCell>{fmtDate(acc.openedAt)}</TableCell>
                  <TableCell className="flex gap-1">
                    <Button variant="outline" size="sm" onClick={() => setHistoryId(acc.id)}>History</Button>
                    {isAdmin && (
                      <Dialog
                        open={statusOpen === acc.id}
                        onOpenChange={o => {
                          setStatusOpen(o ? acc.id : null)
                          if (o) setStatusForm({ status: acc.status, reason: '' })
                          else setStatusForm({ status: 'ACTIVE', reason: '' })
                        }}
                      >
                        <DialogTrigger className="inline-flex h-8 items-center justify-center rounded-md border border-input bg-background px-3 text-sm font-medium hover:bg-accent hover:text-accent-foreground">
                          Change Status
                        </DialogTrigger>
                        <DialogContent>
                          <DialogHeader>
                            <DialogTitle>Change Status — {acc.accountNumber}</DialogTitle>
                          </DialogHeader>
                          <div className="space-y-3 pt-2">
                            <div className="space-y-1">
                              <Label>New Status</Label>
                              <Select
                                value={statusForm.status}
                                onValueChange={v => setStatusForm(f => ({ ...f, status: v as AccountStatus }))}
                              >
                                <SelectTrigger><SelectValue /></SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="ACTIVE">ACTIVE</SelectItem>
                                  <SelectItem value="LOCKED">LOCKED</SelectItem>
                                  <SelectItem value="CLOSED">CLOSED</SelectItem>
                                </SelectContent>
                              </Select>
                            </div>
                            <div className="space-y-1">
                              <Label>Reason</Label>
                              <Input
                                value={statusForm.reason}
                                onChange={e => setStatusForm(f => ({ ...f, reason: e.target.value }))}
                                placeholder="Reason for status change"
                                required
                              />
                            </div>
                            {updateStatus.error && (
                              <p className="text-sm text-red-500">{(updateStatus.error as Error).message}</p>
                            )}
                            <Button
                              className="w-full"
                              disabled={updateStatus.isPending || !statusForm.reason}
                              onClick={() =>
                                updateStatus.mutate({ id: acc.id, status: statusForm.status, reason: statusForm.reason })
                              }
                            >
                              {updateStatus.isPending ? 'Saving...' : 'Save'}
                            </Button>
                          </div>
                        </DialogContent>
                      </Dialog>
                    )}
                  </TableCell>
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

      {/* Status History Dialog */}
      <Dialog open={!!historyId} onOpenChange={o => { if (!o) setHistoryId(null) }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>Status History</DialogTitle></DialogHeader>
          {historyLoading && <p className="text-muted-foreground">Loading...</p>}
          {history && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Old Status</TableHead>
                  <TableHead>New Status</TableHead>
                  <TableHead>Reason</TableHead>
                  <TableHead>Changed By</TableHead>
                  <TableHead>Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {history.content.map(h => (
                  <TableRow key={h.id}>
                    <TableCell>{h.oldStatus ?? '—'}</TableCell>
                    <TableCell>
                      <Badge variant={statusVariant[h.newStatus as AccountStatus] ?? 'secondary'}>{h.newStatus}</Badge>
                    </TableCell>
                    <TableCell>{h.reason ?? '—'}</TableCell>
                    <TableCell>{h.changedBy ?? '—'}</TableCell>
                    <TableCell>{fmtDate(h.changedAt)}</TableCell>
                  </TableRow>
                ))}
                {history.content.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground py-4">No status changes recorded</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
