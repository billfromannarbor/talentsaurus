import Link from "next/link";
import "./globals.css";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en"><body>
      <header className="border-b bg-white"><div className="mx-auto flex max-w-3xl gap-4 px-4 py-3"><Link href="/" className="font-semibold">Talentsaurus</Link><Link href="/upload">Upload</Link><Link href="/profile/review">Profile</Link></div></header>
      <main className="mx-auto max-w-3xl px-4 py-8">{children}</main>
    </body></html>
  );
}
