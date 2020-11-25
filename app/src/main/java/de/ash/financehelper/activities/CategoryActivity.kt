package de.ash.financehelper.activities

import android.app.ListActivity
import android.os.Bundle
import de.ash.financehelper.categories.CategoryListAdapter
import de.ash.financehelper.util.Data

class CategoryActivity : ListActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val adapter = CategoryListAdapter(this, Data.instance.allCategories)
        listAdapter = adapter
    }
}