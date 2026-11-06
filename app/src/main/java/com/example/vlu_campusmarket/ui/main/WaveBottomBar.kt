package com.example.vlu_campusmarket.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class WaveBottomBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPath = Path()
    private val mPaint = Paint().apply {
        color = Color.parseColor("#E11D48") // Đỏ Văn Lang
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var mCurvedCenterX = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mCurvedCenterX == 0f) {
            val tabWidth = w / 5f
            mCurvedCenterX = tabWidth / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPath.reset()

        val width = width.toFloat()
        val height = height.toFloat()

        // Tọa độ chuẩn tạo lòng chảo trắng ôm trọn FAB
        val topMargin = 75f      // Mép trên nền đỏ lùi xuống dưới
        val curveRadius = 95f    // Bán kính miệng lòng chảo
        val curveDepth = 85f     // Độ sâu lõm xuống của lòng chảo màu trắng

        mPath.moveTo(0f, height)
        mPath.lineTo(0f, topMargin)
        mPath.lineTo(mCurvedCenterX - curveRadius, topMargin)

        // 1. Uốn cong vuốt mượt từ mép đỏ chìm xuống lòng chảo trắng
        mPath.cubicTo(
            mCurvedCenterX - (curveRadius * 0.45f), topMargin,
            mCurvedCenterX - (curveRadius * 0.4f), topMargin + curveDepth,
            mCurvedCenterX, topMargin + curveDepth
        )

        // 2. Uốn cong đi từ đáy lòng chảo trắng trôi lên mép đỏ bên phải
        mPath.cubicTo(
            mCurvedCenterX + (curveRadius * 0.4f), topMargin + curveDepth,
            mCurvedCenterX + (curveRadius * 0.45f), topMargin,
            mCurvedCenterX + curveRadius, topMargin
        )

        mPath.lineTo(width, topMargin)
        mPath.lineTo(width, height)
        mPath.close()

        canvas.drawPath(mPath, mPaint)
    }

    fun animateCurveTo(targetX: Float) {
        val animator = ValueAnimator.ofFloat(mCurvedCenterX, targetX)
        animator.duration = 280
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            mCurvedCenterX = animation.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    fun setCurvePosition(targetX: Float) {
        mCurvedCenterX = targetX
        invalidate()
    }
}