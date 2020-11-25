package de.ash.financehelper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import de.ash.financehelper.database.Expense

class ExpenseListAdapter(private val inflater: LayoutInflater, private val items: List<Expense>) : BaseAdapter()
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        val view = convertView
                   ?: inflater.inflate(R.layout.expense_element_listadapter, parent, false)
        val item = getItem(position)

        // only show the date if it is different from the previous element in the list
        if (position == 0 || getItem(position - 1).date != item.date)
        {
            view.findViewById<TextView>(R.id.tv_expense_date).text = item.date.toString()
        } else
        {
            view.findViewById<TextView>(R.id.tv_expense_date).text = "-"
        }

        view.findViewById<TextView>(R.id.tv_expense_name).text = item.getMoneyString()
        view.findViewById<TextView>(R.id.tv_expense_descriptor).text = item.getCategory().name

        return view
    }

    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getCount() = items.size
}