import { cookies } from "next/headers";
import { prisma } from "@/lib/prisma";

const COOKIE = "talentsaurus_user_id";

export async function getUserFromCookie() {
  const jar = await cookies();
  const id = jar.get(COOKIE)?.value;
  if (!id) return null;
  return prisma.user.findUnique({ where: { id } });
}

export async function getOrCreateUser() {
  const jar = await cookies();
  const existing = await getUserFromCookie();
  if (existing) return existing;

  const user = await prisma.user.create({ data: {} });
  jar.set(COOKIE, user.id, { httpOnly: true, sameSite: "lax", path: "/", maxAge: 31536000 });
  return user;
}
