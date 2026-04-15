'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getAlerts, reviewAlert } from '@/lib/api/fraud-alerts'
import type { FraudAlertStatus } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { fmtDate } from '@/lib/format-currency-and-date'

const statusVariant: Record<FraudAlertStatus, 'default' | 'secondary' | 'outline'> = {
  PENDING: 'default',
  REVIEWED: 'secondary',
  DISMISSED: 'outline',
}

// yellow-ish for pending via className override
const statusClass: Record<FraudAlertStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  REVIEWED: 'bg-green-100 text-green-800 border-green-200',
  DISMISSED: '',
}

type FilterTab = 'ALL' | FraudAlertStatus

export default function FraudAlertsPage() {
  const [filter, setFilter] = useState<FilterTab>('ALL')
  const [page, setPage] = useState(0)
  const [reviewId, setReviewId] = useState<number | null>(null)
  const [reviewedBy, setReviewedBy] = useState('admin')

  const qc = useQueryClient()

  const statusParam = filter === 'ALL' ? undefined : filter as FraudAlertStatus

  const { data, isLoading, error } = useQuery({
    queryKey: ['fraud-alerts', filter, page],
    queryFn: () => getAlerts(statusParam, page),
  })

  const review = useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'REVIEWED' | 'DISMISSED' }) =>
      reviewAlert(id, status, reviewedBy),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['fraud-alerts'] })
      setReviewId(null)
    },
  })

  const handleFilterChange = (v: string | number) => {
    setFilter(v as FilterTab)
    setPage(0)
  }

  return (
    <div className="space-y-4 p-6">
      <h1 className="text-2xl font-semibold">Fraud Alerts</h1>

      <Tabs value={filter} onValueChange={handleFilterChange}>
        <TabsList>
          <TabsTrigger value="ALL">All</TabsTrigger>
          <TabsTrigger value="PENDING">Pending</TabsTrigger>
          <TabsTrigger value="REVIEWED">Reviewed</TabsTrigger>
          <TabsTrigger value="DISMISSED">Dismissed</TabsTrigger>
        </TabsList>

        <TabsContent value={filter} className="pt-4">
          {isLoading && <p className="text-muted-foreground">Loading...</p>}
          {error && <p className="text-red-500">{(error as Error).message}</p>}

          {data && (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>ID</TableHead>
                    <TableHead>Transaction ID</TableHead>
                    <TableHead>Rule</TableHead>
                    <TableHead>Reason</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Reviewed By</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map(alert => (
                    <TableRow key={alert.id}>
                      <TableCell className="font-mono text-xs">#{alert.id}</TableCell>
                      <TableCell>#{alert.transactionId}</TableCell>
                      <TableCell className="font-medium">{alert.ruleName}</TableCell>
                      <TableCell className="max-w-48 truncate text-sm text-muted-foreground">{alert.reason}</TableCell>
                      <TableCell>
                        <Badge variant={statusVariant[alert.status]} className={statusClass[alert.status]}>
                          {alert.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{alert.reviewedBy ?? '—'}</TableCell>
                      <TableCell>{fmtDate(alert.createdAt)}</TableCell>
                      <TableCell>
                        {alert.status === 'PENDING' && (
                          <div className="flex gap-1">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => {
                                setReviewId(alert.id)
                              }}
                            >
                              Review
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={review.isPending}
                              onClick={() => review.mutate({ id: alert.id, status: 'DISMISSED' })}
                            >
                              Dismiss
                            </Button>
                          </div>
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
        </TabsContent>
      </Tabs>

      <Dialog open={reviewId !== null} onOpenChange={o => !o && setReviewId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Review Alert #{reviewId}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 pt-2">
            <div className="space-y-1">
              <Label>Reviewed By</Label>
              <Input value={reviewedBy} onChange={e => setReviewedBy(e.target.value)} />
            </div>
            {review.error && <p className="text-sm text-red-500">{(review.error as Error).message}</p>}
            <Button
              className="w-full"
              disabled={review.isPending}
              onClick={() => reviewId !== null && review.mutate({ id: reviewId, status: 'REVIEWED' })}
            >
              {review.isPending ? 'Saving...' : 'Mark as Reviewed'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
