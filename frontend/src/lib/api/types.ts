export type ApiResponse<T> = {
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type FieldError = {
  field: string;
  message: string;
};

export type ErrorResponse = {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  code: string;
  traceId?: string;
  timestamp?: string;
  errors?: FieldError[];
};

export function fieldErrorsToRecord(
  errors: FieldError[] | undefined,
): Record<string, string> {
  if (!errors) return {};
  return errors.reduce<Record<string, string>>((acc, e) => {
    acc[e.field] = e.message;
    return acc;
  }, {});
}
