package com.arrl.radiocraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extra words the creative search finds an item by, never shown (maintainer, 2026-10-31): every number in its name
 * spelled out, so "twenty" finds the 20m radio and "five" the 5-BTV; a band ("20m") also as "twenty meter(s)" and
 * "twenty metre(s)".
 */
public final class SearchAliases {

	private SearchAliases() {}

	private static final Pattern NUMBER = Pattern.compile("(\\d+)(m\\b)?");
	private static final String[] ONES = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen" };
	private static final String[] TENS = { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };

	/** The words for the numbers in {@code name}, empty if it has none. */
	public static List<String> of(String name) {
		List<String> out = new ArrayList<>();
		Matcher matcher = NUMBER.matcher(name);
		while(matcher.find()) {
			String digits = matcher.group(1);
			if(digits.length() > 4)
				continue;
			String words = words(Integer.parseInt(digits));
			if(!out.contains(words))
				out.add(words);
			if(matcher.group(2) != null)
				for(String unit : List.of("meter", "meters", "metre", "metres"))
					out.add(words + " " + unit);
		}
		return out;
	}

	/** {@code n} (0 to 9999) in English words: 146 is "one hundred forty six". */
	public static String words(int n) {
		if(n < 20)
			return ONES[n];
		if(n < 100)
			return TENS[n / 10] + (n % 10 == 0 ? "" : " " + ONES[n % 10]);
		if(n < 1000)
			return ONES[n / 100] + " hundred" + (n % 100 == 0 ? "" : " " + words(n % 100));
		return words(n / 1000) + " thousand" + (n % 1000 == 0 ? "" : " " + words(n % 1000));
	}
}
