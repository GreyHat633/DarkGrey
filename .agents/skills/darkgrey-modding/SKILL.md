---
name: darkgrey-modding
description: 审核、开发、修复和打包 DarkGrey Minecraft 1.7.10 Mod。修改或新增物品、武器、组件、实体、渲染器、网络消息、Excel/JSON 数据、Forge 注册项、热重载或发布 Jar 时必须使用本技能。
---

# DarkGrey 开发规则

先读取仓库根目录的 `.agents/AGENTS.md`，再检查 `git status`、相关源码和实际数据。保留用户已有改动，不要根据旧文档猜测当前架构。

## 1. 明确数据源与发布边界

- 把 `src/main/resources/assets/dark_grey/data/rpg_items.json` 视为发布时的权威物品注册表。
- 把 `E:\Java\MinecraftMod\RPGItem\RPGItems.xlsx` 和 `excel_update.py` 视为编辑/生成源；外部 `rpg_items.json` 不是可单独分发的注册补丁。
- 在启动时允许 `RPGItemDataManager` 用 Jar 内 JSON 覆盖外部工作副本，并把不同的旧文件保存为 `rpg_items.json.previous`。
- 把 `id`、`type` 和 `texture` 视为注册形状。新增、删除、改名或改变注册形状时，重新构建 Jar，并让客户端与服务端使用同一 Jar 后完整重启。
- 只用热重载修改伤害、耐久、附魔和已存在组件的参数。拒绝运行时增删 ID、改变类型/贴图或使用未知组件；解析失败时保留上一份有效配置。
- 服务端重载后把完整有效配置同步给远程客户端；集成服务器本地通道共享单例，不要重复应用。
- 发布版本只修改 `gradle.properties` 中的 `modVersion`，按 `0.1.0`、`0.1.1`、`0.1.2` 递增。运行时产物固定命名为 `dark_grey-<modVersion>.jar`；禁止把 Git 分支、提交数、哈希、`dirty` 或问题描述附加到交付文件名。

## 2. 新增或修改物品的固定流程

1. 先检查工作树、现有物品 ID、类型映射、组件注册和资源文件，避免覆盖用户改动或重复注册。
2. 同步修改 `RPGItems.xlsx`、`excel_update.py` 的 `generate_template()` 默认行、`COMPONENT_GUIDES` 和类型下拉。不要用一次性脚本绕过数据验证，也不要在未核对现有工作簿时盲目执行 `template`。
3. 用 `python excel_update.py export` 导出 UTF-8 JSON。逐项比较生成器、工作簿、外部 JSON 与 Jar 资源中的 `id/type/texture/components`。
4. 对照组件的 `configure(JsonObject)` 核对每一个参数名、单位、默认值和上下限；涉及箭数、蓄力等设计常量时必须把实际值显式写入工作簿、生成器和 JSON，禁止只依赖 Java 默认值，也禁止写入不会被代码读取的键。
5. 把最新 JSON 写入 Jar 资源路径。解析时拒绝空 ID、非 `[a-z0-9_]+` ID、重复 ID、未知类型、未知组件和非对象组件条目。
6. 新增组件时，先实现组件/实体/渲染/网络代码，再在 `DarkGrey.preInit` 中注册组件名，最后调用 `RPGItemLoader.loadItemsFromData()`。新增类型时同步增加容器映射、Excel 下拉和模板默认值。
7. 先构造并验证全部物品与组件，再进行任何 `GameRegistry.registerItem`；任一配置失败都中止初始化，禁止留下半套注册表。
8. 生成物品贴图时使用图像生成能力并明确要求透明 PNG。按当前渲染器需要准备背包图标与 `_equipped` 贴图；不要用自动抠白底代替透明源图。
   - **弓类物品贴图强制方向规范**：所有弓类物品贴图（包括主图标与 `_pulling_0~2` 帧）必须严格遵循 Minecraft 原版弓方向：呈 45 度倾斜放置，**左上方为向外弯曲的弓臂（Limb），右下方为弓弦（Bowstring）**。拉弓动画中弓弦与能量箭矢统一向右下方拉扯。任何反向、水平、垂直或异形朝向均属不合规贴图。
