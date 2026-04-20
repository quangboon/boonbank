import { AxiosError } from "axios";
import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import type { ErrorResponse, FieldError } from "./types";

export function asAxiosError(e: unknown): AxiosError<ErrorResponse> | null {
  if (e instanceof AxiosError) return e as AxiosError<ErrorResponse>;
  return null;
}

export function extractMessage(e: unknown, fallback = "Đã có lỗi xảy ra."): string {
  const axErr = asAxiosError(e);
  if (!axErr) return fallback;
  const body = axErr.response?.data;
  return body?.detail ?? body?.title ?? axErr.message ?? fallback;
}

export function extractFieldErrors(e: unknown): FieldError[] {
  const axErr = asAxiosError(e);
  return axErr?.response?.data?.errors ?? [];
}

/**
 * Map lỗi server về field của form:
 * 1. Ưu tiên `errors[]` (RFC 7807 field-level errors từ BE).
 * 2. Fallback theo `codeToField` — map ErrorCode.code → form field.
 * Trả về true nếu đã set ít nhất 1 field error (caller có thể dùng để bỏ toast generic).
 */
export function mapServerFieldErrors<T extends FieldValues>(
  e: unknown,
  setError: UseFormSetError<T>,
  codeToField: Record<string, Path<T>> = {},
): boolean {
  const axErr = asAxiosError(e);
  const data = axErr?.response?.data;
  if (!data) return false;

  if (data.errors?.length) {
    data.errors.forEach((fe) =>
      setError(fe.field as Path<T>, { message: fe.message }),
    );
    return true;
  }

  const field = codeToField[data.code];
  if (field) {
    setError(field, {
      message: data.detail ?? data.title ?? "Giá trị không hợp lệ",
    });
    return true;
  }
  return false;
}
