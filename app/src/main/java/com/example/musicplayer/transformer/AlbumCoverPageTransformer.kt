package com.example.musicplayer.transformer

import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

class AlbumCoverPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        val motionLayout = view as? MotionLayout ?: return

        // Áp dụng phép biến đổi mượt mà hơn, sử dụng đường cong làm mềm (easing)
        val progress = when {
            // Trang ở bên trái hoàn toàn: trạng thái start (0.0)
            position <= -1f -> 0f
            // Trang đang di chuyển từ giữa sang trái hoặc từ phải vào giữa
            position < 0f -> {
                // Sử dụng cubic easing để làm mượt chuyển động
                val positionAbs = abs(position)
                val easing = 1 - (1 - positionAbs) * (1 - positionAbs) * (1 - positionAbs)
                0.5f - easing / 2
            }
            // Trang đang di chuyển từ giữa sang phải hoặc từ trái vào giữa
            position < 1f -> {
                // Sử dụng cubic easing để làm mượt chuyển động
                val easing = position * position * position
                0.5f + easing / 2
            }
            // Trang ở bên phải hoàn toàn: trạng thái end (1.0)
            else -> 1f
        }

        // Đảm bảo progress nằm trong khoảng [0, 1]
        val clampedProgress = progress.coerceIn(0f, 1f)

        // Áp dụng progress vào MotionLayout
        motionLayout.progress = clampedProgress

        // Đảm bảo trang hiện tại luôn hiển thị trên cùng (z-index cao hơn)
        if (abs(position) < 0.5) {
            view.elevation = 1f
        } else {
            view.elevation = 0f
        }
    }
}
