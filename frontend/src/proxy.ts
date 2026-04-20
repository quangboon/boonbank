import { NextResponse, type NextRequest } from "next/server";
import {
  SESSION_COOKIE,
  parseSession,
  sessionLandingPath,
} from "@/lib/auth/session-read";

function redirect(req: NextRequest, path: string, search?: string) {
  const url = req.nextUrl.clone();
  url.pathname = path;
  url.search = search ?? "";
  return NextResponse.redirect(url);
}

export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const session = parseSession(req.cookies.get(SESSION_COOKIE)?.value);

  if (pathname === "/login") {
    if (session) {
      return redirect(req, sessionLandingPath(session));
    }
    return NextResponse.next();
  }

  if (pathname.startsWith("/admin")) {
    if (!session) {
      return redirect(req, "/login", `?next=${encodeURIComponent(pathname)}`);
    }
    if (session.role !== "ADMIN" && session.role !== "FRAUD") {
      return redirect(req, "/forbidden");
    }
    return NextResponse.next();
  }

  if (pathname.startsWith("/my")) {
    if (!session) {
      return redirect(req, "/login", `?next=${encodeURIComponent(pathname)}`);
    }
    if (session.role !== "CUSTOMER") {
      return redirect(req, "/forbidden");
    }
    return NextResponse.next();
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/login", "/admin/:path*", "/my/:path*"],
};