9. 发布前递增 `gradle.properties` 的 `modVersion`，运行完整 Gradle `build`，从 `build/libs` 选择 `dark_grey-<modVersion>.jar`；禁止分发 `-dev.jar`、`-sources.jar` 或带 Git/`dirty` 尾缀的 Jar。打开 Jar 验证内置 JSON、关键类和全部预期 ID。
10. 将同一个运行时 Jar 交付给客户端与服务端并比较 SHA-256。遇到 `Fatally missing blocks and items` 时，先比较 Jar 哈希和双方注册日志，不要单独给玩家发送 JSON。

## 3. 注册、ID 与文本规则

- 优先通过 `IRPGItemContainer#getRpgItemId()` 精确比较 ID，例如 `"arrow_of_light".equals(id)`。不要用 `endsWith` 判断物品；它可能误匹配其他后缀相同的 ID。
- `getUnlocalizedName()` 会返回带 `item.` 前缀的名称。只有在无法取得容器 ID 时才精确比较完整值，例如 `item.dark_grey:arrow_of_light`。
- 删除硬编码注册时搜索全部调用方，只移除旧的 `GameRegistry.registerItem` 和确认无引用的类。不要假定 Forge 会安全忽略重复注册；当前加载器应在注册前直接报错。
- 以 UTF-8 保存 Java、JSON、Markdown 和脚本。当前 Gradle 由现代 JDK 运行，不能笼统假定 Windows 一定按 GBK 编译；在旧工具链或易受编码影响的 `switch(type)` 中可使用 Unicode 转义，但必须通过构建验证。
- 组件实例由物品容器共享。不要把“当前玩家、当前目标、当前蓄力”等玩家级可变状态存到组件字段；把状态放进 ItemStack NBT、实体、玩家数据或受控追踪器。

## 4. 服务端权威与多人安全

- 只在服务端消耗弹药、扣除物品、写入决定性 NBT、结算伤害、生成实体和设置冷却；客户端只做即时声音、粒子、动画和预测，并避免同一效果在两侧重复播放。
- 所有 AoE、贯穿、残影、召唤物和爆炸伤害都走 `CombatTargeting.canDamage` 与 `attackEntityFrom`，以保留 Forge/KCauldron 的伤害、死亡、掉落、PVP 和队伍事件链。
- 默认调用 `CombatTargeting.canDamage(..., false)`，排除创造玩家。只有倒悬时针技能可显式传 `true`；它仍应遵守服务端 PVP 和队伍友伤规则。不要把此例外扩散到其他武器。
- 所有爆炸使用不破坏方块的形式，例如 `createExplosion(..., false)`。禁止技能直接调用 `setBlockToAir`、`destroyBlock` 或自行遍历删除方块；需要破坏能力时必须另行接入领地/保护事件并取得用户明确授权。
- 不要用 `setHealth(0)` 后手动调用 `onDeath`。使用 `attackEntityFrom` 进入正常死亡流程，防止双掉落、重复死亡事件、刷物品或绕过保护插件。
- 最大生命值比例、倍率叠加等同一次近战伤害修正必须实现 `IModifyMeleeDamage`，在原始 `LivingHurtEvent` 的 `LOWEST` 优先级中修改伤害；不要依赖 `damageType == "player"` 等容易被 KCauldron 或其他 Mod 改写的字符串，只排除投射物、魔法、爆炸和明确的间接来源。需要无视护甲/抗性时在同一个 `DamageSource` 上保留 `bypassArmor/absolute` 语义；禁止从 `IOnHit` 再调用 `attackEntityFrom`。高血量目标上的第二次完整伤害事件会让 Forge、KCauldron 和保护/战斗插件重复处理，造成数秒级延迟。
- 确实需要独立结算的血祭、爆炸、持续伤害等二次伤害会重新进入 `LivingHurtEvent`。为这类独立伤害增加 `ThreadLocal`/来源标记并在 `finally` 清理，防止组件递归到栈溢出或重复结算。
- 直接命中后又生成贯穿实体时，把首次目标加入排除集合；持续实体必须按实体 ID 去重，防止每 Tick 对同一目标重复造成高额伤害。

