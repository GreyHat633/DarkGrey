import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const mode = process.argv[2] ?? "before";
const sourcePath = "E:/Java/MinecraftMod/RPGItem/RPGItems.xlsx";
const outputPath =
  "E:/Java/MinecraftMod/RPGItem/outputs/019fa4a6-022f-7ca2-af4d-7e54a6a77694/RPGItems.xlsx";
const previewDir =
  `E:/Java/MinecraftMod/DarkGrey/.codex-temp/supernova-armor-workbook/${mode === "edit" ? "after" : "before"}`;

const durableTypes = new Set([
  "剑", "斧", "镐", "铲", "锄", "弓", "头盔", "胸甲", "护腿", "靴子", "法杖", "镰刀", "长枪",
  "sword", "weapon", "Weapon", "axe", "pickaxe", "shovel", "hoe", "bow",
  "helmet", "chestplate", "leggings", "boots", "armor", "Armor", "wand", "scythe", "lance",
]);
const supernovaArmor = new Map([
  ["supernova_helmet", 3],
  ["supernova_chestplate", 8],
  ["supernova_leggings", 6],
  ["supernova_boots", 3],
]);

await fs.mkdir(previewDir, { recursive: true });
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
console.log(
  (
    await workbook.inspect({
      kind: "workbook,sheet,table",
      maxChars: 8000,
      tableMaxRows: 8,
      tableMaxCols: 21,
      tableMaxCellChars: 160,
    })
  ).ndjson,
);

const editedRows = [];
for (const sheet of workbook.worksheets.items) {
  const usedRange = sheet.getUsedRange();
  if (usedRange && sheet.name === "RPG Items") {
    const values = usedRange.values;
    const rowCount = values.length;

    if (mode === "edit") {
      sheet.getRange("U1").copyFrom(sheet.getRange("T1"), "all");
      sheet.getRange("U1").values = [["防御力 armor"]];
      sheet.getRange("U1").format.fill = "#4F81BD";
      sheet.getRange("U1").format.font = { bold: true, color: "#FFFFFF" };
      sheet.getRange("U1").format.horizontalAlignment = "center";
      sheet.getRange("U1").format.verticalAlignment = "center";
      sheet.getRange("U1").format.columnWidth = 20;
      if (rowCount > 1) {
        sheet.getRange(`U2:U${rowCount}`).copyFrom(sheet.getRange(`T2:T${rowCount}`), "all");
        sheet.getRange(`U2:U${rowCount}`).values = Array.from({ length: rowCount - 1 }, () => [null]);
        for (let excelRow = 2; excelRow <= rowCount; excelRow++) {
          sheet.getRange(`U${excelRow}`).format.fill = excelRow % 2 === 0 ? "#DCE6F1" : "#FFFFFF";
          sheet.getRange(`U${excelRow}`).format.horizontalAlignment = "center";
        }
      }
    }

    for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
      const id = values[rowIndex]?.[0];
      const type = values[rowIndex]?.[1];
      if (!id) continue;

      const record = { row: rowIndex + 1, id, type };
      let changed = false;
      if (durableTypes.has(type)) {
        record.oldDurability = values[rowIndex]?.[5];
        record.newDurability = 0;
        changed = true;
        if (mode === "edit") sheet.getCell(rowIndex, 5).values = [[0]];
      }
      if (supernovaArmor.has(id)) {
        record.armor = supernovaArmor.get(id);
        changed = true;
        if (mode === "edit") sheet.getCell(rowIndex, 20).values = [[record.armor]];
      }
      if (changed) editedRows.push(record);
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

console.log(JSON.stringify({ mode, editedRows }, null, 2));

if (mode === "edit") {
  const errors = await workbook.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
    options: { useRegex: true, maxResults: 300 },
    summary: "final formula error scan",
  });
  console.log(errors.ndjson);

  const targetCheck = await workbook.inspect({
    kind: "match",
    searchTerm: "supernova_",
    options: { maxResults: 20 },
    summary: "supernova armor rows",
    maxChars: 6000,
  });
  console.log(targetCheck.ndjson);

  const output = await SpreadsheetFile.exportXlsx(workbook);
  await output.save(outputPath);
}
