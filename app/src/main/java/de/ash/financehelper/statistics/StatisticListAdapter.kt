package de.ash.financehelper.statistics

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import de.ash.financehelper.R

class StatisticListAdapter(private val inflater: LayoutInflater, private val items: List<Statistic>) : BaseAdapter()
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        val view = convertView
                   ?: inflater.inflate(R.layout.statistic_element_listadapter, parent, false)
        val item = getItem(position)

        view.findViewById<TextView>(R.id.tv_statistic).text = item.toString()

        return view
    }

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = items.size
}