## 5. 实体、网络与生命周期

- 给每个临时实体设置明确的服务端 TTL；无有效所有者、从 NBT 恢复后无法重建语义或越界时立即 `setDead()`，不要让孤儿实体永久 Tick。
- 在实体生成前写好决定视觉的字段。使用 `DataWatcher` 或 `IEntityAdditionalSpawnData` 同步蓄力等级、运动方向、起点、视觉领队等状态；不要依赖客户端猜测服务端构造参数。
- 保持实体注册 ID、追踪距离、更新频率稳定。新增消息时在公共侧注册编解码器，把客户端类访问隔离到 `ClientProxy`，并把消息处理调度回客户端主线程。
- 不要给公共构造器、公共字段或公共调用链上必需的方法添加 `@SideOnly(Side.CLIENT)`；专用服务端会把这些成员剥离并导致 `NoSuchFieldError/NoSuchMethodError`。客户端专用实现应放入 client 包或代理。
- 同时检查 Forge 事件总线与 FML 事件总线注册。实体/伤害事件通常走 `MinecraftForge.EVENT_BUS`，玩家/服务端 Tick 和登录事件通常还需要 `FMLCommonHandler.instance().bus()`。

## 6. 性能与视觉表现

- 保留武器标志性法阵、爆炸和轨迹，但必须把粒子数量乘以 `Config.particleDensity` 并设置硬上限。
- 多箭齐射不要让每支箭都生成完整大型法阵。只指定一支视觉领队生成共享法阵，其余箭保留轻量轨迹；把大爆发放在状态切换或命中瞬间，不要每 Tick 重建复杂几何。
- 禁止每 Tick 扫描 `world.loadedEntityList`。使用局部 AABB、事件回调、实体自身更新或有界追踪器；限制半径、实体数量、持续时间和触发频率。
- 对 JSON 可配置的 `arrowCount`、半径、持续时间、伤害、冷却和蓄力设置上下限，并处理 NaN、负数、零蓄力和极大值。低于首阶段的点击释放不得生成投射物，以免创造/无限玩家刷实体。
- 渲染器必须成对执行 `glPushMatrix/glPopMatrix`，用 `try/finally` 恢复矩阵、颜色、混合、剔除和深度状态。先验证实体/物品类型再强转，避免客户端崩溃和后续场景花屏。
- 高分辨率挤出贴图要把真实宽高传给 `ItemRenderer.renderItemIn2D`。缩放后重新居中，但不要把“只调尺寸”变成无依据的挂点位移。
- `Entity#posY` 不是视线高度。眼部位置使用 `posY + getEyeHeight()`；腰部/身体中心优先基于 `boundingBox.minY` 与 `height` 计算，并在玩家、其他生物和不同姿态下验证。

## 7. 当前武器的不可回退约束

