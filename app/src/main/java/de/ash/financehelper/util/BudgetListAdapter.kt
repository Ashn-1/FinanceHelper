package de.ash.financehelper.util

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import de.ash.financehelper.R
import de.ash.financehelper.statistics.Statistic

class BudgetListAdapter(private val inflater: LayoutInflater, private val items: List<Budget>) : BaseAdapter()
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        val view = convertView
                   ?: inflater.inflate(R.layout.budget_element_listadapter, parent, false)
        val item = getItem(position)

        view.findViewById<TextView>(R.id.tv_budget).text = item.toString()

        return view
    }

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = items.size
}
