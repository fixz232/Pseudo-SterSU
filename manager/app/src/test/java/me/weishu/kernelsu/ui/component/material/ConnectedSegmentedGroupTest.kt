package me.weishu.kernelsu.ui.component.material

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectedSegmentedGroupTest {
    @Test
    fun onlyTheOuterCornersAreRoundedAtEveryGroupSize() {
        for (count in 1..8) for (index in 0 until count) {
            for (scale in listOf(1f, 2f, 3f)) {
                val shape = connectedSegmentedShape(index == 0, index == count - 1)
                val size = Size(320f * scale, 72f * scale)
                val density = Density(scale)
                val top = if (index == 0) 16f * scale else 0f
                val bottom = if (index == count - 1) 16f * scale else 0f
                assertEquals(top, shape.topStart.toPx(size, density), 0f)
                assertEquals(top, shape.topEnd.toPx(size, density), 0f)
                assertEquals(bottom, shape.bottomStart.toPx(size, density), 0f)
                assertEquals(bottom, shape.bottomEnd.toPx(size, density), 0f)
            }
        }
    }

    @Test
    fun adjacentRowsHaveNoRoundedCutoutsAtTheirConnection() {
        for (width in listOf(120f, 240f, 320f, 720f)) {
            for (count in 2..8) for (index in 0 until count - 1) {
                val upper = connectedSegmentedShape(index == 0, false)
                val lower = connectedSegmentedShape(false, index + 1 == count - 1)
                val size = Size(width, 72f)
                val density = Density(1f)
                assertEquals(0f, upper.bottomStart.toPx(size, density), 0f)
                assertEquals(0f, upper.bottomEnd.toPx(size, density), 0f)
                assertEquals(0f, lower.topStart.toPx(size, density), 0f)
                assertEquals(0f, lower.topEnd.toPx(size, density), 0f)
            }
        }
    }
}