- 厄劫使用 JSON 类型 `镰刀`、容器 `ItemRPGScythe`、技能实体 `EntityScythe`。冷却固定为 60 Tick（3 秒），动画总长 20 Tick、第 10 Tick以玩家为圆心结算半径 5 格的水平圆形横扫，实体跟随持有者腰部附近；粒子环、渲染尺寸、伤害半径、伤害时点和实体 TTL 必须同步修改。
- 虹之愿当前读取 `damage1/damage2/damage3`，蓄力阶段固定为 20/40/60 Tick。不要继续使用不会被组件读取的 `chargeTime1/2/3`；若要配置蓄力阈值，先同步实现代码、模板、说明和边界校验。
- 圆环之理当前仍以 `player.onGround` 作为使用门槛，并精确查找主背包中的 `arrow_of_light`。这是用户明确暂缓处理的行为；不要在无新指示时擅自改成其他飞行判定。
- 圆环之理固定释放 30 支光之矢，每支基础伤害 150。`RPGItems.xlsx`、`excel_update.py`、外部 JSON 和 Jar JSON 必须显式记录 `{"damage":150.0,"charge":100,"arrowCount":30}`；不得用空参数对象依赖 Java 默认值。箭阵应在玩家前方组成第一人称可见、以视线为中心的 6×5 水平阵列：生成面垂直于瞄准方向，每支箭沿瞄准方向近似平行前进。禁止改成从玩家头顶垂直落地的 MMO 式箭雨；`RenderMadokaArrow` 不得为空，也禁止让无重力光矢每Tick增加向上速度，否则实体会飞到极高 Y 坐标并在视觉上叠成一支箭。Lore 固定使用“圆环之理 | 万物起源的永恒循环”“必须滞空时才能展开圆环”“至圆环完整”，蓄力不足时不发送聊天提示。
- 枯火杖的 Debuff 免疫以服务端 `PlayerTickEvent` 的当前手持物检查为权威来源，每 Tick调用一次 `IOnHeldTick`；物品 `onUpdate` 只负责客户端手持视觉，不能依赖其 `isSelected` 参数完成服务端免疫。检查负面状态时先验证药水 ID 是否位于 `Potion.potionTypes` 范围内，并隔离异常的 Mod 药水实现；单个药水错误不得把玩家踢出服务器。
- 枯火杖和耀斑长枪的四张贴图固定保留仓库 `HEAD` 中的会话初始版本；用户已明确要求不要再重绘或生成这两件武器的贴图。除非用户以后再次明确授权，否则新增数值或逻辑修改不得触碰 `charred_fire_staff*.png` 与 `solar_flare_lance*.png`。
- 灵气洪流在蓄力移动期间和松手后的持续法阵中均每 10 Tick（0.5秒）结算一次，每次固定读取配置伤害 250；达到最大蓄力半径后只停止扩张，禁止停止后续伤害。只有 `attackEntityFrom` 成功后才施加 20 Tick的反胃、虚弱 II、失明、缓慢 II 和挖掘疲劳 II，并恢复目标原运动量，避免技能击退。两条路径必须调用同一个服务端效果函数，防止伤害或 Debuff 再次分叉丢失。
- 耀斑冲锋只有玩家碰撞盒与合法实体的碰撞盒真实接触时才能结算伤害、击退、反弹、音效和残影。沿本 Tick 位移的扫掠检测只用于把高速移动截停在最早接触点，禁止直接把扫掠候选当作已命中目标，否则第一人称会提前触发；也禁止扫掠盒隔墙选中目标。实体命中由服务端调用 `attackEntityFrom` 确认，只有返回成功后才能施加印记、反弹、音效、击退和残影，再用客户端消息同步反弹速度；客户端真实接触后等待服务器包时不得把水平速度强制清零，否则网络延迟会表现为“先顿住、后爆炸”。禁止客户端自行预测完整命中反馈，否则 PVP、队伍或保护规则拒绝伤害时会出现假命中。墙体中断不能只信任可能滞后的 `isCollidedHorizontally`，还要检查实际水平速度、起步宽限与当前前向的纯方块碰撞盒，不能把友军或其他实体碰撞盒当成墙。保留“跨一格、撞两格”的设计：前方有碰撞后，把玩家碰撞盒抬高一格再次测试，只有仍被阻挡才触发反弹；高草、花等无碰撞盒方块不得触发。撞墙只反弹和播放墙体音效，不生成伤害残影；直接命中的目标必须从 `EntityPhantomStrike` 排除，残影要同步生成数据、限制 15 Tick 生命周期并去重命中。
- 耀斑实体命中音效必须由服务端以被撞实体为声源广播；不要把命中声绑到同 Tick高速回弹的玩家位置，也不要只在客户端预测播放。
- 耀斑长枪物品基础伤害固定为 100；冲锋、残影和焦灼引爆仍沿用现有倍率逻辑。修改这一数值时必须同步工作簿、生成器和 Jar JSON，不能只改 Java 技能倍率。
- 百分比最大生命值的“切割”组件已经删除，禁止重新注册或写回 JSON/Excel。替代组件“重击”读取 `intervalSeconds`（默认5秒，0-3600）和 `multiplier`（默认4，0-1000000）；触发时只读取当前手持 `ItemStack#getAttributeModifiers()` 中 `generic.attackDamage` 的物品贡献，并额外追加“武器攻击力 × multiplier”的伤害。禁止直接使用玩家完整攻击属性或当前事件伤害，否则会把力量、暴击、附魔/其他修正重复放大；没有正数攻击属性的空手或非武器不得触发、不得消耗冷却。冷却记录在武器 ItemStack NBT 中，并在原始 `LivingHurtEvent` 上追加伤害；不得创建第二次 `attackEntityFrom`，不得设置无视护甲或绝对伤害。两个参数均应支持现有 JSON 热更新链。每次触发后把“已提示就绪”标志清零，由服务端手持 Tick 在冷却真正结束时仅向持有者发送一次“下一次近战命中将触发重击”提示；切换物品后再次手持也应补发尚未提示的就绪通知。
- 超新星两件套禁止再使用目标最大生命值百分比伤害，也禁止读取或写回 `damagePercent` 或旧的固定伤害参数 `damage`。它与武器重击共用武器攻击力倍率与冷却语义，默认 `intervalSeconds=5`、`multiplier=4`，但使用玩家级独立 NBT 冷却，不能占用或重置手持武器的重击冷却；两者同时就绪时可在同一次近战中分别追加“当前手持武器攻击力 × multiplier”的伤害。超新星重击只允许在 `LOWEST LivingHurtEvent` 的直接近战路径结算，不得被弓箭、魔法或爆炸触发，并由套装 Tick 单独提示“超新星重击”就绪。四件套击杀回满与药水增益保持不变。
- 倒悬时针是唯一允许伤害创造玩家的特例。其他爆炸、镰刀、箭雨、残影和印记爆炸都保持 `allowCreative=false`。

