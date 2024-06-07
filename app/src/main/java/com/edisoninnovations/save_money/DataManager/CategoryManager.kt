package com.edisoninnovations.save_money.DataManager

object CategoryManager {
    private val incomeCategories = mapOf(
        1 to "Ahorros",
        2 to "Trabajo",
        3 to "Diario",
        4 to "Regalo",
        5 to "Inversion",
        20 to "Prestamo"
    )

    private val expenseCategories = mapOf(
        6 to "Fiesta",
        7 to "Comida",
        8 to "Ropa",
        9 to "Salud",
        10 to "Carro",
        11 to "Gasolina",
        12 to "Vacaciones",
        13 to "Hijos",
        14 to "Deporte",
        15 to "Belleza",
        16 to "Entretenimiento",
        17 to "Facturas",
        18 to "Compras",
        19 to "Casa",
        4 to "Regalo",
        21 to "Deuda"
    )

    fun getCategoryID(categoryName: String, isIncome: Boolean): Int {
        println("######################isIncome: $isIncome" +"categoryName: $categoryName")
        val categories = if (isIncome) incomeCategories else expenseCategories
        return categories.entries.firstOrNull { it.value == categoryName }?.key ?: -1
    }
}
