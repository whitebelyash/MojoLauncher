package net.kdt.pojavlaunch.utils

import androidx.annotation.NonNull
import java.util.AbstractList

class FilteredSubList<E>(motherList: Array<E>, filter: BasicPredicate<E>) : AbstractList<E>(), MutableList<E> {

    private val mArrayList = ArrayList<E>()

    init {
        refresh(motherList, filter)
    }

    fun refresh(motherArray: Array<E>, filter: BasicPredicate<E>) {
        mArrayList.clear()
        for (item in motherArray) {
            if (filter.test(item)) {
                mArrayList.add(item)
            }
        }
        mArrayList.trimToSize()
    }

    override val size: Int get() = mArrayList.size

    override fun iterator(): MutableIterator<E> = mArrayList.iterator()

    override fun remove(element: E): Boolean = mArrayList.remove(element)

    override fun removeAll(elements: Collection<E>): Boolean = mArrayList.removeAll(elements)

    override fun retainAll(elements: Collection<E>): Boolean = mArrayList.retainAll(elements)

    override fun clear() = mArrayList.clear()

    override fun get(index: Int): E = mArrayList[index]

    override fun removeAt(index: Int): E = mArrayList.removeAt(index)

    override fun listIterator(): MutableListIterator<E> = mArrayList.listIterator()

    override fun listIterator(index: Int): MutableListIterator<E> = mArrayList.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = mArrayList.subList(fromIndex, toIndex)

    @Deprecated("Not supported", ReplaceWith(""))
    override fun add(element: E): Boolean = throw UnsupportedOperationException()

    @Deprecated("Not supported", ReplaceWith(""))
    override fun add(index: Int, element: E) = throw UnsupportedOperationException()

    @Deprecated("Not supported", ReplaceWith(""))
    override fun addAll(index: Int, elements: Collection<E>): Boolean = throw UnsupportedOperationException()

    @Deprecated("Not supported", ReplaceWith(""))
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()

    @Deprecated("Not supported", ReplaceWith(""))
    override fun set(index: Int, element: E): E = throw UnsupportedOperationException()

    interface BasicPredicate<E> {
        fun test(item: E): Boolean
    }
}
