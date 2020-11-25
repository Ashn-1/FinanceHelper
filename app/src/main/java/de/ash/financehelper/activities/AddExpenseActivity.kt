package de.ash.financehelper.activities

import android.icu.util.Currency
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isEmpty
import de.ash.financehelper.DiscardWarningDialog
import de.ash.financehelper.R
import de.ash.financehelper.categories.Category
import de.ash.financehelper.categories.PrimaryCategory
import de.ash.financehelper.categories.SecondaryCategory
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.database.Expense
import de.ash.financehelper.util.Data
import de.ash.financehelper.util.EditDate
import de.ash.financehelper.util.format
import kotlinx.android.synthetic.main.activity_add_expense.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddExpenseActivity : AppCompatActivity(), CoroutineScope by MainScope()
{
    private lateinit var chosenDate: EditDate

    private var isEdit: Boolean = false
    private var entryId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        if (intent.hasExtra(KeyStrings.IS_EDIT)) isEdit = true

        entryId = if (isEdit)
        {
            require(intent.hasExtra(KeyStrings.ID)) { "editing expense, but no id was provided" }
            intent.getIntExtra(KeyStrings.ID, 0)
        } else 0

        // # Set the toolbar
        tb_add_expense.setTitle(R.string.add_expense)
        setSupportActionBar(tb_add_expense)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // # Configuration of the currency spinner
        val currencyList = Currency.getAvailableCurrencies().toMutableList()
        currencyList.sortBy { it.currencyCode }

        val currencySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyList)
        currencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s_expenses_currency.adapter = currencySpinnerAdapter
        s_expenses_currency.setSelection(currencySpinnerAdapter.getPosition(Data.instance.currentCurrency))

        // # Configuration of the expense category spinner
        // Secondary Category Spinner
        val secondaryList = mutableListOf<Category>()
        val spinnerAdapterSecondaryCategory = ArrayAdapter(this, android.R.layout.simple_spinner_item, secondaryList)
        spinnerAdapterSecondaryCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s_expenses_category_secondary.adapter = spinnerAdapterSecondaryCategory

        // Primary Category Spinner
        val spinnerAdapterPrimaryCategory =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, Data.instance.primaryCategories)
        spinnerAdapterPrimaryCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s_expenses_category_primary.adapter = spinnerAdapterPrimaryCategory
        s_expenses_category_primary.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
            {
                val primary = s_expenses_category_primary.getItemAtPosition(position) as PrimaryCategory
                secondaryList.clear()
                secondaryList.add(primary)
                secondaryList.addAll(primary.secondaries)
                spinnerAdapterSecondaryCategory.notifyDataSetChanged()
                //s_expenses_category_secondary.setSelection(spinnerAdapterSecondaryCategory.getPosition(primary))
            }
        }

        // # Initialize the expense date picker
        chosenDate = EditDate(e_expense_date)
        e_expense_date.setOnClickListener(chosenDate.OnClickDatePicker(this))

        // # Check if this should add a new expense or just edit an existing entry
        if (isEdit)
        {
            // edit an entry (has to load entry from database -> open new co-routine for this)
            launch {
                val originalEntry: Expense = Data.instance.databaseViewModel.getDao().get(entryId)
                s_expenses_currency.setSelection(currencySpinnerAdapter.getPosition(Currency.getInstance(originalEntry.currency)))
                chosenDate.date = originalEntry.date
                e_expense_date.setText(chosenDate.date.toString())
                when (val category: Category = originalEntry.getCategory())
                {
                    is PrimaryCategory -> s_expenses_category_primary.setSelection(
                        spinnerAdapterPrimaryCategory.getPosition(category)
                    )
                    is SecondaryCategory ->
                    {
                        val primaryPosition = spinnerAdapterPrimaryCategory.getPosition(category.parent)
                        s_expenses_category_primary.setSelection(primaryPosition)

                        secondaryList.clear()
                        secondaryList.add(category.parent)
                        secondaryList.addAll(category.parent.secondaries)
                        spinnerAdapterSecondaryCategory.notifyDataSetChanged()

                        s_expenses_category_secondary.setSelection(spinnerAdapterSecondaryCategory.getPosition(category))
                    }
                    else -> throw IllegalArgumentException("category is neither primary nor secondary")
                }
                e_expense_amount.setText(originalEntry.expense.format((s_expenses_currency.selectedItem as Currency).defaultFractionDigits))
            }
        }

        // # Today Button functionality
        b_today.setOnClickListener {
            chosenDate.date = LocalDate.now()
            e_expense_date.setText(chosenDate.date.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        menuInflater.inflate(R.menu.menu_add, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId)
    {
        R.id.action_done ->
        {
            // # Check if necessary fields are filled in
            if (e_expense_amount.text.isEmpty())
            {
                Toast.makeText(this, R.string.expense_field_empty_warning, Toast.LENGTH_LONG).show()
            } else
            {
                // # Save the instance from this activity
                val expense = buildExpense()

                Data.instance.currentCurrency = s_expenses_currency.selectedItem as Currency

                if (isEdit)
                {
                    expense.id = entryId
                    Data.instance.databaseViewModel.update(expense)
                } else
                {
                    Data.instance.databaseViewModel.insert(expense)
                }

                finish()
            }
            true
        }

        android.R.id.home ->
        { // Up button
            onBackPressed()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed()
    {
        // Show warning that instance will be discarded
        DiscardWarningDialog(this).show()
    }

    private fun buildExpense(): Expense
    {
        val categoryId: Int =
            if (s_expenses_category_primary.isEmpty()) Category.NULL_CATEGORY_ID
            else (s_expenses_category_secondary.selectedItem as Category).id

        return Expense(
            if (e_expense_amount.text.isEmpty()) 0f else e_expense_amount.text.toString().toFloat(),
            chosenDate.date,
            categoryId,
            (s_expenses_currency.selectedItem as Currency).currencyCode
        )
    }
}
