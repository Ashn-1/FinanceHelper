package de.ash.financehelper.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CategoryListAdapter(context: Context, private val categories: List<Category>) :
    ArrayAdapter<Category>(context, android.R.layout.simple_list_item_1, categories)
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
    {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val row = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val text = row.findViewById<TextView>(android.R.id.text1)
        text.text = categories[position].toString()

        return row
    }
}