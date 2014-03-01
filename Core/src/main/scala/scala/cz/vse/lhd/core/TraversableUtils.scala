package cz.vse.lhd.core

object TraversableUtils {
  
  def lazyFilterFolding[T, U](trv : TraversableOnce[T])(x : U)(y : T => U) = {
    new Iterator[T] {
      private val it = trv.toIterator
      private var curr: U = x
      private var currIt: T = _
      def hasNext: Boolean = {
        if (!it.hasNext)
          false
        else {
          currIt = it.next
          val newCurr = y(currIt)
          if (newCurr == curr)
            hasNext
          else {
            curr = newCurr
            true
          }
        }
      }
      def next = currIt
    }
  }
  
  def lazySortedSeqGroupBy[T, U](trv : TraversableOnce[T])(y : T => U) = {
    new Iterator[List[T]] {
      private val it = trv.toIterator
      private var key : U = _
      private var retList : List[T] = Nil
      private var curList : List[T] = Nil
      private var end = true
      def hasNext: Boolean = {
        if (!it.hasNext) {
          if (end)
            false
          else {
            end = true
            retList = curList
            true
          }
        } else {
          val currIt = it.next
          val newKey = y(currIt)
          if (end) {
            key = newKey
            end = false
          }
          if (newKey == key) {
            curList = currIt :: curList
            hasNext
          } else {
            retList = curList
            curList = List(currIt)
            key = newKey
            true
          }
        }
      }
      def next = retList
    }
  }
  
}