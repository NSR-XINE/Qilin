package com.mydonghua.app.domain.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CountdownCalculatorTest {

    private var originalTz: TimeZone? = null

    private val dayNames = arrayOf(
        "", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
    )

    @Before
    fun setUp() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTz)
    }

    @Test
    fun invalidInputs_returnNull() {
        assertNull(CountdownCalculator.getNextReleaseDateMs(null, "10:00"))
        assertNull(CountdownCalculator.getNextReleaseDateMs("monday", null))
        assertNull(CountdownCalculator.getNextReleaseDateMs("monday", ""))
        assertNull(CountdownCalculator.getNextReleaseDateMs("monday", "no-colon"))
        assertNull(CountdownCalculator.getNextReleaseDateMs("funday", "10:00"))
    }

    @Test
    fun nextRelease_resolvesToTargetWeekdayAndTime() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val targetDay = (today % 7) + 1

        val ms = CountdownCalculator.getNextReleaseDateMs(dayNames[targetDay], "10:00")
        assertNotNull(ms)

        val cal = Calendar.getInstance().apply { timeInMillis = ms!! }
        assertEquals(targetDay, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue("target must be in the future", ms!! > System.currentTimeMillis())
    }

    @Test
    fun timeRemaining_null_isElapsed() {
        val r = CountdownCalculator.calculateTimeRemaining(null)
        assertTrue(r.elapsed)
        assertFalse(r.airingNow)
        assertEquals(0L, r.days)
        assertEquals(0L, r.hours)
        assertEquals(0L, r.minutes)
    }

    @Test
    fun timeRemaining_future_computesDayHourMinute() {
        val now = System.currentTimeMillis()
        val target = now + 2L * 24 * 60 * 60 * 1000 +
            3L * 60 * 60 * 1000 +
            4L * 60 * 1000 +
            5L * 1000

        val r = CountdownCalculator.calculateTimeRemaining(target)
        assertFalse(r.elapsed)
        assertFalse(r.airingNow)
        assertEquals(2L, r.days)
        assertEquals(3L, r.hours)
        assertEquals(4L, r.minutes)
        assertTrue("seconds should be 4-5 (sub-second drift)", r.seconds == 4L || r.seconds == 5L)
    }

    @Test
    fun timeRemaining_pastWithin90min_isAiringNow() {
        val now = System.currentTimeMillis()
        val r = CountdownCalculator.calculateTimeRemaining(now - 30L * 60 * 1000)
        assertTrue(r.airingNow)
        assertFalse(r.elapsed)
    }

    @Test
    fun timeRemaining_pastBeyond90min_isElapsed() {
        val now = System.currentTimeMillis()
        val r = CountdownCalculator.calculateTimeRemaining(now - 2L * 60 * 60 * 1000)
        assertTrue(r.elapsed)
        assertFalse(r.airingNow)
    }
}
