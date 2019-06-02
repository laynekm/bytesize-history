package laynekm.bytesize_history

import org.junit.Assert
import org.junit.Test

class ContentProviderTest {
    @Test
    fun equalsTest() {
        val contentProvider = ContentProvider()
        val list1 = mutableListOf(1,2,3,4,5)
        val subList1 = mutableListOf(1,2)
        val count1 = 2
        Assert.assertEquals(mutableListOf(3,4), contentProvider.getAvailableItems(list1, subList1, count1))

        val list2 = mutableListOf(1,2,3,4,5)
        val subList2 = mutableListOf(1,2)
        val count2 = 5
        Assert.assertEquals(mutableListOf(3,4,5), contentProvider.getAvailableItems(list2, subList2, count2))
    }
}