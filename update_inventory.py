import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """    val mockInventory = listOf(
        Triple("Fresh Bakery Bagels", "Expires in 2 days", "Fresh"),
        Triple("Mixed Veggie Pasta", "Expires in 4 hours", "Expiring Soon"),
        Triple("Catering Salads", "Expired 2 hours ago", "Expired")
    )"""

replacement = """    val mockInventory = listOf(
        Triple("Whole Wheat Flour (Atta) - 10kg", "Expires in 3 months", "Fresh"),
        Triple("Fresh Tomatoes & Onions - 5kg", "Expires in 3 days", "Fresh"),
        Triple("Cooked Basmati Rice & Dal", "Expires in 4 hours", "Expiring Soon"),
        Triple("Catering Paneer Sabzi", "Expired 2 hours ago", "Expired")
    )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
