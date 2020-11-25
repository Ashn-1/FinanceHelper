package de.ash.financehelper.activities.fragments

import android.content.ComponentName
import android.content.Context
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class MainScreenFragment(val navId: Int, val fabClass: Class<*>) : Fragment()
{
    protected var currentlyActive: Boolean = false

    override fun onResume()
    {
        super.onResume()
        currentlyActive = true
    }

    override fun onPause()
    {
        super.onPause()
        currentlyActive = false
    }
}
