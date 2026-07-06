# DarkGrey Mod 架构与性能排查修复计划

> 排查日期：2026-07-06。目标环境：MC 1.7.10 / Forge / KCauldron 多人服务器。
> 本文档供二次排查与执行修复使用。每项均给出 位置 / 问题 / 影响 / 修复方案。
> 执行顺序建议：P0 → P1 → P2。P0 中 #1、#3 是确定的运行时 bug，#2 是线上多人服环境下的崩溃隐患。

---

## P0 严重问题（全局状态污染 / 线程安全 / 内存泄漏）

### 1. `DamageSource.magic` 全局静态对象被永久污染
- **位置**：`src/main/java/com/greyhat/dark_grey/api/impl/ComponentBloodSacrifice.java:72` 和 `:75`
- **问题**：`DamageSource.magic.setDamageBypassesArmor().setDamageIsAbsolute()` —— `DamageSource.magic` 是 vanilla 的全局静态单例，`setDamageIsAbsolute()` 是对它的**永久性修改**。血祭组件第一次触发暴击后，全服所有来源的魔法伤害（药水、其它 mod、本 mod 的 `EntityAuraTorrent`/`EntityPhantomStrike` 等）都会变成无视抗性/附魔的绝对伤害。
- **影响**：全局游戏平衡被静默破坏，且极难排查（症状出现在完全无关的地方）。
- **修复**：改为新建实例：`new DamageSource("darkgrey.bloodsacrifice").setDamageBypassesArmor().setDamageIsAbsolute().setMagicDamage()`，可提取为该组件的 `private static final` 字段。
- **附带**：`component/ComponentBloodSacrifice.java`（另一个同名类）是**死代码**（`DarkGrey.java:83` 注册的是 `api.impl` 版本），且两者反噬语义不同（一个按伤害百分比、一个按最大生命百分比）。确认保留哪套语义后删除另一个，避免后续维护改错文件。

### 2. WatchService 后台线程直接执行 reload，跨线程操作游戏对象
- **位置**：`src/main/java/com/greyhat/dark_grey/api/RPGItemDataManager.java:84-119`（watcher 线程）、`:180-190`（reload 内部）
- **问题**：`DarkGrey-RPG-Watcher` 守护线程检测到 JSON 变化后直接调用 `reload(true)`，该方法在**非主线程**：
  1. 遍历 `Item.itemRegistry` 并调用 `rebuildComponents()`，替换各 Item 的组件列表字段（这些字段无 volatile/无同步，服务器主线程正在并发读取它们做事件分发）；
  2. 调用 `MinecraftServer.getConfigurationManager().sendChatMsg(...)` —— 1.7.10 网络层非线程安全。
- **影响**：数据竞争导致主线程可能读到半构建的组件列表（不可见/空指针）；跨线程发包在 KCauldron 上可能触发 ConcurrentModificationException 或数据包错乱，属偶发崩溃隐患。
- **修复**：watcher 线程只置一个 `volatile boolean reloadPending = true` 标志；注册一个 `FMLCommonHandler` 的 `ServerTickEvent`（Phase.START）处理器，在主线程检测标志并执行 `reload(true)`。`Thread.sleep(500)` 防抖逻辑可保留在 watcher 侧。
- **验证**：修改 config/dark_grey/rpg_items.json，确认热重载仍生效且日志在主线程（`Server thread`）打印。

### 3. `EntityPhantomStrike` 客户端幽灵实体永不消亡（客户端内存泄漏 + 粒子刷屏）
- **位置**：
  - `src/main/java/com/greyhat/dark_grey/entity/EntityPhantomStrike.java:48`：`if (!this.worldObj.isRemote && this.ticksAlive > 50)` —— 寿命判定**只在服务端生效**；
  - `src/main/java/com/greyhat/dark_grey/component/ComponentSolarFlare.java:170-178`：客户端分支手动 `world.spawnEntityInWorld(phantom)` 生成了一个纯客户端实体。
