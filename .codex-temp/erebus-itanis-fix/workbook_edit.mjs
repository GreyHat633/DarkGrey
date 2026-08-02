import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const mode = process.argv[2] ?? "before";
const workbookPath = "E:\\Java\\MinecraftMod\\RPGItem\\RPGItems.xlsx";
const previewDir = path.join(
  "E:\\Java\\MinecraftMod\\DarkGrey\\.codex-temp\\erebus-itanis-fix",
  mode === "edit" ? "after" : "before",
);
const relevantIds = new Set(["erebus", "bone_flask", "corruption_bomb", "itanis"]);

await fs.mkdir(previewDir, { recursive: true });
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(workbookPath));
const summary = await workbook.inspect({
  kind: "workbook,sheet,table",
  maxChars: 6000,
  tableMaxRows: 6,
  tableMaxCols: 20,
  tableMaxCellChars: 120,
});
console.log(summary.ndjson);

let editedRows = [];
for (const sheet of workbook.worksheets.items) {
  const usedRange = sheet.getUsedRange();
  if (usedRange) {
    const values = usedRange.values;
    for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
      const id = values[rowIndex]?.[0];
      if (!relevantIds.has(id)) continue;

      editedRows.push({
        sheet: sheet.name,
        row: rowIndex + 1,
        id,
        componentParams: values[rowIndex]?.[14],
        maxStackSize: values[rowIndex]?.[19],
      });

      if (mode === "edit") {
        if (id === "erebus") {
          const params = {
            markId: "poison",
            markStableDurationTicks: 200,
            minRadius: 3,
            maxRadius: 7,
            radiusStep: 1,
            verticalHalfHeight: 3.0,
            respectWalls: false,
            cooldownTicks: 20,
            rangeResetDelayTicks: 100,
            baseStacks: 4,
            bonusStacks1: 6,
            bonusChance1: 0.75,
            bonusStacks2: 7,
            bonusChance2: 0.4,
            showRadiusMessage: true,
            showResetMessage: true,
            showAffectedTargetCount: true,
          };
          sheet.getCell(rowIndex, 14).values = [[JSON.stringify(params)]];
        } else if (id === "bone_flask" || id === "corruption_bomb") {
          sheet.getCell(rowIndex, 19).values = [[1]];
        }
      }
    }

    if (mode === "edit" && sheet.name === "组件参数说明书 (Guide)") {
      for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
        const componentName = values[rowIndex]?.[0];
        if (componentName === "伊塔尼斯") {
          sheet.getRangeByIndexes(rowIndex, 1, 1, 3).values = [[
            "传说之弓专属机制：左键切换普通/蓄力。普通箭全部追踪，三种额外箭概率独立计算；同一目标被多支伊塔尼斯箭命中时，每支箭的伤害都会独立结算。蓄力时每1秒生成一支40%武器攻击力的浮游追踪箭，松手后索敌发射；满蓄力额外发射5000伤害贯穿箭，并获得抗性提升II。",
            "normalDrawTicks(普通拉弓Tick，默认20), bonusChance1/2/3(独立额外箭概率0-1), bonusMultiplier1/2/3(额外箭伤害倍率), formationIntervalTicks(浮游箭生成间隔Tick，默认20即1秒), formationDamageMultiplier(浮游箭倍率，默认0.4), maxChargeTicks(完整蓄力Tick，默认200即10秒), fullChargeDamage(满蓄力贯穿箭伤害，默认5000), targetRange(索敌半径，1-128), formationRemainTicks(松手后浮游箭保留Tick，默认100)",
            "{\"normalDrawTicks\":20,\"bonusChance1\":0.85,\"bonusMultiplier1\":0.35,\"bonusChance2\":0.65,\"bonusMultiplier2\":0.60,\"bonusChance3\":0.25,\"bonusMultiplier3\":0.90,\"formationIntervalTicks\":20,\"formationDamageMultiplier\":0.40,\"maxChargeTicks\":200,\"fullChargeDamage\":5000.0,\"targetRange\":32.0,\"formationRemainTicks\":100}",
          ]];
        } else if (componentName === "厄瑞波斯") {
          sheet.getRangeByIndexes(rowIndex, 1, 1, 3).values = [[
            "诅咒之物：右键展开圆形剧毒领域，半径从3格逐次扩大至7格，1秒冷却，连续5秒不使用重置。必定施加4层剧毒，并分别独立判定75%额外6层与40%再次额外7层。",
            "markId: 印记ID；markStableDurationTicks: 本次施加的稳定期；minRadius/maxRadius/radiusStep: 范围参数；cooldownTicks: 冷却；baseStacks: 必定层数；bonusStacks1/bonusChance1、bonusStacks2/bonusChance2: 两组独立额外层数与概率",
            "{\"markId\":\"poison\",\"markStableDurationTicks\":200,\"minRadius\":3,\"maxRadius\":7,\"radiusStep\":1,\"cooldownTicks\":20,\"baseStacks\":4,\"bonusStacks1\":6,\"bonusChance1\":0.75,\"bonusStacks2\":7,\"bonusChance2\":0.40}",
          ]];
        }
      }
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
  const finalRows = [];
  for (const sheet of workbook.worksheets.items) {
    const values = sheet.getUsedRange()?.values ?? [];
    for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
      const id = values[rowIndex]?.[0];
      if (relevantIds.has(id)) {
        finalRows.push({
          sheet: sheet.name,
          row: rowIndex + 1,
          id,
          componentParams: values[rowIndex]?.[14],
          maxStackSize: values[rowIndex]?.[19],
        });
      }
    }
  }
  console.log(JSON.stringify({ finalRows }, null, 2));

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
