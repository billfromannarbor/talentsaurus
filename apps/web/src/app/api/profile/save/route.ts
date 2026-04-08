import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getOrCreateUser } from "@/lib/user";
import type { ParsedResumeDraft } from "@/lib/parseResume";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  let input: ParsedResumeDraft;
  try {
    input = (await req.json()) as ParsedResumeDraft;
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const user = await getOrCreateUser();

  await prisma.$transaction(async (tx) => {
    await tx.userProfile.upsert({
      where: { userId: user.id },
      create: { userId: user.id, ...input.profile },
      update: { ...input.profile },
    });

    await tx.skill.deleteMany({ where: { userId: user.id } });
    if (input.skills.length) {
      await tx.skill.createMany({
        data: input.skills
          .filter((s) => (s.name ?? "").trim())
          .map((s) => ({ userId: user.id, name: s.name.trim() })),
      });
    }

    await tx.experience.deleteMany({ where: { userId: user.id } });
    if (input.experiences.length) {
      await tx.experience.createMany({
        data: input.experiences.map((e) => ({
          userId: user.id,
          company: (e.company ?? "").trim() || "Unknown",
          title: e.title ?? null,
          startDate: e.startDate ?? null,
          endDate: e.endDate ?? null,
          description: e.description ?? null,
        })),
      });
    }

    await tx.education.deleteMany({ where: { userId: user.id } });
    if (input.educations.length) {
      await tx.education.createMany({
        data: input.educations.map((e) => ({
          userId: user.id,
          institution: (e.institution ?? "").trim() || "Unknown",
          degree: e.degree ?? null,
          field: e.field ?? null,
          startDate: e.startDate ?? null,
          endDate: e.endDate ?? null,
        })),
      });
    }
  });

  return NextResponse.json({ ok: true });
}
