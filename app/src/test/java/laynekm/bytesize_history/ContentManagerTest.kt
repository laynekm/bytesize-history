package laynekm.bytesize_history

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

// TODO: Add test that fetches data for every day of the year and makes sure HistoryItems are generated
class ContentManagerTest {

    @Test
    fun contentManagerTest() {
        val contentManager = ContentManager()

        val allDates = generateAllDatesInYear()
        assertEquals(366, allDates.size)
        allDates.forEach {
            contentManager.fetchHistoryItemsTest(it, ::callbackTest)
        }

    }

    private fun callbackTest(success: Boolean, historyItems: MutableList<HistoryItem>) {
        assertTrue(success)
    }
}