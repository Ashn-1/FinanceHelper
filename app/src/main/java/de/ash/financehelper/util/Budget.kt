package de.ash.financehelper.util

import android.icu.util.Currency
import android.util.Log
import de.ash.financehelper.categories.Category
import de.ash.financehelper.constants.Periodicity
import de.ash.financehelper.database.Expense
import org.json.JSONObject
import java.lang.NullPointerException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Budget(
    var category: Category,
    val limit: Float,
    val periodicity: Periodicity,
    val currency: Currency,
    val timeSpan: TimeSpan,
    val id: Int = Data.instance.generateId()
) : JSONable
{
    /**
     * The current surplus/deficit from this budget.
     */
    private var result = 0f

    fun updateBudget(allExpenses: List<Expense>)
    {
        val totalExpensesInTimeSpan: Float = allExpenses
            .filter {
                // expense is in the statistic time span
                (it.date in timeSpan)
                // expense is in the right currency
                && it.currency == currency.currencyCode
                // expense has the right category
                && it.categoryId == category.id
            }
            .sumByDouble { it.expense.toDouble() }
            .toFloat()

        val toDate = if (timeSpan.toDate == LocalDate.MAX) LocalDate.now() else timeSpan.toDate

        val timeBetween: Float = when (periodicity)
        {
            Periodicity.DAILY -> ChronoUnit.DAYS.between(timeSpan.fromDate, toDate) + 1
            Periodicity.WEEKLY -> ChronoUnit.WEEKS.between(timeSpan.fromDate, toDate) + 1
            Periodicity.MONTHLY -> ChronoUnit.MONTHS.between(timeSpan.fromDate, toDate) + 1
            Periodicity.YEARLY -> ChronoUnit.YEARS.between(timeSpan.fromDate, toDate) + 1
            Periodicity.ALL_TIME -> 1
        }.toFloat()


        result = (limit * timeBetween) - totalExpensesInTimeSpan
    }

    fun categoryRemoved(category: Category)
    {
        if (this.category == category) this.category = Category.nullCategory
    }

    override fun toJson(): JSONObject
    {
        return JSONObject().apply {
            put(ID, id)
            put(CATEGORY, category.id)
            put(LIMIT, limit.toDouble())
            put(PERIODICITY, periodicity.name)
            put(CURRENCY, currency.currencyCode)
            put(TIMESPAN, timeSpan.toJson())
        }
    }

    override fun toString(): String
    {
        return StringBuilder()
            .append("${category.name} budget\n")
            .append("Limit: ${currency.symbol} $limit\n")
            .append("TimeSpan: $timeSpan\n")
            .append("Periodicity: $periodicity\n")
            .append("Budget Balance: ${currency.symbol} $result\n")

            .toString()
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Budget

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int
    {
        return id
    }

    companion object
    {
        const val ID = "id"
        const val CATEGORY = "category"
        const val LIMIT = "limit"
        const val PERIODICITY = "periodicity"
        const val CURRENCY = "currency"
        const val TIMESPAN = "timespan"
    }

    class BudgetBuilder
    {
        var id: Int? = null
        var category: Category? = null
        var limit: Float? = null
        var periodcity: Periodicity? = null
        var currency: Currency? = null
        var timeSpan: TimeSpan? = null

        fun build(): Budget
        {

            return if (id == null)
            {
                Budget(
                    category ?: throw NullPointerException("budget builder: category null"),
                    limit ?: throw NullPointerException("budget builder: limit null"),
                    periodcity ?: throw NullPointerException("budget builder: periodicity null"),
                    currency ?: throw NullPointerException("budget builder: currency null"),
                    timeSpan ?: throw NullPointerException("budget builder: time span null")
                )
            } else
            {
                Budget(
                    category ?: throw NullPointerException("budget builder: category null"),
                    limit ?: throw NullPointerException("budget builder: limit null"),
                    periodcity ?: throw NullPointerException("budget builder: periodicity null"),
                    currency ?: throw NullPointerException("budget builder: currency null"),
                    timeSpan ?: throw NullPointerException("budget builder: time span null"),
                    id ?: throw NullPointerException("budget builder: id null")
                )
            }
        }
    }
}
