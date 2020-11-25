package de.ash.financehelper

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.ash.financehelper.categories.PrimaryCategory
import de.ash.financehelper.database.Migrations
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest
{
    @get:Rule
    val testHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        de.ash.financehelper.database.ExpenseDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration4to5()
    {
        val dbName = "test_db_4_to_5"
        val db = testHelper.createDatabase(dbName, 4)

        val data = ContentValues().also {
            it.put("id", 10)
            it.put("expense", 100)
            it.put("date", 0b1001101000001011111101010)
            it.put("category", "Food")
            it.put("currency", "EUR")
        }

        db.insert("expense", SQLiteDatabase.CONFLICT_REPLACE, data)
        db.close()

        val migrations = Migrations(listOf(
            PrimaryCategory(12, "Appliance"),
            PrimaryCategory(13, "Food")
        ))
        testHelper.runMigrationsAndValidate(dbName, 5, false, migrations.from4to5)

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            de.ash.financehelper.database.ExpenseDatabase::class.java, dbName
        ).addMigrations(migrations.from2To3, migrations.from3to4, migrations.from4to5).build().apply {
            val migratedDb = openHelper.writableDatabase
            val cursor = migratedDb.query("SELECT id, category FROM expense")
            assertEquals(1, cursor.count)
            cursor.moveToNext()
            val category = cursor.getInt(cursor.getColumnIndexOrThrow("category"))
            assertEquals(13, category)
            close()
        }
    }
}