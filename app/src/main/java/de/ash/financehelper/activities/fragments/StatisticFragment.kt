package de.ash.financehelper.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.core.util.forEach
import androidx.lifecycle.Observer
import de.ash.financehelper.R
import de.ash.financehelper.activities.AddStatisticActivity
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.statistics.Statistic
import de.ash.financehelper.statistics.StatisticListAdapter
import de.ash.financehelper.util.Data
import java.lang.NullPointerException

class StatisticFragment : MainScreenFragment(R.id.action_nav_statistics, AddStatisticActivity::class.java)
{
    private val statistics: MutableList<Statistic> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // # Load layout of the statistic fragment
        val view = inflater.inflate(R.layout.fragment_statistic, container, false)

        // # Set adapter of statistics list
        val statisticList = view.findViewById<ListView>(R.id.lv_displayed_statistics)
        statisticList.adapter = StatisticListAdapter(inflater, statistics)

        // # Open context menu on long click of any statistic
        registerForContextMenu(statisticList)

        Data.instance.databaseViewModel.allExpenses.observe(viewLifecycleOwner, Observer { allExpenses ->
            statistics.forEach { it.updateResult(allExpenses) }
            (statisticList.adapter as BaseAdapter).notifyDataSetChanged()
        })

        Data.instance.databaseViewModel.statisticsLiveData.observe(viewLifecycleOwner, Observer {
            statistics.clear()
            statistics.addAll(it)
            (statisticList.adapter as BaseAdapter).notifyDataSetChanged()
        })

        return view
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?)
    {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.lv_displayed_statistics)
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
                Intent(context, AddStatisticActivity::class.java).also {
                    it.putExtra(KeyStrings.ID, statistics[info.position].id)
                    startActivity(it)
                }
            }

            R.id.action_delete ->
            {
                Data.instance.removeStatistic(statistics[info.position])
            }

            else -> super.onContextItemSelected(item)
        }
        return true
    }

    override fun onResume()
    {
        super.onResume()
        (view?.findViewById<ListView>(R.id.lv_displayed_statistics)?.adapter as BaseAdapter).notifyDataSetChanged()
    }

    fun changedDateCallback()
    {
        val expenses = Data.instance.databaseViewModel.allExpenses.value
        if (expenses != null) statistics.forEach { it.updateResult(expenses) }
    }
}