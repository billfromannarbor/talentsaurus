export default async function UploadPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Upload resume</h1>
      {error ? (
        <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}
      <form action="/api/resume/upload" method="post" encType="multipart/form-data" className="rounded border bg-white p-4">
        <input name="resume" type="file" accept="application/pdf" required />
        <button type="submit" className="ml-3 rounded bg-emerald-700 px-3 py-2 text-white">Extract and review</button>
      </form>
    </div>
  );
}
