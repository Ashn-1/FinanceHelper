package de.ash.financehelper.categories

import org.json.JSONArray
import org.json.JSONObject

class PrimaryCategory(id: Int, name: String, val secondaries: MutableList<SecondaryCategory> = mutableListOf()) :
    Category(id, name)
{
    companion object
    {
        fun fromJson(data: JSONObject) : PrimaryCategory
        {
            val id = data.getInt("id")
            val name = data.getString("name")
            val secondariesArray = data.getJSONArray("sec_categories")
            val primary = PrimaryCategory(id, name)

            if(secondariesArray.length() != 0)
            {
                for(i in 0 until secondariesArray.length())
                {
                    primary.secondaries.add(
                        SecondaryCategory.fromJson(
                            secondariesArray.getJSONObject(i),
                            primary
                        )
                    )
                }
            }

            return primary
        }
    }

    override fun toJson(): JSONObject
    {
        val data = super.toJson()

        // additionally to base info put all the secondary categories info into the primary one
        data.put("sec_categories", JSONArray().also { arr ->
            secondaries.forEach { arr.put(it.toJson()) }
        })

        return data
    }
}