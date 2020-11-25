package de.ash.financehelper.util

import de.ash.financehelper.constants.Periodicity
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TimeSpan() : Comparable<LocalDate>
{
    /**
     * Used for updating the [fromDate] and [toDate] in case this [TimeSpan] is dynamic.
     */
    var periodicity: Periodicity = Periodicity.ALL_TIME
        private set

    /**
     * Start date of this [TimeSpan]. An error is thrown if [fromDate] is set to a
     * date later than [toDate].
     */
    var fromDate: LocalDate = LocalDate.now()
        set(value)
        {
            require(field <= toDate) { "from date has to be before to date" }
            field = value
        }

    /**
     * End date of this [TimeSpan]. An error is thrown if [toDate] is set to a
     * date earlier than [fromDate].
     */
    var toDate: LocalDate = LocalDate.now()
        set(value)
        {
            require(field >= fromDate) { "to date has to be after before date" }
            field = value
        }

    /**
     * Dynamically sets the periodicity according to the current date (today) and the
     * given [periodicity].
     */
    constructor(periodicity: Periodicity) : this()
    {
        this.periodicity = periodicity
        update()
    }

    /**
     * Statically sets the [fromDate] and [toDate] of this [TimeSpan] object.
     */
    constructor(from: LocalDate, to: LocalDate) : this()
    {
        fromDate = from
        toDate = to
    }

    /**
     * Updates the [fromDate] and [toDate] according to the [periodicity] that was
     * set in the constructor. By default [periodicity] is set to [Periodicity.ALL_TIME],
     * which will result in this function doing nothing.
     */
    fun update()
    {
        if (this.periodicity == Periodicity.ALL_TIME) return

        fromDate = periodicity.getStartDate()
        toDate = LocalDate.now()
    }

    fun toJson(): JSONObject
    {
        return JSONObject().also {
            if (periodicity == Periodicity.ALL_TIME)
            {
                it.put("is_average", false)
                it.put("from_date", fromDate.toString())
                it.put("to_date", toDate.toString())
            } else
            {
                it.put("is_average", true)
                it.put("periodicity", periodicity.name)
            }
        }
    }

    override fun compareTo(other: LocalDate): Int
    {
        return when
        {
            other < fromDate -> return -1
            other > toDate -> return 1
            else -> 0
        }
    }

    override fun toString(): String
    {
        return if(toDate == LocalDate.MAX) "from $fromDate to today" else "from $fromDate to $toDate"
    }

    operator fun contains(element: LocalDate) = (element in fromDate..toDate)

    companion object
    {
        fun fromJson(data: JSONObject): TimeSpan
        {
            return if (data.getBoolean("is_average"))
            {
                TimeSpan(Periodicity.valueOf(data.getString("periodicity")))
            } else
            {
                TimeSpan(
                    LocalDate.parse(data.getString("from_date")),
                    LocalDate.parse(data.getString("to_date"))
                )
            }
        }
    }
}