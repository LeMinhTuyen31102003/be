package be.spring.vanconhung.util;

import java.text.Collator;
import java.util.Locale;

public final class SortingUtils {

    public static final Collator VI_COLLATOR = Collator.getInstance(new Locale("vi"));

    private SortingUtils() {
    }

    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static int compareGrade(String a, String b) {
        String ga = nullToEmpty(a);
        String gb = nullToEmpty(b);
        boolean aNum = ga.matches("\\d+");
        boolean bNum = gb.matches("\\d+");
        if (aNum && bNum) {
            return Integer.compare(Integer.parseInt(ga), Integer.parseInt(gb));
        }
        if (aNum) {
            return -1;
        }
        if (bNum) {
            return 1;
        }
        return VI_COLLATOR.compare(ga, gb);
    }
}
