package de.ash.financehelper.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.lifecycle.Observer
import de.ash.financehelper.ExpenseListAdapter
import de.ash.financehelper.R
import de.ash.financehelper.activities.AddExpenseActivity
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.database.Expense
import de.ash.financehelper.util.Data
import de.ash.financehelper.util.ExpenseDateComparator
import java.lang.NullPointerException

class ExpenseFragment : MainScreenFragment(R.id.action_nav_expenses, AddExpenseActivity::class.java)
{
    private var expenses: ArrayList<Expense> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        requireNotNull(context)

        // # inflate the layout of this fragment
        val view = inflater.inflate(R.layout.fragment_expense, container, false)

        // # Initialize the list view of the shown expenses
        val expenseListView = view.findViewById<ListView>(R.id.lv_displayed_expenses)
        expenseListView.adapter = ExpenseListAdapter(inflater, expenses)

        // # Open context menu after long press on item
        registerForContextMenu(expenseListView)

        // # Set observer for changes in the database
        Data.instance.databaseViewModel.allExpenses.observe(viewLifecycleOwner, Observer {
            expenses.clear()
            expenses.addAll(it.sortedWith(ExpenseDateComparator()).reversed())
            (expenseListView.adapter as BaseAdapter).notifyDataSetChanged()
        })

        return view
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?)
    {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.lv_displayed_expenses)
        {
            activity?.menuInflater?.inflate(R.menu.menu_element_long_press, menu)
            ?: throw NullPointerException("activity or menu inflater null")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean
    {
        if (!currentlyActive) return false

        val info: AdapterView.AdapterContextMenuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo

        when (item.itemId)
        {
            R.id.action_edit ->
            {
                Intent(context, AddExpenseActivity::class.java).also {
                    it.putExtra(KeyStrings.IS_EDIT, true)
                    it.putExtra(KeyStrings.ID, expenses[info.position].id)
                    startActivity(it)
                }
            }

            R.id.action_delete ->
            {
                Data.instance.databaseViewModel.delete(expenses[info.position])
            }

            else -> super.onContextItemSelected(item)
        }
        return true
    }
}