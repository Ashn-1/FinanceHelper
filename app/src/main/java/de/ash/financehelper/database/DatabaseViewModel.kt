package de.ash.financehelper.database

import android.app.Application
import androidx.lifecycle.*
import de.ash.financehelper.categories.Category
import de.ash.financehelper.statistics.Statistic
import de.ash.financehelper.util.Budget
import kotlinx.coroutines.launch

/**
 * Use the static [create] to get an instance of this class
 */
class DatabaseViewModel(app: Application) : AndroidViewModel(app)
{
    companion object
    {
        fun create(
            owner: ViewModelStoreOwner,
            dao: ExpenseDao,
            statistics: ArrayList<Statistic>,
            budgets: ArrayList<Budget>,
            categories: ArrayList<Category>
        ): DatabaseViewModel
        {
            return ViewModelProvider(owner).get(DatabaseViewModel::class.java).also {
                it.dao = dao
                it.statisticsLiveData = MutableLiveData(statistics)
                it.budgetsLiveData = MutableLiveData(budgets)
                it.categoriesLiveData = MutableLiveData(categories)
            }
        }
    }

    private lateinit var dao: ExpenseDao

    val allExpenses: LiveData<List<Expense>> by lazy { dao.getAll() }

    lateinit var statisticsLiveData: MutableLiveData<ArrayList<Statistic>>
        private set

    lateinit var budgetsLiveData: MutableLiveData<ArrayList<Budget>>
        private set

    lateinit var categoriesLiveData: MutableLiveData<ArrayList<Category>>
        private set

    fun insert(expense: Expense) = viewModelScope.launch {
        dao.insert(expense)
    }

    fun delete(expense: Expense) = viewModelScope.launch {
        dao.delete(expense)
    }

    fun update(expense: Expense) = viewModelScope.launch {
        dao.update(expense)
    }

    fun getDao() = dao
}