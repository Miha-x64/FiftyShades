# Fifty Shades: CSS-style shadows for Android

## What?

In CSS, shadows are specified by `(dx, dy, blurRadius, colour)` (I call it `ShadowSpec`).
This library implements such shadows for Android.

## Why?

I know only two shadow implementations in Android SDK out of the box:
`Paint#setShadowLayer` and `View#elevation`.

ShadowLayer produces nice-looking shadows but works only with software rendering.
A sheet with shadow could be big, containing many pixels, but this becomes even worse
if you set SOFTWARE_LAYER on a whole full-screen RecyclerView
in order to draw background properly.

Elevation is implemented somewhere deep inside `RenderNode`,
there's no direct control over this shadow, there's no such a thing
like `Canvas#drawElevation`. Also, speaking in terms of CSS, `dx=0`,
and both `dy` and `blurRadius` are driven by `elevation`.
Elevation colour is one more pain in the ass, but, well, I think you already know it.

There's also `MaterialShapeDrawable` which is actually
a poor man's elevation. Same problems as above apply plus drawing artifacts.
It also drops shadowColor's alpha; `Drawable#setAlpha` invocations
don't alter shadow transparency either.

## How?

```groovy

repositories {
    // Groovy:
    maven { url 'https://jitpack.io' }
    
    // Kotlin:
    maven(url = "https://jitpack.io")
}

// module-level build.gradle:
dependencies {
    implementation('com.github.Miha-x64:FiftyShades:-SNAPSHOT')
}
```

### Static shadow

```kotlin
RectWithShadow.createDrawable(
    RectSpec(Color.WHITE, dp(20)),
    ShadowSpec(dp(2), dp(3), dp(20), Color.BLACK)
)
```
This will return a `Drawable` (a `NinePatchDrawable` wrapped in `InsetDrawable`, actually)
with a white rectangle, round corners (20dp radius),
and a black shadow blurred by 20dp and offset by (2dp; 3dp).

Keep in mind that it will draw out of bounds,
so `clipChildren=false` on parent layout is required.

### Dynamic shadow

```kotlin
LayerDrawable(arrayOf(
    RectShadow(dp(20), ShadowSpec(dp(2), dp(3), dp(20), Color.BLACK)),
    RoundRectDrawable(Color.WHITE, dp(20)) // explained later
))
```

`RectShadow` draws a shadow (out of bounds, remember!)
while RoundRectDrawable, well, it draws a round rect.

Now you can modify properties of `RectShadow` at runtime: `.cornerRadius(100500).shadow(nicerShadow)`

### Inner shadow

```kotlin
LayerDrawable(arrayOf(
    RoundRectDrawable(Color.WHITE, dp(20)), // explained later
    RectInnerShadow(dp(20), ShadowSpec(dp(2), dp(3), dp(20), Color.BLACK))
))
```

Add `Inner`, make it draw *after* round rect, and that's it: inner shadow,
known as `inset` in CSS. Interface is the same.

#### Dafuq is RoundRectDrawable?

It's just standard `GradientDrawable` (a.k.a. `<shape>`):

```kotlin
fun RoundRectDrawable(@ColorInt color: Int, @Px radius: Int): Drawable =
    GradientDrawable().apply {
        setColor(color)
        setCornerRadius(radius)
    }
```

### ItemDecoration for RecyclerView

You may want to create a `RecyclerView`, set `clipChildren=false`,
and set drawables with shadow as item backgrounds.
This will work 99% of time but fail miserably during item animations:
when alpha < 1, `clipChildren` becomes effectively true
because of intermediate buffer which has its bounds,
so you will have your shadows clipped.

The saviour is `RectItemsWithShadows(rect, shadow)` item decorator
with an already familiar constructor. Individual item properties are controllable
and animatable:

```kotlin
itemView.stateListAnimator = StateListAnimator().apply {
    addState(intArrayOf(android.R.attr.state_selected),
        ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat(DECOR_SHADOW_RADIUS, dp(32f)),
            PropertyValuesHolder.ofInt(DECOR_SHADOW_COLOR, 0xFF_AAFFCC.toInt()).argb(),
            PropertyValuesHolder.ofInt(DECOR_RECT_FILL_COLOR, 0xFF_AAFFCC.toInt()).argb(),
        )
    )
    addState(intArrayOf(),
        ObjectAnimator.ofPropertyValuesHolder(null as Any?,
            PropertyValuesHolder.ofFloat(DECOR_SHADOW_RADIUS, dp(8f)),
            PropertyValuesHolder.ofInt(DECOR_SHADOW_COLOR, 0x66_000000).argb(),
            PropertyValuesHolder.ofInt(DECOR_RECT_FILL_COLOR, Color.WHITE).argb(),
        )
    )
}

private val argbEvaluator = ArgbEvaluator()
fun PropertyValuesHolder.argb(): PropertyValuesHolder = apply { setEvaluator(argbEvaluator) }
```

![Some shadows](/example.png)
