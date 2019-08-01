package laynekm.bytesize_history

import junit.framework.Assert.*
import org.junit.Test

// Tests every date in year to make sure history items are fetched and parsed correctly
class ContentManagerTest {

    @Test
    fun contentManagerTest() {
        val contentManager = ContentManager()
        var totalItemsOfAllDates = 0

        val allDates = generateAllDatesInYear()
        assertEquals(366, allDates.size)
        for (date in allDates) {
            val mappedHistoryItems = contentManager.fetchHistoryItemsTest(date)
            assertNotNull(mappedHistoryItems)

            if (mappedHistoryItems == null) {
                print("${buildDateForURL(date)}: NULL")
                break
            }

            // Print a summary of how many items are in each date
            var totalItems = 0
            var itemsOfEachType = ""
            for (type in Type.values()) {
                totalItems += mappedHistoryItems[type]!!.size
                if (itemsOfEachType != "") itemsOfEachType += ", "
                itemsOfEachType += "${mappedHistoryItems[type]!!.size} ${type}s"
            }
            totalItemsOfAllDates += totalItems

            print("------------------------------------------------------------\n")
            print("${buildDateForURL(date)}: Fetched $totalItems items ($itemsOfEachType)\n")
            print("------------------------------------------------------------\n")

            // Assert that history items exist for each type as well as some basic attribute verification
            for (type in Type.values()) {
                assertTrue(mappedHistoryItems[type]!!.size > 0)
            }

            for (mappedHistoryItem in mappedHistoryItems) {
                for (historyItem in mappedHistoryItem.value) {
                    print("     ${historyItem.type} - ${historyItem.year}: ${historyItem.desc}\n")
                    assertEquals(mappedHistoryItem.key, historyItem.type)
                    assertEquals(date, historyItem.date)
                    assertNotNull(historyItem.desc)
                    assertNotNull(historyItem.links)
                    assertNotNull(historyItem.depth)
                    assertNotNull(historyItem.formattedYear)
                    assertNotNull(historyItem.era)

                    if (historyItem.type == Type.OBSERVANCE) {
                        assertNull(historyItem.year)
                    } else {
                        assertNotNull(historyItem.year)
                    }
                }
            }
        }

        print("Test complete! Total history items fetched: $totalItemsOfAllDates")
    }
}