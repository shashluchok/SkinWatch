package com.shashluchok.skinwatch.presentation.component.modal.bottomsheet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CONTAINER_HEIGHT = 2992
private const val SHORT_SHEET_HEIGHT = 512
private const val TALL_SHEET_HEIGHT = 2400

class BottomSheetPresentationTest {
    @Test
    fun `reads zero while the sheet sits below the container`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = CONTAINER_HEIGHT,
            sheetHeightPx = SHORT_SHEET_HEIGHT,
            sheetOffsetPx = CONTAINER_HEIGHT.toFloat(),
        )

        assertEquals(expected = 0f, actual = fraction)
    }

    @Test
    fun `completes when a sheet shorter than half the container has fully risen`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = CONTAINER_HEIGHT,
            sheetHeightPx = SHORT_SHEET_HEIGHT,
            sheetOffsetPx = (CONTAINER_HEIGHT - SHORT_SHEET_HEIGHT).toFloat(),
        )

        assertEquals(expected = 1f, actual = fraction)
    }

    @Test
    fun `completes when a sheet taller than half the container rests at that half`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = CONTAINER_HEIGHT,
            sheetHeightPx = TALL_SHEET_HEIGHT,
            sheetOffsetPx = CONTAINER_HEIGHT / 2f,
        )

        assertEquals(expected = 1f, actual = fraction)
    }

    @Test
    fun `stays complete once a tall sheet is dragged past its resting height`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = CONTAINER_HEIGHT,
            sheetHeightPx = TALL_SHEET_HEIGHT,
            sheetOffsetPx = (CONTAINER_HEIGHT - TALL_SHEET_HEIGHT).toFloat(),
        )

        assertEquals(expected = 1f, actual = fraction)
    }

    @Test
    fun `reads half way up its resting height`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = CONTAINER_HEIGHT,
            sheetHeightPx = SHORT_SHEET_HEIGHT,
            sheetOffsetPx = CONTAINER_HEIGHT - SHORT_SHEET_HEIGHT / 2f,
        )

        assertEquals(expected = 0.5f, actual = fraction)
    }

    @Test
    fun `reads zero before the sheet has been measured`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = CONTAINER_HEIGHT,
            sheetHeightPx = 0,
            sheetOffsetPx = 0f,
        )

        assertEquals(expected = 0f, actual = fraction)
    }

    @Test
    fun `reads zero before the container has been measured`() {
        val fraction = sheetRevealFraction(
            containerHeightPx = 0,
            sheetHeightPx = SHORT_SHEET_HEIGHT,
            sheetOffsetPx = 0f,
        )

        assertEquals(expected = 0f, actual = fraction)
    }

    /**
     * The keyboard shrinks the container and pushes the settled sheet up by the same amount frame by
     * frame. Both numbers come from one layout pass, so the reveal has to hold -- reading the
     * container from the window minus the ime inset instead had it collapse to zero mid-animation.
     */
    @Test
    fun `holds steady while the keyboard shrinks the container under a settled sheet`() {
        val fractions = generateSequence(CONTAINER_HEIGHT) { it - 128 }
            .takeWhile { it > SHORT_SHEET_HEIGHT }
            .map { containerHeight ->
                sheetRevealFraction(
                    containerHeightPx = containerHeight,
                    sheetHeightPx = SHORT_SHEET_HEIGHT,
                    sheetOffsetPx = (containerHeight - SHORT_SHEET_HEIGHT).toFloat(),
                )
            }.toList()

        assertTrue(actual = fractions.isNotEmpty())
        assertTrue(
            actual = fractions.all { it == 1f },
            message = "reveal dipped during the keyboard animation: $fractions",
        )
    }
}
