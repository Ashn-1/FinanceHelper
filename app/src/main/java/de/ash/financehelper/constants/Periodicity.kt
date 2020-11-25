package de.ash.financehelper.constants

import de.ash.financehelper.R
import de.ash.financehelper.activities.MainActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class Periodicity(val id: Int)
{
    DAILY(R.string.daily),
    WEEKLY(R.string.weekly),
    MONTHLY(R.string.monthly),
    YEARLY(R.string.yearly),
    ALL_TIME(R.string.total);

    fun getStartDate(): LocalDate
    {
        val today = LocalDate.now()

        return when (this)
        {
            DAILY -> today
            WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            MONTHLY -> today.with(TemporalAdjusters.firstDayOfMonth())
            YEARLY -> today.with(TemporalAdjusters.firstDayOfYear())
            ALL_TIME -> LocalDate.MIN
        }
    }

    override fun toString(): String
    {
        return MainActivity.getContext().getString(id)
    }
}
