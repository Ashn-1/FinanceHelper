package de.ash.financehelper.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.icu.util.Currency
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import de.ash.financehelper.activities.MainActivity
import de.ash.financehelper.categories.Category
import de.ash.financehelper.categories.CategoryComparator
import de.ash.financehelper.categories.PrimaryCategory
import de.ash.financehelper.categories.SecondaryCategory
import de.ash.financehelper.constants.FileNames
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.constants.Periodicity
import de.ash.financehelper.database.DatabaseViewModel
import de.ash.financehelper.database.Expense
import de.ash.financehelper.database.ExpenseDatabase
import de.ash.financehelper.database.Migrations
import de.ash.financehelper.statistics.Statistic
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList

class Data private constructor(
    activity: AppCompatActivity,
    private var preferences: SharedPreferences,
    databaseName: String
)
{
    companion object
    {
        lateinit var instance: Data
            private set

        fun init(
            activity: AppCompatActivity,
            preferences: SharedPreferences,
            databaseName: String
        )
        {
            instance = Data(activity, preferences, databaseName)
            instance.loadBudgets()
            instance.loadStatistics()
        }
    }

    var databaseViewModel: DatabaseViewModel private set

    var currentCurrency: Currency = Currency.getInstance(Locale.getDefault())
        set(value)
        {
            with(preferences.edit())
            {
                putString(KeyStrings.CURRENCY, value.currencyCode)
                apply()
            }
            field = value
        }

    private val primaryCategoriesInternal = ArrayList<PrimaryCategory>() // mutable list for internal use
    val primaryCategories: List<PrimaryCategory> = primaryCategoriesInternal // immutable list for external use

    private val allCategoriesInternal = ArrayList<Category>() // mutable list for internal use
    val allCategories: List<Category> = allCategoriesInternal // immutable list for external use

    private var database: ExpenseDatabase

    private val statistics = ArrayList<Statistic>()

    private val budgets = ArrayList<Budget>()

    private var idGenerator: Int = Int.MIN_VALUE + 1

    init
    {
        // # Initialize database
        val migrations = Migrations(allCategoriesInternal)
        database = Room
            .databaseBuilder(
                activity, ExpenseDatabase::
                class.java, databaseName
            )
            .addMigrations(migrations.from2To3, migrations.from3to4, migrations.from4to5)
            .build()

        // # load the current id for id generation
        idGenerator = preferences.getInt(KeyStrings.ID_GENERATOR, Int.MIN_VALUE)

        // # Load current currency
        currentCurrency = Currency.getInstance(
            preferences.getString(
                KeyStrings.CURRENCY,
                Currency.getInstance(Locale.getDefault()).currencyCode
            )
        )

        // # Migrate old category system to new one
        if (!preferences.getBoolean(KeyStrings.MIGRATED_TO_NEW_CATEGORY_SYSTEM, false))
        { // not migrated yet
            // Load the string categories from the shared preferences
            val categoriesSet = preferences.getStringSet(KeyStrings.AVAILABLE_CATEGORIES, hashSetOf())
                                ?: throw Resources.NotFoundException("no category set saved in shared preferences")
            // Create a primary category out of each of the old categories
            categoriesSet.forEach { addCategory(PrimaryCategory(generateId(), it)) }
            saveCategories()
            // Add a flag so this migration is not done again when loading the app
            with(preferences.edit())
            {
                putBoolean(KeyStrings.MIGRATED_TO_NEW_CATEGORY_SYSTEM, true)
                apply()
            }
        }

        // # Load the available categories
        loadCategories()

        // # Initialize the view model
        databaseViewModel = DatabaseViewModel.create(activity, database.expenseDao(), statistics, budgets, allCategoriesInternal)
    }

    /**
     * Properly adds the new category to the category system, sorts the categories and saves them to the
     * categories file. Also updates statistics with the new category.
     * The new category has to have a unique ID and a unique name.
     *
     * @exception IllegalArgumentException if there already exists a category with the id of the given category
     * @return true if the category could be added, false if the name is not unique
     */
    fun addCategory(category: Category): Boolean
    {
        // the id and the name of the new category has to be unique
        allCategoriesInternal.forEach {
            require(it.id != category.id) { "category id has to be unique: ${it.id}" }
            if (it.name == category.name) return false
        }

        allCategoriesInternal.add(category)
        if (category is PrimaryCategory) primaryCategoriesInternal.add(category)
        if (category is SecondaryCategory)
        {
            category.parent.secondaries.add(category)
            statistics.forEach { it.categoryAdded(category) }
        }

        sortCategories()
        saveCategories()

        databaseViewModel.categoriesLiveData.value = allCategoriesInternal

        return true
    }

    /**
     * Removes the given [category] from the category list. If it is a [PrimaryCategory], the category itself and all its
     * secondaries are removed. Also saves the categories to the category file.
     *
     * Also updates all database entries that currently use this category. For entries that use [SecondaryCategory], it
     * changes them to their parent category. If a [PrimaryCategory] is removed, the
     */
    fun removeCategory(category: Category)
    {
        allCategoriesInternal.remove(category)

        // if a primary category is removed, remove it and all its secondary categories
        if (category is PrimaryCategory)
        {
            primaryCategoriesInternal.remove(category)
            allCategoriesInternal.removeAll(category.secondaries)
        }

        // if a secondary is removed, remove it also from the secondaries list of its parent category
        if (category is SecondaryCategory)
        {
            category.parent.secondaries.remove(category)
        }

        // update all expense entries that use this primary category
        val updatedExpenses = mutableListOf<Expense>()
        databaseViewModel.allExpenses.value?.forEach {
            if (it.categoryId == category.id)
            {
                // create an updated version of the expense depending on the type of category
                val newCategoryId: Int = when (category)
                {
                    is PrimaryCategory -> Category.NULL_CATEGORY_ID
                    is SecondaryCategory -> category.parent.id
                    else -> throw RuntimeException("category is neither primary nor secondary")
                }

                val expense = Expense(it.expense, it.expenseDate, newCategoryId, it.currency)
                expense.id = it.id
                updatedExpenses.add(expense)
            }
        }
        updatedExpenses.forEach { databaseViewModel.update(it) }

        // Update statistics that use this category
        statistics.forEach { it.categoryRemoved(category) }

        budgets.forEach { it.categoryRemoved(category) }

        saveCategories()
        saveStatistics()

        databaseViewModel.categoriesLiveData.value = allCategoriesInternal
    }

    /**
     * Sorts and then saves all the categories.
     */
    fun updateCategories()
    {
        sortCategories()
        saveCategories()
    }

    /**
     * Updates a secondary category in case its parent category was changed.
     *
     * @param sec the secondary category to update
     * @param newParent the new parent category of the given secondary category
     */
    fun updateSecondaryCategory(sec: SecondaryCategory, newParent: PrimaryCategory)
    {
        sec.parent.secondaries.remove(sec)
        sec.parent = newParent
        sec.parent.secondaries.add(sec)

        sortCategories()
        saveCategories()

        databaseViewModel.categoriesLiveData.value = allCategoriesInternal
    }

    /**
     * Adds a statistic to the statistic list. The id of the new statistic has to be unique. Also updates the statistic
     * list live data. Lastly it saves the statistic list.
     *
     * @param statistic the statistic to add
     * @throws IllegalArgumentException if the id of the statistic is not unique
     */
    fun addStatistic(statistic: Statistic)
    {
        statistics.forEach { require(it.id != statistic.id) { "statistic with id ${statistic.id} already exists" } }
        statistics.add(statistic)
        databaseViewModel.statisticsLiveData.value = statistics
        saveStatistics()
    }

    /**
     * Edits an existing statistic by replacing it with a new version. Also updatest the statistic list live data.
     * Lastly it saves the statistic list.
     *
     * @param statistic the statistic that was edited
     * @throws IllegalArgumentException if the statistic does not already exist
     */
    fun editStatistic(statistic: Statistic)
    {
        require(statistics.contains(statistic)) { "statistic could not be edited because it does not exist" }
        statistics[statistics.indexOf(statistic)] = statistic
        databaseViewModel.statisticsLiveData.value = statistics
        saveStatistics()
    }

    /*
    fun removeStatistics(list: List<Statistic>)
    {
        statistics.removeAll(list)
        databaseViewModel.statisticsLiveData.value = statistics
        saveStatistics()
    }
    */
    fun removeStatistic(element: Statistic)
    {
        statistics.remove(element)
        databaseViewModel.statisticsLiveData.value = statistics
        saveStatistics()
    }

    fun addBudget(budget: Budget)
    {
        budgets.forEach { require(it.id != budget.id) { "budget with id ${budget.id} already exists" } }
        budgets.add(budget)
        databaseViewModel.budgetsLiveData.value = budgets
        saveBudgets()
    }

    fun editBudget(budget: Budget)
    {
        require(budgets.contains(budget)) { "budget could not be edited because it does not exist" }
        budgets[budgets.indexOf(budget)] = budget
        databaseViewModel.budgetsLiveData.value = budgets
        saveBudgets()
    }

    fun removeBudget(element: Budget)
    {
        budgets.remove(element)
        databaseViewModel.budgetsLiveData.value = budgets
        saveBudgets()
    }

    /**
     * Returns the budget with the given id.
     */
    fun getBudget(id: Int) = budgets.first { it.id == id }

    /**
     * Returns the statistic with the given id.
     */
    fun getStatistic(id: Int) = statistics.first { it.id == id }

    /**
     * Returns the category with the given id.
     */
    fun getCategoryFromId(id: Int): Category
    {
        try
        {
            if (id == Category.NULL_CATEGORY_ID) return Category.nullCategory
            return allCategoriesInternal.first { it.id == id }
        } catch (e: NoSuchElementException)
        {
            Log.e("category", "No match for given category id $id\n${allCategories}")

            throw e
        }
    }

    private fun saveJson(data: List<JSONable>, key: String, filename: String)
    {
        val jsonArray = JSONArray()
        data.forEach { jsonArray.put(it.toJson()) }

        val jsonFile = JSONObject().put(key, jsonArray)
        MainActivity.getContext().openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(jsonFile.toString().toByteArray())
        }
    }

    private fun loadJson(data: MutableList<JSONable>, key: String, filename: String, fromJson: (JSONObject) -> JSONable)
    {
        val file = File(MainActivity.getContext().filesDir, filename)

        if (!file.exists()) return

        FileInputStream(file).bufferedReader().use {
            val jsonArray = JSONObject(it.readText()).getJSONArray(key)

            if (jsonArray.length() != 0)
            {
                for (i in 0 until jsonArray.length())
                {
                    data.add(fromJson(jsonArray.getJSONObject(i)))
                }
            }
        }
    }

    private fun saveStatistics()
    {
        saveJson(statistics, "statistics", FileNames.FILE_STATISTICS)
        return

        /*
        val statisticsJsonArray = JSONArray()
        statistics.forEach { statisticsJsonArray.put(it.toJson()) }

        val jsonFile = JSONObject().put("statistics", statisticsJsonArray)
        MainActivity.getContext().openFileOutput(FileNames.FILE_STATISTICS, Context.MODE_PRIVATE).use {
            it.write(jsonFile.toString().toByteArray())
        }*/
    }

    private fun loadStatistics()
    {
        val tmpList = mutableListOf<JSONable>()
        loadJson(tmpList, "statistics", FileNames.FILE_STATISTICS, ::statisticFromJson)
        tmpList.forEach { if (it is Statistic) statistics.add(it) }

        /*
        val statisticsFile = File(MainActivity.getContext().filesDir, FileNames.FILE_STATISTICS)

        if (!statisticsFile.exists()) return

        FileInputStream(statisticsFile).bufferedReader().use {
            val statisticsArray = JSONObject(it.readText()).getJSONArray("statistics")

            if (statisticsArray.length() != 0)
            {
                for (i in 0 until statisticsArray.length())
                {
                    statistics.add(Statistic.fromJson(statisticsArray.getJSONObject(i)))
                }
            }
        }*/
    }

    private fun saveBudgets()
    {
        saveJson(budgets, "budgets", FileNames.FILE_BUDGETS)
    }

    private fun loadBudgets()
    {
        val tmpList = mutableListOf<JSONable>()
        loadJson(tmpList, "budgets", FileNames.FILE_BUDGETS, ::budgetFromJson)
        tmpList.forEach { if (it is Budget) budgets.add(it) }
    }

    private fun saveCategories()
    {
        saveJson(primaryCategoriesInternal, "categories", FileNames.FILE_CATEGORIES)

        /*
        val categoryJsonArray = JSONArray()
        primaryCategoriesInternal.forEach { categoryJsonArray.put(it.toJson()) }

        val jsonFile = JSONObject().put("categories", categoryJsonArray)
        MainActivity.getContext().openFileOutput(FileNames.FILE_CATEGORIES, Context.MODE_PRIVATE).use {
            it.write(jsonFile.toString().toByteArray())
        }
         */
    }

    private fun loadCategories()
    {
        /*
        val categoryFile = File(MainActivity.getContext().filesDir, FileNames.FILE_CATEGORIES)

        if (!categoryFile.exists()) return

        FileInputStream(categoryFile).bufferedReader().use {
            val categoriesArray = JSONObject(it.readText()).getJSONArray("categories")

            if (categoriesArray.length() != 0)
            {
                for (i in 0 until categoriesArray.length())
                {
                    primaryCategoriesInternal.add(PrimaryCategory.fromJson(categoriesArray.getJSONObject(i)))
                }
            }
        }
         */

        val tmpList = mutableListOf<JSONable>()
        loadJson(tmpList, "categories", FileNames.FILE_CATEGORIES, ::categoryFromJson)
        tmpList.forEach { if (it is PrimaryCategory) primaryCategoriesInternal.add(it) }

        // add all loaded categories to the total category list
        allCategoriesInternal.addAll(primaryCategoriesInternal)
        primaryCategoriesInternal.forEach { allCategoriesInternal.addAll(it.secondaries) }

        sortCategories()
    }

    private fun sortCategories()
    {
        // Primary categories can be sorted by natural sort order
        primaryCategoriesInternal.sort()

        // Secondary categories in each individual primary category can be sorted by natural sort order
        primaryCategoriesInternal.forEach { it.secondaries.sort() }

        // All categories are sorted by primary categories with all their secondary categories following them in
        // their natural sort order
        allCategoriesInternal.sortWith(CategoryComparator())
    }

    fun generateId(): Int
    {
        val id = idGenerator++

        with(preferences.edit())
        {
            putInt(KeyStrings.ID_GENERATOR, idGenerator)
            apply()
        }

        return id
    }

    private fun statisticFromJson(data: JSONObject): Statistic
    {
        val builder = Statistic.StatisticBuilder()

        builder.id = data.getInt(Statistic.ID)
        builder.name = data.getString(Statistic.NAME)
        builder.timeSpan = TimeSpan.fromJson(data.getJSONObject(Statistic.TIMESPAN))
        builder.periodicity = Periodicity.valueOf(data.getString(Statistic.PERIODICITY))
        builder.currency = Currency.getInstance(data.getString(Statistic.CURRENCY))

        val categoryJsonArray = data.getJSONArray(Statistic.CATEGORIES)
        if (categoryJsonArray.length() != 0)
        {
            for (i in 0 until categoryJsonArray.length())
            {
                builder.categories.add(Data.instance.getCategoryFromId(categoryJsonArray.getInt(i)))
            }
        }

        return builder.build()
    }

    private fun categoryFromJson(data: JSONObject): PrimaryCategory
    {
        val id = data.getInt("id")
        val name = data.getString("name")
        val secondariesArray = data.getJSONArray("sec_categories")
        val primary = PrimaryCategory(id, name)

        if (secondariesArray.length() != 0)
        {
            for (i in 0 until secondariesArray.length())
            {
                primary.secondaries.add(
                    SecondaryCategory.fromJson(
                        secondariesArray.getJSONObject(i),
                        primary
                    )
                )
            }
        }

        return primary
    }

    private fun budgetFromJson(data: JSONObject): Budget
    {
        val builder = Budget.BudgetBuilder()

        builder.id = data.getInt(Budget.ID)
        builder.category = Data.instance.getCategoryFromId(data.getInt(Budget.CATEGORY))
        builder.currency = Currency.getInstance(data.getString(Budget.CURRENCY))
        builder.limit = data.getDouble(Budget.LIMIT).toFloat()
        builder.periodcity = Periodicity.valueOf(data.getString(Budget.PERIODICITY))
        builder.timeSpan = TimeSpan.fromJson(data.getJSONObject(Budget.TIMESPAN))

        return builder.build()
    }
}