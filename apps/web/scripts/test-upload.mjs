import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";
import PDFDocument from "pdfkit";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const root = path.resolve(__dirname, "..");
const fixtureDir = path.join(root, "tests", "fixtures");
const samplePdfPath = path.join(fixtureDir, "sample.pdf");
const port = 3100;
const baseUrl = `http://127.0.0.1:${port}`;

async function createSamplePdf() {
  await fs.promises.mkdir(fixtureDir, { recursive: true });
  await new Promise((resolve, reject) => {
    const doc = new PDFDocument();
    const out = fs.createWriteStream(samplePdfPath);
    out.on("finish", resolve);
    out.on("error", reject);
    doc.pipe(out);
    doc.fontSize(18).text("John Doe");
    doc.moveDown();
    doc.fontSize(12).text("john.doe@example.com");
    doc.text("+1 555 123 4567");
    doc.moveDown();
    doc.text("Skills: TypeScript, React, PostgreSQL, Prisma");
    doc.end();
  });
}

function startServer() {
  return new Promise((resolve, reject) => {
    const child = spawn("npm", ["run", "start", "--", "--port", String(port)], {
      cwd: root,
      env: { ...process.env },
      stdio: ["ignore", "pipe", "pipe"],
    });

    let ready = false;
    const timer = setTimeout(() => {
      if (!ready) {
        child.kill("SIGTERM");
        reject(new Error("Timed out waiting for dev server"));
      }
    }, 60_000);

    const onData = (chunk) => {
      const text = chunk.toString();
      if (
        text.includes("Ready in") ||
        text.includes("ready - started server") ||
        text.includes("Local:")
      ) {
        ready = true;
        clearTimeout(timer);
        resolve(child);
      }
    };

    child.stdout.on("data", onData);
    child.stderr.on("data", onData);
    child.on("exit", (code) => {
      if (!ready) {
        clearTimeout(timer);
        reject(new Error(`Dev server exited early with code ${code}`));
      }
    });
  });
}

async function runUploadTest() {
  const pdfBuffer = await fs.promises.readFile(samplePdfPath);
  const form = new FormData();
  form.append("resume", new Blob([pdfBuffer], { type: "application/pdf" }), "sample.pdf");

  const uploadRes = await fetch(`${baseUrl}/api/resume/upload`, {
    method: "POST",
    body: form,
    redirect: "manual",
  });

  if (uploadRes.status < 300 || uploadRes.status > 399) {
    throw new Error(`Expected redirect, got status ${uploadRes.status}`);
  }

  const location = uploadRes.headers.get("location");
  if (!location || !location.includes("/profile/review")) {
    throw new Error(`Expected redirect to /profile/review, got ${location ?? "null"}`);
  }

  const cookie = uploadRes.headers.get("set-cookie");
  if (!cookie || !cookie.includes("talentsaurus_user_id=")) {
    throw new Error("Expected talentsaurus_user_id cookie to be set");
  }

  const reviewRes = await fetch(`${baseUrl}/profile/review`, {
    headers: { cookie },
  });
  const html = await reviewRes.text();
  if (!reviewRes.ok || !html.includes("Review extracted profile")) {
    throw new Error("Review page did not load after upload");
  }
}

async function main() {
  let server;
  try {
    console.log("Creating tests/fixtures/sample.pdf ...");
    await createSamplePdf();

    console.log(`Starting app server on port ${port} ...`);
    server = await startServer();

    console.log("Running upload flow test ...");
    await runUploadTest();

    console.log("PASS: sample.pdf upload redirects to /profile/review and page renders.");
  } finally {
    if (server && !server.killed) server.kill("SIGTERM");
  }
}

main().catch((err) => {
  console.error(`FAIL: ${err.message}`);
  process.exit(1);
});
