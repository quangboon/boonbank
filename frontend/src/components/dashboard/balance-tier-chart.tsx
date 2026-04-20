"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { BalanceTierStats } from "@/types/domain";

const COLORS: Record<string, string> = {
  HIGH: "#15803d",
  MID: "#2563eb",
  LOW: "#6b7280",
};

const LABEL: Record<string, string> = {
  HIGH: "Số dư cao",
  MID: "Trung bình",
  LOW: "Thấp",
};

export function BalanceTierChart({ data }: { data: BalanceTierStats[] }) {
  const mapped = data.map((d) => ({
    name: LABEL[d.tier] ?? d.tier,
    value: d.count,
    key: d.tier,
  }));
  return (
    <ResponsiveContainer width="100%" height={240}>
      <PieChart>
        <Pie data={mapped} dataKey="value" nameKey="name" outerRadius={90} label>
          {mapped.map((d) => (
            <Cell key={d.key} fill={COLORS[d.key] ?? "#6b7280"} />
          ))}
        </Pie>
        <Tooltip />
      </PieChart>
    </ResponsiveContainer>
  );
}
