package de.ash.financehelper.activities

import android.icu.util.Currency
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.ash.financehelper.CategoryChooserDialog
import de.ash.financehelper.DiscardWarningDialog
import de.ash.financehelper.R
import de.ash.financehelper.statistics.Statistic
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.constants.Periodicity
import de.ash.financehelper.util.Data
import de.ash.financehelper.util.EditDate
import de.ash.financehelper.util.TimeSpan
import de.ash.financehelper.util.enableView
import kotlinx.android.synthetic.main.activity_add_statistic.*

class AddStatisticActivity : AppCompatActivity()
{
    private var statisticId: Int = 0
    private var isEdit: Boolean = false

    private val builder = Statistic.StatisticBuilder()

    private var previousCategorySelection: Int = R.id.rb_all_categories

    private lateinit var fromDate: EditDate
    private lateinit var toDate: EditDate

    private val dynamicTimeSpanNames = mapOf(
        Periodicity.DAILY to R.string.current_day,
        Periodicity.WEEKLY to R.string.current_week,
        Periodicity.MONTHLY to R.string.current_month,
        Periodicity.YEARLY to R.string.current_year
    )

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_statistic)

        // # Set the toolbar
        tb_add_statistic.setTitle(R.string.add_statistic)
        setSupportActionBar(tb_add_statistic)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // # Radio Group total/average selection
        rg_total_average.setOnCheckedChangeListener { _, checkedId ->
            s_average_periodicity.enableView(checkedId == R.id.rb_average)
        }

        // # Radio Group fixed/dynamic date selection
        rg_fixed_dynamic_date.setOnCheckedChangeListener { _, checkedId ->
            et_from_date.enableView(checkedId == R.id.rb_fixed_date)
            et_to_date.enableView(checkedId == R.id.rb_fixed_date)
            s_dynamic_date.enableView(checkedId != R.id.rb_fixed_date)
        }

        // # From/to date picker
        fromDate = EditDate(et_from_date)
        toDate = EditDate(et_to_date)
        et_from_date.setOnClickListener(fromDate.OnClickDatePicker(this))
        et_to_date.setOnClickListener(toDate.OnClickDatePicker(this))

        // # Spinner Periodicity
        val spinnerAveragePeriodicityAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, Periodicity.values()).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        s_average_periodicity.adapter = spinnerAveragePeriodicityAdapter

        // # Spinner Dynamic Date
        val spinnerDynamicDateAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, dynamicTimeSpanNames.keys.toList()).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        s_dynamic_date.adapter = spinnerDynamicDateAdapter

        rb_all_categories.setOnClickListener {
            previousCategorySelection = rb_all_categories.id
        }

        rb_select_categories.setOnClickListener {
            if (previousCategorySelection == R.id.rb_all_categories) builder.categories.clear()
            CategoryChooserDialog(this, builder.categories).also { dialog ->
                dialog.setOnDismissListener {
                    if (dialog.isPositiveDismiss)
                    {
                        rg_statistics_category_selection.check(rb_select_categories.id)
                        previousCategorySelection = rb_select_categories.id
                        builder.categories = dialog.selectedCategories
                    } else
                    {
                        rg_statistics_category_selection.check(previousCategorySelection)
                    }
                }
                dialog.show()
            }
        }

        // # Spinner currency
        val currencyList = Currency.getAvailableCurrencies().toMutableList()
        currencyList.sortBy { it.currencyCode }
        val currencySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyList)
        currencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s_statistics_currency.adapter = currencySpinnerAdapter
        s_statistics_currency.setSelection(currencySpinnerAdapter.getPosition(Data.instance.currentCurrency))

        // # check if a statistic is added or edited and adjust views accordingly
        if (intent.hasExtra(KeyStrings.ID))
        {
            statisticId = intent.getIntExtra(KeyStrings.ID, -1)
            isEdit = true

            val statistic = Data.instance.getStatistic(statisticId)

            et_statistic_name.setText(statistic.name)

            if (statistic.periodicity == Periodicity.ALL_TIME) rg_total_average.check(rb_total.id)
            else
            {
                rg_total_average.check(rb_average.id)
                s_average_periodicity.setSelection(spinnerAveragePeriodicityAdapter.getPosition(statistic.periodicity))
            }

            if (statistic.timeSpan.periodicity == Periodicity.ALL_TIME)
            {
                rg_fixed_dynamic_date.check(rb_fixed_date.id)
                fromDate.date = statistic.timeSpan.fromDate
                toDate.date = statistic.timeSpan.toDate
                et_from_date.setText(fromDate.date.toString())
                et_to_date.setText(toDate.date.toString())
            } else
            {
                rg_fixed_dynamic_date.check(rb_dynamic_date.id)
                s_dynamic_date.setSelection(spinnerDynamicDateAdapter.getPosition(statistic.timeSpan.periodicity))
            }

            if (statistic.categories.isNotEmpty())
            {
                rg_statistics_category_selection.check(rb_select_categories.id)
            }

            s_statistics_currency.setSelection(currencySpinnerAdapter.getPosition(statistic.currency))

            builder.categories = statistic.categories.toMutableSet()
            if (builder.categories.isNotEmpty())
            {
                previousCategorySelection = R.id.rb_select_categories
            }
        }

        s_average_periodicity.enableView(rg_total_average.checkedRadioButtonId == R.id.rb_average)
        et_from_date.enableView(rg_fixed_dynamic_date.checkedRadioButtonId == R.id.rb_fixed_date)
        et_to_date.enableView(rg_fixed_dynamic_date.checkedRadioButtonId == R.id.rb_fixed_date)
        s_dynamic_date.enableView(rg_fixed_dynamic_date.checkedRadioButtonId != R.id.rb_fixed_date)
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
            if (et_statistic_name.text.isEmpty())
            {
                // name text field is empty
                Toast.makeText(this, R.string.statistics_name_empty, Toast.LENGTH_SHORT).show()

            } else if (rg_fixed_dynamic_date.checkedRadioButtonId == rb_fixed_date.id
                       && (et_from_date.text.isEmpty() || et_to_date.text.isEmpty())
            )
            {
                // fixed date selected but from and to date empty
                Toast.makeText(this, R.string.warning_empty_date_for_fixed_date, Toast.LENGTH_SHORT).show()

            } else if (rg_total_average.checkedRadioButtonId == rb_average.id
                       && rg_fixed_dynamic_date.checkedRadioButtonId == rb_dynamic_date.id
                       && (s_dynamic_date.selectedItem as Periodicity) <= (s_average_periodicity.selectedItem as Periodicity)
            )
            {
                Toast.makeText(this, R.string.dynamic_date_less_than_average_periodicity, Toast.LENGTH_LONG).show()
            } else // create statistic
            {
                val timeSpan =
                    if (rg_fixed_dynamic_date.checkedRadioButtonId == rb_dynamic_date.id)
                        TimeSpan(s_dynamic_date.selectedItem as Periodicity)
                    else
                        TimeSpan(fromDate.date, toDate.date)

                val periodicity =
                    if (rg_total_average.checkedRadioButtonId == rb_total.id)
                        Periodicity.ALL_TIME
                    else
                        s_average_periodicity.selectedItem as Periodicity

                builder.timeSpan = timeSpan
                builder.periodicity = periodicity
                builder.name = et_statistic_name.text.toString()
                builder.currency = Currency.getInstance(s_statistics_currency.selectedItem.toString())

                if (isEdit) builder.id = statisticId
                val statistic = builder.build()

                val expenses = Data.instance.databaseViewModel.allExpenses.value
                if (expenses != null) statistic.updateResult(expenses)

                if (isEdit) Data.instance.editStatistic(statistic)
                else Data.instance.addStatistic(statistic)

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
}