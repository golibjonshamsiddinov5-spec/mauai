package uz.mau.pet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

enum class CatState { WALK, SIT, SLEEP, PURR }

/**
 * Mushukcha rasmi. Chizma 140x100 birlikda, ko'rinish balandligi uning 1.5 baravari:
 * mushuk pastda, yuqorida yurakchalar va "z" uchun joy qoladi.
 */
class CatView(context: Context) : View(context) {

    var state = CatState.WALK
    var facingRight = true
    var time = 0f // soniyalarda, xizmat har kadrda yangilaydi

    private val fur = Color.parseColor("#E58A3A")
    private val furD = Color.parseColor("#B8631C")
    private val belly = Color.parseColor("#F8E4C9")
    private val ear = Color.parseColor("#F2A7A0")
    private val ink = Color.parseColor("#27303A")
    private val heart = Color.parseColor("#E0526A")

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val rect = RectF()
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val s = width / 140f
        c.save()
        c.scale(s, s)
        val top = height / s - 100f
        drawEffects(c, top)
        c.translate(0f, top)
        if (!facingRight) {
            c.translate(140f, 0f)
            c.scale(-1f, 1f)
        }
        drawCat(c)
        c.restore()
    }

    private fun drawEffects(c: Canvas, top: Float) {
        if (state == CatState.PURR) {
            label.textSize = 16f
            label.clearShadowLayer()
            for (i in 0..2) {
                val p = (time * 0.7f + i / 3f) % 1f
                label.color = Color.argb(((1f - p) * 255).toInt(), Color.red(heart), Color.green(heart), Color.blue(heart))
                c.drawText("♥", 60f + i * 22f, top + 12f - p * 40f, label)
            }
        }
        if (state == CatState.SLEEP) {
            label.textSize = 18f
            label.color = Color.WHITE
            label.setShadowLayer(3f, 0f, 1f, Color.BLACK)
            val x = if (facingRight) 114f else 26f
            c.drawText("z", x, top + 6f + sin(time * 2.6f) * 3f, label)
            label.textSize = 12f
            c.drawText("z", x + 12f, top - 6f + sin(time * 2.6f + 1f) * 3f, label)
            label.clearShadowLayer()
        }
    }

    private fun leg(c: Canvas, x: Float, w: Float, h: Float, angle: Float, color: Int) {
        c.save()
        c.rotate(angle, x + w / 2f, 70f)
        fill.color = color
        rect.set(x, 68f, x + w, 68f + h)
        c.drawRoundRect(rect, w / 2f, w / 2f, fill)
        c.restore()
    }

    private fun drawCat(c: Canvas) {
        val walking = state == CatState.WALK
        val sleeping = state == CatState.SLEEP
        val phase = time * (2f * Math.PI.toFloat() / 0.9f)
        val legAng = if (walking) sin(phase) * 26f else 0f
        val bob = if (walking) -abs(sin(phase)) * 2.5f else 0f
        val shake = if (state == CatState.PURR) sin(time * 70f) * 0.6f else 0f
        val dy = (if (sleeping) 9f else 0f) + bob + shake
        val legH = if (sleeping) 8f else 20f
        val wagSpeed = when (state) {
            CatState.WALK -> 6.3f
            CatState.PURR -> 12f
            else -> 2.6f
        }
        val wag = sin(time * wagSpeed) * 8f

        c.save()
        c.translate(0f, dy)

        // dum
        c.save()
        c.rotate(wag, 34f, 56f)
        stroke.color = furD
        stroke.strokeWidth = 10f
        path.reset()
        path.moveTo(34f, 56f)
        path.cubicTo(6f, 58f, 4f, 26f, 22f, 20f)
        c.drawPath(path, stroke)
        c.restore()

        // uzoqdagi oyoqlar
        leg(c, 42f, 10f, legH, -legAng, furD)
        leg(c, 92f, 10f, legH, legAng, furD)

        // tana
        fill.color = fur
        rect.set(26f, 36f, 110f, 80f)
        c.drawOval(rect, fill)

        // yo'llar
        stroke.color = furD
        stroke.alpha = 180
        stroke.strokeWidth = 4f
        path.reset()
        path.moveTo(46f, 40f); path.quadTo(50f, 50f, 46f, 60f)
        path.moveTo(60f, 37f); path.quadTo(64f, 49f, 60f, 61f)
        path.moveTo(74f, 37f); path.quadTo(78f, 49f, 74f, 61f)
        c.drawPath(path, stroke)
        stroke.alpha = 255

        // qorin
        fill.color = belly
        rect.set(48f, 63f, 96f, 79f)
        c.drawOval(rect, fill)

        // yaqindagi oyoqlar
        leg(c, 52f, 11f, legH, legAng, fur)
        leg(c, 82f, 11f, legH, -legAng, fur)

        drawHead(c, sleeping)
        c.restore()
    }

    private fun drawHead(c: Canvas, sleeping: Boolean) {
        c.save()
        if (sleeping) {
            c.translate(0f, 8f)
            c.rotate(14f, 100f, 62f)
        }

        // quloqlar
        fill.color = fur
        path.reset(); path.moveTo(94f, 30f); path.lineTo(97f, 8f); path.lineTo(110f, 24f); path.close()
        c.drawPath(path, fill)
        path.reset(); path.moveTo(112f, 24f); path.lineTo(124f, 8f); path.lineTo(127f, 32f); path.close()
        c.drawPath(path, fill)
        fill.color = ear
        path.reset(); path.moveTo(98f, 26f); path.lineTo(100f, 15f); path.lineTo(106f, 24f); path.close()
        c.drawPath(path, fill)
        path.reset(); path.moveTo(116f, 24f); path.lineTo(122f, 15f); path.lineTo(123f, 28f); path.close()
        c.drawPath(path, fill)

        // bosh
        fill.color = fur
        c.drawCircle(110f, 44f, 20f, fill)
        stroke.color = furD
        stroke.strokeWidth = 3f
        c.drawLine(104f, 26f, 104f, 33f, stroke)
        c.drawLine(110f, 25f, 110f, 33f, stroke)
        c.drawLine(116f, 26f, 116f, 33f, stroke)

        // tumshuq
        fill.color = belly
        rect.set(102f, 44f, 124f, 60f)
        c.drawOval(rect, fill)

        // ko'zlar
        if (sleeping) {
            stroke.color = ink
            stroke.strokeWidth = 2f
            path.reset()
            path.moveTo(101f, 43f); path.quadTo(105f, 46f, 109f, 43f)
            path.moveTo(115f, 43f); path.quadTo(119f, 46f, 123f, 43f)
            c.drawPath(path, stroke)
        } else {
            val t = time % 5f
            val k = if (t > 4.7f && t < 4.85f) 0.1f else 1f
            fill.color = ink
            rect.set(105f - 2.8f, 42f - 3.6f * k, 105f + 2.8f, 42f + 3.6f * k)
            c.drawOval(rect, fill)
            rect.set(119f - 2.8f, 42f - 3.6f * k, 119f + 2.8f, 42f + 3.6f * k)
            c.drawOval(rect, fill)
        }

        // burun, og'iz, mo'ylov
        fill.color = ear
        path.reset(); path.moveTo(126f, 47f); path.lineTo(120f, 44.5f); path.lineTo(120f, 49.5f); path.close()
        c.drawPath(path, fill)
        stroke.color = ink
        stroke.strokeWidth = 1.6f
        path.reset(); path.moveTo(124f, 50f); path.quadTo(121f, 55f, 116f, 53f)
        c.drawPath(path, stroke)
        stroke.strokeWidth = 1f
        stroke.alpha = 140
        c.drawLine(121f, 49f, 136f, 45f, stroke)
        c.drawLine(121f, 51f, 136f, 53f, stroke)
        stroke.alpha = 255

        c.restore()
    }
}
