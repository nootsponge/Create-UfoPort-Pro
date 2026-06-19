# Create-UfoPort

这是 Create Fabric 面向 Minecraft 1.21.1 的非官方移植版。

这个分支主要目标是在 Fabric 1.21.1 渲染栈上保持 Create 可用，同时将 Create 的渲染集成迁移到 Flywheel 1.x 和 IrisFLW 可接管的 visualizer 管线。

## 当前状态

- Minecraft 目标版本：1.21.1
- Fabric Loader 目标版本：0.16.0 或更新版本
- Fabric API 目标版本：0.105.0+1.21.1 或更新版本
- 内置 Flywheel：面向 Minecraft 1.21.1 的 Flywheel 1.0.6
- 兼容模块：`create_flywheel_compat`
- 发布 jar 会将项目模块作为嵌套 jar 打包到 `META-INF/jars/` 下。

Flywheel 1.x 已内置在 Create-UfoPort jar 中。不要再额外安装 Flywheel 0.x。除非你是在有意测试替换构建，否则也不要额外安装 Flywheel 1.x。

## 安装方式

安装必需运行依赖：

- [Fabric Loader](https://fabricmc.net/use/installer/) 0.16.0 或更新版本
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.105.0+1.21.1 或更新版本
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)

然后将 `build/libs/` 中构建出的 Create-UfoPort jar 放入实例的 `mods` 文件夹。

## 运行环境要求与注意事项

- 需要 Java 21。建议使用稳定的 Java 21 运行时，例如 JetBrains Runtime 21、Eclipse Temurin 21 或 Microsoft Build of OpenJDK 21。
- 不要把几乎全部物理内存都分配给 Minecraft。大型光影/整合包实例应为 Windows、GPU 驱动、native buffer、资源包和启动器留下内存。在 32 GB 系统上，建议从 8-12 GB 开始，只有在证明确实存在 Java 堆压力时再增加。
- 保持 Windows 页面文件开启，最好使用系统管理大小。关闭页面文件可能会把 native/GPU 内存压力放大成整机不稳定，并阻止 Windows 写出有用的崩溃转储。
- Iris 光影支持目标为 Iris + IrisFLW，并使用 `irisflw:iris_instancing` 后端。
- 原生 IrisFLW 光影路径需要 OpenGL 3.3 能力的上下文。Create-UfoPort 默认会在窗口创建前请求 OpenGL 3.3 上下文。
- 测试栈支持 Sodium 0.6.x。不要给 Sodium 0.6.x 添加 Indium；Sodium 0.6+ 已经包含相关 Fabric Rendering API 支持，Indium 不再兼容该版本线。
- Create-UfoPort 默认跳过 IrisFLW 的 Flywheel 实例化阴影 pass，以减少运动 Create 视觉在光影下造成的 GPU 峰值。这可能会降低这些运动视觉的动态阴影准确度。

已知不兼容：

- Farmer's Delight Refabricated

光影/渲染栈中的已知风险：

- Caxton 0.6.4 会报告与 Iris 1.8.8+mc1.21.1 的加载器冲突。
- Ixeris 曾在测试中造成 Windows OpenGL 窗口/上下文原生崩溃。
- PointBlank 的高级 Iris 集成曾在测试中造成黑屏；如果出现该症状，应禁用该功能。

原生 `jvm.dll`、`opengl32.dll`、GPU 驱动或整机崩溃不是普通 Java Mod 崩溃报告。如果发生这类问题，请先测试另一个 Java 21 运行时，检查生成的 `hs_err_pid*.log`，并对图形类 Mod 做 A/B 测试，不要直接假定是普通 Create 异常。

## JVM 启动参数

以下所有参数都是可选 JVM system property。在启动器中，将它们作为 Java/JVM 参数添加，格式为 `-Dname=value`。

推荐预设：

- 正常启动：不添加额外 Create-UfoPort JVM 参数。
- 紧急光影回退：`-Dcreate_ufoport.flywheel.disableIrisFlwWithShaders=true`。
- 在已知崩溃已修复后重试 IrisFLW：单次测试启动使用 `-Dcreate_ufoport.flywheel.forceIrisFlwWithShaders=true`，然后移除。
- 以可能带回 GPU 峰值为代价，恢复更准确的运动视觉光影阴影：`-Dcreate_ufoport.flywheel.disableIrisFlwShadowPassSkip=true`。

Create-UfoPort 光影后端选择：

