import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function KpiCard({
  label,
  value,
  hint,
}: {
  label: string;
  value: string | number | undefined;
  hint?: string;
}) {
  return (
    <Card>
      <CardHeader className="pb-1">
        <CardTitle className="text-sm text-neutral-500">{label}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-semibold">{value ?? "—"}</div>
        {hint ? <div className="mt-1 text-xs text-neutral-500">{hint}</div> : null}
      </CardContent>
    </Card>
  );
}
