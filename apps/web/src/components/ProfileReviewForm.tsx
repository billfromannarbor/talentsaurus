"use client";

import { useState, useTransition } from "react";
import type { ParsedResumeDraft } from "@/lib/parseResume";

export function ProfileReviewForm({
  initial,
  parseWarnings = [],
}: {
  initial: ParsedResumeDraft;
  parseWarnings?: string[];
}) {
  const [data, setData] = useState(initial);
  const [skillsJson, setSkillsJson] = useState(JSON.stringify(initial.skills, null, 2));
  const [expJson, setExpJson] = useState(JSON.stringify(initial.experiences, null, 2));
  const [eduJson, setEduJson] = useState(JSON.stringify(initial.educations, null, 2));
  const [msg, setMsg] = useState<string>("");
  const [pending, start] = useTransition();

  async function onSave() {
    setMsg("");
    try {
      const payload: ParsedResumeDraft = {
        profile: data.profile,
        skills: JSON.parse(skillsJson),
        experiences: JSON.parse(expJson),
        educations: JSON.parse(eduJson),
      };
      const res = await fetch("/api/profile/save", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error("save failed");
      setMsg("Saved.");
    } catch {
      setMsg("Invalid JSON in one of the list editors, or save failed.");
    }
  }

  const p = data.profile;
  return (
    <div className="space-y-5">
      {parseWarnings.length > 0 ? (
        <div className="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          <p className="font-medium">Parser warnings</p>
          <ul className="ml-5 list-disc">
            {parseWarnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </div>
      ) : null}
      {msg && <p className="text-sm text-zinc-700">{msg}</p>}
      <div className="grid gap-3 sm:grid-cols-2">
        {(["fullName", "headline", "location", "phone", "email", "linkedInUrl"] as const).map((k) => (
          <label key={k} className="text-sm">{k}
            <input className="mt-1 w-full rounded border px-3 py-2" value={(p[k] ?? "") as string} onChange={(e) => setData((d) => ({ ...d, profile: { ...d.profile, [k]: e.target.value || null } }))} />
          </label>
        ))}
      </div>
      <label className="block text-sm">summary
        <textarea className="mt-1 w-full rounded border px-3 py-2" rows={4} value={p.summary ?? ""} onChange={(e) => setData((d) => ({ ...d, profile: { ...d.profile, summary: e.target.value || null } }))} />
      </label>

      <label className="block text-sm">skills (JSON array)
        <textarea className="mt-1 w-full rounded border px-3 py-2 font-mono text-xs" rows={8} value={skillsJson} onChange={(e) => setSkillsJson(e.target.value)} />
      </label>
      <label className="block text-sm">experiences (JSON array)
        <textarea className="mt-1 w-full rounded border px-3 py-2 font-mono text-xs" rows={8} value={expJson} onChange={(e) => setExpJson(e.target.value)} />
      </label>
      <label className="block text-sm">educations (JSON array)
        <textarea className="mt-1 w-full rounded border px-3 py-2 font-mono text-xs" rows={8} value={eduJson} onChange={(e) => setEduJson(e.target.value)} />
      </label>

      <button onClick={() => start(onSave)} disabled={pending} className="rounded bg-emerald-700 px-4 py-2 text-white">{pending ? "Saving..." : "Save profile"}</button>
    </div>
  );
}
