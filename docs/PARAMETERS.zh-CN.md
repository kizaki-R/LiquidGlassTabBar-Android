# 参数总表

语言：[English](PARAMETERS.md) · [繁體中文](PARAMETERS.zh-TW.md) · 简体中文

`LiquidGlassTabBar` 的所有可调数值，由 `LiquidGlassStyle.kt` 自动生成。长度单位是 dp，指按到底时在屏幕上的大小。从 `LiquidGlassDefaults.style(darkTheme)` 开始，用 `copy(...)` 改你要改的字段。

## 尺寸与布局 (`metrics`)

底栏和选中胶囊的大小与布局。

| 参数 | 默认值 | 作用 |
|---|---|---|
| `barHeight` | `54.dp` | 玻璃底栏的高度。 |
| `pillHeight` | `48.dp` | 静止时选中胶囊的高度；按住后的透镜高度是它乘以 `lens.liftScaleY`。 |
| `contentPadding` | `8.dp` | 底栏左右边缘到第一格、最后一格之间的内边距。 |
| `pillInset` | `3.dp` | 胶囊停在第一格或最后一格时，离底栏边缘的最小距离。 |
| `pillExtraWidth` | `10.dp` | 胶囊比单个格子宽出多少。 |
| `maxLensOverhang` | `14.dp` | 拖到两端时，浮起的透镜最多可以凸出底栏多远。 |
| `maxRubberBand` | `3.dp` | 手指拖过头时，整条底栏被横向拉动的最大距离（橡皮筋效果）。 |
| `panelPressGrow` | `12.dp` | 按住时，玻璃底栏（和它看不见的副本）在宽度上变大多少；是等比放大，所以高度也会按比例变高。 |

## 底栏玻璃 (`panel`)

底栏本身的磨砂玻璃，不含透镜。

| 参数 | 默认值 | 作用 |
|---|---|---|
| `blur` | `16.dp` | 底栏后方内容的模糊半径。 |
| `rimWidth` | `12.dp` | 底栏边缘折射带的宽度。 |
| `rimRefraction` | `20.dp` | 边缘折射带把后方内容弯折的强度。 |

## 透镜 (`lens`)

浮起的透镜，也就是按住时的选中胶囊。

| 参数 | 默认值 | 作用 |
|---|---|---|
| `liftScaleX` | `1.42f` | 按住后透镜宽度相对于静止胶囊的倍率。 |
| `liftScaleY` | `1.40f` | 按住后透镜高度相对于静止胶囊的倍率。 |
| `contentScale` | `0.92f` | 透镜里看到的底栏面板，相对于透镜外底栏的大小（以透镜中心缩放）。 |
| `tabLiftScale` | `1.25f` | 透镜里的图标和文字相对于外面的倍率；实际看到的是这个值乘以 `contentScale`（默认约 1.15 倍）。 |
| `rimWidth` | `10.dp` | 透镜边缘折射带的宽度，从边缘向内量。 |
| `rimRefraction` | `6.dp` | 最外圈像素向内取样多深。要远小于约 8 dp：再深就会取样到底栏自己的面板，而不是后方内容，边缘的光影看起来像固定不动。 |
| `dispersionWidth` | `10.dp` | 透镜边缘色散（彩色边）带的宽度，内侧 40% 逐渐淡出。 |
| `dispersion` | `4.5.dp` | 色散的最大红蓝分离量，在两端最强。0 表示没有色散。 |
| `innerShadowRadius` | `9.dp` | 透镜边缘内阴影的模糊半径。 |
| `innerShadowOffsetY` | `6.dp` | 内阴影的垂直位移，形成上缘受光的斜面。 |
| `innerShadowAlpha` | `0.6f` | 内阴影的不透明度。 |

## 颜色 (`colors`)

玻璃的颜色，以及随主题而不同的透明度。浅色与深色的默认值不同。

| 参数 | 浅色 | 深色 | 作用 |
|---|---|---|---|
| `panelColor` | `#FAFAFA` @ 0.8 | `#0C0C0E` @ 0.9 | 玻璃底栏表面的颜色（含透明度）。 |
| `restingPillColor` | `black` @ 0.08 | `white` @ 0.1 | 静止时灰色选中胶囊的颜色。 |
| `lensSeenBlur` | `6.dp` | `16.dp` | 透镜里看到的底栏的模糊；比 `panel.blur` 小，就能看到更多后方内容。 |
| `lensSeenSurfaceAlpha` | `0.68f` | `1f` | 透镜里看到的底栏面板的不透明度倍率；越小透镜越透。 |
| `pressGlow` | `1f` | `0f` | 按住时跟着手指的白色柔光强度（0 = 关闭，1 = 全开）。 |

## 动画 (`motion`)

动画的快慢和手感。每个弹簧都是 `SpringConfig(dampingRatio, stiffness)`：刚度越高越快，阻尼比小于 1 会过冲。

| 参数 | 默认值 | 作用 |
|---|---|---|
| `tapSlide` | `SpringConfig(0.85f, 450f)` | 单击后胶囊滑向该标签的弹簧；刚度越低，滑得越慢、越看得清楚。 |
| `follow` | `SpringConfig(1f, 1000f)` | 拖动时胶囊跟着手指的弹簧。 |
| `pressGlow` | `SpringConfig(1f, 1000f)` | 按下时玻璃效果（折射、高光、阴影）淡入的弹簧。 |
| `liftX` | `SpringConfig(0.6f, 250f)` | 按下时透镜变宽的弹簧。 |
| `liftY` | `SpringConfig(0.7f, 250f)` | 按下时透镜变高的弹簧。 |
| `releaseGlow` | `SpringConfig(1f, 700f)` | 放手时玻璃效果淡出的弹簧。 |
| `releaseScale` | `SpringConfig(0.8f, 550f)` | 放手时透镜缩回静止胶囊的弹簧。 |
| `velocity` | `SpringConfig(0.5f, 300f)` | 手指速度的平滑弹簧，速度再去驱动果冻拉伸。 |
| `rubberBand` | `SpringConfig(0.6f, 400f)` | 拖过头放手后，底栏弹回原位的弹簧。 |
| `jellyStretchX` | `0.75f` | 每单位速度造成的横向拉伸；越大越有果冻感。 |
| `jellyStretchY` | `0.25f` | 每单位速度造成的纵向压扁。 |
| `jellyLimit` | `0.2f` | 拉伸与压扁的比例上限；0.2 对应最宽约多 25%。 |

## 示例

```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)
LiquidGlassTabBar(/* ... */ style = style) { /* ... */ }
```
