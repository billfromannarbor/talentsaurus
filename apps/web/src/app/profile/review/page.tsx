import { prisma } from "@/lib/prisma";
import { getUserFromCookie } from "@/lib/user";
import { ProfileReviewForm } from "@/components/ProfileReviewForm";
import type { ParsedResumeDraft } from "@/lib/parseResume";

const empty: ParsedResumeDraft = { profile: {}, skills: [], experiences: [], educations: [] };

export default async function ReviewPage() {
  const user = await getUserFromCookie();
  if (!user) {
    return <div className="space-y-4"><h1 className="text-2xl font-semibold">Review extracted profile</h1><p>No upload yet. Go to /upload first.</p></div>;
  }
  const latest = await prisma.resumeUpload.findFirst({ where: { userId: user.id }, orderBy: { createdAt: "desc" } });
  const draft = (latest?.parsedSnapshot as ParsedResumeDraft | null) ?? empty;
  const warnings = draft.parseMeta?.warnings ?? [];

  return <div className="space-y-4"><h1 className="text-2xl font-semibold">Review extracted profile</h1>{latest ? <ProfileReviewForm initial={draft} parseWarnings={warnings} /> : <p>No upload yet. Go to /upload first.</p>}</div>;
}
