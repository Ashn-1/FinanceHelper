package de.ash.financehelper.database

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import de.ash.financehelper.categories.Category

class Migrations(val allCategories: List<Category>)
{
    val from2To3 = object : Migration(2, 3)
    {
        override fun migrate(database: SupportSQLiteDatabase)
        {
            database.execSQL("UPDATE expense SET month = month + 1")
        }
    }

    // Migration from year, month, day to one date integer
    val from3to4 = object : Migration(3, 4)
    {
        override fun migrate(database: SupportSQLiteDatabase)
        {
            database.execSQL("ALTER TABLE expense ADD COLUMN date INTEGER DEFAULT 0")
            database.execSQL("UPDATE expense SET date = (day | (month<<8) | (year<<12))")

            database.execSQL("CREATE TABLE expense_new (id INTEGER PRIMARY KEY NOT NULL, expense FLOAT NOT NULL, date INTEGER NOT NULL, category TEXT NOT NULL, currency TEXT NOT NULL)")
            // Copy the data
            database.execSQL("INSERT INTO expense_new (id, expense, date, category, currency) SELECT id, expense, date, category, currency FROM expense")
            // Remove the old table
            database.execSQL("DROP TABLE expense")
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE expense_new RENAME TO expense")
        }
    }

    val from4to5 = object : Migration(4, 5)
    {
        override fun migrate(database: SupportSQLiteDatabase)
        {
            database.execSQL("ALTER TABLE expense ADD COLUMN categoryId INTEGER NOT NULL DEFAULT -1")

            val entries: Cursor = database.query(SimpleSQLiteQuery("SELECT id, category FROM expense"))
            while (entries.moveToNext())
            {
                val id: Int = entries.getInt(entries.getColumnIndexOrThrow("id"))
                val categoryName: String = entries.getString(entries.getColumnIndexOrThrow("category"))
                val categoryId: Int = allCategories.first { it.name == categoryName }.id

                database.execSQL("UPDATE expense SET categoryId = $categoryId WHERE id = $id")
            }

            database.execSQL("CREATE TABLE expense_new (id INTEGER PRIMARY KEY NOT NULL, expense FLOAT NOT NULL, date INTEGER NOT NULL, category INTEGER NOT NULL, currency TEXT NOT NULL)")
            // Copy the data
            database.execSQL("INSERT INTO expense_new (id, expense, date, category, currency) SELECT id, expense, date, categoryId, currency FROM expense")
            // Remove the old table
            database.execSQL("DROP TABLE expense")
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE expense_new RENAME TO expense")
        }
    }
}