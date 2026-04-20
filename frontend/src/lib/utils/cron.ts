/**
 * Cron helper — phục vụ Spring 6-field cron (sec min hour dom month dow).
 * Chỉ cover 3 pattern phổ biến: hàng ngày / hàng tuần / hàng tháng.
 * Pattern khác gọi là "custom" — FE giữ raw string, không try parse.
 */

export type Frequency = "DAILY" | "WEEKLY" | "MONTHLY";

export type Schedule = {
  frequency: Frequency;
  dayOfWeek?: 1 | 2 | 3 | 4 | 5 | 6 | 7; // 1=Mon ... 7=Sun (ISO)
  dayOfMonth?: number; // 1-28 để tránh tháng 2 lệch
  hour: number; // 0-23
  minute: number; // 0-59
};

const DOW_SPRING: Record<number, string> = {
  1: "MON",
  2: "TUE",
  3: "WED",
  4: "THU",
  5: "FRI",
  6: "SAT",
  7: "SUN",
};

const DOW_VI: Record<number, string> = {
  1: "thứ 2",
  2: "thứ 3",
  3: "thứ 4",
  4: "thứ 5",
  5: "thứ 6",
  6: "thứ 7",
  7: "chủ nhật",
};

function pad(n: number) {
  return n.toString().padStart(2, "0");
}

export function composeCron(s: Schedule): string {
  const m = s.minute;
  const h = s.hour;
  // Quartz: không được set đồng thời day-of-month và day-of-week khác '?'. Slot
  // không dùng PHẢI là '?'. BE dự án này chạy Quartz scheduler.
  switch (s.frequency) {
    case "DAILY":
      return `0 ${m} ${h} * * ?`;
    case "WEEKLY":
      return `0 ${m} ${h} ? * ${DOW_SPRING[s.dayOfWeek ?? 1]}`;
    case "MONTHLY":
      return `0 ${m} ${h} ${s.dayOfMonth ?? 1} * ?`;
  }
}

/**
 * Parse Spring cron 6-field về Schedule nếu match 3 pattern;
 * trả null nếu là custom schedule.
 */
export function parseCron(cron: string): Schedule | null {
  const parts = cron.trim().split(/\s+/);
  if (parts.length !== 6) return null;
  const [sec, min, hr, dom, mon, dow] = parts;
  if (sec !== "0" || mon !== "*") return null;
  const m = Number(min);
  const h = Number(hr);
  if (Number.isNaN(m) || Number.isNaN(h)) return null;

  // Chấp nhận '*' và '?' làm wildcard (tương thích Spring cũ + Quartz).
  const isWild = (s: string) => s === "*" || s === "?";

  if (isWild(dom) && isWild(dow)) {
    return { frequency: "DAILY", hour: h, minute: m };
  }
  if (isWild(dom) && !isWild(dow)) {
    const entry = Object.entries(DOW_SPRING).find(([, v]) => v === dow);
    if (!entry) return null;
    return {
      frequency: "WEEKLY",
      dayOfWeek: Number(entry[0]) as Schedule["dayOfWeek"],
      hour: h,
      minute: m,
    };
  }
  if (isWild(dow) && !isWild(dom)) {
    const d = Number(dom);
    if (Number.isNaN(d) || d < 1 || d > 28) return null;
    return { frequency: "MONTHLY", dayOfMonth: d, hour: h, minute: m };
  }
  return null;
}

/**
 * Sinh label tiếng Việt cho cron string. Fallback về raw string nếu custom.
 */
export function humanizeCron(cron: string): string {
  const s = parseCron(cron);
  if (!s) return cron;
  const time = `${pad(s.hour)}:${pad(s.minute)}`;
  switch (s.frequency) {
    case "DAILY":
      return `Hàng ngày lúc ${time}`;
    case "WEEKLY":
      return `${DOW_VI[s.dayOfWeek ?? 1]} hàng tuần lúc ${time}`;
    case "MONTHLY":
      return `Ngày ${s.dayOfMonth} hàng tháng lúc ${time}`;
  }
}

export const DEFAULT_SCHEDULE: Schedule = {
  frequency: "DAILY",
  hour: 9,
  minute: 0,
};
