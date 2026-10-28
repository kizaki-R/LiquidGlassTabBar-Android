# LiquidGlassTabBar-Android

[English](README.md) · [繁體中文](README.zh-TW.md) · 简体中文

Jetpack Compose 的底部标签栏（以下称底栏）。按住选中胶囊时，它会变成一片玻璃透镜：浮起来，把底栏后面的画面模糊、折射，边缘带一圈很细的彩色边，还会顺着手指拖动的方向微微拉长。松手后停在最近的标签；单击则是胶囊滑向新的标签。

透镜是用 AGSL 着色器画的，需要 Android 13（API 33）。外观和动画背后的数值，是同一个 `LiquidGlassStyle` 对象的 39 个字段，全部列在 [docs/PARAMETERS.zh-CN.md](docs/PARAMETERS.zh-CN.md)。

## 要求

下表是这个库实际构建、运行时用的版本，更旧的版本没试过。

| | 版本 |
|---|---|
| Android | `minSdk` 24；透镜效果需要 API 33 以上；真机跑过 API 36 |
| Kotlin | 2.4.10 |
| Compose | BOM 2026.09.00（Compose 1.12） |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.5.0 |
| JDK | 字节码 17，用 21 构建 |
| `compileSdk` | 37 |

模糊与折射来自 [Kyant0 的 backdrop 库](https://github.com/Kyant0/AndroidLiquidGlass) 2.0.1，会自动带进来。这个版本本身是用 Compose 1.12 和 Kotlin 2.4.10 构建的，项目如果用更旧的版本，依赖可能解析不了。

## 安装

从 JitPack：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.kizaki-R:LiquidGlassTabBar-Android:v0.1.0")
}
```

任何标签的正确写法，都能在 <https://jitpack.io/#kizaki-R/LiquidGlassTabBar-Android> 查到。

也可以从源码引入：把 `liquidglass` 文件夹复制进项目（用 git submodule 也行），再 include 进来。如果 Gradle 找不到 `com.android.library` 插件，就在根目录的构建文件里、紧接着 application 插件声明它，版本用你自己的 AGP 版本，并加上 `apply false`。

```kotlin
// settings.gradle.kts
include(":liquidglass")

// app/build.gradle.kts
dependencies {
    implementation(project(":liquidglass"))
}
```

## 快速上手

```kotlin
@Composable
fun App() {
    var selected by remember { mutableIntStateOf(0) }
    val tint = if (isSystemInDarkTheme()) Color.White else Color.Black

    LiquidGlassScaffold(
        bar = {
            LiquidGlassTabBar(
                selectedIndex = selected,
                onTabSelected = { selected = it },
                tabsCount = 4,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                listOf(Icons.Outlined.Home, Icons.Outlined.Search, Icons.Outlined.Favorite, Icons.Outlined.Person)
                    .forEachIndexed { index, icon ->
                        LiquidGlassTab(onClick = { selected = index }) {
                            Icon(icon, contentDescription = null, tint = tint)
                        }
                    }
            }
        },
    ) {
        MyScreen(selected) // 画面要延伸到底栏后面
    }
}
```

要 import 的是 `io.github.kizaki_r.liquidglass.LiquidGlassScaffold`、`LiquidGlassTabBar` 和 `LiquidGlassTab`。示例用 Material 3 的 `Icon`，图标来自 `androidx.compose.material:material-icons-core`；Material 3 不会把这个包对外提供，照抄示例就要自己加上。标签里放任何 composable 都可以。

Scaffold 会把你的画面录下来，让底栏有东西可以折射，整个设置就是这一层包装。另外有两条规则。真正的画面要放在 Scaffold 的内容里，而且要延伸到底栏后面，画面若在底栏上方就结束，玻璃就没有东西可以弯折。底栏则放在 `bar` 区，不要放进内容里，否则它会折射到自己。

`tabsCount` 是底栏里的格数。每一格都要有 `Modifier.weight(1f)`，`LiquidGlassTab` 已经加好了。

### 不是标签的中间按钮

用 `isSelectable` 标出那一格。胶囊会跳过它，它保留自己的点击。

```kotlin
LiquidGlassTabBar(
    selectedIndex = selected,
    onTabSelected = { selected = it },
    tabsCount = 5,
    isSelectable = { it != 2 },
) {
    LiquidGlassTab(onClick = { selected = 0 }) { Icon(Icons.Outlined.Home, null, tint = tint) }
    LiquidGlassTab(onClick = { selected = 1 }) { Icon(Icons.Outlined.Search, null, tint = tint) }
    Box(
        Modifier.weight(1f).fillMaxHeight().clickable { /* 打开发布界面 */ },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Add, contentDescription = "新增", tint = tint) }
    LiquidGlassTab(onClick = { selected = 3 }) { Icon(Icons.Outlined.Favorite, null, tint = tint) }
    LiquidGlassTab(onClick = { selected = 4 }) { Icon(Icons.Outlined.Person, null, tint = tint) }
}
```

## 调整外观

从你主题的默认样式开始，只改要改的字段。

```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)

