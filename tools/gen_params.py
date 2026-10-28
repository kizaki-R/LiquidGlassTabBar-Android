"""Generates docs/PARAMETERS*.md from LiquidGlassStyle.kt so the tables can never drift from the code.

Names and default values are read from the source. English descriptions are the KDoc comments; the
Traditional and Simplified Chinese descriptions live in ZH below (keyed by "group.field").

Run from the repository root:  python tools/gen_params.py
"""
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..")
SRC = os.path.join(ROOT, "liquidglass", "src", "main", "java", "io", "github", "kizaki_r", "liquidglass", "LiquidGlassStyle.kt")
OUT = os.path.join(ROOT, "docs")

GROUPS = [  # (class name, key, English title)
    ("LiquidGlassMetrics", "metrics", "Metrics"),
    ("LiquidGlassPanel", "panel", "Panel"),
    ("LiquidGlassLens", "lens", "Lens"),
    ("LiquidGlassColors", "colors", "Colors"),
    ("LiquidGlassMotion", "motion", "Motion"),
]

TITLES = {
    "en": {"metrics": "Metrics", "panel": "Panel", "lens": "Lens", "colors": "Colors", "motion": "Motion"},
    "zh-TW": {"metrics": "尺寸與版面", "panel": "底欄玻璃", "lens": "透鏡", "colors": "顏色", "motion": "動畫"},
    "zh-CN": {"metrics": "尺寸与布局", "panel": "底栏玻璃", "lens": "透镜", "colors": "颜色", "motion": "动画"},
}

GROUP_INTRO = {
    "metrics": (
        "Size and layout of the bar and the selection pill.",
        "底欄和選取藥丸的大小與版面。",
        "底栏和选中胶囊的大小与布局。",
    ),
    "panel": (
        "The frosted glass of the bar itself, not the lens.",
        "底欄本身的磨砂玻璃，不含透鏡。",
        "底栏本身的磨砂玻璃，不含透镜。",
    ),
    "lens": (
        "The lifted lens: the selection pill while it is pressed.",
        "浮起的透鏡，也就是按住時的選取藥丸。",
        "浮起的透镜，也就是按住时的选中胶囊。",
    ),
    "colors": (
        "Colors and the theme-dependent opacity of the glass. The defaults differ for light and dark.",
        "玻璃的顏色，以及隨主題而不同的透明度。淺色與深色的預設值不同。",
        "玻璃的颜色，以及随主题而不同的透明度。浅色与深色的默认值不同。",
    ),
    "motion": (
        "Timing and feel. Every spring is `SpringConfig(dampingRatio, stiffness)`: higher stiffness is faster, a damping ratio below 1 overshoots.",
        "動畫的快慢和手感。每個彈簧都是 `SpringConfig(dampingRatio, stiffness)`：剛性越高越快，阻尼比小於 1 會過衝。",
        "动画的快慢和手感。每个弹簧都是 `SpringConfig(dampingRatio, stiffness)`：刚度越高越快，阻尼比小于 1 会过冲。",
    ),
}

