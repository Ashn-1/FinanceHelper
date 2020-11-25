package de.ash.financehelper.categories

class CategoryComparator : Comparator<Category>
{
    override fun compare(first: Category?, second: Category?): Int
    {
        return when
        {
            first == second -> 0
            first == null -> -1
            second == null -> 1

            first is PrimaryCategory && second is PrimaryCategory -> first.compareTo(second)
            first is PrimaryCategory && second is SecondaryCategory ->
            {
                if (first == second.parent) -1
                else first.compareTo(second.parent)
            }
            first is SecondaryCategory && second is PrimaryCategory ->
            {
                if (first.parent == second) 1
                else first.parent.compareTo(second)
            }
            first is SecondaryCategory && second is SecondaryCategory ->
            {
                if (first.parent == second.parent) first.compareTo(second)
                else first.parent.compareTo(second.parent)
            }

            else -> throw RuntimeException("this case does not exist")
        }
    }
}