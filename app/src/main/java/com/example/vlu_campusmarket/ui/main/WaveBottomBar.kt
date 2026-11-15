package com.example.vlu_campusmarket.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.vlu_campusmarket.R

class WaveBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val primaryColor = Color.parseColor("#E11D48")
    private val inactiveColor = Color.parseColor("#94A3B8")

    private val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#1E293B") else Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(
            10f * resources.displayMetrics.density,
            0f,
            -3f * resources.displayMetrics.density,
            Color.argb(if (isDarkMode) 60 else 25, 0, 0, 0)
        )
    }

    private val path = Path()
    private var currentSelectedIndex = 0
    var onItemSelected: ((Int) -> Unit)? = null

    private val tabItems = listOf(
        TabItem(R.drawable.ic_home, "Trang chủ"),
        TabItem(R.drawable.ic_cart, "Giỏ hàng"),
        TabItem(R.drawable.ic_add, "Tạo bài"),
        TabItem(R.drawable.ic_order, "Hóa đơn"),
        TabItem(R.drawable.ic_menu, "Menu")
    )

    private val tabViews = mutableListOf<View>()
    private lateinit var floatingBubble: CardView
    private lateinit var floatingIcon: ImageView
    private lateinit var tabsContainer: LinearLayout

    private var animator: ValueAnimator? = null
    private var bubbleCenterX = 0f
    private var isInitialized = false

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
        clipChildren = false
        clipToPadding = false
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setupViews()
    }

    private fun setupViews() {
        tabsContainer = LinearLayout(context).apply {
            id = generateViewId()
            orientation = LinearLayout.HORIZONTAL
            weightSum = 5f
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpTypeToPx(72)
            ).apply {
                bottomToBottom = LayoutParams.PARENT_ID
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
            }
        }
        addView(tabsContainer)

        val bubbleSize = dpTypeToPx(56)
        floatingBubble = CardView(context).apply {
            layoutParams = LayoutParams(bubbleSize, bubbleSize)
            radius = bubbleSize / 2f
            cardElevation = dpTypeToPx(6).toFloat()
            setCardBackgroundColor(primaryColor) // VLU Red distinct background for active FAB
            useCompatPadding = false
            preventCornerOverlap = false
            clipChildren = false
            clipToPadding = false
        }

        floatingIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpTypeToPx(24),
                dpTypeToPx(24)
            ).apply {
                gravity = Gravity.CENTER
            }
            setColorFilter(Color.WHITE) // White icon on red background
        }

        floatingBubble.addView(floatingIcon)
        addView(floatingBubble)

        tabItems.forEachIndexed { index, item ->
            val tabView = LayoutInflater.from(context)
                .inflate(R.layout.item_wave_tab, tabsContainer, false).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                    )
                    setOnClickListener {
                        selectTab(index, true)
                        onItemSelected?.invoke(index)
                    }
                }

            val iconView = tabView.findViewById<ImageView>(R.id.tabIcon)
            val textView = tabView.findViewById<TextView>(R.id.tabText)
            iconView.setImageResource(item.iconRes)
            textView.text = item.title

            tabViews.add(tabView)
            tabsContainer.addView(tabView)
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isInitialized && tabViews.isNotEmpty()) {
            isInitialized = true
            updateTabSelection(currentSelectedIndex, false)
        }
    }

    fun selectTab(index: Int, animate: Boolean) {
        if (index !in tabItems.indices) return
        currentSelectedIndex = index
        updateTabSelection(index, animate)
    }

    private fun updateTabSelection(index: Int, animate: Boolean) {
        if (tabViews.isEmpty() || !isInitialized) return

        val tabView = tabViews[index]
        val targetX = tabView.left + tabView.width / 2f

        floatingIcon.setImageResource(tabItems[index].iconRes)

        val barHeight = dpTypeToPx(72).toFloat()
        val barTop = height - barHeight

        floatingBubble.y = barTop - floatingBubble.height / 2f + dpTypeToPx(4)

        animator?.cancel()

        if (animate && bubbleCenterX != 0f) {
            animator = ValueAnimator.ofFloat(bubbleCenterX, targetX).apply {
                duration = 350L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    bubbleCenterX = animation.animatedValue as Float
                    floatingBubble.x = bubbleCenterX - floatingBubble.width / 2f
                    invalidate()
                }
                start()
            }
        } else {
            bubbleCenterX = targetX
            floatingBubble.x = targetX - floatingBubble.width / 2f
            invalidate()
        }

        tabViews.forEachIndexed { i, view ->
            val iconView = view.findViewById<ImageView>(R.id.tabIcon)
            val textView = view.findViewById<TextView>(R.id.tabText)
            if (i == index) {
                iconView.visibility = INVISIBLE
                textView.visibility = VISIBLE
                textView.setTextColor(primaryColor)
            } else {
                iconView.visibility = VISIBLE
                iconView.setColorFilter(inactiveColor)
                textView.visibility = VISIBLE
                textView.setTextColor(inactiveColor)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        val barHeight = dpTypeToPx(72).toFloat()
        val barTop = height - barHeight
        val notchRadius = dpTypeToPx(46).toFloat()
        val notchDepth = dpTypeToPx(28).toFloat()

        val centerX = bubbleCenterX

        path.moveTo(0f, barTop)

        val startX = (centerX - notchRadius).coerceAtLeast(0f)
        val endX = (centerX + notchRadius).coerceAtMost(width)

        path.lineTo(startX, barTop)

        val leftControlX = (centerX - notchRadius * 0.5f).coerceAtLeast(startX)
        val rightControlX = (centerX + notchRadius * 0.5f).coerceAtMost(endX)

        path.cubicTo(
            leftControlX, barTop,
            leftControlX, barTop + notchDepth,
            centerX.coerceIn(startX, endX), barTop + notchDepth
        )

        path.cubicTo(
            rightControlX, barTop + notchDepth,
            rightControlX, barTop,
            endX, barTop
        )

        path.lineTo(width, barTop)
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        canvas.drawPath(path, paint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun dpTypeToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    data class TabItem(
        val iconRes: Int,
        val title: String
    )
}
