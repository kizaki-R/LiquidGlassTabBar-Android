# LiquidGlassTabBar-Android

[English](README.md) · 繁體中文 · [简体中文](README.zh-CN.md)

Jetpack Compose 的底部分頁列（以下稱底欄）。按住選取藥丸時，它會變成一片玻璃透鏡：浮起來，把底欄後面的畫面模糊、折射，邊緣帶一圈很細的彩色邊，還會順著手指拖動的方向微微拉長。放手後停在最近的分頁；單擊則是藥丸滑向新的分頁。

透鏡是用 AGSL 著色器畫的，需要 Android 13（API 33）。外觀和動畫背後的數值，是同一個 `LiquidGlassStyle` 物件的 39 個欄位，全部列在 [docs/PARAMETERS.zh-TW.md](docs/PARAMETERS.zh-TW.md)。

## 需求

下表是這個函式庫實際建置、執行時用的版本，更舊的工具版本沒試過。

| | 版本 |
|---|---|
| Android | `minSdk` 24；透鏡效果需要 API 33 以上；實機跑過 API 36，模擬器跑過 API 32 至 35 |
| Kotlin | 2.4.10 |
| Compose | BOM 2026.09.00（Compose 1.12） |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.5.0 |
| JDK | 位元組碼 17，用 21 建置 |
| `compileSdk` | 37 |

模糊與折射來自 [Kyant0 的 backdrop 函式庫](https://github.com/Kyant0/AndroidLiquidGlass) 2.0.1，會自動帶進來。這個版本本身是用 Compose 1.12 和 Kotlin 2.4.10 建置的，專案如果用更舊的版本，依賴可能解析不了。

## 安裝

從 JitPack：

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
    implementation("com.github.kizaki-R:LiquidGlassTabBar-Android:v0.2.0")
}
```

任何標籤的正確寫法，都能在 <https://jitpack.io/#kizaki-R/LiquidGlassTabBar-Android> 查到。

也可以從原始碼引入：把 `liquidglass` 資料夾複製進專案（用 git submodule 也行），再 include 進來。如果 Gradle 找不到 `com.android.library` 外掛，就在根目錄的建置檔裡、緊接著 application 外掛宣告它，版本用你自己的 AGP 版本，並加上 `apply false`。

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
        MyScreen(selected) // 畫面要延伸到底欄後面
    }
}
```

要 import 的是 `io.github.kizaki_r.liquidglass.LiquidGlassScaffold`、`LiquidGlassTabBar` 和 `LiquidGlassTab`。範例用 Material 3 的 `Icon`，圖示來自 `androidx.compose.material:material-icons-core`；Material 3 不會把這個套件對外提供，照抄範例就要自己加上。分頁裡放任何 composable 都可以。

Scaffold 會把你的畫面錄下來，讓底欄有東西可以折射，整個設定就是這一層包裝。另外有兩條規則。真正的畫面要放在 Scaffold 的內容裡，而且要延伸到底欄後面，畫面若在底欄上方就結束，玻璃就沒有東西可以彎折。底欄則放在 `bar` 區，不要放進內容裡，否則它會折射到自己。

`tabsCount` 是底欄裡的格數。每一格都要有 `Modifier.weight(1f)`，`LiquidGlassTab` 已經加好了。

### 不是分頁的中間按鈕

用 `isSelectable` 標出那一格。藥丸會跳過它，它保留自己的點擊。

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
        Modifier.weight(1f).fillMaxHeight().clickable { /* 開啟發文畫面 */ },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Add, contentDescription = "新增", tint = tint) }
    LiquidGlassTab(onClick = { selected = 3 }) { Icon(Icons.Outlined.Favorite, null, tint = tint) }
    LiquidGlassTab(onClick = { selected = 4 }) { Icon(Icons.Outlined.Person, null, tint = tint) }
}
```

## 調整外觀

從你主題的預設樣式開始，只改要改的欄位。

```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)

