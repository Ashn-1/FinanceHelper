package de.ash.financehelper.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import de.ash.financehelper.R
import de.ash.financehelper.activities.MainActivity
import de.ash.financehelper.categories.Category
import de.ash.financehelper.categories.PrimaryCategory
import java.lang.NullPointerException

class CategoryExpandableListAdapter(val header: List<PrimaryCategory>) : BaseExpandableListAdapter()
{
    override fun getGroup(groupPosition: Int): Any
    {
        return header[groupPosition]
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean
    {
        return true
    }

    override fun hasStableIds(): Boolean
    {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View
    {
        val text: String = getGroup(groupPosition).toString()
        var view = convertView

        if (view == null)
        {
            val inflater: LayoutInflater =
                MainActivity.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            view = inflater.inflate(R.layout.exp_list_group, null)
        }

        view?.findViewById<TextView>(R.id.tv_exp_list_group)?.text = text
        return view ?: throw NullPointerException("view cannot be null here")
    }

    override fun getChildrenCount(groupPosition: Int): Int
    {
        return header[groupPosition].secondaries.size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any
    {
        return header[groupPosition].secondaries[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long
    {
        return header[groupPosition].id.toLong()
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View
    {
        val text: String = getChild(groupPosition, childPosition).toString()
        var view = convertView

        if (view == null)
        {
            val inflater: LayoutInflater =
                MainActivity.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            view = inflater.inflate(R.layout.exp_list_item, null)
        }

        view?.findViewById<TextView>(R.id.tv_exp_list_item)?.text = text
        return view ?: throw NullPointerException("view cannot be null here")
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long
    {
        return header[groupPosition].secondaries[childPosition].id.toLong()
    }

    override fun getGroupCount(): Int
    {
        return header.size
    }
}