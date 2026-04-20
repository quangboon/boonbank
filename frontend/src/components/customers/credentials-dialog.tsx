"use client";

import { useState } from "react";
import { Copy, Check } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

export function CredentialsDialog({
  open,
  onOpenChange,
  username,
  tempPassword,
  onClose,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  username: string;
  tempPassword: string;
  onClose?: () => void;
}) {
  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        onOpenChange(v);
        if (!v) onClose?.();
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Thông tin đăng nhập</DialogTitle>
          <DialogDescription>
            Gửi thông tin này cho khách hàng. Mật khẩu sẽ không hiển thị lại —
            copy và lưu ngay.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <CopyRow label="Tên đăng nhập" value={username} />
          <CopyRow label="Mật khẩu tạm" value={tempPassword} mono />
        </div>
        <DialogFooter>
          <Button onClick={() => onOpenChange(false)}>Đã ghi lại</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function CopyRow({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }
  return (
    <div className="space-y-1">
      <p className="text-xs text-neutral-500">{label}</p>
      <div className="flex items-center gap-2 rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2">
        <code className={mono ? "flex-1 font-mono text-sm" : "flex-1 text-sm"}>
          {value}
        </code>
        <Button
          size="sm"
          variant="ghost"
          onClick={copy}
          className="gap-1"
          translate="no"
        >
          {copied ? (
            <Check key="check" className="h-4 w-4" />
          ) : (
            <Copy key="copy" className="h-4 w-4" />
          )}
          <span>{copied ? "Đã copy" : "Copy"}</span>
        </Button>
      </div>
    </div>
  );
}