| 参数 | 默认值 | 效果 |
| --- | --- | --- |
| `-Dcreate_ufoport.flywheel.disableIrisFlwWithShaders=true` | `false` | 禁用 IrisFLW 光影后端路径和 Create IrisFLW mesh-emitter mixin。 |
| `-Dcreate_ufoport.flywheel.allowIrisFlwWithShaders=true` | `false` | 在 Iris 光影包活跃、OpenGL 3.3 可用且崩溃保护干净时，允许使用 `irisflw:iris_instancing`。通常由 `create_flywheel_compat.json` 处理。 |
| `-Dcreate_ufoport.flywheel.autoIrisFlwWithShaders=true` | `false` | 当运行时检查通过时，自动允许光影下使用 IrisFLW，不需要兼容配置显式 opt-in。 |
| `-Dcreate_ufoport.flywheel.forceIrisFlwWithShaders=true` | `false` | 强制在活跃光影下重试 IrisFLW，并绕过未干净结束会话的崩溃保护。仅用于定向测试。 |
| `-Dcreate_ufoport.flywheel.disableIrisFlwCrashGuard=true` | `false` | 禁用 IrisFLW 活跃会话标记和清理保护。仅调试使用。 |
| `-Dcreate_ufoport.flywheel.disableCompatShaderConfig=true` | `false` | 在光影后端和阴影调优决策中忽略 `config/create_flywheel_compat.json`。 |
| `-Dcreate_ufoport.flywheel.forceBackendWithShaders=true` | `false` | 强制旧 `com.jozufozu.flywheel` 兼容后端与光影包一起运行。仅调试使用。 |
| `-Dflywheel.forceBackendWithShaders=true` | `false` | 上游别名，效果等同于强制旧 Flywheel 兼容后端。 |

OpenGL 与 IrisFLW 光影兼容：

| 参数 | 默认值 | 效果 |
| --- | --- | --- |
| `-Dcreate_ufoport.flywheel.disableOpenGl33ContextHint=true` | `false` | 阻止 Create-UfoPort 在窗口创建前请求 OpenGL 3.3。 |
| `-Dcreate_ufoport.flywheel.disableIrisFlwGlslVersionPatch=true` | `false` | 禁用 Create-UfoPort 的 IrisFLW GLSL 兼容补丁。仅调试使用。 |
| `-Dcreate_ufoport.flywheel.forceIrisFlwGlsl330=true` | `false` | 强制使用 IrisFLW GLSL 330 路径，并禁用低版本重写。 |
| `-Dcreate_ufoport.flywheel.irisFlwShaderDumpLimit=<n>` | `6` | 将前 `<n>` 个重写后的 IrisFLW shader 源码 dump 到 `config/create-ufoport-debug/irisflw-shaders`。设为 `0` 可禁用 dump。 |

性能与阴影调优：

| 参数 | 默认值 | 效果 |
| --- | --- | --- |
| `-Dcreate_ufoport.flywheel.skipIrisFlwShadowPass=true` | `false` | 显式启用 IrisFLW Flywheel 实例化阴影 pass 跳过。当前构建默认已经启用该行为，除非被禁用。 |
| `-Dcreate_ufoport.flywheel.disableIrisFlwShadowPassSkip=true` | `false` | 禁用默认阴影 pass 跳过。这可以恢复运动 Create 视觉更准确的动态阴影，但可能带回光影 GPU 峰值。 |

诊断与开发：

| 参数 | 默认值 | 效果 |
| --- | --- | --- |
| `-Dflw.loadRenderDoc=true` | 未设置 | 尝试在客户端启动时加载原生 `renderdoc` 库。需要 RenderDoc 位于系统库路径中。 |
| `-DportingLib.debug=true` | `false` | 启用额外 Porting Lib 调试行为。仅开发使用。 |
| `-Dforge.debugBlockSnapshot=true` | `false` | 启用内置 Porting Lib 代码中的 BlockSnapshot 调试行为。仅开发使用。 |
| `-Dfabric-api.datagen=true` | 未设置 | 启用 Registrate/Fabric 工具使用的数据生成行为。不要用于正常游戏启动。 |

## 从源码构建

使用 Java 21 并运行：

```powershell
.\gradlew.bat --no-daemon --max-workers=1 clean build
```

主输出文件是：

```text
build/libs/create-fabric-ufoport-0.9.3-d+mc1.21.1.jar
```

如果 Gradle 或 TinyRemapper 在 JVM 内部崩溃，而不是报告正常 Java 异常，请优先尝试另一个 Java 21 运行时。在某些本地机器上，临时使用 `-XX:+UseParallelGC -XX:TieredStopAtLevel=1` 可以帮助隔离 JVM/工具链不稳定问题，但玩家正常游戏不应需要自定义 GC 参数。

## 使用的代码来源

- [Build tools](https://github.com/Fabricators-of-Create/Porting-Lib)
- [Create Fabric](https://github.com/Fabricators-of-Create/Create)
- [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib)
- [Flywheel](https://github.com/Engine-Room/Flywheel)
- [Milk Lib](https://github.com/TropheusJ/milk-lib)
- [Registrate Refabricated](https://github.com/Fabricators-of-Create/Registrate-Refabricated)
