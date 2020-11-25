package de.ash.financehelper.util

import android.app.DatePickerDialog
import android.content.Context
import android.view.View
import android.widget.EditText
import java.time.LocalDate

class EditDate(private val dateEdit: EditText, date: LocalDate = LocalDate.now())
{
    var date: LocalDate = LocalDate.MIN
        set(value)
        {
            dateEdit.setText(value.toString())
            field = value
        }

    init
    {
        this.date = date // assignment done here so custom setter is also called
    }

    inner class OnClickDatePicker(val context: Context): View.OnClickListener
    {
        override fun onClick(v: View?)
        {
            val initialDay = date.dayOfMonth
            val initialMonth = date.monthValue
            val initialYear = date.year

            val picker = DatePickerDialog(
                context,
                DatePickerDialog.OnDateSetListener { _, newYear, newMonth, newDay ->
                    date = LocalDate.of(newYear, newMonth + 1, newDay)
                }, initialYear, initialMonth - 1, initialDay
            )
            picker.show()
        }
    }
}