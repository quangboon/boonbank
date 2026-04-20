import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import {
  SESSION_COOKIE,
  parseSession,
  sessionLandingPath,
} from "@/lib/auth/session-read";

export default async function HomePage() {
  const cookieStore = await cookies();
  const session = parseSession(cookieStore.get(SESSION_COOKIE)?.value);
  redirect(session ? sessionLandingPath(session) : "/login");
}
