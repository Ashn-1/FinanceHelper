package de.ash.financehelper.activities

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import de.ash.financehelper.R
import de.ash.financehelper.categories.Category
import de.ash.financehelper.categories.CategoryExpandableListAdapter
import de.ash.financehelper.constants.KeyStrings
import de.ash.financehelper.util.Data
import kotlinx.android.synthetic.main.activity_add_expense.*
import kotlinx.android.synthetic.main.category_expandable_list.*
import java.lang.NullPointerException

class CategoryExpandableActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.category_expandable_list)

        tb_categories.setTitle(R.string.categories)
        setSupportActionBar(tb_categories)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        elv_categories.setAdapter(CategoryExpandableListAdapter(Data.instance.primaryCategories))

        registerForContextMenu(elv_categories)

        Data.instance.databaseViewModel.categoriesLiveData.observe(this, Observer {
            (elv_categories.expandableListAdapter as BaseExpandableListAdapter).notifyDataSetChanged()
        })
    }

    override fun onDestroy()
    {
        super.onDestroy()
        unregisterForContextMenu(elv_categories)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?)
    {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.elv_categories)
        {
            menuInflater.inflate(R.menu.menu_element_long_press, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean
    {
        val info: ExpandableListView.ExpandableListContextMenuInfo =
            item.menuInfo as ExpandableListView.ExpandableListContextMenuInfo

        when (item.itemId)
        {
            R.id.action_edit ->
            {
                Toast.makeText(
                    this,
                    "Edit ${Data.instance.getCategoryFromId(info.id.toInt())}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            R.id.action_delete ->
            {
                Data.instance.removeCategory(Data.instance.getCategoryFromId(info.id.toInt()))
            }

            else -> super.onContextItemSelected(item)
        }
        return true
    }
}