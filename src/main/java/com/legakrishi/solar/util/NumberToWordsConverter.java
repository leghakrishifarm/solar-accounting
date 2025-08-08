package com.legakrishi.solar.util;

public class NumberToWordsConverter {
    private static final String[] tensNames = {
            "", " Ten", " Twenty", " Thirty", " Forty", " Fifty", " Sixty", " Seventy", " Eighty", " Ninety"
    };

    private static final String[] numNames = {
            "", " One", " Two", " Three", " Four", " Five", " Six", " Seven", " Eight", " Nine", " Ten",
            " Eleven", " Twelve", " Thirteen", " Fourteen", " Fifteen", " Sixteen", " Seventeen", " Eighteen", " Nineteen"
    };

    private static String convertLessThanOneThousand(int number) {
        String soFar;
        if (number % 100 < 20){
            soFar = numNames[number % 100];
            number /= 100;
        }
        else {
            soFar = numNames[number % 10];
            number /= 10;

            soFar = tensNames[number % 10] + soFar;
            number /= 10;
        }
        if (number == 0) return soFar;
        return numNames[number] + " Hundred" + soFar;
    }

    public static String convert(long number) {
        if (number == 0) { return "Zero"; }

        String prefix = "";

        if (number < 0) {
            number = -number;
            prefix = "Negative";
        }

        StringBuilder sb = new StringBuilder();

        int crore = (int)(number / 10000000);
        number %= 10000000;

        int lakh = (int)(number / 100000);
        number %= 100000;

        int thousand = (int)(number / 1000);
        number %= 1000;

        int hundred = (int) number;

        if (crore > 0) {
            sb.append(convertLessThanOneThousand(crore)).append(" Crore ");
        }
        if (lakh > 0) {
            sb.append(convertLessThanOneThousand(lakh)).append(" Lakh ");
        }
        if (thousand > 0) {
            sb.append(convertLessThanOneThousand(thousand)).append(" Thousand ");
        }
        if (hundred > 0) {
            sb.append(convertLessThanOneThousand(hundred));
        }

        return prefix + sb.toString().trim() + " Only";
    }
}
