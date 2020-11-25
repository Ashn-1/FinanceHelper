package de.ash.financehelper.util

import org.json.JSONObject

interface JSONable
{
    fun toJson(): JSONObject
}