LiquidGlassTabBar(/* ... */ darkTheme = darkTheme, style = style) { /* ... */ }
```

`darkTheme` 請傳 App 自己的主題。它的預設值是系統主題，App 如果有自己的深色切換，就會對不上。樣式有五組：`metrics`、`panel`、`lens`、`colors`、`motion`。自己建一組時（像上面的 `LiquidGlassLens(dispersion = 3.dp)`），那一組沒寫到的欄位都回到預設值。`colors` 沒有自己的預設值，請從 `LiquidGlassDefaults.colors(darkTheme).copy(...)` 開始。

最常會動到的幾個：

| 想改 | 欄位 | 預設值 |
|---|---|---|
| 透鏡的寬和高 | `lens.liftScaleX`、`lens.liftScaleY` | `1.42f`、`1.40f` |
| 彩色邊 | `lens.dispersion` | `4.5.dp`（`0.dp` 就關掉） |
| 邊緣把後方內容彎折多少 | `lens.rimRefraction` | `6.dp` |
| 單擊的滑動速度 | `motion.tapSlide` | `SpringConfig(0.85f, 450f)`，剛性越低越慢 |
| 放手時玻璃效果淡出的速度 | `motion.releaseGlow` | `SpringConfig(1f, 700f)` |
| 果凍拉伸 | `motion.jellyStretchX`、`motion.jellyLimit` | `0.75f`、`0.2f` |
| 底欄的顏色與透明度 | `colors.panelColor` | 淺色 `#FAFAFA`、0.8；深色 `#0C0C0E`、0.9 |
| 底欄高度 | `metrics.barHeight` | `54.dp` |

39 個欄位連同淺色、深色的預設值，都在 [docs/PARAMETERS.zh-TW.md](docs/PARAMETERS.zh-TW.md)。

## 搭配 AI 程式助手

`skills/liquid-glass-tab-bar` 資料夾是一個 Claude Code 技能。複製到專案的 `.claude/skills/`（或 `~/.claude/skills/`），助手就知道怎麼加上這個底欄、哪個欄位對應哪種外觀，以及哪些錯要避開。

## 運作方式

底欄分三層。

1. 玻璃面板。把底欄後面的內容模糊、輕微折射，上面再畫你的分頁。
2. 面板和分頁的隱藏複本，另外錄成一個 backdrop。除了透鏡，沒有東西會顯示它。
3. 藥丸。靜止時是一個灰色膠囊。按住後放大到原來的 1.42 × 1.40，透過它同時看到頁面和隱藏的複本。複本以透鏡中心縮到 0.92，所以透鏡邊緣和面板邊緣之間，會露出約 8 dp 的真實畫面。

這約 8 dp 的距離，決定邊緣能往內取樣多深。backdrop 函式庫自己的透鏡，在螢幕上大約往內取樣 9 到 17 dp，一定落在面板複本上，所以邊緣的光影從來不隨後面的內容變化。這裡的透鏡是自寫的著色器，由函式庫的版本改寫，預設的 `lens.rimRefraction` 6 dp 留在這段距離以內；超過約 8 dp，就會變回那種固定的樣子。

彩色邊在著色器裡有自己的寬度和強度，不再依賴折射量，因為面板亮邊所在的位置，折射量幾乎是零。

## 限制

- 只支援 Android。透鏡需要 API 33；API 32 會略過透鏡著色器，膠囊畫成沒有折射和彩邊的半透明形狀，這條路徑只在模擬器的淺色主題跑過；API 32 以下還沒跑過。
- 實機只跑過一支手機：Android 16（API 36）；另在 x86_64 模擬器跑過 API 32 至 35。模擬器是軟體繪製，不能代表畫面流暢度。低階機型沒有量過效能。
- 彩色邊很細，內容裡有銳利邊緣的地方最容易看到，純色背景上很淡。
- 從右到左的版面，程式裡有處理，沒試過。
- 每格等寬、排成一列。徽章、文字標籤之類的東西，放在你傳進來的分頁內容裡。

## 來源與授權

建立在 Kyant0 的 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（backdrop 2.0.1、shapes 1.2.1）之上。拖曳動畫、互動高光、拖曳手勢偵測，以及透鏡著色器的折射數學，改寫自該專案（Apache-2.0）；[NOTICE](NOTICE) 有逐項列出。

以 [Apache License 2.0](LICENSE) 授權。
