package de.ash.financehelper.categories

import org.json.JSONObject

class SecondaryCategory(id: Int, name: String, var parent: PrimaryCategory) :
    Category(id, name)
{
    companion object
    {
        fun fromJson(data: JSONObject, parent: PrimaryCategory): SecondaryCategory
        {
            val id = data.getInt("id")
            val name = data.getString("name")
            return SecondaryCategory(id, name, parent)
        }
    }
}