ZH = {
    "metrics.barHeight": ("玻璃底欄的高度。", "玻璃底栏的高度。"),
    "metrics.pillHeight": ("靜止時選取藥丸的高度；按住後的透鏡高度是它乘上 `lens.liftScaleY`。", "静止时选中胶囊的高度；按住后的透镜高度是它乘以 `lens.liftScaleY`。"),
    "metrics.contentPadding": ("底欄左右邊緣到第一格、最後一格之間的內縮。", "底栏左右边缘到第一格、最后一格之间的内边距。"),
    "metrics.pillInset": ("藥丸停在第一格或最後一格時，離底欄邊緣的最小距離。", "胶囊停在第一格或最后一格时，离底栏边缘的最小距离。"),
    "metrics.pillExtraWidth": ("藥丸比單一格寬出多少。", "胶囊比单个格子宽出多少。"),
    "metrics.maxLensOverhang": ("拖到兩端時，浮起的透鏡最多可以凸出底欄多遠。", "拖到两端时，浮起的透镜最多可以凸出底栏多远。"),
    "metrics.maxRubberBand": ("手指拖過頭時，整條底欄被橫向拉動的最大距離（橡皮筋效果）。", "手指拖过头时，整条底栏被横向拉动的最大距离（橡皮筋效果）。"),
    "metrics.panelPressGrow": ("按住時，玻璃底欄（和它看不見的複本）在寬度上長大多少；是等比放大，所以高度也會按比例變高。", "按住时，玻璃底栏（和它看不见的副本）在宽度上变大多少；是等比放大，所以高度也会按比例变高。"),
    "panel.blur": ("底欄後方內容的模糊半徑。", "底栏后方内容的模糊半径。"),
    "panel.rimWidth": ("底欄邊緣折射帶的寬度。", "底栏边缘折射带的宽度。"),
    "panel.rimRefraction": ("邊緣折射帶把後方內容彎折的強度。", "边缘折射带把后方内容弯折的强度。"),
    "lens.liftScaleX": ("按住後透鏡寬度相對於靜止藥丸的倍率。", "按住后透镜宽度相对于静止胶囊的倍率。"),
    "lens.liftScaleY": ("按住後透鏡高度相對於靜止藥丸的倍率。", "按住后透镜高度相对于静止胶囊的倍率。"),
    "lens.contentScale": ("透鏡裡看到的底欄面板，相對於透鏡外底欄的大小（以透鏡中心縮放）。", "透镜里看到的底栏面板，相对于透镜外底栏的大小（以透镜中心缩放）。"),
    "lens.tabLiftScale": ("透鏡裡的圖示和文字相對於外面的倍率；實際看到的是這個值乘上 `contentScale`（預設約 1.15 倍）。", "透镜里的图标和文字相对于外面的倍率；实际看到的是这个值乘以 `contentScale`（默认约 1.15 倍）。"),
    "lens.rimWidth": ("透鏡邊緣折射帶的寬度，從邊緣往內量。", "透镜边缘折射带的宽度，从边缘向内量。"),
    "lens.rimRefraction": ("最外圈像素往內取樣多深。要遠小於約 8 dp：再深就會取樣到底欄自己的面板，而不是後方內容，邊緣的光影看起來像固定不動。", "最外圈像素向内取样多深。要远小于约 8 dp：再深就会取样到底栏自己的面板，而不是后方内容，边缘的光影看起来像固定不动。"),
    "lens.dispersionWidth": ("透鏡邊緣色散（彩色邊）帶的寬度，內側 40% 逐漸淡出。", "透镜边缘色散（彩色边）带的宽度，内侧 40% 逐渐淡出。"),
    "lens.dispersion": ("色散的最大紅藍分離量，在兩端最強。0 表示沒有色散。", "色散的最大红蓝分离量，在两端最强。0 表示没有色散。"),
    "lens.innerShadowRadius": ("透鏡邊緣內陰影的模糊半徑。", "透镜边缘内阴影的模糊半径。"),
    "lens.innerShadowOffsetY": ("內陰影的垂直位移，形成上緣受光的斜面。", "内阴影的垂直位移，形成上缘受光的斜面。"),
    "lens.innerShadowAlpha": ("內陰影的不透明度。", "内阴影的不透明度。"),
    "colors.panelColor": ("玻璃底欄表面的顏色（含透明度）。", "玻璃底栏表面的颜色（含透明度）。"),
    "colors.restingPillColor": ("靜止時灰色選取藥丸的顏色。", "静止时灰色选中胶囊的颜色。"),
    "colors.lensSeenBlur": ("透鏡裡看到的底欄的模糊；比 `panel.blur` 小，就能看到更多後方內容。", "透镜里看到的底栏的模糊；比 `panel.blur` 小，就能看到更多后方内容。"),
    "colors.lensSeenSurfaceAlpha": ("透鏡裡看到的底欄面板的不透明度倍率；越小透鏡越透。", "透镜里看到的底栏面板的不透明度倍率；越小透镜越透。"),
    "colors.pressGlow": ("按住時跟著手指的白色柔光強度（0 = 關閉，1 = 全開）。", "按住时跟着手指的白色柔光强度（0 = 关闭，1 = 全开）。"),
    "motion.tapSlide": ("單擊後藥丸滑向該分頁的彈簧；剛性越低，滑得越慢、越看得清楚。", "单击后胶囊滑向该标签的弹簧；刚度越低，滑得越慢、越看得清楚。"),
    "motion.follow": ("拖曳時藥丸跟著手指的彈簧。", "拖动时胶囊跟着手指的弹簧。"),
    "motion.pressGlow": ("按下時玻璃效果（折射、高光、陰影）淡入的彈簧。", "按下时玻璃效果（折射、高光、阴影）淡入的弹簧。"),
    "motion.liftX": ("按下時透鏡變寬的彈簧。", "按下时透镜变宽的弹簧。"),
    "motion.liftY": ("按下時透鏡變高的彈簧。", "按下时透镜变高的弹簧。"),
    "motion.releaseGlow": ("放手時玻璃效果淡出的彈簧。", "放手时玻璃效果淡出的弹簧。"),
    "motion.releaseScale": ("放手時透鏡縮回靜止藥丸的彈簧。", "放手时透镜缩回静止胶囊的弹簧。"),
    "motion.velocity": ("手指速度的平滑彈簧，速度再去驅動果凍拉伸。", "手指速度的平滑弹簧，速度再去驱动果冻拉伸。"),
    "motion.rubberBand": ("拖過頭放手後，底欄彈回原位的彈簧。", "拖过头放手后，底栏弹回原位的弹簧。"),
    "motion.jellyStretchX": ("每單位速度造成的橫向拉伸；越大越有果凍感。", "每单位速度造成的横向拉伸；越大越有果冻感。"),
    "motion.jellyStretchY": ("每單位速度造成的縱向壓扁。", "每单位速度造成的纵向压扁。"),
    "motion.jellyLimit": ("拉伸與壓扁的比例上限；0.2 對應最寬約多 25%。", "拉伸与压扁的比例上限；0.2 对应最宽约多 25%。"),
}

