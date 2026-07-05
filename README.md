# DarkGrey Minecraft RPG 模组

DarkGrey 是一款基于 Minecraft 1.7.10 和 Forge 构建的高自由度 RPG 道具与装备模组。项目基于 GTNH (GregTech New Horizons) 1.7.10 稳定版模组开发骨架（ExampleMod）构建，专为拓展高客制化的 RPG 武器、装备、饰品及机制而设计。

整体而言，DarkGrey几乎专为灰之黑开发，代码和素材全部由AI操刀完成，内部代码稳定性如何十分可疑，使用前务必仔细审查相关代码。

---

## 目录

* [核心特色](#核心特色)
* [项目架构与文件结构](#项目架构与文件结构)
* [已注册的 RPG 组件 (Components)](#已注册的-rpg-组件-components)
* [动态 JSON 道具加载与配置说明](#动态-json-道具加载与配置说明)
* [指令列表](#指令列表)
* [开发与编译指南](#开发与编译指南)

---

## 核心特色

- **JSON 数据驱动**：无需编写 Java 代码，即可通过修改 `.minecraft/config/dark_grey/rpg_items.json` 配置文件，动态注册及修改 RPG 剑、弓、盔甲、法杖、工具等。
- **模块化组件设计**：支持通过灵活组合“生命偷取”、“群体切割”、“蓄力法阵”、“全屏扫击”、“DoT伤害流”等多种 RPG 行为组件来拼装一把独特的武器。
- **自定义套装效果 (Set Bonus)**：内置套装检测与属性激活系统，支持类似“超新星套装”二件套生命百分比伤害、四件套生命恢复及增益的效果。
- **自定义动画与渲染**：对部分经典 RPG 武器进行特效级渲染，如“圆环之理”拉弓蓄力特效与箭雨射击动画，以及“灵气洪流”蓄力排斥尘雾圈。

---

## 项目架构与文件结构

本项目将 RPG 的逻辑与表现分离开：
- **`com.greyhat.dark_grey.api`**: 提供 RPG 属性和组件规范的通用接口（如 `IRPGComponent`、套装判定 `SetBonusManager`、加载解析 `RPGItemDataManager`）。
- **`com.greyhat.dark_grey.component` / `api.impl`**: 各类 RPG 特效组件的具体逻辑实现。
- **`com.greyhat.dark_grey.item`**: 继承自 Vanilla Minecraft 物品（如 `ItemBow`、`ItemSword` 等），重构其事件响应逻辑，挂载并触发 RPG 组件钩子（如 `onUsingTick`、`onPlayerStoppedUsing` 等）。
- **`com.greyhat.dark_grey.entity`**: 自定义弹射物（如光之矢 `EntityMadokaArrow`）与持续效能区域实体（如法杖脱手后的 `EntityAuraTorrent`）。

---

## 已注册的 RPG 组件 (Components)

目前模组中已内置注册以下 RPG 组件。在 JSON 中直接使用对应的**中文组件 ID** 即可为武器挂载：

| 中文 ID | 内部类名 | 说明 | 可配置参数 |
| :--- | :--- | :--- | :--- |
| **切割** | `CleaveComponent` | 普攻时对目标周围敌人造成溅射伤害（横扫攻击）。 | `percent`（溅射百分比） |
| **吸血** | `LifestealComponent` | 普攻命中敌人时，按造成伤害的百分比恢复使用者生命。 | `percent`（吸血比例） |
| **超新星** | `ComponentSupernovaSet` | 护甲套装组件。检测玩家装备件数以激活特定增益（2件套按目标最大生命值造成额外真实伤害；4件套击杀瞬间回满生命并获得力量II）。 | `damagePercent`、`buffDuration` 等 |
| **虹之愿** | `ComponentLawOfCycles` | 多段蓄力魔导弓箭组件（虹之弓）。支持3段蓄力，每段蓄力完成有音效和粒子提示，射出多段伤害箭矢。 | `damage1`, `damage2`, `damage3` |
| **圆环之理** | `ComponentTrueLawOfCycles` | 终极弓箭组件（圆环之理）。仅可在滞空时蓄力释放，需消耗“光之矢”，蓄力完成后射出大范围的真实伤害箭雨。 | `damage`, `charge`, `arrowCount` |
| **血祭** | `ComponentBloodSacrifice` | 每次攻击时消耗使用者的一定生命，将其转化为极高额的伤害输出。 | / |
| **劫难** | `ComponentCalamity` | 武器主动技能（厄劫镰刀）。右键释放 360° 毁灭横扫攻击，击退并重伤周围怪物。有独立冷却机制。 | / |
| **灵气洪流** | `ComponentAuraTorrent` | 枯火法杖的主动技能。右键长按蓄力，在腰部产生一圈不断扩大的黑暗压迫感烟环，造成魔法 DoT 伤害与动量压制。松手后在原地留下一个持续10秒的降雨法阵。 | `duration`, `damage`, `cooldown` |
| **炬火残光** | `ComponentTorchAfterglow` | 枯火法杖的被动技能。持有该武器时，每 0.5 秒批量清除玩家身上所有负面药水效果。 | / |

---

## 动态 JSON 道具加载与配置说明

配置文件路径：`run/config/dark_grey/rpg_items.json`
模组会在游戏初始化（Pre-Init）时读取该文件，并自动在游戏中注册对应的物品，赋予其独特的 RPG Lore。

### 示例配置

```json
{
  "items": [
    {
      "id": "charred_fire_staff",
      "type": "法杖",
      "displayName": {
        "zh_CN": "枯火杖",
        "en_US": "Charred Fire Staff"
      },
      "texture": "dark_grey:charred_fire_staff",
      "durability": 1024,
      "damage": 7,
      "components": [
        {
          "name": "炬火残光",
          "params": {}
        },
        {
          "name": "灵气洪流",
          "params": {
            "duration": 200,
            "damage": 20.0,
            "cooldown": 30
          }
        }
      ],
      "enchantments": ""
    }
  ]
}
```

### 属性详解
- **`id`**: 物品唯一标识。
- **`type`**: 物品类别，支持 `"剑"`、`"弓"`、`"头盔"`、`"胸甲"`、`"护腿"`、`"靴子"`、`"法杖"`、`"工具"`、`"箭"` 等。
- **`displayName`**: 多语言显示名。
- **`texture`**: 纹理资源路径（位于 `assets/dark_grey/textures/items/`）。
- **`durability`**: 武器/护甲耐久度。
- **`damage`**: 基础面板物理攻击力。
- **`components`**: 挂载的 RPG 组件列表，在 `name` 传入组件中文 ID，在 `params` 传入对应的配置参数。
- **`enchantments`**: 默认附带的魔咒。格式为 `id:等级,id:等级`（例如 `"16:5,21:2"` 即锋利V与抢夺II）。

---

## 指令列表

模组在服务端提供了一套便捷的指令系统：

- **`/rpg reload`**：重新加载 `rpg_items.json` 配置文件。可在不停机的情况下实时刷新武器属性、耐久度及组件参数。
- **`/rpg help`**：查看当前模组可用的 RPG 指令与帮助。

---

## 开发与编译指南

### 1. 配置工作空间
解压项目到本地后，在根目录使用控制台运行以下命令：
```bash
# 构建 Forge 反混淆环境
./gradlew setupDecompWorkspace
```
如果使用 IntelliJ IDEA，可在运行完上述命令后直接导入 `build.gradle.kts`。

### 2. 编译项目
使用 Gradle 编译打包模组：
```bash
./gradlew build -x spotlessJava
```
编译生成的 `jar` 档案位于 `build/libs/` 下。
