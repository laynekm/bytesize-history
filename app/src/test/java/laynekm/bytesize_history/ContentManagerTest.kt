package laynekm.bytesize_history

import junit.framework.Assert.*
import org.junit.Test

class ContentManagerTest {

    @Test
    fun contentManagerTest() {
        val contentManager = ContentManager()

        val allDates = generateAllDatesInYear()
        assertEquals(366, allDates.size)
        allDates.forEach {
            val historyItems = contentManager.fetchHistoryItemsTest(it)
            print("${buildDateForURL(it)}: ${historyItems!!.size} items\n")

            assertNotNull(historyItems)
        }
    }
}