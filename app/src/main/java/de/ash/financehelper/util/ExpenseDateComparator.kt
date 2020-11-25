package de.ash.financehelper.util

import de.ash.financehelper.database.Expense

class ExpenseDateComparator : Comparator<Expense>
{
    override fun compare(o1: Expense?, o2: Expense?): Int
    {
        return when
        {
            o1 == o2 -> 0
            o1 == null -> -1
            o2 == null -> 1
            o1.date == o2.date -> 0
            o1.date < o2.date -> -1
            else -> 1 // o2.date > o1.date
        }
    }
}