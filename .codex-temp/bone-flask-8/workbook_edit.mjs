import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const mode = process.argv[2] ?? "before";
if (mode !== "before" && mode !== "edit") {
  throw new Error(`Unsupported mode: ${mode}`);
}

const sourcePath = "E:/Java/MinecraftMod/RPGItem/RPGItems.xlsx";
const outputPath =
  "E:/Java/MinecraftMod/RPGItem/outputs/019fa4a6-022f-7ca2-af4d-7e54a6a77694/RPGItems.xlsx";
const previewDir =
  `E:/Java/MinecraftMod/DarkGrey/.codex-temp/bone-flask-8/${mode === "edit" ? "after" : "before"}`;

await fs.mkdir(previewDir, { recursive: true });
await fs.mkdir(path.dirname(outputPath), { recursive: true });

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
console.log(
  (
    await workbook.inspect({
      kind: "workbook,sheet,table",
      maxChars: 8000,
      tableMaxRows: 8,
      tableMaxCols: 21,
      tableMaxCellChars: 180,
    })
  ).ndjson,
);

const itemSheet = workbook.worksheets.getItem("RPG Items");
const usedRange = itemSheet.getUsedRange();
const values = usedRange.values;
let targetRowIndex = -1;

for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
  if (values[rowIndex]?.[0] === "bone_flask") {
    if (targetRowIndex !== -1) {
      throw new Error("Duplicate bone_flask rows found");
    }
    targetRowIndex = rowIndex;
  }
}
if (targetRowIndex === -1) {
  throw new Error("bone_flask row not found");
}

const excelRow = targetRowIndex + 1;
console.log(
  (
    await workbook.inspect({
      kind: "computedStyle",
      sheetId: "RPG Items",
      range: `N${excelRow}:O${excelRow}`,
      maxChars: 3000,
    })
  ).ndjson,
);

const originalParamsText = values[targetRowIndex]?.[14];
if (typeof originalParamsText !== "string") {
  throw new Error(`Expected component params text in O${excelRow}`);
}
const params = JSON.parse(originalParamsText);
if (params.directDamage !== 12) {
  throw new Error(`Expected directDamage 12, found ${params.directDamage}`);
}

if (mode === "edit") {
  params.directDamage = 8;
  itemSheet.getCell(targetRowIndex, 14).values = [[JSON.stringify(params)]];
}

for (const sheet of workbook.worksheets.items) {
  const preview = await workbook.render({
    sheetName: sheet.name,
    autoCrop: "all",
    scale: 1,
    format: "png",
  });
  const safeName = sheet.name.replace(/[<>:"/\\|?*]/g, "_");
  await fs.writeFile(
    path.join(previewDir, `${safeName}.png`),
    new Uint8Array(await preview.arrayBuffer()),
  );
}

console.log(
  JSON.stringify(
    {
      mode,
      sheet: "RPG Items",
      row: excelRow,
      id: values[targetRowIndex]?.[0],
      oldDirectDamage: 12,
      newDirectDamage: mode === "edit" ? 8 : 12,
    },
    null,
    2,
  ),
);

if (mode === "edit") {
  const errors = await workbook.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
    options: { useRegex: true, maxResults: 300 },
    summary: "final formula error scan",
  });
  console.log(errors.ndjson);

  const targetCheck = await workbook.inspect({
    kind: "region",
    sheetId: "RPG Items",
    range: `A${excelRow}:U${excelRow}`,
    maxChars: 5000,
  });
  console.log(targetCheck.ndjson);

  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(outputPath);
}
