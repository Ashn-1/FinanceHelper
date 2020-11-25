package de.ash.financehelper.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.lifecycle.Observer
import de.ash.financehelper.R
import de.ash.financehelper.activities.AddBudgetActivity
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.util.Budget
import de.ash.financehelper.util.BudgetListAdapter
import de.ash.financehelper.util.Data
import java.lang.IllegalArgumentException
import java.lang.NullPointerException

class BudgetFragment : MainScreenFragment(R.id.action_nav_budget, AddBudgetActivity::class.java)
{
    private val budgets: MutableList<Budget> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // # inflate the layout of this fragment
        val view = inflater.inflate(R.layout.fragment_budget, container, false)

        // # Set list adapter
        val budgetList = view.findViewById<ListView>(R.id.lv_displayed_budgets)
        budgetList.adapter = BudgetListAdapter(inflater, budgets)

        // # Open menu after long click on item
        registerForContextMenu(budgetList)

        // # Observe data to adjust budgets if new elements are added
        Data.instance.databaseViewModel.allExpenses.observe(viewLifecycleOwner, Observer { allExpenses ->
            budgets.forEach { it.updateBudget(allExpenses) }
            (budgetList.adapter as BaseAdapter).notifyDataSetChanged()
        })

        Data.instance.databaseViewModel.budgetsLiveData.observe(viewLifecycleOwner, Observer {
            budgets.clear()
            budgets.addAll(it)
            (budgetList.adapter as BaseAdapter).notifyDataSetChanged()
        })

        return view
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?)
    {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.lv_displayed_budgets)
        {
            activity?.menuInflater?.inflate(R.menu.menu_element_long_press, menu)
            ?: throw NullPointerException("activity or menu inflater null")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean
    {
        if(!currentlyActive) return false

        val info: AdapterView.AdapterContextMenuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo

        when (item.itemId)
        {
            R.id.action_edit ->
            {
                Intent(context, AddBudgetActivity::class.java).also {
                    it.putExtra(KeyStrings.ID, budgets[info.position].id)
                    startActivity(it)
                }
            }

            R.id.action_delete ->
            {
                Data.instance.removeBudget(budgets[info.position])
            }

            else -> super.onContextItemSelected(item)
        }
        return true
    }

    override fun onResume()
    {
        super.onResume()
        changedDateCallback()
        (view?.findViewById<ListView>(R.id.lv_displayed_budgets)?.adapter as BaseAdapter).notifyDataSetChanged()
    }

    fun changedDateCallback()
    {
        val expenses = Data.instance.databaseViewModel.allExpenses.value
        if (expenses != null) budgets.forEach { it.updateBudget(expenses) }
    }
}