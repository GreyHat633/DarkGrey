import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const workbookPath = "E:/Java/MinecraftMod/RPGItem/RPGItems.xlsx";
const previewDir = "E:/Java/MinecraftMod/DarkGrey/.codex-temp/mark-stability-sheet/previews-before";

const input = await FileBlob.load(workbookPath);
const workbook = await SpreadsheetFile.importXlsx(input);

console.log(
  (
    await workbook.inspect({
      kind: "workbook,sheet,table",
      maxChars: 10000,
      tableMaxRows: 10,
      tableMaxCols: 20,
      tableMaxCellChars: 200,
    })
  ).ndjson,
);

for (const term of ["bone_crusher", "erebus", "bone_flask", "corruption_bomb", "厄瑞波斯", "碎骨瓶", "腐败瓶"]) {
  console.log(
    (
      await workbook.inspect({
        kind: "match",
        searchTerm: term,
        options: { maxResults: 20 },
        summary: `查找 ${term}`,
        maxChars: 5000,
      })
    ).ndjson,
  );
}

await fs.mkdir(previewDir, { recursive: true });
const sheetInfo = await workbook.inspect({ kind: "sheet", include: "id,name", maxChars: 5000 });
console.log(sheetInfo.ndjson);

for (const sheet of workbook.worksheets.items) {
  const preview = await workbook.render({
    sheetName: sheet.name,
    autoCrop: "all",
    scale: 1,
    format: "png",
  });
  const safeName = sheet.name.replace(/[<>:"/\\|?*]/g, "_");
  await fs.writeFile(`${previewDir}/${safeName}.png`, new Uint8Array(await preview.arrayBuffer()));
}
