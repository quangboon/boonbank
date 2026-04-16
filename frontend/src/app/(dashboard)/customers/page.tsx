'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getCustomers, searchCustomers, createCustomer, updateCustomer, deleteCustomer } from '@/lib/api/customers'
import type { CustomerSearchParams } from '@/lib/api/customers'
import type { CustomerRequest } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Plus, Search, X } from 'lucide-react'
import { useAuthStore } from '@/lib/stores/auth-store'
import { fmtDate } from '@/lib/format-currency-and-date'

export default function CustomersPage() {
  const { role } = useAuthStore()
  const isAdmin = role === 'ADMIN'
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<CustomerRequest>({
    name: '',
    email: '',
    phone: '',
    address: '',
    location: '',
    customerTypeId: 1,
  })

  // Search state
  const [searchActive, setSearchActive] = useState(false)
  const [search, setSearch] = useState<CustomerSearchParams>({ name: '', email: '', phone: '', location: '' })
  const [appliedSearch, setAppliedSearch] = useState<CustomerSearchParams>({})

  const qc = useQueryClient()

  const listQuery = useQuery({
    queryKey: ['customers', page],
    queryFn: () => getCustomers(page),
    enabled: !searchActive,
  })

  const searchQuery = useQuery({
    queryKey: ['customers-search', appliedSearch, page],
    queryFn: () => searchCustomers({ ...appliedSearch, page }),
    enabled: searchActive,
  })

  const data = searchActive ? searchQuery.data : listQuery.data
  const isLoading = searchActive ? searchQuery.isLoading : listQuery.isLoading
  const error = searchActive ? searchQuery.error : listQuery.error

  const [editId, setEditId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<CustomerRequest>({ name: '', email: '', phone: '', address: '', location: '', customerTypeId: 1 })
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const deleteName = data?.content.find(c => c.id === deleteId)?.name ?? ''

  const create = useMutation({
    mutationFn: createCustomer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      qc.invalidateQueries({ queryKey: ['customers-search'] })
      setOpen(false)
      setForm({ name: '', email: '', phone: '', address: '', location: '', customerTypeId: 1 })
    },
  })

  const update = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CustomerRequest }) => updateCustomer(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      qc.invalidateQueries({ queryKey: ['customers-search'] })
      setEditId(null)
    },
  })

  const remove = useMutation({
    mutationFn: deleteCustomer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      qc.invalidateQueries({ queryKey: ['customers-search'] })
      setDeleteId(null)
    },
  })

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const hasFilter = search.name || search.email || search.phone || search.location
    if (hasFilter) {
      setAppliedSearch({ ...search })
      setSearchActive(true)
      setPage(0)
    }
  }

  const clearSearch = () => {
    setSearch({ name: '', email: '', phone: '', location: '' })
    setAppliedSearch({})
    setSearchActive(false)
    setPage(0)
  }

  return (
    <div className="space-y-4 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Customers</h1>
        {isAdmin && <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger className="inline-flex h-9 items-center justify-center gap-1 rounded-md bg-primary px-3 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            <Plus className="h-4 w-4" />Create Customer
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New Customer</DialogTitle>
            </DialogHeader>
            <div className="space-y-3 pt-2">
              <div className="space-y-1">
                <Label>Name</Label>
                <Input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="space-y-1">
                <Label>Email</Label>
                <Input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
              </div>
              <div className="space-y-1">
                <Label>Phone</Label>
                <Input value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
              </div>
              <div className="space-y-1">
                <Label>Address</Label>
                <Input value={form.address} onChange={e => setForm(f => ({ ...f, address: e.target.value }))} />
              </div>
              <div className="space-y-1">
                <Label>Location</Label>
                <Input value={form.location} onChange={e => setForm(f => ({ ...f, location: e.target.value }))} />
              </div>
              <div className="space-y-1">
                <Label>Type</Label>
                <Select
                  value={form.customerTypeId === 2 ? 'Enterprise' : 'Individual'}
                  onValueChange={v => setForm(f => ({ ...f, customerTypeId: v === 'Enterprise' ? 2 : 1 }))}
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Individual">Individual</SelectItem>
                    <SelectItem value="Enterprise">Enterprise</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {create.error && <p className="text-sm text-red-500">{(create.error as Error).message}</p>}
              <Button type="button" className="w-full" disabled={create.isPending || !form.name || !form.email || !form.location}
                onClick={() => create.mutate(form)}>
                {create.isPending ? 'Creating...' : 'Create'}
              </Button>
            </div>
          </DialogContent>
        </Dialog>}
      </div>

      {/* Search Bar - Admin only */}
      {isAdmin && (
        <form onSubmit={handleSearch} className="rounded-lg border bg-card p-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Name</Label>
              <Input placeholder="Search by name..." value={search.name} onChange={e => setSearch(s => ({ ...s, name: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Email</Label>
              <Input placeholder="Search by email..." value={search.email} onChange={e => setSearch(s => ({ ...s, email: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Phone</Label>
              <Input placeholder="Search by phone..." value={search.phone} onChange={e => setSearch(s => ({ ...s, phone: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Location</Label>
              <Input placeholder="Search by location..." value={search.location} onChange={e => setSearch(s => ({ ...s, location: e.target.value }))} />
            </div>
            <div className="flex items-end gap-2">
              <Button type="submit" size="sm" className="gap-1"><Search className="h-4 w-4" />Search</Button>
              {searchActive && (
                <Button type="button" variant="outline" size="sm" className="gap-1" onClick={clearSearch}><X className="h-4 w-4" />Clear</Button>
              )}
            </div>
          </div>
          {searchActive && (
            <p className="mt-2 text-xs text-muted-foreground">Found {data?.totalElements ?? 0} result(s)</p>
          )}
        </form>
      )}

      {isLoading && <p className="text-muted-foreground">Loading...</p>}
      {error && <p className="text-red-500">{(error as Error).message}</p>}

      {data && (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Phone</TableHead>
                <TableHead>Location</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Created</TableHead>
                {isAdmin && <TableHead></TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map(c => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell>{c.email}</TableCell>
                  <TableCell>{c.phone}</TableCell>
                  <TableCell>{c.location}</TableCell>
                  <TableCell>{c.customerTypeName}</TableCell>
                  <TableCell>{fmtDate(c.createdAt)}</TableCell>
                  {isAdmin && (
                    <TableCell className="flex gap-1">
                      <Button variant="outline" size="sm" onClick={() => {
                        setEditForm({ name: c.name, email: c.email, phone: c.phone, address: c.address, location: c.location, customerTypeId: c.customerTypeId })
                        setEditId(c.id)
                      }}>Edit</Button>
                      <Button variant="destructive" size="sm" onClick={() => setDeleteId(c.id)}>Delete</Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
              {data.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={isAdmin ? 7 : 6} className="text-center text-muted-foreground py-8">
                    {searchActive ? 'No customers found matching your search.' : 'No customers yet.'}
                  </TableCell>
                </TableRow>
              )}
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

      {/* Edit Dialog */}
      <Dialog open={!!editId} onOpenChange={o => { if (!o) setEditId(null) }}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit Customer</DialogTitle></DialogHeader>
          <div className="space-y-3 pt-2">
            <div className="space-y-1">
              <Label>Name</Label>
              <Input value={editForm.name} onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label>Email</Label>
              <Input type="email" value={editForm.email} onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label>Phone</Label>
              <Input value={editForm.phone} onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label>Address</Label>
              <Input value={editForm.address} onChange={e => setEditForm(f => ({ ...f, address: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label>Location</Label>
              <Input value={editForm.location} onChange={e => setEditForm(f => ({ ...f, location: e.target.value }))} />
            </div>
            <div className="space-y-1">
              <Label>Type</Label>
              <Select value={editForm.customerTypeId === 2 ? 'Enterprise' : 'Individual'} onValueChange={v => setEditForm(f => ({ ...f, customerTypeId: v === 'Enterprise' ? 2 : 1 }))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="Individual">Individual</SelectItem>
                  <SelectItem value="Enterprise">Enterprise</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {update.error && <p className="text-sm text-red-500">{(update.error as Error).message}</p>}
            <Button type="button" className="w-full" disabled={update.isPending}
              onClick={() => update.mutate({ id: editId!, data: editForm })}>
              {update.isPending ? 'Saving...' : 'Save'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={!!deleteId} onOpenChange={o => { if (!o) setDeleteId(null) }}>
        <DialogContent>
          <DialogHeader><DialogTitle>Delete Customer</DialogTitle></DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            Are you sure you want to delete <strong>{deleteName}</strong>? This cannot be undone.
          </p>
          {remove.error && <p className="text-sm text-red-500">{(remove.error as Error).message}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setDeleteId(null)}>Cancel</Button>
            <Button variant="destructive" disabled={remove.isPending} onClick={() => remove.mutate(deleteId!)}>
              {remove.isPending ? 'Deleting...' : 'Delete'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
