export const fmtVnd = (n: number) => `${new Intl.NumberFormat('vi-VN').format(n)} VND`

export const fmtDate = (d: string | null) =>
  d ? new Date(d).toLocaleDateString('vi-VN') : '—'
