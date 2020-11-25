package de.ash.financehelper.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExpenseDao
{
    @Insert
    suspend fun insert(entry: Expense)

    @Delete
    suspend fun delete(entry: Expense)

    @Update
    suspend fun update(entry: Expense)

    @Query("SELECT * FROM expense WHERE id LIKE :id")
    suspend fun get(id: Int): Expense

    @Query("SELECT * FROM expense")
    fun getAll(): LiveData<List<Expense>>

    @Query("SELECT date FROM expense")
    fun getAllDates(): LiveData<List<Int>>

    @Query("SELECT * FROM expense WHERE category LIKE :category")
    fun findByCategory(category: String): List<Expense>

    @Query("SELECT * FROM expense WHERE date BETWEEN :first AND :second")
    fun findBetweenDates(first: Int, second: Int): LiveData<List<Expense>>
}