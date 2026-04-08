import Link from "next/link";

export default function Home() {
  return <div className="space-y-4"><h1 className="text-2xl font-semibold">Talentsaurus</h1><p>Upload a resume PDF, review extracted profile data, edit it, and save.</p><Link href="/upload" className="rounded bg-emerald-700 px-4 py-2 text-white">Start</Link></div>;
}
