'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getScheduled, createScheduled, toggleActive, deleteScheduled } from '@/lib/api/scheduled-txns'
import type { ScheduledTransactionRequest, TransactionType } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Plus } from 'lucide-react'
import { fmtVnd, fmtDate } from '@/lib/format-currency-and-date'

const emptyForm = (): ScheduledTransactionRequest => ({
  accountId: 0,
  type: 'TRANSFER',
  amount: 0,
  cronExpression: '0 9 * * 1',
  description: '',
})

export default function ScheduledPage() {
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<ScheduledTransactionRequest>(emptyForm())

  const qc = useQueryClient()
  const { data, isLoading, error } = useQuery({
    queryKey: ['scheduled', page],
    queryFn: () => getScheduled(page),
  })

  const create = useMutation({
    mutationFn: createScheduled,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['scheduled'] })
      setOpen(false)
      setForm(emptyForm())
    },
  })

  const toggle = useMutation({
    mutationFn: ({ uuid, active }: { uuid: string; active: boolean }) => toggleActive(uuid, active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['scheduled'] }),
  })

  const del = useMutation({
    mutationFn: deleteScheduled,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['scheduled'] }),
  })

  return (
    <div className="space-y-4 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Scheduled Transactions</h1>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger className="inline-flex h-9 items-center justify-center gap-1 rounded-md bg-primary px-3 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4" />Create
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New Scheduled Transaction</DialogTitle>
            </DialogHeader>
            <form
              onSubmit={e => {
                e.preventDefault()
                create.mutate(form)
              }}
              className="space-y-3 pt-2"
            >
              <div className="space-y-1">
                <Label>Account ID</Label>
                <Input
                  type="number"
                  value={form.accountId || ''}
                  onChange={e => setForm(f => ({ ...f, accountId: Number(e.target.value) }))}
                  required
                />
              </div>
              <div className="space-y-1">
                <Label>Type</Label>
                <Select
                  value={form.type}
                  onValueChange={v => setForm(f => ({ ...f, type: v as TransactionType }))}
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DEPOSIT">DEPOSIT</SelectItem>
                    <SelectItem value="WITHDRAWAL">WITHDRAWAL</SelectItem>
                    <SelectItem value="TRANSFER">TRANSFER</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {(form.type === 'DEPOSIT' || form.type === 'TRANSFER') && (
                <div className="space-y-1">
                  <Label>To Account ID</Label>
                  <Input
                    type="number"
                    value={form.toAccountId ?? ''}
                    onChange={e => setForm(f => ({ ...f, toAccountId: Number(e.target.value) || undefined }))}
                  />
                </div>
              )}
              <div className="space-y-1">
                <Label>Amount (VND)</Label>
                <Input
                  type="number"
                  value={form.amount || ''}
                  onChange={e => setForm(f => ({ ...f, amount: Number(e.target.value) }))}
                  required
                />
              </div>
              <div className="space-y-1">
                <Label>Cron Expression</Label>
                <Input
                  value={form.cronExpression}
                  onChange={e => setForm(f => ({ ...f, cronExpression: e.target.value }))}
                  placeholder="0 9 * * 1"
                  required
                />
              </div>
              <div className="space-y-1">
                <Label>Description</Label>
                <Input
                  value={form.description ?? ''}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                />
              </div>
              {create.error && <p className="text-sm text-red-500">{(create.error as Error).message}</p>}
              <Button type="submit" className="w-full" disabled={create.isPending}>
                {create.isPending ? 'Creating...' : 'Create'}
              </Button>
            </form>
          </DialogContent>
        </Dialog>
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
                <TableHead>Cron</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Active</TableHead>
                <TableHead>Next Run</TableHead>
                <TableHead>Last Run</TableHead>
                <TableHead></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map(s => (
                <TableRow key={s.uuid}>
                  <TableCell className="font-mono text-xs">{s.uuid.slice(0, 8)}</TableCell>
                  <TableCell>{s.type}</TableCell>
                  <TableCell>{fmtVnd(s.amount)}</TableCell>
                  <TableCell className="font-mono text-xs">{s.cronExpression}</TableCell>
                  <TableCell className="max-w-32 truncate text-sm text-muted-foreground">{s.description ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant={s.active ? 'default' : 'outline'}>{s.active ? 'Active' : 'Inactive'}</Badge>
                  </TableCell>
                  <TableCell>{fmtDate(s.nextRunAt)}</TableCell>
                  <TableCell>{fmtDate(s.lastRunAt)}</TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={toggle.isPending}
                        onClick={() => toggle.mutate({ uuid: s.uuid, active: !s.active })}
                      >
                        {s.active ? 'Disable' : 'Enable'}
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        disabled={del.isPending}
                        onClick={() => del.mutate(s.uuid)}
                      >
                        Delete
                      </Button>
                    </div>
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
    </div>
  )
}
