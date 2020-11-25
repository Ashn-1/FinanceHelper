package de.ash.financehelper.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.ash.financehelper.util.Data
import de.ash.financehelper.util.format
import de.ash.financehelper.util.toInteger
import java.time.LocalDate
import java.util.*

@Entity
data class Expense(
    @ColumnInfo(name = "expense") val expense: Float,
    @ColumnInfo(name = "date") val expenseDate: Int,
    @ColumnInfo(name = "category") val categoryId: Int,
    @ColumnInfo(name = "currency") val currency: String
)
{
    constructor(expense: Float, date: LocalDate, category: Int, currency: String) : this(
        expense,
        date.toInteger(),
        category,
        currency
    )

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="id")
    var id: Int = 0

    @Ignore
    val date: LocalDate = LocalDate.of(
        (expenseDate and 0x0FFFF000) shr 12,
        (expenseDate and 0x00000F00) shr 8,
        (expenseDate and 0x000000FF)
    )

    fun getMoneyString(): String
    {
        val cur = Currency.getInstance(currency)
        return "${cur.symbol} ${expense.format(cur.defaultFractionDigits)}"
    }

    fun getCategory() = Data.instance.getCategoryFromId(categoryId)
}