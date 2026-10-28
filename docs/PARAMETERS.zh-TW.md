# 參數總表

語言：[English](PARAMETERS.md) · 繁體中文 · [简体中文](PARAMETERS.zh-CN.md)

`LiquidGlassTabBar` 的所有可調數值，由 `LiquidGlassStyle.kt` 自動產生。長度單位是 dp，指按到底時在螢幕上的大小。從 `LiquidGlassDefaults.style(darkTheme)` 開始，用 `copy(...)` 改你要改的欄位。

## 尺寸與版面 (`metrics`)

底欄和選取藥丸的大小與版面。

| 參數 | 預設值 | 作用 |
|---|---|---|
| `barHeight` | `54.dp` | 玻璃底欄的高度。 |
| `pillHeight` | `48.dp` | 靜止時選取藥丸的高度；按住後的透鏡高度是它乘上 `lens.liftScaleY`。 |
| `contentPadding` | `8.dp` | 底欄左右邊緣到第一格、最後一格之間的內縮。 |
| `pillInset` | `3.dp` | 藥丸停在第一格或最後一格時，離底欄邊緣的最小距離。 |
| `pillExtraWidth` | `10.dp` | 藥丸比單一格寬出多少。 |
| `maxLensOverhang` | `14.dp` | 拖到兩端時，浮起的透鏡最多可以凸出底欄多遠。 |
| `maxRubberBand` | `3.dp` | 手指拖過頭時，整條底欄被橫向拉動的最大距離（橡皮筋效果）。 |
| `panelPressGrow` | `12.dp` | 按住時，玻璃底欄（和它看不見的複本）在寬度上長大多少；是等比放大，所以高度也會按比例變高。 |

## 底欄玻璃 (`panel`)

底欄本身的磨砂玻璃，不含透鏡。

| 參數 | 預設值 | 作用 |
|---|---|---|
| `blur` | `16.dp` | 底欄後方內容的模糊半徑。 |
| `rimWidth` | `12.dp` | 底欄邊緣折射帶的寬度。 |
| `rimRefraction` | `20.dp` | 邊緣折射帶把後方內容彎折的強度。 |

## 透鏡 (`lens`)

浮起的透鏡，也就是按住時的選取藥丸。

| 參數 | 預設值 | 作用 |
|---|---|---|
| `liftScaleX` | `1.42f` | 按住後透鏡寬度相對於靜止藥丸的倍率。 |
| `liftScaleY` | `1.40f` | 按住後透鏡高度相對於靜止藥丸的倍率。 |
| `contentScale` | `0.92f` | 透鏡裡看到的底欄面板，相對於透鏡外底欄的大小（以透鏡中心縮放）。 |
| `tabLiftScale` | `1.25f` | 透鏡裡的圖示和文字相對於外面的倍率；實際看到的是這個值乘上 `contentScale`（預設約 1.15 倍）。 |
| `rimWidth` | `10.dp` | 透鏡邊緣折射帶的寬度，從邊緣往內量。 |
| `rimRefraction` | `6.dp` | 最外圈像素往內取樣多深。要遠小於約 8 dp：再深就會取樣到底欄自己的面板，而不是後方內容，邊緣的光影看起來像固定不動。 |
| `dispersionWidth` | `10.dp` | 透鏡邊緣色散（彩色邊）帶的寬度，內側 40% 逐漸淡出。 |
| `dispersion` | `4.5.dp` | 色散的最大紅藍分離量，在兩端最強。0 表示沒有色散。 |
| `innerShadowRadius` | `9.dp` | 透鏡邊緣內陰影的模糊半徑。 |
| `innerShadowOffsetY` | `6.dp` | 內陰影的垂直位移，形成上緣受光的斜面。 |
| `innerShadowAlpha` | `0.6f` | 內陰影的不透明度。 |

## 顏色 (`colors`)

玻璃的顏色，以及隨主題而不同的透明度。淺色與深色的預設值不同。

| 參數 | 淺色 | 深色 | 作用 |
|---|---|---|---|
| `panelColor` | `#FAFAFA` @ 0.8 | `#0C0C0E` @ 0.9 | 玻璃底欄表面的顏色（含透明度）。 |
| `restingPillColor` | `black` @ 0.08 | `white` @ 0.1 | 靜止時灰色選取藥丸的顏色。 |
| `lensSeenBlur` | `6.dp` | `16.dp` | 透鏡裡看到的底欄的模糊；比 `panel.blur` 小，就能看到更多後方內容。 |
| `lensSeenSurfaceAlpha` | `0.68f` | `1f` | 透鏡裡看到的底欄面板的不透明度倍率；越小透鏡越透。 |
| `pressGlow` | `1f` | `0f` | 按住時跟著手指的白色柔光強度（0 = 關閉，1 = 全開）。 |

## 動畫 (`motion`)

動畫的快慢和手感。每個彈簧都是 `SpringConfig(dampingRatio, stiffness)`：剛性越高越快，阻尼比小於 1 會過衝。

| 參數 | 預設值 | 作用 |
|---|---|---|
| `tapSlide` | `SpringConfig(0.85f, 450f)` | 單擊後藥丸滑向該分頁的彈簧；剛性越低，滑得越慢、越看得清楚。 |
| `follow` | `SpringConfig(1f, 1000f)` | 拖曳時藥丸跟著手指的彈簧。 |
| `pressGlow` | `SpringConfig(1f, 1000f)` | 按下時玻璃效果（折射、高光、陰影）淡入的彈簧。 |
| `liftX` | `SpringConfig(0.6f, 250f)` | 按下時透鏡變寬的彈簧。 |
| `liftY` | `SpringConfig(0.7f, 250f)` | 按下時透鏡變高的彈簧。 |
| `releaseGlow` | `SpringConfig(1f, 700f)` | 放手時玻璃效果淡出的彈簧。 |
| `releaseScale` | `SpringConfig(0.8f, 550f)` | 放手時透鏡縮回靜止藥丸的彈簧。 |
| `velocity` | `SpringConfig(0.5f, 300f)` | 手指速度的平滑彈簧，速度再去驅動果凍拉伸。 |
| `rubberBand` | `SpringConfig(0.6f, 400f)` | 拖過頭放手後，底欄彈回原位的彈簧。 |
| `jellyStretchX` | `0.75f` | 每單位速度造成的橫向拉伸；越大越有果凍感。 |
| `jellyStretchY` | `0.25f` | 每單位速度造成的縱向壓扁。 |
| `jellyLimit` | `0.2f` | 拉伸與壓扁的比例上限；0.2 對應最寬約多 25%。 |

## 範例

```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)
LiquidGlassTabBar(/* ... */ style = style) { /* ... */ }
```
