# Create-UfoPort

An unofficial Create Fabric port for Minecraft 1.21.1.

This fork focuses on keeping Create usable on the Fabric 1.21.1 rendering stack while migrating Create's rendering integration toward Flywheel 1.x and IrisFLW-compatible visualizers.

## Current Status

- Minecraft target: 1.21.1
- Fabric Loader target: 0.16.0 or newer
- Fabric API target: 0.105.0+1.21.1 or newer
- Bundled Flywheel: Flywheel 1.0.6 for Minecraft 1.21.1
- Compatibility module: `create_flywheel_compat`
- Release jars embed the project modules as nested jars under `META-INF/jars/`.

Flywheel 1.x is bundled inside the Create-UfoPort jar. Do not install a separate Flywheel 0.x jar alongside it. Do not install a separate Flywheel 1.x jar unless you are intentionally testing a replacement build.

## Installation

Install the required runtime dependencies:

- [Fabric Loader](https://fabricmc.net/use/installer/) 0.16.0 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.105.0+1.21.1 or newer
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)

Then place the built Create-UfoPort jar from `build/libs/` into the instance `mods` folder.

## Runtime Requirements And Notes

- Java 21 is required. A stable Java 21 runtime such as JetBrains Runtime 21, Eclipse Temurin 21, or Microsoft Build of OpenJDK 21 is recommended.
- Do not allocate nearly all physical RAM to Minecraft. Large shader/modpack instances should leave memory for Windows, the GPU driver, native buffers, resource packs, and the launcher. On a 32 GB system, start around 8-12 GB and increase only if Java heap pressure is proven.
- Keep a Windows page file enabled, preferably system managed. Disabling the page file can turn native/GPU memory pressure into whole-system instability and can prevent Windows from writing useful crash dumps.
- Iris shader support targets Iris + IrisFLW with the `irisflw:iris_instancing` backend.
- The native IrisFLW shader path requires an OpenGL 3.3-capable context. Create-UfoPort requests an OpenGL 3.3 context before window creation by default.
- Sodium 0.6.x is supported in the tested stack. Do not add Indium to Sodium 0.6.x; Sodium 0.6+ already includes the relevant Fabric Rendering API support and Indium is no longer compatible with that line.
- Create-UfoPort skips the IrisFLW Flywheel instanced shadow pass by default to reduce shader GPU spikes from moving Create visuals. This can make dynamic shadows from those moving visuals less accurate.

Known incompatible:

- Farmer's Delight Refabricated

Known risky with shader/rendering stacks:

- Caxton 0.6.4 reports a loader conflict with Iris 1.8.8+mc1.21.1.
- Ixeris has caused Windows OpenGL window/context native crashes in testing.
- PointBlank's advanced Iris integration has caused black-screen behavior in testing and should be disabled if that symptom appears.

Native `jvm.dll`, `opengl32.dll`, GPU driver, or whole-machine crashes are not normal Java mod crash reports. If they happen, test another Java 21 runtime, inspect any generated `hs_err_pid*.log`, and A/B graphics mods before assuming a normal Create exception.

## JVM Startup Parameters

All parameters below are optional JVM system properties. In launchers, add them to the Java/JVM arguments as `-Dname=value`.

Recommended presets:

- Normal launch: no extra Create-UfoPort JVM parameter.
- Emergency shader fallback: `-Dcreate_ufoport.flywheel.disableIrisFlwWithShaders=true`.
- Retry IrisFLW after a known fixed crash: `-Dcreate_ufoport.flywheel.forceIrisFlwWithShaders=true` for one test launch, then remove it.
- Restore more accurate moving-visual shader shadows at the cost of possible GPU spikes: `-Dcreate_ufoport.flywheel.disableIrisFlwShadowPassSkip=true`.

Create-UfoPort shader backend selection:

| Parameter | Default | Effect |
| --- | --- | --- |
| `-Dcreate_ufoport.flywheel.disableIrisFlwWithShaders=true` | `false` | Disables the IrisFLW shader backend path and the Create IrisFLW mesh-emitter mixin. |
| `-Dcreate_ufoport.flywheel.allowIrisFlwWithShaders=true` | `false` | Allows `irisflw:iris_instancing` while an Iris shaderpack is active when OpenGL 3.3 is available and the crash guard is clean. Usually handled by `create_flywheel_compat.json` instead. |
| `-Dcreate_ufoport.flywheel.autoIrisFlwWithShaders=true` | `false` | Automatically allows IrisFLW under active shaders when runtime checks pass, without requiring the compat config opt-in. |
| `-Dcreate_ufoport.flywheel.forceIrisFlwWithShaders=true` | `false` | Forces a retry of IrisFLW under active shaders and bypasses the unclean-session crash guard. Use only for targeted testing. |
| `-Dcreate_ufoport.flywheel.disableIrisFlwCrashGuard=true` | `false` | Disables the IrisFLW active-session marker and cleanup guard. Debug only. |
| `-Dcreate_ufoport.flywheel.disableCompatShaderConfig=true` | `false` | Ignores `config/create_flywheel_compat.json` for shader backend and shadow tuning decisions. |
| `-Dcreate_ufoport.flywheel.forceBackendWithShaders=true` | `false` | Forces the old `com.jozufozu.flywheel` compatibility backend to run with shaderpacks. Debug only. |
| `-Dflywheel.forceBackendWithShaders=true` | `false` | Upstream alias for the same old Flywheel compatibility backend force behavior. |

OpenGL and IrisFLW shader compatibility:

| Parameter | Default | Effect |
| --- | --- | --- |
| `-Dcreate_ufoport.flywheel.disableOpenGl33ContextHint=true` | `false` | Stops Create-UfoPort from requesting OpenGL 3.3 before window creation. |
| `-Dcreate_ufoport.flywheel.disableIrisFlwGlslVersionPatch=true` | `false` | Disables the Create-UfoPort IrisFLW GLSL compatibility patch. Debug only. |
| `-Dcreate_ufoport.flywheel.forceIrisFlwGlsl330=true` | `false` | Forces the IrisFLW GLSL 330 path and disables the lower-version rewrite. |
| `-Dcreate_ufoport.flywheel.irisFlwShaderDumpLimit=<n>` | `6` | Dumps the first `<n>` rewritten IrisFLW shader sources to `config/create-ufoport-debug/irisflw-shaders`. Set to `0` to disable dumps. |

Performance and shadow tuning:

| Parameter | Default | Effect |
| --- | --- | --- |
| `-Dcreate_ufoport.flywheel.skipIrisFlwShadowPass=true` | `false` | Explicitly enables the IrisFLW Flywheel instanced shadow-pass skip. The current build already enables this behavior by default unless disabled. |
| `-Dcreate_ufoport.flywheel.disableIrisFlwShadowPassSkip=true` | `false` | Disables the default shadow-pass skip. This can restore more accurate dynamic shadows from moving Create visuals but may bring back shader GPU spikes. |

Diagnostics and development:

| Parameter | Default | Effect |
| --- | --- | --- |
| `-Dflw.loadRenderDoc=true` | unset | Attempts to load the native `renderdoc` library at client startup. Requires RenderDoc to be available on the system library path. |
| `-DportingLib.debug=true` | `false` | Enables extra Porting Lib debug behavior. Development only. |
| `-Dforge.debugBlockSnapshot=true` | `false` | Enables BlockSnapshot debug behavior in bundled Porting Lib code. Development only. |
| `-Dfabric-api.datagen=true` | unset | Enables datagen behavior used by Registrate/Fabric tooling. Do not use for normal gameplay launches. |

## Building From Source

Use Java 21 and run:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 clean build
```

The main output is:

```text
build/libs/create-fabric-ufoport-0.9.3-d+mc1.21.1.jar
```

If Gradle or TinyRemapper crashes inside the JVM rather than reporting a normal Java exception, try another Java 21 runtime first. On some local machines, temporarily using `-XX:+UseParallelGC -XX:TieredStopAtLevel=1` can help isolate JVM/toolchain instability, but players should not need custom GC flags for normal gameplay.

## Code Used From

- [Build tools](https://github.com/Fabricators-of-Create/Porting-Lib)
- [Create Fabric](https://github.com/Fabricators-of-Create/Create)
- [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib)
- [Flywheel](https://github.com/Engine-Room/Flywheel)
- [Milk Lib](https://github.com/TropheusJ/milk-lib)
- [Registrate Refabricated](https://github.com/Fabricators-of-Create/Registrate-Refabricated)
