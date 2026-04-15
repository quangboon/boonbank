'use client'

import { useState } from 'react'
import { downloadExcel, downloadPdf } from '@/lib/api/reports'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FileSpreadsheet, FileText } from 'lucide-react'

export default function ReportsPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [downloading, setDownloading] = useState<'excel' | 'pdf' | null>(null)
  const [err, setErr] = useState<string | null>(null)

  const triggerDownload = (blob: Blob, filename: string) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleExcel = async () => {
    if (!from || !to) return
    setDownloading('excel')
    setErr(null)
    try {
      const blob = await downloadExcel(from, to)
      triggerDownload(blob, `transactions_${from}_${to}.xlsx`)
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setDownloading(null)
    }
  }

  const handlePdf = async () => {
    if (!from || !to) return
    setDownloading('pdf')
    setErr(null)
    try {
      const blob = await downloadPdf(from, to)
      triggerDownload(blob, `transactions_${from}_${to}.pdf`)
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setDownloading(null)
    }
  }

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Reports</h1>

      <div className="max-w-sm space-y-4">
        <div className="space-y-1">
          <Label>From</Label>
          <Input type="date" value={from} onChange={e => setFrom(e.target.value)} />
        </div>
        <div className="space-y-1">
          <Label>To</Label>
          <Input type="date" value={to} onChange={e => setTo(e.target.value)} />
        </div>

        {err && <p className="text-sm text-red-500">{err}</p>}

        <div className="flex gap-3">
          <Button
            variant="outline"
            disabled={!from || !to || downloading !== null}
            onClick={handleExcel}
          >
            <FileSpreadsheet className="mr-2 h-4 w-4" />
            {downloading === 'excel' ? 'Downloading...' : 'Download Excel'}
          </Button>
          <Button
            variant="outline"
            disabled={!from || !to || downloading !== null}
            onClick={handlePdf}
          >
            <FileText className="mr-2 h-4 w-4" />
            {downloading === 'pdf' ? 'Downloading...' : 'Download PDF'}
          </Button>
        </div>
      </div>
    </div>
  )
}