LiquidGlassTabBar(/* ... */ darkTheme = darkTheme, style = style) { /* ... */ }
```

`darkTheme` 请传应用自己的主题。它的默认值是系统主题，应用如果有自己的深色切换，就会对不上。样式有五组：`metrics`、`panel`、`lens`、`colors`、`motion`。自己建一组时（像上面的 `LiquidGlassLens(dispersion = 3.dp)`），那一组没写到的字段都回到默认值。`colors` 没有自己的默认值，请从 `LiquidGlassDefaults.colors(darkTheme).copy(...)` 开始。

最常会动到的几个：

| 想改 | 字段 | 默认值 |
|---|---|---|
| 透镜的宽和高 | `lens.liftScaleX`、`lens.liftScaleY` | `1.42f`、`1.40f` |
| 彩色边 | `lens.dispersion` | `4.5.dp`（`0.dp` 就关掉） |
| 边缘把后方内容弯折多少 | `lens.rimRefraction` | `6.dp` |
| 单击的滑动速度 | `motion.tapSlide` | `SpringConfig(0.85f, 450f)`，刚度越低越慢 |
| 松手时玻璃效果淡出的速度 | `motion.releaseGlow` | `SpringConfig(1f, 700f)` |
| 果冻拉伸 | `motion.jellyStretchX`、`motion.jellyLimit` | `0.75f`、`0.2f` |
| 底栏的颜色与透明度 | `colors.panelColor` | 浅色 `#FAFAFA`、0.8；深色 `#0C0C0E`、0.9 |
| 底栏高度 | `metrics.barHeight` | `54.dp` |

39 个字段连同浅色、深色的默认值，都在 [docs/PARAMETERS.zh-CN.md](docs/PARAMETERS.zh-CN.md)。

## 搭配 AI 编程助手

`skills/liquid-glass-tab-bar` 文件夹是一个 Claude Code 技能。复制到项目的 `.claude/skills/`（或 `~/.claude/skills/`），助手就知道怎么加上这个底栏、哪个字段对应哪种外观，以及哪些错要避开。

## 工作方式

底栏分三层。

1. 玻璃面板。把底栏后面的内容模糊、轻微折射，上面再画你的标签。
2. 面板和标签的隐藏副本，另外录成一个 backdrop。除了透镜，没有东西会显示它。
3. 胶囊。静止时是一个灰色胶囊。按住后放大到原来的 1.42 × 1.40，透过它同时看到页面和隐藏的副本。副本以透镜中心缩到 0.92，所以透镜边缘和面板边缘之间，会露出约 8 dp 的真实画面。

这约 8 dp 的距离，决定边缘能往内取样多深。backdrop 库自己的透镜，在屏幕上大约往内取样 9 到 17 dp，一定落在面板副本上，所以边缘的光影从来不随后面的内容变化。这里的透镜是自写的着色器，由库的版本改写，默认的 `lens.rimRefraction` 6 dp 留在这段距离以内；超过约 8 dp，就会变回那种固定的样子。

彩色边在着色器里有自己的宽度和强度，不再依赖折射量，因为面板亮边所在的位置，折射量几乎是零。

## 限制

- 只支持 Android。透镜需要 API 33；更低的版本，代码会跳过透镜着色器，这条路径还没跑过。
- 只在一部手机上跑过：Android 16（API 36）。低端机型没有测过性能。
- 彩色边很细，内容里有锐利边缘的地方最容易看到，纯色背景上很淡。
- 从右到左的布局，代码里有处理，没试过。
- 每格等宽、排成一行。徽标、文字标签之类的东西，放在你传进来的标签内容里。

## 来源与许可

建立在 Kyant0 的 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（backdrop 2.0.1、shapes 1.2.1）之上。拖动动画、交互高光、拖动手势检测，以及透镜着色器的折射数学，改写自该项目（Apache-2.0）；[NOTICE](NOTICE) 有逐项列出。

以 [Apache License 2.0](LICENSE) 授权。
