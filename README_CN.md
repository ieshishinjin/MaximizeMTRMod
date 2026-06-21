# MaximizeMTRMod

**Minecraft Transit Railway (MTR 4.x) 客户端性能优化模组**

<div align="center">

<kbd>[English](README.md)</kbd> <kbd>[简体中文](README_CN.md)</kbd>

</div>

纯客户端 Fabric 模组，通过距离剔除、数据同步节流、区块加载平滑等方式降低 MTR 的渲染开销，提升 FPS。装进 `mods/` 即可生效，服务端无需安装。

MTR 4 已将列车调度逻辑独立到后台线程，通过 HTTP 与游戏主线程通信。优化重点全部在客户端渲染和数据同步频率上——本模组围绕这一点设计。

---

## 功能

### 列车渲染距离剔除
`RenderVehicles.render()` 每帧遍历所有列车并进行复杂的模型绘制。当玩家远离所有列车时（地下、家里、其他维度），直接跳过整个渲染调用。

- 阈值：`vehicleMaxRenderDistance`（默认 128 格）
- 配合帧交错功能可使远处列车隔帧渲染

### 方块实体渲染距离剔除
拦截 Minecraft 的 `BlockEntityRenderDispatcher.render()`，对所有 MTR 方块实体（屏蔽门 PSD/APG、信号机、PIDS 显示屏、站名牌、电梯按钮等）统一做距离检查。超出距离即刻取消渲染。

- 阈值：`blockEntityMaxRenderDistance`（默认 56 格）
- 地铁站内 PSD 密集，此优化效果最为明显

### 轨道渲染距离剔除
`RenderRails.render()` 每帧绘制所有节点和轨道段。无轨道或距离太远时跳过。玩家手持有 MTR 物品（画笔、连接器等）时始终渲染，确保放置预览正常显示。

- 阈值：128 格
- 轨道数量多、绘制耗时长，此优化效果显著

### 电梯渲染距离剔除
`RenderLifts.render()` 遍历全部电梯进行渲染。当所有电梯远离玩家时跳过。利用 `@Accessor` 获取电梯内部 `minPosition`/`maxPosition` 字段计算中心坐标。

- 阈值：`liftMaxRenderDistance`（默认 80 格）

### 数据同步频率节流
MTR 4 的客户端通过 HTTP 轮询 Transport Simulation Core 获取列车位置和状态数据。玩家远离所有列车和电梯时降低轮询频率，减少 CPU 开销和 HTTP 请求。

- 全速同步距离：`fullSyncDistance`（默认 40 格）
- 降频周期：`reducedSyncInterval`（默认每 12 tick 一次）
- 超出 `noSyncDistance`（160 格）完全停止同步

### 区块加载性能平滑
客户端无法强制服务器加载区块，但可以检测到列车正驶向未加载区块的时刻，并产生一个「性能压力」信号。渲染模块读取此信号后临时收紧所有渲染距离，在区块加载期间减轻 GPU 负载，平滑掉帧。

- 检测频率：每 200ms
- 压力值范围：0.0（无压力）～ 1.0（最大）
- 压力指数衰减，列车远离后自动恢复

### 渲染帧交错（频率降级）
远处物体每帧都渲染是浪费。当帧计数器不在基准帧上时，收紧 `vehicleMaxRenderDistance`（乘以 0.65），使远处列车在当前帧跳过渲染。由于远处细节人眼不敏感，此优化几乎无视觉感知但 GPU 开销减半。

- 周期：`distantVehicleFrameCycle`（默认 2，即隔帧降距）
- 仅在车辆距离超过 `vehicleFullRenderDistance` 时生效
- 近处列车每帧正常渲染，不受影响

---

## 安装

1. 确保已安装 **Fabric Loader 0.14.0+** 和 **MTR 4.0.0+**
2. 下载 `mmtr-*.jar` 放入 `.minecraft/mods/` 目录
3. 启动游戏，Mod 自动生效

可选依赖：Fabric API（必须）

---

## 配置

首次运行后自动生成 `config/mmtr.json`，编辑后重启游戏生效。

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `vehicleFullRenderDistance` | 40 | 列车完整渲染距离（格） |
| `vehicleMaxRenderDistance` | 128 | 列车超过此距离完全停止渲染 |
| `blockEntityMaxRenderDistance` | 56 | PSD/信号/PIDS/站名牌最大渲染距离 |
| `liftMaxRenderDistance` | 80 | 电梯最大渲染距离 |
| `fullSyncDistance` | 40 | 全速数据同步范围 |
| `reducedSyncDistance` | 80 | 降频同步范围 |
| `reducedSyncInterval` | 12 | 降频同步间隔（tick） |
| `noSyncDistance` | 160 | 超过此距离停止所有同步 |
| `enableChunkPreloading` | true | 启用区块加载预测和平滑 |
| `preloadChunkCount` | 5 | 向前预检测的区块数量 |
| `enableRenderFrequencyReduction` | true | 启用帧交错降频 |
| `distantVehicleFrameCycle` | 2 | 帧交错周期（2=隔帧） |
| `enableDistanceLOD` | true | 启用列车/轨道距离剔除 |
| `enableBlockEntityCulling` | true | 启用方块实体距离剔除 |
| `enableDataSyncOptimization` | true | 启用数据同步节流 |
| `enableLiftCulling` | true | 启用电梯距离剔除 |
| `enableRailCulling` | true | 启用轨道距离剔除 |

---

## 构建

```bash
# macOS / Linux
./gradlew build

# Windows
gradlew.bat build
```

构建产物位于 `build/libs/mmtr-0.0.1.jar`。

---

## 技术架构

```
mmtr/
├── mmtrMod.java                     # Mod 主入口（预加载配置）
├── config/MmtrConfig.java           # JSON 配置管理
└── client/
    ├── mmtrModClient.java           # 客户端入口（注册 Tick 事件）
    ├── chunk/ChunkPreloadManager    # 区块加载压力检测
    ├── lod/LODUtil                  # LOD 工具类（距离+压力+帧交错）
    └── mixin/
        ├── LiftAccessor             # @Accessor 获取电梯位置
        ├── RenderVehiclesMixin      # 列车渲染剔除
        ├── MTRBlockEntityRendererMixin  # 方块实体渲染剔除
        ├── RenderLiftsMixin         # 电梯渲染剔除
        ├── RenderRailsMixin         # 轨道渲染剔除
        └── MinecraftClientDataMixin # 数据同步节流
```

所有 Mixin 均为 `@Inject(HEAD, cancellable=true)` 模式，零侵入 MTR 原有逻辑，失败或禁用时完全不影响原功能。

## 兼容性

兼容 MTR 4.x，不修改渲染管线，与 Sodium / Iris / Entity Culling 无冲突。

## 许可

MIT
