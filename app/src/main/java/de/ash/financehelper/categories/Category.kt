package de.ash.financehelper.categories

import android.content.Context
import de.ash.financehelper.R
import de.ash.financehelper.util.JSONable
import org.json.JSONObject

abstract class Category(val id: Int, categoryName: String) : Comparable<Category>, JSONable
{
    companion object
    {
        const val NULL_CATEGORY_ID: Int = Int.MIN_VALUE
        lateinit var nullCategory: Category private set

        fun init(context: Context)
        {
            if(isInitialized()) return
            nullCategory = PrimaryCategory(NULL_CATEGORY_ID, context.getString(R.string.no_category))
        }

        fun isInitialized() = ::nullCategory.isInitialized
    }

    init
    {
        if (isInitialized()) require(id != NULL_CATEGORY_ID) { "id $id cannot be used" }
    }

    var name: String = categoryName
        set(value)
        {
            require(value != "") { "category name cannot be empty" }
            field = value
        }

    override fun toJson(): JSONObject
    {
        val data = JSONObject()
        data.put("id", id)
        data.put("name", name)
        return data
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (other !is Category) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id
    override fun toString() = name
    override fun compareTo(other: Category) = name.compareTo(other.name)
}
