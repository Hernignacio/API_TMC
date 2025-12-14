public class Validator {
    public static boolean isValidYear(String year) {
        if (year == null) return false;
        return year.matches("^\\d{4}$");
    }

    public static boolean isValidMonth(String month) {
        if (month == null) return false;
        return month.matches("^(0[1-9]|1[0-2])$");
    }
}