HEAD = {
    "en": (
        "# Parameter reference",
        "Every tunable value of `LiquidGlassTabBar`, generated from `LiquidGlassStyle.kt`. Lengths are dp on screen at full press. "
        "Start from `LiquidGlassDefaults.style(darkTheme)` and override what you need with `copy(...)`.",
        ("Parameter", "Default", "What it does"),
        ("Parameter", "Light", "Dark", "What it does"),
        "Language: English · [繁體中文](PARAMETERS.zh-TW.md) · [简体中文](PARAMETERS.zh-CN.md)",
        "Example",
    ),
    "zh-TW": (
        "# 參數總表",
        "`LiquidGlassTabBar` 的所有可調數值，由 `LiquidGlassStyle.kt` 自動產生。長度單位是 dp，指按到底時在螢幕上的大小。"
        "從 `LiquidGlassDefaults.style(darkTheme)` 開始，用 `copy(...)` 改你要改的欄位。",
        ("參數", "預設值", "作用"),
        ("參數", "淺色", "深色", "作用"),
        "語言：[English](PARAMETERS.md) · 繁體中文 · [简体中文](PARAMETERS.zh-CN.md)",
        "範例",
    ),
    "zh-CN": (
        "# 参数总表",
        "`LiquidGlassTabBar` 的所有可调数值，由 `LiquidGlassStyle.kt` 自动生成。长度单位是 dp，指按到底时在屏幕上的大小。"
        "从 `LiquidGlassDefaults.style(darkTheme)` 开始，用 `copy(...)` 改你要改的字段。",
        ("参数", "默认值", "作用"),
        ("参数", "浅色", "深色", "作用"),
        "语言：[English](PARAMETERS.md) · [繁體中文](PARAMETERS.zh-TW.md) · 简体中文",
        "示例",
    ),
}

EXAMPLE = '''```kotlin
val style = LiquidGlassDefaults.style(darkTheme).copy(
    lens = LiquidGlassLens(dispersion = 3.dp, liftScaleY = 1.35f),
    motion = LiquidGlassMotion(tapSlide = SpringConfig(0.85f, 300f)),
)
LiquidGlassTabBar(/* ... */ style = style) { /* ... */ }
```'''