- **问题**：该实体已用 `registerModEntity(..., sendVelocityUpdates=true)` 注册，服务端生成后会自动同步到客户端，客户端再手动 spawn 一份是重复的。且这份客户端本地实体不受服务端 `setDead()` 控制，自身寿命检查又被 `!isRemote` 挡掉——只要不撞墙就**永远存活**，每 tick 生成 30 个粒子。
- **影响**：客户端实体列表持续增长（内存泄漏）、粒子系统被占满（fps 下降）、视觉效果重影。
- **修复**：
  1. `EntityPhantomStrike.onUpdate()` 中寿命判定去掉 `!this.worldObj.isRemote` 条件（两端都在 50 tick 后 setDead）；
  2. 删除 `ComponentSolarFlare.onUsingTick` 中 `else`（isRemote）分支里手动 spawn phantom 的代码（`:170-178`），墙面撞击分支 `:116-117` 同样只应在服务端 spawn（当前无 isRemote 判断，两端都会执行，同样产生客户端幽灵实体——需一并修复）。

### 4. `onLivingUpdate` 给全服每个实体每 tick 强制创建/查询 EntityData NBT
- **位置**：`src/main/java/com/greyhat/dark_grey/event/RPGCoreEventHandler.java:311-322`
- **问题**：`LivingUpdateEvent` 对**每个活体实体每 tick** 触发，处理器无条件调用 `entity.getEntityData()`。1.7.10 中 `Entity.getEntityData()` 是惰性创建：第一次调用就为该实体永久分配一个 `NBTTagCompound` 并挂在实体上。也就是说这个 handler 会给全服所有活体实体（村民、动物、刷怪笼怪物…）都分配一个本不需要的 NBT compound，且每 tick 做一次 `hasKey` 字符串哈希查找。
- **影响**：服务器实体量大时（该服常年数千实体）是每 tick 的固定开销 + 每实体约几十字节的无谓内存，并且这些 NBT 会随实体一起序列化进区块存档，放大 NBT 读写量（该服已确认 NBTFixer/NBT 相关是最大延迟源，应避免再往实体 NBT 里加数据）。
- **修复**：焦灼印记改用内存 Map 而非 EntityData：
  1. 在 handler 中建 `private static final WeakHashMap<EntityLivingBase, Integer> SCORCHED_MARKS`（弱引用，实体卸载自动清理；印记本身只有 5 秒，不需要持久化）；
  2. `ComponentSolarFlare`、`EntityPhantomStrike` 写印记处（`ComponentSolarFlare.java:157`、`EntityPhantomStrike.java:80`）改为写入该 Map（提供一个静态工具方法，如 `ScorchedMarkTracker.mark(entity)` / `getTimer` / `clear`）；
  3. `onLivingUpdate` 改为只在 `SCORCHED_MARKS` 非空时遍历该 Map 递减计时（可改在 `ServerTickEvent` 里统一递减，彻底不订阅 LivingUpdateEvent），`onLivingHurt` 的引爆判定（`RPGCoreEventHandler.java:66`）同步改为查 Map。
  4. 同理 `SolarDashHasHit`（`ComponentSolarFlare.java:50,123,137`）也用玩家 EntityData，可顺带迁移到同类内存结构（低优先，量小）。

---

## P1 性能热点与逻辑缺陷

### 5. 热重载只重建 `ItemRPGWeapon`，其余 7 种物品类型组件不更新
- **位置**：`src/main/java/com/greyhat/dark_grey/api/RPGItemDataManager.java:180-184`
- **问题**：reload 时只对 `instanceof ItemRPGWeapon` 调 `rebuildComponents()`，而 Tool/Hoe/Bow/Armor/Accessory/Ammo/Lance/Scythe 等都实现了 `IRPGItemContainer.rebuildComponents()` 却不被调用。
- **影响**：热重载后武器行为更新、其它装备仍是旧组件，数据不一致；也是运营改数值时"改了没生效"的隐患。
- **修复**：条件改为 `obj instanceof IRPGItemContainer`，调用接口方法。

