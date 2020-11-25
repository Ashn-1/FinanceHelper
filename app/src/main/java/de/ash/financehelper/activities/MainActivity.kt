package de.ash.financehelper.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import de.ash.financehelper.R
import de.ash.financehelper.activities.fragments.*
import de.ash.financehelper.categories.Category
import de.ash.financehelper.constants.FileNames
import de.ash.financehelper.util.Data
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import java.time.LocalDate

class MainActivity : AppCompatActivity()
{
    companion object
    {
        private lateinit var appContext: WeakReference<Context>

        fun getContext(): Context
        {
            return appContext.get() ?: throw NullPointerException("static app context is null")
        }
    }

    private val today: LocalDate = LocalDate.now()

    private var fragments: Array<MainScreenFragment> =
        arrayOf(ExpenseFragment(), StatisticFragment(), BudgetFragment(), WishlistFragment())
    private val statisticFragment: StatisticFragment
        get()
        {
            return fragments[1] as StatisticFragment
        }
    private val budgetFragment: BudgetFragment
        get()
        {
            return fragments[2] as BudgetFragment
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        appContext = WeakReference(this)

        Category.init(this)
        Data.init(this, getPreferences(Context.MODE_PRIVATE), FileNames.DATABASE_EXPENSES)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        setContentView(R.layout.activity_main)
        setSupportActionBar(tb_main)

        // # Initialize viewpager
        vp_main.adapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
        {
            override fun getItem(position: Int): Fragment
            {
                require(position in 0 until count) { "viewpager position invalid" }
                return fragments[position]
            }

            override fun getCount() = fragments.size
        }
        vp_main.addOnPageChangeListener(object : ViewPager.OnPageChangeListener
        {
            override fun onPageScrollStateChanged(state: Int) = Unit
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit

            override fun onPageSelected(position: Int)
            {
                nav_bottom.selectedItemId = fragments[position].navId
            }
        })

        // # Initialize bottom navigation
        nav_bottom.setOnNavigationItemSelectedListener {
            when (it.itemId)
            {
                R.id.action_nav_expenses ->
                {
                    vp_main.currentItem = 0
                    true
                }
                R.id.action_nav_statistics ->
                {
                    vp_main.currentItem = 1
                    true
                }
                R.id.action_nav_budget ->
                {
                    vp_main.currentItem = 2
                    true
                }
                R.id.action_nav_wishlist ->
                {
                    vp_main.currentItem = 3
                    true
                }
                else -> false
            }
        }

        fab_main.setOnClickListener {
            val activityClass =
                ((vp_main.adapter as FragmentPagerAdapter).getItem(vp_main.currentItem) as MainScreenFragment).fabClass
            startActivity(Intent(this, activityClass))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId)
        {
            R.id.action_edit_categories ->
            {
                Intent(this, CategoryExpandableActivity::class.java).also {
                    startActivity(it)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (today != LocalDate.now())
        {
            statisticFragment.changedDateCallback()
            budgetFragment.changedDateCallback()
        }
    }
}