def parse():
    text = open(SRC, encoding="utf-8").read()
    classes = {}
    for cname, key, _ in GROUPS:
        m = re.search(r"data class %s\((.*?)\n\)" % cname, text, re.S)
        body = m.group(1)
        fields, doc, in_doc = [], [], False
        for line in body.split("\n"):
            s = line.strip()
            if s.startswith("/**"):
                doc = []
                in_doc = True
                s = s[3:].strip()
                if s.endswith("*/"):
                    doc.append(s[:-2].strip())
                    in_doc = False
                elif s:
                    doc.append(s)
                continue
            if in_doc:
                if s.endswith("*/"):
                    s = s[:-2].strip()
                    in_doc = False
                s = s.lstrip("*").strip()
                if s:
                    doc.append(s)
                continue
            m2 = re.match(r"val (\w+): ([\w.]+)(?: = (.+?))?,?$", s)
            if m2:
                fields.append((m2.group(1), m2.group(2), m2.group(3), re.sub(r"\[([\w.]+)\]", r"`\1`", " ".join(doc))))
                doc = []
        classes[key] = fields
    # theme colours
    cm = re.search(r"fun colors\(darkTheme: Boolean\): LiquidGlassColors =\s*if \(darkTheme\) \{(.*?)\} else \{(.*?)\n        \}", text, re.S)
    def kv(block):
        out = {}
        for k, v in re.findall(r"(\w+) = (.+?),\n", block + "\n"):
            out[k] = v.strip().rstrip(",")
        return out
    dark, light = kv(cm.group(1)), kv(cm.group(2))
    return classes, light, dark


def clean(v):
    v = v.replace("Color(0x", "#").replace(".copy(alpha = ", " @ ").replace(")", "") if v.startswith("Color(") else v
    return v


def colour(v):
    # Color(0xFF0C0C0E).copy(alpha = 0.9f)  ->  #0C0C0E @ 0.9 ; Color.White.copy(alpha = 0.1f) -> white @ 0.1
    m = re.match(r"Color\(0xFF([0-9A-Fa-f]{6})\)\.copy\(alpha = ([\d.]+)f\)", v)
    if m:
        return "`#%s` @ %s" % (m.group(1).upper(), m.group(2))
    m = re.match(r"Color\.(\w+)\.copy\(alpha = ([\d.]+)f\)", v)
    if m:
        return "`%s` @ %s" % (m.group(1).lower(), m.group(2))
    return "`%s`" % v


def pretty(v):
    return "`%s`" % v.replace("SpringConfig(", "SpringConfig(")


def render(lang, classes, light, dark):
    title, intro, head3, head4, langline, example_title = HEAD[lang]
    out = [title, "", langline, "", intro, ""]
    for cname, key, _ in GROUPS:
        out += ["## %s (`%s`)" % (TITLES[lang][key], key), ""]
        gi = GROUP_INTRO[key]
        out += [gi[0 if lang == "en" else 1 if lang == "zh-TW" else 2], ""]
        if key == "colors":
            out += ["| %s | %s | %s | %s |" % head4, "|---|---|---|---|"]
            for name, typ, default, doc in classes[key]:
                desc = doc if lang == "en" else ZH["colors." + name][0 if lang == "zh-TW" else 1]
                if name in ("panelColor", "restingPillColor"):
                    l, d = colour(light[name]), colour(dark[name])
                else:
                    l, d = pretty(light[name]), pretty(dark[name])
                out.append("| `%s` | %s | %s | %s |" % (name, l, d, desc))
        else:
            out += ["| %s | %s | %s |" % head3, "|---|---|---|"]
            for name, typ, default, doc in classes[key]:
                desc = doc if lang == "en" else ZH["%s.%s" % (key, name)][0 if lang == "zh-TW" else 1]
                out.append("| `%s` | %s | %s |" % (name, pretty(default), desc))
        out.append("")
    out += ["## %s" % example_title, "", EXAMPLE, ""]
    return "\n".join(out)


def main():
    classes, light, dark = parse()
    # every field must have a Chinese description, otherwise fail loudly
    for key, fields in classes.items():
        for name, *_ in fields:
            assert ("%s.%s" % (key, name)) in ZH, "missing zh text for %s.%s" % (key, name)
    os.makedirs(OUT, exist_ok=True)
    for lang, fname in (("en", "PARAMETERS.md"), ("zh-TW", "PARAMETERS.zh-TW.md"), ("zh-CN", "PARAMETERS.zh-CN.md")):
        with open(os.path.join(OUT, fname), "w", encoding="utf-8", newline="\n") as f:
            f.write(render(lang, classes, light, dark))
        print("wrote", fname)
    print({k: len(v) for k, v in classes.items()}, "fields")


if __name__ == "__main__":
    main()
