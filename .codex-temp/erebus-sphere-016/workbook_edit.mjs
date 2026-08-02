import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const mode = process.argv[2] ?? "before";
const workbookPath = "E:\\Java\\MinecraftMod\\RPGItem\\RPGItems.xlsx";
const previewDir = path.join(
  "E:\\Java\\MinecraftMod\\DarkGrey\\.codex-temp\\erebus-sphere-016",
  mode,
);

await fs.mkdir(previewDir, { recursive: true });
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(workbookPath));

console.log(
  (
    await workbook.inspect({
      kind: "workbook,sheet,table",
      maxChars: 5000,
      tableMaxRows: 5,
      tableMaxCols: 20,
      tableMaxCellChars: 100,
    })
  ).ndjson,
);

const erebusRows = [];
for (const sheet of workbook.worksheets.items) {
  const used = sheet.getUsedRange();
  const values = used?.values ?? [];

  for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
    if (sheet.name === "RPG Items" && values[rowIndex]?.[0] === "erebus") {
      const beforeParams = JSON.parse(values[rowIndex][14]);
      const afterParams = { ...beforeParams };
      delete afterParams.verticalHalfHeight;
      erebusRows.push({
        sheet: sheet.name,
        row: rowIndex + 1,
        beforeParams,
        afterParams,
      });
      if (mode === "edit") {
        sheet.getCell(rowIndex, 14).values = [[JSON.stringify(afterParams)]];
      }
    }

    if (mode === "edit" && sheet.name === "组件参数说明书 (Guide)" && values[rowIndex]?.[0] === "厄瑞波斯") {
      sheet.getRangeByIndexes(rowIndex, 1, 1, 3).values = [[
        "诅咒之物：右键展开球形剧毒领域，球心位于施法者身体中心，半径从3格逐次扩大至7格，1秒冷却，连续5秒不使用重置。必定施加4层剧毒，并分别独立判定75%额外6层与40%再次额外7层。",
        "markId: 印记ID；markStableDurationTicks: 本次施加的稳定期；minRadius/maxRadius/radiusStep: 球形范围半径参数；cooldownTicks: 冷却；baseStacks: 必定层数；bonusStacks1/bonusChance1、bonusStacks2/bonusChance2: 两组独立额外层数与概率",
        "{\"markId\":\"poison\",\"markStableDurationTicks\":200,\"minRadius\":3,\"maxRadius\":7,\"radiusStep\":1,\"cooldownTicks\":20,\"baseStacks\":4,\"bonusStacks1\":6,\"bonusChance1\":0.75,\"bonusStacks2\":7,\"bonusChance2\":0.40}",
      ]];
    }
  }

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

console.log(JSON.stringify({ mode, erebusRows }, null, 2));

if (mode === "edit") {
  const errors = await workbook.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
    options: { useRegex: true, maxResults: 300 },
    summary: "final formula error scan",
  });
  console.log(errors.ndjson);

  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(workbookPath);
}
