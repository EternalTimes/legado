package io.legado.app.ui.book.read.page.delegate

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.VelocityTracker
import io.legado.app.data.entities.Book
import io.legado.app.help.book.isImage
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.provider.ChapterProvider

@Suppress("UnnecessaryVariable")
class ScrollPageDelegate(readView: ReadView) : PageDelegate(readView) {

    // 滑动追踪的时间
    private val velocityDuration = 1000

    //速度追踪器
    private val mVelocity: VelocityTracker = VelocityTracker.obtain()

    var noAnim: Boolean = false

    override fun onAnimStart(animationSpeed: Int) {
        //惯性滚动
        fling(
            0, touchY.toInt(), 0, mVelocity.yVelocity.toInt(),
            0, 0, -10 * viewHeight, 10 * viewHeight
        )
    }

    override fun onAnimStop() {
        // nothing
    }

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                abortAnim()
                mVelocity.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                onScroll(event)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                onAnimStart(readView.defaultAnimationSpeed)
            }
        }
    }

    override fun onScroll() {
        curPage.scroll((touchY - lastY).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        // nothing
    }

    private fun onScroll(event: MotionEvent) {
        mVelocity.addMovement(event)
        mVelocity.computeCurrentVelocity(velocityDuration)
        //取最后添加(即最新的)一个触摸点来计算滚动位置
        //多点触控时即最后按下的手指产生的事件点
        val pointX = event.getX(event.pointerCount - 1)
        val pointY = event.getY(event.pointerCount - 1)
        readView.setTouchPoint(pointX, pointY)
        if (!isMoved) {
            val deltaX = (pointX - startX).toInt()
            val deltaY = (pointY - startY).toInt()
            val distance = deltaX * deltaX + deltaY * deltaY
            isMoved = distance > readView.slopSquare
        }
        if (isMoved) {
            isRunning = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mVelocity.recycle()
    }

    override fun abortAnim() {
        isStarted = false
        isMoved = false
        isRunning = false
        if (!scroller.isFinished) {
            readView.isAbortAnim = true
            scroller.abortAnimation()
        } else {
            readView.isAbortAnim = false
        }
    }

    override fun nextPageByAnim(animationSpeed: Int) {
        if (readView.isAbortAnim) {
            return
        }
        if (noAnim) {
            curPage.scroll(calcNextPageOffset())
            return
        }
        readView.setStartPoint(0f, 0f, false)
        startScroll(0, 0, 0, calcNextPageOffset(), animationSpeed)
    }

    override fun prevPageByAnim(animationSpeed: Int) {
        if (readView.isAbortAnim) {
            return
        }
        if (noAnim) {
            curPage.scroll(calcPrevPageOffset())
            return
        }
        readView.setStartPoint(0f, 0f, false)
        startScroll(0, 0, 0, calcPrevPageOffset(), animationSpeed)
    }

    /**
     * 计算点击翻页保留一行的滚动距离
     * 图片页使用可视高度作为滚动距离
     */
    private fun calcNextPageOffset(): Int {
        val visibleHeight = ChapterProvider.visibleHeight
        val book = ReadBook.book!!
        if (book.isImage) {
            return -visibleHeight
        }
        val visiblePage = readView.getCurVisiblePage()
        val isTextStyle = book.getImageStyle().equals(Book.imgStyleText, true)
        if (!isTextStyle && visiblePage.hasImageOrEmpty()) {
            return -visibleHeight
        }
        val lastLineTop = visiblePage.lines.last().lineTop.toInt()
        val offset = lastLineTop - ChapterProvider.paddingTop
        return -offset
    }

    private fun calcPrevPageOffset(): Int {
        val visibleHeight = ChapterProvider.visibleHeight
        val book = ReadBook.book!!
        if (book.isImage) {
            return visibleHeight
        }
        val visiblePage = readView.getCurVisiblePage()
        val isTextStyle = book.getImageStyle().equals(Book.imgStyleText, true)
        if (!isTextStyle && visiblePage.hasImageOrEmpty()) {
            return visibleHeight
        }
        val firstLineBottom = visiblePage.lines.first().lineBottom.toInt()
        val offset = visibleHeight - (firstLineBottom - ChapterProvider.paddingTop)
        return offset
    }
}