### 6. 每物品每 tick 的 `getConfig()` 同步锁竞争（6 个物品类的 `onUpdate`）
- **位置**：`ItemRPGWeapon.java:431-488`、`ItemRPGTool.java:269-295`、`ItemRPGAccessory.java:166-192`、`ItemRPGHoe.java:206+`、`ItemRPGBow.java:144+`、`ItemRPGArmor.java:149+`（func_77663_a）
- **问题**：背包里每个 RPG 物品每 tick 都会先 `dataManager.getConfig(rpgItemId)`（进入 `synchronized(configCache)` 块 + HashMap 查找），然后才做 version 比对提前返回。锁和查找发生在快路径上。多人服 + 满背包 RPG 装备时是每 tick 大量无谓的同步开销。
- **修复**（改 `RPGItemDataManager` + 各 onUpdate 的快路径顺序）：
  1. `dataVersion` 字段改为 `volatile int`；
  2. onUpdate 快路径重排：先读 NBT 的 `DarkGreyRPG_DataVersion` 与 `dataManager.getDataVersion()` 比对，**相等则直接 return，不调 getConfig**；只有 version 不匹配才走 getConfig + sync；
  3. 顺带修复：当前代码对没有 NBT 的 stack 每 tick `new NBTTagCompound()` 并 setTagCompound（首 tick 一次性行为，可接受，但重排后应仅在确实需要 sync 时创建）；
  4. 可选进一步：`configCache` 换成"不可变 Map + volatile 引用整体替换"（reload 时构建新 Map 后一次性赋值），读路径完全无锁，`getConfig` 去掉 synchronized。
- **注意**：本修复与 #10（代码去重）一起做最省事——先提取公共类再改一处即可。

### 7. `getItemAttributeModifiers()` 每次调用都新建 Multimap 并解析 UUID
- **位置**：`ItemRPGWeapon.java:353-393`、`ItemRPGTool.java:225-245`
- **问题**：1.7.10 中 `EntityLivingBase` 每 tick 对比手持物品属性时会调用该方法；当前实现每次 `HashMultimap.create()` + `UUID.fromString("CB3F55D3-...")`（字符串解析）+ new AttributeModifier。
- **影响**：持续 GC 压力（服务器 + 客户端 tooltip 渲染都在调）。
- **修复**：
  1. `UUID.fromString(...)` 提为 `private static final UUID ITEM_DAMAGE_UUID`（vanilla 的 `Item.field_111210_e` 即此值，也可直接引用）；
  2. 将构建好的 Multimap 缓存为实例字段，附带记录构建时的 dataVersion；当 `RPGItemDataManager.getDataVersion()` 变化时才重建（damage 数值来自 config，只有 reload 会变）。

### 8. `ComponentSolarFlare` 冲刺中断后 `stepHeight` 永久残留为 1.0
- **位置**：`src/main/java/com/greyhat/dark_grey/component/ComponentSolarFlare.java:59`（每 tick 置 1.0F）、`:99` / `:143`（`clearItemInUse()` 中断）、`:184-187`（仅 `onPlayerStoppedUsing` 恢复 0.5F）
- **问题**：1.7.10 的 `clearItemInUse()` **不会**触发 `onPlayerStoppedUsing` 回调。撞墙或撞人中断冲刺时 stepHeight 停留在 1.0，玩家此后永久自动上一格台阶，且服务端/客户端可能不一致。
- **修复**：在两个 `clearItemInUse()` 调用点之前手动 `player.stepHeight = 0.5F`；或封装一个 `endDash(player)` 统一做清理。

### 9. `EntityPhantomStrike` 对沿途实体每 tick 重复施加全额伤害
- **位置**：`src/main/java/com/greyhat/dark_grey/entity/EntityPhantomStrike.java:61-82`
- **问题**：存活 50 tick，每 tick 对 AABB 内所有实体 `attackEntityFrom(600% 伤害)`，无"已命中"去重（只靠 vanilla `hurtResistantTime` 兜底，实际每目标仍可被打 ~5 次）。每 tick 还做一次 AABB 实体扫描。
- **修复**：加 `Set<Integer> hitEntityIds`（存 entityId），命中过的跳过；这样既符合"贯穿一次"的技能语义，也减少无效的 attackEntityFrom 调用（该服曾确认第三方 mod 在伤害事件链上有昂贵逻辑，减少伤害事件次数收益明显）。

