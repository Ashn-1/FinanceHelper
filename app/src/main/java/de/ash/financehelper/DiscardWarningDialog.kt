package de.ash.financehelper

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface

class DiscardWarningDialog(activity: Activity) : AlertDialog(activity)
{
    init
    {
        setTitle(R.string.discard)
        setMessage(context.getString(R.string.warning_data_will_be_lost))
        setButton(BUTTON_POSITIVE, context.getString(R.string.yes)) { _, _ ->
            activity.finish()
        }
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)) { dialogInterface, _ ->
            dialogInterface.cancel()
        }
    }
}