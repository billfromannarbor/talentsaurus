import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { parseResumeText } from "@/lib/parseResume";
import { extractTextFromPdf } from "@/lib/pdf";

const COOKIE = "talentsaurus_user_id";
export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  const redirectWithError = (message: string) =>
    NextResponse.redirect(new URL(`/upload?error=${encodeURIComponent(message)}`, req.url));

  const contentType = req.headers.get("content-type") ?? "";
  if (!contentType.toLowerCase().startsWith("multipart/form-data")) {
    return redirectWithError("Invalid upload payload. Please submit from the Upload page.");
  }

  let form: FormData;
  try {
    form = await req.formData();
  } catch {
    return redirectWithError("Upload payload was malformed. Please choose file again and retry.");
  }
  const file = form.get("resume");
  if (!file || !(file instanceof File)) return redirectWithError("Please choose a file.");

  // Some browsers do not send application/pdf reliably.
  const looksLikePdf =
    file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
  if (!looksLikePdf) return redirectWithError("Please upload a PDF file.");

  const buffer = Buffer.from(await file.arrayBuffer());
  if (!buffer.length) return redirectWithError("The uploaded file was empty.");

  let text = "";
  try {
    text = await extractTextFromPdf(buffer);
  } catch {
    return redirectWithError("Could not read that PDF. Try a different file.");
  }
  if (!text.trim()) return redirectWithError("No text was found in this PDF.");

  try {
    const parsed = parseResumeText(text);
    if (process.env.NODE_ENV !== "production") {
      console.log("[resume-upload] parsed counts", {
        sections: parsed.parseMeta?.sectionsDetected,
        skills: parsed.parseMeta?.skillCount,
        experiences: parsed.parseMeta?.experienceCount,
        educations: parsed.parseMeta?.educationCount,
      });
    }

    const cookieId = req.cookies.get(COOKIE)?.value;
    let user = cookieId ? await prisma.user.findUnique({ where: { id: cookieId } }) : null;
    if (!user) user = await prisma.user.create({ data: {} });

    await prisma.resumeUpload.create({
      data: {
        userId: user.id,
        fileName: file.name || "resume.pdf",
        mimeType: file.type,
        extractedText: text,
        parsedSnapshot: parsed as object,
      },
    });

    const res = NextResponse.redirect(new URL("/profile/review", req.url));
    res.cookies.set(COOKIE, user.id, { httpOnly: true, sameSite: "lax", path: "/", maxAge: 31536000 });
    return res;
  } catch {
    return redirectWithError("Upload succeeded, but saving failed. Check DATABASE_URL and run db:push.");
  }
}