### 10. 粒子生成量过大（客户端卡顿来源）
- **位置**：
  - `EntityScythe.java:45-65`：每 tick 60 次循环 ≈110 个粒子 × 20 tick；
  - `ComponentTrueLawOfCycles.renderCrestParticles`（`:97-159`）：满蓄力时每 tick 数百个粒子（外环 60 + 内环 30 + 6 条射线×若干 + 6×15 节点环）；
  - `EntityAuraTorrent.java:56-89` / `ComponentAuraTorrent.onUsingTick`：半径 10 时每 2 tick ~90 个。
- **影响**：1.7.10 粒子上限 4000，一个玩家放技能即可占满其他玩家客户端的粒子预算，低配机型 fps 骤降。
- **修复**：不改视觉设计的前提下：
  1. 给密度乘一个可配置系数（读 `Config`，默认 0.5）；
  2. 距离裁剪：粒子生成前判断 `Minecraft.getMinecraft().thePlayer.getDistanceSq < 64*64`（这些都是 client 侧 spawnParticle，直接在生成处判断即可）。

### 11. 弓箭命中回调取的是"当前手持物"而非射出箭的那把弓
- **位置**：`src/main/java/com/greyhat/dark_grey/event/RPGCoreEventHandler.java:136-147`
- **问题**：箭命中时通过 `attacker.getCurrentEquippedItem()` 判断是否 RPG 弓。箭飞行期间玩家切换手持物品，组件就错触发/漏触发。
- **修复**：射出时在箭实体上记录来源（`EntityArrow` 的 EntityData 或维护 `WeakHashMap<EntityArrow, ItemRPGBow>`），命中时按记录分发。若接受当前行为，至少在代码中注释说明这是已知近似。

### 12. `IOnRightClick` 组件返回 `null` 有崩溃风险（需验证）
- **位置**：`ComponentTrueLawOfCycles.onRightClick`（`:61`、`:77` 返回 null）→ 经 `ItemRPGBow`/`ItemRPGWeapon.onItemRightClick` 链式返回。
- **问题**：1.7.10 `ItemInWorldManager.tryUseItem` 拿到 null 返回值后会执行 `itemstack1.stackSize` 判断，存在 NPE 使物品消失/服务端报错的风险（取决于调用路径，需实测右键触发"落地时释放"分支）。
- **修复**：组件接口约定"取消"语义不用 null 表达——返回原 stack 不变；若需要阻止蓄力，用 `player.clearItemInUse()`（`ComponentAuraTorrent.java:67` 已是这种写法，统一即可）。

---

## P2 架构清理（不影响运行，但降低后续维护成本）

### 13. 6 个物品类复制粘贴了同一套 sync/parse/rebuild 逻辑（每类约 200 行）
- **位置**：`ItemRPGWeapon` / `ItemRPGTool` / `ItemRPGHoe` / `ItemRPGBow` / `ItemRPGArmor` / `ItemRPGAccessory` 中的 `onUpdate`、`syncEnchantments`、`parseEnchantments`、`rebuildComponents`、`getSubItems`。
- **问题**：完全相同的逻辑六份拷贝（已经出现漂移：Weapon 版有日志、Tool 版没有）。#6 的性能修复要改六处。
- **修复**：提取 `RPGItemStackSync` 静态工具类（`syncIfVersionChanged(stack, itemId)`、`syncEnchantments(...)`、`parseEnchantments(...)`）和 `RPGComponentHolder`（封装 allComponents + 各 capability 子列表 + rebuild 逻辑），物品类只保留委托调用。`NBT_TRACKER_TAG`/`NBT_VERSION_TAG` 常量收敛到一处。

### 14. 玩家死亡分发用反射兜底
- **位置**：`RPGCoreEventHandler.firePlayerDeathOnStack`（`:286-298`），每次玩家死亡对非 Weapon/Armor/Bow 物品做 `getClass().getMethod("getPlayerDeathHandlers")` 反射。
- **修复**：`IRPGItemContainer` 接口增加 `List<IOnPlayerDeath> getPlayerDeathHandlers()`（各类已有该方法，补 `@Override` 即可），分发处改为 `instanceof IRPGItemContainer`，删除反射分支。

