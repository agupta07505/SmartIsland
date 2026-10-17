/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.model

/** What the eyes can be. [blinks] eyes close and reopen now and then; curved, closed and heart eyes do not. */
enum class MascotEye {
    Open, Closed, Happy, Heart, Sleepy, Wide, Squint,
    /** The lid slants down toward the other eye. */
    Determined;

    val blinks: Boolean get() = this == Open || this == Wide || this == Squint
}

data class MascotExpression(
    val left: MascotEye,
    val right: MascotEye,
    /** Where the eyes look, in shares of the buddy's size. Y grows downward. */
    val gaze: MascotPoint = MascotPoint.Zero,
    /** The head's lean in radians, clockwise on screen. */
    val tilt: Float = 0f
) {
    /** Open eyes blink now and then. */
    val blinks: Boolean get() = left.blinks && right.blinks
}

enum class MascotMood(val id: String) {
    Idle("idle"), Happy("happy"), Wink("wink"), Love("love"), Sleepy("sleepy"), Determined("determined"),
    Surprised("surprised"), Searching("searching"), Thinking("thinking"), Confused("confused"),
    /** Wide awake, while the torch is on. */
    Alert("alert");

    val expression: MascotExpression
        get() = when (this) {
            Idle -> MascotExpression(MascotEye.Open, MascotEye.Open)
            Happy -> MascotExpression(MascotEye.Happy, MascotEye.Happy, MascotPoint(0f, -0.02f))
            Wink -> MascotExpression(MascotEye.Open, MascotEye.Happy, tilt = -0.08f)
            Love -> MascotExpression(MascotEye.Heart, MascotEye.Heart)
            Sleepy -> MascotExpression(MascotEye.Sleepy, MascotEye.Sleepy, MascotPoint(0f, 0.03f), 0.05f)
            Determined -> MascotExpression(MascotEye.Determined, MascotEye.Determined)
            Surprised -> MascotExpression(MascotEye.Wide, MascotEye.Wide, MascotPoint(0f, -0.02f))
            Searching -> MascotExpression(MascotEye.Open, MascotEye.Open, MascotPoint(0.05f, 0f))
            Thinking -> MascotExpression(MascotEye.Squint, MascotEye.Open, MascotPoint(0.06f, -0.06f), -0.1f)
            Confused -> MascotExpression(MascotEye.Open, MascotEye.Squint, MascotPoint(-0.03f, 0.01f), 0.14f)
            Alert -> MascotExpression(MascotEye.Wide, MascotEye.Wide)
        }
}

/**
 * Where the face sits on each body, in shares of the buddy's size from its top left corner.
 */
data class MascotFaceLayout(
    val leftEye: MascotPoint,
    val rightEye: MascotPoint,
    /** An open eye's width and height. */
    val eyeSize: MascotSize,
    /** The soft highlight on the body: its center, radii and lean. */
    val shineCenter: MascotPoint,
    val shineRadii: MascotSize,
    val shineAngle: Float
)

val MascotLook.face: MascotFaceLayout
    get() {
        if (style == MascotStyle.Robot) {
            return MascotFaceLayout(
                MascotPoint(0.36f, 0.63f), MascotPoint(0.64f, 0.63f), MascotSize(0.085f, 0.16f),
                MascotPoint(0.27f, 0.34f), MascotSize(0.07f, 0.04f), -0.3f
            )
        }
        return when (shape) {
            MascotShape.Ball -> MascotFaceLayout(
                MascotPoint(0.36f, 0.50f), MascotPoint(0.64f, 0.50f), MascotSize(0.10f, 0.20f),
                MascotPoint(0.31f, 0.29f), MascotSize(0.085f, 0.055f), -0.5f
            )
            MascotShape.Egg -> MascotFaceLayout(
                MascotPoint(0.375f, 0.55f), MascotPoint(0.625f, 0.55f), MascotSize(0.095f, 0.19f),
                MascotPoint(0.33f, 0.31f), MascotSize(0.075f, 0.05f), -0.6f
            )
            MascotShape.Squircle -> MascotFaceLayout(
                MascotPoint(0.35f, 0.53f), MascotPoint(0.65f, 0.53f), MascotSize(0.10f, 0.20f),
                MascotPoint(0.24f, 0.28f), MascotSize(0.08f, 0.05f), -0.55f
            )
            MascotShape.Pill -> MascotFaceLayout(
                MascotPoint(0.37f, 0.57f), MascotPoint(0.63f, 0.57f), MascotSize(0.09f, 0.19f),
                MascotPoint(0.20f, 0.42f), MascotSize(0.07f, 0.045f), -0.45f
            )
        }
    }
