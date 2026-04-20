"use client";

import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { TransactionPeriodStats } from "@/types/domain";

export function TransactionsTrendChart({
  data,
}: {
  data: TransactionPeriodStats[];
}) {
  const mapped = data.map((d) => ({
    bucket: new Date(d.bucket).toLocaleDateString("vi-VN"),
    total: Number(d.sumAmount),
    count: d.count,
  }));
  return (
    <ResponsiveContainer width="100%" height={260}>
      <LineChart data={mapped} margin={{ left: 8, right: 16, top: 8, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="bucket" tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 11 }} />
        <Tooltip />
        <Line
          type="monotone"
          dataKey="total"
          stroke="#15803d"
          strokeWidth={2}
          dot={{ r: 2 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