### 15. 死代码与包结构混乱
- `component/ComponentBloodSacrifice.java`：死代码（见 #1），删除。
- `api/impl/` 与 `component/` 两个包都放组件实现，无明确边界 → 统一合并到 `component/`（`api/` 只留接口与框架）。
- 多个文件带 `Decompiled by Procyon` 头和乱码注释（`DarkGrey.java`、`SetBonusManager.java`、`ItemRPGWeapon.java` 的乱码分隔线等）：清理头注释、修复乱码、统一格式（这些文件是反编译恢复的，注意恢复期间不要动逻辑）。
- `DarkGrey.java:86-87`："炬火的残光"/"炬火残光" 双名注册同一组件是兼容旧配置的 hack，加注释说明或在数据侧统一后删除其一。

### 16. `EntityVampire` 空壳 + 使用全局实体 ID
- **位置**：`common/EntityVampire.java`（空的 EntityMob 子类）、`DarkGrey.java:92-93` 用 `registerGlobalEntityID` 注册。
- **问题**：全局实体 ID 是稀缺资源（0-255，与其它 mod 冲突风险），且该实体无 AI、无属性、无渲染注册，疑似未完成的遗留物。
- **修复**：若无用直接删除注册与类；若要保留改用 `registerModEntity`。**注意**：删除前确认现有存档/世界中没有已生成的 vampire 实体（否则加载报 missing entity，需先清档或保留注册）。

### 17. 实体 owner/caster 不序列化
- **位置**：`EntityScythe`（owner）、`EntityAuraTorrent`（caster）、`EntityPhantomStrike`（thrower）均为瞬态字段，NBT 读写不含它们。
- **问题**：区块卸载重载后 owner 为 null：AuraTorrent 会开始伤害施法者自己；Scythe 伤害来源变 magic。这些实体寿命都 ≤10 秒，出现窗口小。
- **修复**（低成本方案）：`readEntityFromNBT` 里若关键字段缺失直接 `setDead()`（短寿命技能实体不值得跨存档恢复）。

### 18. 组件实例是 Item 级单例——写明约束
- **背景**：组件实例挂在 Item 上，而 Item 是全局单例；所以**组件字段 = 全服共享状态**。当前实现里冷却存 stack NBT（正确）、`ComponentBloodSacrifice` 只有 Random（无害），但这是一个很容易踩的坑（例如把"蓄力进度"存组件字段就会串号）。
- **修复**：在 `IRPGComponent` 接口 javadoc 顶部加显著说明："组件实例被所有玩家、所有 ItemStack 共享，禁止存储 per-player / per-stack 可变状态；此类状态必须放 ItemStack NBT 或以实体为 key 的 WeakHashMap。" 同时 `SetBonusManager` 的 representative-instance 机制（`SetBonusManager.java:42`）依赖此约束，一并注明。

### 19. 杂项（顺手修）
- `ComponentTrueLawOfCycles.onRightClick`（`:67`）用 `getUnlocalizedName()` 字符串匹配找光之矢，而 `onBowShoot`（`:173-175`）用 `IRPGItemContainer.getRpgItemId()` —— 统一为后者。
- `RPGItemRenderer.renderItem`（`client/render/RPGItemRenderer.java:36`）每帧 `new ResourceLocation(...)` —— 提为构造时创建的 final 字段（其余几个 renderer 同查）。
- `RPGItemDataManager.reload` 每个物品打一行 `FMLLog.info`（`:167`）——物品多时刷日志，降为 debug 或汇总打印。
- `RPGCoreEventHandler.onLivingHurt` 的焦灼引爆会在 LivingHurtEvent 内再触发 attackEntityFrom（`:86-91`）——已有 `isExplosion` 防自递归，建议加注释说明防递归依据，防止后续改动破坏。

---

## 建议执行批次