## 8. 审核与发布检查清单

至少完成以下静态检查：

- 搜索 `@SideOnly` 是否出现在公共字段、构造路径或服务端会加载的类中。
- 搜索 `setHealth(0)`、手动 `onDeath`、`createExplosion(..., true)`、方块删除、`sendToAll`、无界实体遍历和缺少 TTL 的临时实体。
- 检查所有近战伤害修改组件是否只在 `LOWEST LivingHurtEvent` 中修改原伤害，且没有从 `IOnHit` 再次调用 `attackEntityFrom`。重击必须使用 ItemStack NBT 独立计时、只追加“当前手持物品栈攻击力 × multiplier”且不改变伤害源护甲标志；检查空手/无攻击属性物品不会触发或消耗冷却，独立二次伤害是否有递归保护，AoE 是否经过 `CombatTargeting`，倒悬以外是否误传 `allowCreative=true`。
- 检查圆环之理四处数据源均显式为 30 支，并静态核对生成循环恰好创建 30 个实体；第一人称确认完整 6×5 阵列可见。检查耀斑扫掠路径只截停移动、不提前播放命中效果，实体命中声以目标为声源广播；检查枯火杖由服务端玩家 Tick 每 Tick清除负面状态，灵气洪流超过 40 Tick 后仍持续结算伤害与五重 Debuff。
- 检查所有组件注册发生在 JSON 加载之前，全部类型均有容器映射，组件参数名与 `configure` 完全一致。
- 检查 Forge/FML 两条事件总线、实体注册、渲染注册、网络消息注册与客户端主线程调度。
- 检查渲染器 GL 状态恢复、粒子预算、投射物去重、首次目标排除、实体 TTL 和生成数据同步。
- 解析 Jar 内 JSON，确认 ID 唯一、数量正确、预期 ID 全部存在；比较客户端与服务端 Jar 的 SHA-256。

运行完整构建并区分验证层级：

```powershell
$env:GRADLE_USER_HOME='E:\Java\gradle-home-darkgrey'
$env:JAVA_HOME='E:\Java\jdk-21.0.7'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build --offline --no-daemon --no-configuration-cache
```

若出现 `GradleWorkerMain` 找不到且类路径经过中文/空格目录，优先使用 E 盘 ASCII 路径的 Gradle User Home 或已验证的目录联接；不要把依赖和缓存下载到 C 盘。

最后分别报告：文件修订、静态扫描、编译/完整构建、Jar 内容验证、专用服务端启动、实际多人联机。构建成功不等于已完成专服或多人运行验证。所有实现计划和交付文档使用中文。
