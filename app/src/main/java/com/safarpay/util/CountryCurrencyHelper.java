package com.safarpay.util;
import java.util.*;
public class CountryCurrencyHelper {
    private static final Map<String,String> MAP = new LinkedHashMap<>();
    static {
        MAP.put("Pakistan","PKR"); MAP.put("United States","USD"); MAP.put("United Kingdom","GBP");
        MAP.put("European Union","EUR"); MAP.put("United Arab Emirates","AED"); MAP.put("Saudi Arabia","SAR");
        MAP.put("China","CNY"); MAP.put("Japan","JPY"); MAP.put("India","INR"); MAP.put("Turkey","TRY");
        MAP.put("Canada","CAD"); MAP.put("Australia","AUD"); MAP.put("Malaysia","MYR");
        MAP.put("Singapore","SGD"); MAP.put("Thailand","THB"); MAP.put("Switzerland","CHF");
        MAP.put("Kuwait","KWD"); MAP.put("Qatar","QAR"); MAP.put("Egypt","EGP"); MAP.put("Bangladesh","BDT");
    }
    public static List<String> getCountries() { return new ArrayList<>(MAP.keySet()); }
    public static String getCurrencyCode(String country) { return MAP.getOrDefault(country,"USD"); }
    public static List<String> getCurrencyCodes() {
        Set<String> set = new LinkedHashSet<>(MAP.values());
        return new ArrayList<>(set);
    }
}