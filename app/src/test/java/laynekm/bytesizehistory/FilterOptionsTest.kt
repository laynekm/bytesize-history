package laynekm.bytesizehistory

import org.junit.Assert
import org.junit.Test

class FilterOptionsTest {
    @Test
    fun equalsTest() {
        val order1: Order = Order.ASCENDING
        val types1: MutableList<Type> = mutableListOf(Type.EVENT)
        val eras1: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)
        val options1 = FilterOptions(order1, types1, eras1)

        val order2: Order = Order.ASCENDING
        val types2: MutableList<Type> = mutableListOf(Type.EVENT)
        val eras2: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)
        val options2 = FilterOptions(order2, types2, eras2)

        val order3: Order = Order.DESCENDING
        val types3: MutableList<Type> = mutableListOf(Type.EVENT)
        val eras3: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)
        val options3 = FilterOptions(order3, types3, eras3)

        val order4: Order = Order.DESCENDING
        val types4: MutableList<Type> = mutableListOf(Type.EVENT, Type.DEATH)
        val eras4: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS)
        val options4 = FilterOptions(order4, types4, eras4)

        Assert.assertTrue(options1.equals(options2))
        Assert.assertFalse(options1.equals(options3))
        Assert.assertFalse(options2.equals(options4))
    }

    @Test
    fun listsEqualTest() {
        val order: Order = Order.ASCENDING
        val types: MutableList<Type> = mutableListOf(Type.EVENT)
        val eras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)

        val filterOptions = FilterOptions(order, types, eras)
        val list1 = mutableListOf(Era.TWOTHOUSANDS, Era.ANCIENT, Era.EIGHTEENS)
        val list2 = mutableListOf(Era.ANCIENT, Era.EIGHTEENS, Era.TWOTHOUSANDS)
        Assert.assertTrue(filterOptions.listsEqual(list1, list2))

        val list3 = mutableListOf(Era.TWOTHOUSANDS, Era.ANCIENT)
        val list4 = mutableListOf(Era.TWOTHOUSANDS, Era.ANCIENT, Era.EIGHTEENS)
        Assert.assertFalse(filterOptions.listsEqual(list3, list4))
    }
}