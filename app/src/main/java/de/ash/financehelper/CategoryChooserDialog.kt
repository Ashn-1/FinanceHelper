package de.ash.financehelper

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.util.forEach
import de.ash.financehelper.categories.Category
import de.ash.financehelper.categories.PrimaryCategory
import de.ash.financehelper.categories.SecondaryCategory
import de.ash.financehelper.util.Data

class CategoryChooserDialog(context: Context, initialSelectedCategories: Set<Category>) :
    AlertDialog(context)
{
    var isPositiveDismiss = false
        private set

    val selectedCategories = mutableSetOf<Category>()

    init
    {
        // # Get the dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_category_selection, null)
        setView(dialogView)

        setTitle(R.string.select_categories)

        // # Init the category chooser list
        val listCategories: ListView = dialogView.findViewById(R.id.lv_select_categories)
        val listCategoryAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_multiple_choice,
            Data.instance.allCategories
        )
        listCategories.adapter = listCategoryAdapter
        listCategories.itemsCanFocus = false
        listCategories.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val item = listCategories.getItemAtPosition(pos)
            val isChecked = listCategories.isItemChecked(pos)

            if (item is PrimaryCategory)
            {
                // # check/uncheck all the secondary categories of the primary
                item.secondaries.forEach {
                    val posSecondary = listCategoryAdapter.getPosition(it)
                    listCategories.setItemChecked(posSecondary, isChecked)
                }
            } else if (item is SecondaryCategory)
            {
                // # if secondary is de-selected it should de-select its primary category too
                val posPrimary = listCategoryAdapter.getPosition(item.parent)
                if (!isChecked) listCategories.setItemChecked(posPrimary, false)
            }
        }

        // # Add confirm button
        setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(R.string.confirm)
        ) { _, _ ->
            val checkedPos = listCategories.checkedItemPositions
            checkedPos.forEach { pos, isChecked ->
                if (isChecked) selectedCategories.add(listCategories.getItemAtPosition(pos) as Category)
            }

            isPositiveDismiss = true
        }

        // # Add cancel button
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)) { dialog: DialogInterface, _ ->
            dialog.dismiss()
        }

        // # for editing this list; check all categories that are already checked
        initialSelectedCategories.forEach { listCategories.setItemChecked(listCategoryAdapter.getPosition(it), true) }
    }
}