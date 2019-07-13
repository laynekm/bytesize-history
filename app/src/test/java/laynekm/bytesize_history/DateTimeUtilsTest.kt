package laynekm.bytesize_history

import org.junit.Assert
import org.junit.Test

class DateTimeUtilsTest {
    @Test
    fun to12HourTimeTest() {
        Assert.assertEquals("12:05 AM", stringTo12HourString("00:05"))
        Assert.assertEquals("10:20 AM", stringTo12HourString("10:20"))
        Assert.assertEquals("10:20 PM", stringTo12HourString("22:20"))
        Assert.assertEquals("12:05 PM", stringTo12HourString("12:05"))
    }
}