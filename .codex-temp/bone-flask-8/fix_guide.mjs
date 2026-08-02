import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const sourcePath = "E:/Java/MinecraftMod/RPGItem/RPGItems.xlsx";
const outputPath =
  "E:/Java/MinecraftMod/RPGItem/outputs/019fa4a6-022f-7ca2-af4d-7e54a6a77694/RPGItems.xlsx";
const previewDir = "E:/Java/MinecraftMod/DarkGrey/.codex-temp/bone-flask-8/after";

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
const sheet = workbook.worksheets.getItem("组件参数说明书 (Guide)");
const values = sheet.getUsedRange().values;
const matches = values
  .map((row, index) => ({ row, index }))
  .filter(({ row }) => row?.[0] === "碎骨瓶");

if (matches.length !== 1) {
  throw new Error(`Expected one 碎骨瓶 guide row, found ${matches.length}`);
}

const { row, index } = matches[0];
const example = JSON.parse(row[3]);
if (example.directDamage !== 12) {
  throw new Error(`Expected guide directDamage 12, found ${example.directDamage}`);
}

example.directDamage = 8;
sheet.getCell(index, 3).values = [[JSON.stringify(example)]];

await fs.mkdir(previewDir, { recursive: true });
for (const currentSheet of workbook.worksheets.items) {
  const preview = await workbook.render({
    sheetName: currentSheet.name,
    autoCrop: "all",
    scale: 1,
    format: "png",
  });
  const safeName = currentSheet.name.replace(/[<>:"/\\|?*]/g, "_");
  await fs.writeFile(
    path.join(previewDir, `${safeName}.png`),
    new Uint8Array(await preview.arrayBuffer()),
  );
}

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 300 },
  summary: "final formula error scan",
});
console.log(errors.ndjson);
console.log(JSON.stringify({ sheet: sheet.name, row: index + 1, directDamage: example.directDamage }));

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);
