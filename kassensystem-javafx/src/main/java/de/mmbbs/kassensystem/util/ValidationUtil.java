package de.mmbbs.kassensystem.util;

public class ValidationUtil {

    public static boolean isValidProductName(String name) {
        return name != null && !name.trim().isEmpty() && name.trim().length() >= 2;
    }

    public static String getProductNameError() {
        return "Produktname muss mindestens 2 Zeichen lang sein.";
    }

    public static boolean isValidPrice(String priceStr) {
        try {
            double price = Double.parseDouble(priceStr.replace(",", "."));
            return price > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getPriceError() {
        return "Preis muss eine positive Zahl sein (z.B. 1.50 oder 1,50).";
    }

    public static boolean isValidStock(String stockStr) {
        try {
            double stock = Double.parseDouble(stockStr.trim().replace(",", "."));
            return stock >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getStockError() {
        return "Lagerbestand muss eine Zahl >= 0 sein.";
    }

    public static boolean isValidQuantity(String quantityStr) {
        try {
            double quantity = Double.parseDouble(quantityStr.trim().replace(",", "."));
            return quantity > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getQuantityError() {
        return "Menge muss eine Zahl > 0 sein.";
    }
}
