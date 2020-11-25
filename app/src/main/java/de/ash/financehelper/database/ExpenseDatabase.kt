package de.ash.financehelper.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Expense::class], version = 5)
abstract class ExpenseDatabase : RoomDatabase()
{
    abstract fun expenseDao(): ExpenseDao
}