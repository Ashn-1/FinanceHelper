package de.ash.financehelper.statistics

import android.icu.util.Currency
import android.util.Log
import de.ash.financehelper.categories.Category
import de.ash.financehelper.categories.PrimaryCategory
import de.ash.financehelper.categories.SecondaryCategory
import de.ash.financehelper.database.Expense
import de.ash.financehelper.constants.Periodicity
import de.ash.financehelper.util.Data
import de.ash.financehelper.util.JSONable
import de.ash.financehelper.util.TimeSpan
import de.ash.financehelper.util.format
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Statistic(
    val name: String,
    val timeSpan: TimeSpan,
    val periodicity: Periodicity,
    val categories: MutableSet<Category>,
    val currency: Currency,
    val id: Int = Data.instance.generateId()
) : JSONable
{
    private var result = 0f

    /**
     * Calculates the result of the statistic.
     *
     * @param allExpenses a list with all the available expenses
     */
    fun updateResult(allExpenses: List<Expense>)
    {
        timeSpan.update()

        result = allExpenses
                     .filter {
                         // expense is in the statistic time span
                         (it.date in timeSpan)
                         // expense is in the right currency
                         && it.currency == currency.currencyCode
                         // expense has the right category
                         && if (categories.isEmpty()) true else categories.contains(it.getCategory())
                     }
                     .sumByDouble { it.expense.toDouble() }
                     .toFloat() / getDivisor(allExpenses)
    }

    /**
     * This method should be called if a new secondary category was added to the app.
     * In case the parent category is already included in the statistic, the new secondary has to be included as well.
     *
     * @param newCategory the newly added secondary category
     */
    fun categoryAdded(newCategory: SecondaryCategory)
    {
        if (categories.contains(newCategory.parent)) categories.add(newCategory)
    }

    /**
     * This method should be called if an existing category is deleted from the app. If the category was included in
     * this statistic, it is removed. Also if the removed category was a primary category, all its secondary categories
     * are removed as well.
     *
     * @param category the deleted category
     */
    fun categoryRemoved(category: Category)
    {
        if (category is PrimaryCategory)
        {
            categories.removeAll(category.secondaries)
            categories.remove(category)
        } else if (category is SecondaryCategory)
        {
            categories.remove(category)
        }
    }

    /**
     * Calculates the number by which to divide the total expenses to get the average depending on the periodicity. In
     * case of a total count 1 is returned.
     *
     * If the to date is in the future then a to date of the latest date of an expense after today is taken.
     *
     * @return the number by which to divide the total expenses
     */
    private fun getDivisor(allExpenses: List<Expense>): Float
    {
        val today = LocalDate.now()
        var toDate: LocalDate = timeSpan.toDate

        if (timeSpan.toDate > today)
        {
            toDate = today
            allExpenses.forEach { if (it.date in timeSpan && it.date > toDate) toDate = it.date }
            if (toDate < today) toDate = today
        }

        // TODO replace by TimeSpan.timeBetween(p)
        return when (periodicity)
        {
            Periodicity.DAILY -> ChronoUnit.DAYS.between(timeSpan.fromDate, toDate).toFloat() + 1f
            Periodicity.WEEKLY -> ChronoUnit.WEEKS.between(timeSpan.fromDate, toDate).toFloat() + 1f
            Periodicity.MONTHLY -> ChronoUnit.MONTHS.between(timeSpan.fromDate, toDate).toFloat() + 1f
            Periodicity.YEARLY -> ChronoUnit.YEARS.between(timeSpan.fromDate, toDate).toFloat() + 1f
            Periodicity.ALL_TIME -> 1f
        }
    }

    override fun toString(): String
    {
        return StringBuilder()
            .append("$name\n")
            .append(if (periodicity == Periodicity.ALL_TIME) "Total\n" else "$periodicity Average\n")
            .append("Time span: $timeSpan\n")
            .append("From categories: %s\n".format(if (categories.isEmpty()) "all" else categories.toString()))
            .append("${currency.symbol} ${result.format(currency.defaultFractionDigits)}\n")

            .toString()
    }

    override fun toJson(): JSONObject
    {
        val data = JSONObject()
        data.put(ID, id)
        data.put(NAME, name)
        data.put(TIMESPAN, timeSpan.toJson())
        data.put(PERIODICITY, periodicity.name)
        data.put(CURRENCY, currency.currencyCode)

        val categoryJsonArray = JSONArray()
        categories.forEach { categoryJsonArray.put(it.id) }
        data.put(CATEGORIES, categoryJsonArray)

        return data
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Statistic

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id

    companion object
    {
        const val ID = "id"
        const val NAME = "name"
        const val TIMESPAN = "timespan"
        const val PERIODICITY = "periodicity"
        const val CURRENCY = "currency"
        const val CATEGORIES = "categories"

        fun fromJson(data: JSONObject): Statistic
        {
            val builder = StatisticBuilder()

            builder.id = data.getInt(ID)
            builder.name = data.getString(NAME)
            builder.timeSpan = TimeSpan.fromJson(data.getJSONObject(TIMESPAN))
            builder.periodicity = Periodicity.valueOf(data.getString(PERIODICITY))
            builder.currency = Currency.getInstance(data.getString(CURRENCY))

            val categoryJsonArray = data.getJSONArray(CATEGORIES)
            if (categoryJsonArray.length() != 0)
            {
                for (i in 0 until categoryJsonArray.length())
                {
                    builder.categories.add(Data.instance.getCategoryFromId(categoryJsonArray.getInt(i)))
                }
            }

            return builder.build()
        }
    }

    class StatisticBuilder
    {
        var name: String? = null
        var timeSpan: TimeSpan? = null
        var periodicity: Periodicity? = null
        var categories = mutableSetOf<Category>()
        var currency: Currency? = null
        var id: Int? = null

        fun build(): Statistic
        {
            if (id == null)
            {
                return Statistic(
                    name ?: throw NullPointerException("statistic builder: name is null"),
                    timeSpan ?: throw NullPointerException("statistic builder: timespan is null"),
                    periodicity ?: throw NullPointerException("statistic builder: periodicity is null"),
                    categories,
                    currency ?: throw NullPointerException("statistic builder: currency is null")
                )
            } else
            {
                return Statistic(
                    name ?: throw NullPointerException("statistic builder: name is null"),
                    timeSpan ?: throw NullPointerException("statistic builder: timespan is null"),
                    periodicity ?: throw NullPointerException("statistic builder: periodicity is null"),
                    categories,
                    currency ?: throw NullPointerException("statistic builder: currency is null"),
                    id ?: throw NullPointerException("statistic builder: id is null")
                )
            }
        }
    }
}