| 批次 | 内容 | 风险 |
|------|------|------|
| 1 | #1（DamageSource 污染）、#5（热重载遗漏）、#8（stepHeight）、#12（null 返回） | 低，改动局部 |
| 2 | #2（线程安全）、#3（幽灵实体）、#4（EntityData → WeakHashMap） | 中，需实测热重载与技能表现 |
| 3 | #13（去重提取）+ #6/#7（性能，基于提取后的公共类改一处） | 中，涉及 6 个类重构 |
| 4 | #9、#10、#11、P2 全部 | 低 |

## 复查结论（2026-07-06 第二轮核查）

第一轮修复已核查：P0 #1-#4、P1 #5-#12、P2 #13-#17、#19 均已正确落地，`gradlew compileJava` 通过。
额外确认了一个关键改进：`DarkGrey.java:110` 将事件处理器同时注册到了 FML 总线——1.7.10 中 `TickEvent` 只在 FML 总线上分发，修复前 `PlayerTickEvent` 处理器从未被触发过（即套装加成/穿脱事件此前一直是死代码）。**这意味着套装系统现在才真正开始生效，上线后注意平衡性变化。**

### 残留问题（2026-07-06 第三轮已全部处理，见文末"最终核查结论"）

**R1（回归，必须修）：`RPGItemStackSync.syncIfVersionChanged` 丢失了服务端守卫**
- 位置：`api/RPGItemStackSync.java:23`，调用点 `ItemRPGWeapon.java:201`、`ItemRPGTool.java:272`、`ItemRPGHoe.java:207`、`ItemRPGBow.java:156`、`ItemRPGAccessory.java:168`、`ItemRPGArmor.java:150`
- 问题：重构前每个 `onUpdate` 都有 `if (world == null || world.isRemote) return;`，重构后六处全部丢失，附魔同步逻辑现在**客户端也会执行**。专用服务器场景下，客户端 mod 有自己的 `RPGItemDataManager` 实例和独立的 dataVersion 计数（从 1 起算，与服务端不同步）：服务端把 NBT 版本号写成 N 发给客户端，客户端本地版本是 M，M≠N 时客户端每次收到物品同步包都会本地重写一遍 NBT（改 ench 列表、写版本 M），下一个服务端同步包又覆盖回 N——循环往复，造成附魔光效闪烁、物品栏抖动和无谓的每 tick NBT 操作。
- 修复：给 `syncIfVersionChanged` 增加 `World world` 参数，方法开头 `if (world == null || world.isRemote) return;`，六个调用点传入 world。`forceSync`（getSubItems 用，客户端展示）保持现状不加守卫。

**R2（清理）：`ComponentSolarFlare.onPlayerStoppedUsing` 残留死代码**
- 位置：`component/ComponentSolarFlare.java:181`：`player.getEntityData().removeTag("SolarDashTicks")` —— `SolarDashTicks` 这个 tag 已无任何写入方，且 `getEntityData()` 会为玩家惰性创建 NBT。删除该行（保留 `stepHeight = 0.5F`）。

**R3（运行时验证项）：EntityVampire 注册方式变更的存档兼容**
- `DarkGrey.java:93` 从 `registerGlobalEntityID` 改为 `registerModEntity` 后，实体存档名由全局 `"vampire"` 变为 `"dark_grey.vampire"`。若线上世界中存在旧注册方式生成的 vampire 实体，区块加载时会报 "Skipping Entity with id vampire" 并丢弃。该实体是无 AI 空壳、大概率从未生成过，但部署前建议在服务器日志中 grep 一次 `Skipping Entity` 确认。

### 复查时确认无需处理的点
- `ScorchedMarkTracker` 的 synchronized + WeakHashMap 组合正确；客户端线程也会写入 `SOLAR_DASH_HAS_HIT`（onUsingTick 双端执行），但单机模式下客户端/服务端玩家是不同实体对象、专服下是不同进程，不会串数据。
- `ArrowTracker` 的 `WeakHashMap<EntityArrow, ItemStack>` 无泄漏（value 不持有 key 引用，箭消失后条目自动回收）。
- `getAllConfigs()` 现在返回不可变 Map 本体而非拷贝——现有唯一调用方 `RPGItemLoader` 只读遍历，安全。
- `dataVersion++` 非原子，但写入方已收敛到主线程单写者，安全。

