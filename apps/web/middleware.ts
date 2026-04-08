import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(req: NextRequest) {
  if (req.method === "POST" && req.nextUrl.pathname === "/profile/review") {
    return NextResponse.redirect(new URL("/profile/review", req.url), 303);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/profile/review"],
};
