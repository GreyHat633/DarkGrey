import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const outputPath =
  "E:/Java/MinecraftMod/RPGItem/outputs/019fa4a6-022f-7ca2-af4d-7e54a6a77694/RPGItems.xlsx";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(outputPath));
const sheet = workbook.worksheets.getItem("RPG Items");
const values = sheet.getUsedRange().values;
const matches = values
  .map((row, index) => ({ row, index }))
  .filter(({ row }) => row?.[0] === "bone_flask");

if (matches.length !== 1) {
  throw new Error(`Expected one bone_flask row, found ${matches.length}`);
}

const { row, index } = matches[0];
const params = JSON.parse(row[14]);
if (params.directDamage !== 8) {
  throw new Error(`Expected directDamage 8, found ${params.directDamage}`);
}

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 300 },
  summary: "reopened workbook formula error scan",
});
const directDamageReferences = await workbook.inspect({
  kind: "match",
  searchTerm: "directDamage",
  options: { maxResults: 20 },
  summary: "directDamage references",
  maxChars: 5000,
});

console.log(
  JSON.stringify(
    {
      sheets: workbook.worksheets.items.map((item) => item.name),
      row: index + 1,
      id: row[0],
      directDamage: params.directDamage,
      formulaErrorScan: errors.ndjson,
      directDamageReferences: directDamageReferences.ndjson,
    },
    null,
    2,
  ),
);
