import { api } from "@/lib/api/client";
import { toast } from "sonner";

function filenameFromDisposition(disposition: string | null | undefined, fallback: string) {
  if (!disposition) return fallback;
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1].replace(/"/g, ""));
    } catch {
      /* fallthrough */
    }
  }
  const ascii = /filename="?([^";]+)"?/i.exec(disposition);
  if (ascii?.[1]) return ascii[1];
  return fallback;
}

export async function downloadBlob(
  path: string,
  fallbackName: string,
  params?: Record<string, unknown>,
) {
  try {
    const resp = await api.get<Blob>(path, {
      params,
      responseType: "blob",
    });
    const contentType = resp.headers["content-type"] as string | undefined;
    if (contentType?.includes("application/json")) {
      const text = await (resp.data as Blob).text();
      try {
        const parsed = JSON.parse(text) as { detail?: string; title?: string };
        toast.error(parsed.detail ?? parsed.title ?? "Tải xuống thất bại");
      } catch {
        toast.error("Tải xuống thất bại");
      }
      return;
    }
    const name = filenameFromDisposition(
      resp.headers["content-disposition"] as string | undefined,
      fallbackName,
    );
    const url = URL.createObjectURL(resp.data as Blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  } catch {
    /* axios interceptor already toasted */
  }
}
