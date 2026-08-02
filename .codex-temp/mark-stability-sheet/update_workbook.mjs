import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const workbookPath = "E:/Java/MinecraftMod/RPGItem/RPGItems.xlsx";
const jsonPath = "E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/data/rpg_items.json";
const previewDir = "E:/Java/MinecraftMod/DarkGrey/.codex-temp/mark-stability-sheet/previews-after";

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(workbookPath));
const data = JSON.parse((await fs.readFile(jsonPath, "utf8")).replace(/^\uFEFF/, ""));

const main = workbook.worksheets.getItem("RPG Items");
const validation = workbook.worksheets.getItem("ValidationData");
const guide = workbook.worksheets.getItem("组件参数说明书 (Guide)");

const enchantmentNames = new Map();
for (const [value] of validation.getRange("B2:B100").values) {
  if (typeof value === "string" && value.includes(":")) {
    enchantmentNames.set(value.split(":", 1)[0], value);
  }
}

const rows = data.items.map((item) => {
  const enchantmentCells = Array(6).fill(null);
  const enchantments = typeof item.enchantments === "string" && item.enchantments.trim()
    ? item.enchantments.split(",").map((part) => part.trim()).filter(Boolean)
    : [];
  enchantments.slice(0, 3).forEach((entry, index) => {
    const [id, level = "1"] = entry.split(":");
    enchantmentCells[index * 2] = enchantmentNames.get(id) ?? id;
    enchantmentCells[index * 2 + 1] = Number(level);
  });

  const componentCells = Array(6).fill(null);
  (item.components ?? []).slice(0, 3).forEach((component, index) => {
    componentCells[index * 2] = component.name;
    componentCells[index * 2 + 1] = JSON.stringify(component.params ?? {});
  });

  return [
    item.id,
    item.type,
    item.displayName?.zh_CN ?? "",
    item.displayName?.en_US ?? "",
    item.texture ?? "",
    item.durability ?? 0,
    item.damage ?? 0,
    ...enchantmentCells,
    ...componentCells,
    item.maxStackSize ?? null,
  ];
});

main.getRange("T1").copyFrom(main.getRange("S1"), "all");
main.getRange("T1").values = [["最大堆叠 maxStackSize"]];
main.getRange("T2").copyFrom(main.getRange("G2"), "all");
const lastMainRow = rows.length + 1;
for (let row = 2; row <= lastMainRow; row++) {
  main.getRange(`A${row}:T${row}`).copyFrom(main.getRange("A2:T2"), "all");
}
main.getRange(`A2:T${lastMainRow}`).values = rows;

for (const table of [...main.tables.items]) {
  table.delete();
}
const mainTable = main.tables.add(`A1:T${lastMainRow}`, true, "RPGItemsTable");
mainTable.style = "TableStyleMedium2";

main.getRange(`F2:G${lastMainRow}`).format.numberFormat = "0.00";
main.getRange(`I2:I${lastMainRow}`).format.numberFormat = "0";
main.getRange(`K2:K${lastMainRow}`).format.numberFormat = "0";
main.getRange(`M2:M${lastMainRow}`).format.numberFormat = "0";
main.getRange(`T2:T${lastMainRow}`).format.numberFormat = "0";
main.getRange(`A1:T${lastMainRow}`).format.autofitRows();

const components = [
  "重击",
  "吸血",
  "超新星",
  "虹之愿",
  "圆环之理",
  "血祭",
  "劫难",
  "厄瑞波斯",
  "灵气洪流",
  "炬火的残光",
  "炬火残光",
  "耀斑",
  "倒悬",
  "伊塔尼斯",
  "地底太阳",
  "烈阳",
  "腐败瓶",
  "碎骨瓶",
  "粉碎之骨",
];
validation.getRange("A2:A100").clear({ applyTo: "contents" });
validation.getRange(`A2:A${components.length + 1}`).values = components.map((name) => [name]);

const existingGuideRows = guide.getRange("A2:D13").values;
const updatedGuideRows = existingGuideRows.concat([
  [
    "厄瑞波斯",
    "右键展开圆形剧毒领域，范围随连续使用扩大。",
    "markId；markStableDurationTicks（剧毒默认200 Tick）；minRadius、maxRadius、radiusStep、cooldownTicks等。",
    '{"markId":"poison","markStableDurationTicks":200,"minRadius":3,"maxRadius":7}',
  ],
  [
    "粉碎之骨",
    "连续命中同一目标后施加骨折印记。",
    "requiredHits；fractureMarkId；fractureStacksPerTrigger；fractureStableDurationTicks（骨折默认100 Tick）。",
    '{"requiredHits":3,"fractureMarkId":"fracture","fractureStacksPerTrigger":1,"fractureStableDurationTicks":100}',
  ],
  [
    "碎骨瓶",
    "投掷后生成骨刺场，骨刺命中后施加骨折印记。",
    "directDamage；lingeringDamage；fieldDuration；fractureStableDurationTicks（骨折默认100 Tick）；投射物参数。",
    '{"directDamage":12.0,"lingeringDamage":2.0,"fieldDuration":1200,"fractureStableDurationTicks":100}',
  ],
  [
    "腐败瓶",
    "碰撞后对范围内合法目标施加剧毒印记。",
    "markId；markStacks；markStableDurationTicks（剧毒默认200 Tick）；范围、投射物及目标筛选参数。",
    '{"markId":"poison","markStacks":3,"markStableDurationTicks":200,"areaWidth":5.0,"areaHeight":5.0}',
  ],
  [
    "地底太阳",
    "蓄力储存并发射地底太阳，对范围内合法目标造成爆炸伤害。",
    "chargeTicks、maxStoredOrbs、damageMultiplier、explosionRadius、projectileSpeed、orbitRadius等。",
    '{"chargeTicks":40,"maxStoredOrbs":3,"damageMultiplier":5.0,"explosionRadius":20.0}',
  ],
  [
    "烈阳",
    "蓄力生成可缩放火球，并施加红日灼烧机制。",
    "maxChargeTicks、火球尺寸/伤害、cooldownTicks、投射物参数、burnDurationTicks等。",
    '{"maxChargeTicks":180,"minFireballDamage":100.0,"maxFireballDamage":1250.0,"cooldownTicks":200}',
  ],
]);
const lastGuideRow = updatedGuideRows.length + 1;
for (let row = 14; row <= lastGuideRow; row++) {
  guide.getRange(`A${row}:D${row}`).copyFrom(guide.getRange("A13:D13"), "all");
}
guide.getRange(`A2:D${lastGuideRow}`).values = updatedGuideRows;
for (const table of [...guide.tables.items]) {
  table.delete();
}
const guideTable = guide.tables.add(`A1:D${lastGuideRow}`, true, "ComponentGuideTable");
guideTable.style = "TableStyleMedium2";
guide.getRange(`A1:D${lastGuideRow}`).format.wrapText = true;
guide.getRange(`A1:D${lastGuideRow}`).format.autofitRows();

const keyCheck = await workbook.inspect({
  kind: "table",
  range: `RPG Items!A1:T${lastMainRow}`,
  include: "values,formulas",
  tableMaxRows: 25,
  tableMaxCols: 20,
  maxChars: 30000,
});
console.log(keyCheck.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 300 },
  summary: "final formula error scan",
});
console.log(errors.ndjson);

await fs.mkdir(previewDir, { recursive: true });
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

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(workbookPath);
