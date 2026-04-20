"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { LocationStats } from "@/types/domain";

export function LocationChart({ data }: { data: LocationStats[] }) {
  const top = [...data]
    .sort((a, b) => b.customerCount - a.customerCount)
    .slice(0, 10);
  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={top} margin={{ left: 8, right: 16, top: 8, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="city" tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
        <Tooltip />
        <Bar dataKey="customerCount" fill="#2563eb" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
