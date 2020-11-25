package de.ash.financehelper.activities

import android.icu.util.Currency
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isEmpty
import de.ash.financehelper.DiscardWarningDialog
import de.ash.financehelper.R
import de.ash.financehelper.categories.Category
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.constants.Periodicity
import de.ash.financehelper.util.*
import kotlinx.android.synthetic.main.activity_add_budget.*
import kotlinx.android.synthetic.main.activity_add_expense.*
import kotlinx.android.synthetic.main.category_add_alert_dialog.*
import java.time.LocalDate

class AddBudgetActivity : AppCompatActivity()
{
    private lateinit var startDate: EditDate
    private lateinit var endDate: EditDate

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_budget)

        // # Set the toolbar
        tb_add_budget.setTitle(R.string.add_budget)
        setSupportActionBar(tb_add_budget)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // # Init category spinner
        val categoryAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, Data.instance.allCategories).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                s_budget_category.adapter = it
            }

        // # Init periodicity spinner
        val periodicityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Periodicity.values()).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            s_budget_periodicity.adapter = it
        }

        // # Init currency spinner
        val currencyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Currency.getAvailableCurrencies().toMutableList()
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            s_budget_currency.adapter = it
            s_budget_currency.setSelection(it.getPosition(Data.instance.currentCurrency))
        }

        // # Init start and end date text field and the end date enable checkbox
        startDate = EditDate(et_budget_start_date)
        endDate = EditDate(et_budget_end_date)
        et_budget_start_date.setOnClickListener(startDate.OnClickDatePicker(this))
        et_budget_end_date.setOnClickListener(endDate.OnClickDatePicker(this))

        // # Adjust values in case this activity is editing an existing budget
        if (intent.hasExtra(KeyStrings.ID))
        {
            val id = intent.getIntExtra(KeyStrings.ID, 0)
            val budget = Data.instance.getBudget(id)

            et_budget_limit.setText(budget.limit.format(budget.currency.defaultFractionDigits))
            s_budget_category.setSelection(categoryAdapter.getPosition(budget.category))
            s_budget_periodicity.setSelection(periodicityAdapter.getPosition(budget.periodicity))
            s_budget_currency.setSelection(currencyAdapter.getPosition(budget.currency))
            startDate.date = budget.timeSpan.fromDate
            cb_budget_end_date.isChecked = budget.timeSpan.toDate != LocalDate.MAX
            if (cb_budget_end_date.isChecked) endDate.date = budget.timeSpan.toDate
        }

        // # Enable/disable views that are not needed
        et_budget_end_date.enableView(cb_budget_end_date.isChecked)
        cb_budget_end_date.setOnCheckedChangeListener { _, isChecked ->
            et_budget_end_date.enableView(isChecked)
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
            if (et_budget_limit.text.isEmpty())
            {
                Toast.makeText(this, R.string.empty_limit, Toast.LENGTH_SHORT).show()
            } else // create budget
            {
                val builder = Budget.BudgetBuilder()
                builder.limit = et_budget_limit.text.toString().toFloat()
                builder.category =
                    if (s_budget_category.isEmpty()) Category.nullCategory
                    else (s_budget_category.selectedItem as Category)
                builder.currency = Currency.getInstance(s_budget_currency.selectedItem.toString())
                builder.periodcity = s_budget_periodicity.selectedItem as Periodicity

                val end: LocalDate = if (cb_budget_end_date.isChecked) endDate.date else LocalDate.MAX
                builder.timeSpan = TimeSpan(startDate.date, end)

                if (intent.hasExtra(KeyStrings.ID)) builder.id = intent.getIntExtra(KeyStrings.ID, 0)

                val budget: Budget = builder.build()
                val expenses = Data.instance.databaseViewModel.allExpenses.value
                if (expenses != null) budget.updateBudget(expenses)

                if (intent.hasExtra(KeyStrings.ID)) Data.instance.editBudget(budget)
                else Data.instance.addBudget(budget)

                finish()
            }
            true
        }
        android.R.id.home ->
        {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed()
    {
        DiscardWarningDialog(this).show()
    }
}