## 验证清单（每批次后）
1. `gradlew build` 通过；
2. 单机进入世界：拿剑/弓/斧各一件，确认 tooltip、附魔同步、右键技能正常；
3. 修改 `config/dark_grey/rpg_items.json` 数值，验证热重载对**所有**物品类型生效且无并发报错；
4. 耀斑冲锋撞墙/撞怪后，走一格台阶验证 stepHeight 已复位；客户端 F3 观察实体数（`E:` 计数）在技能释放后回落；
5. 血祭触发暴击后，喝一瓶伤害药水/被女巫攻击，确认魔法伤害仍受抗性药水影响（验证 #1 修复）；
6. 服务器（KCauldron）上开 timings/森林观察 `LivingUpdateEvent` handler 与 `onUpdate` 的占比变化。

---

## 最终核查结论（2026-07-06 第三轮）

R1/R2/R3 已全部闭环，`gradlew compileJava` 通过，全 mod 源码中已无任何 `getEntityData()` 调用、无全局 DamageSource 污染、无 `LivingUpdateEvent` 订阅、无组件 `return null`。

- **R1 已修**：`RPGItemStackSync.syncIfVersionChanged` 增加 `World` 参数并在开头做 `world == null || world.isRemote` 守卫；六个物品类调用点（Weapon/Tool/Hoe/Bow/Accessory/Armor）全部传入 world。
- **R2 已修**：第二轮改动把耀斑冲刺的终止机制从 `clearItemInUse()` 改成了 `SolarDashHasHit` 标志短路（设计更优，`onPlayerStoppedUsing` 能正常触发），但把标志存储退回了玩家 EntityData，且删除了 `ScorchedMarkTracker` 中的对应方法。本轮统一：标志全部走 `ScorchedMarkTracker.set/getSolarDashHasHit`（内存 WeakHashMap，不写玩家存档），Tracker 中补回方法，`onPlayerStoppedUsing` 中的死代码 `removeTag("SolarDashTicks")` 已删除。
- **R3 已验证**：服务器三份 fml-server 日志中无 `Skipping Entity with id vampire`、无任何 vampire 实体记录，`EntityVampire` 注册方式变更无存档兼容风险。

第三轮同时复核未被第二轮改动破坏的关键修复：血祭静态 DamageSource、双总线注册（DarkGrey.java:161-164）、reloadPending 主线程消费、PhantomStrike 双端寿命判定与命中去重、热重载全类型 rebuild——均完好。

---

## 热修（2026-07-06 第四轮）：耀斑冲锋撞击实体无伤害

- **症状**：冲锋撞上实体只有反弹和音效，无任何伤害。
- **根因**：第二轮重构把撞墙分支从 `clearItemInUse()` 改为"任意速度下置 `SolarDashHasHit=true`"，叠加 `onUsingTick` 顶部的标志早退。`isCollidedHorizontally` 在服务端（EntityPlayerMP）只随移动包的 `moveEntity` 更新，玩家静止起手时保留上一次贴墙行走的陈旧 `true`——冲锋第 1 tick 服务端就被置标志，整个按住右键期间服务端每 tick 早退、从不执行 `!world.isRemote` 内的伤害逻辑；客户端碰撞字段每 tick 重算为 false，照常检测命中并播放反弹/音效，故表现为"有效果无伤害"。
- **修复**：`ComponentSolarFlare.onUsingTick` 撞墙分支加入速度门槛：`isCollidedHorizontally && actualHorizontalSpeed >= 0.6` 才判定为撞墙中断（置标志 + 反冲 + 幻影），低速下的碰撞残留值直接忽略、冲锋继续。已编译通过。
- **验证**：贴墙站立→右键蓄力冲向怪物，应正常造成 600% 伤害并施加焦灼印记；高速撞墙仍应中断并触发幻影贯穿。
