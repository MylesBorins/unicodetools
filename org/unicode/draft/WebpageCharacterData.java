package org.unicode.draft;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * Here are the latest results of the code point frequencies for one
whole Base segment:

http://www.corp.google.com/~erikv/unicode-count62.txt

The 1st column is the code point.

Then there are 3 groups of 4 columns, where each group is:

pre-HTML code point count, post-HTML code point count, document count, UTF-8 document count

The 1st group includes "bad" docs (error during input conversion or
contains unassigned or high private use), 2nd group excludes "bad"
docs, 3rd group is multiplied by pagerank (and excludes "bad" docs).

Then there are up to 3 groups, where each group is:

navboost, pagerank, language, encoding, url

...Data/unicode-count62.txt
 */
public class WebpageCharacterData {
    
	private static final UnicodeSet DEBUG_SET = new UnicodeSet(0x0020,0x0020).freeze();
    private static final String SOURCE_DATA = "unicode-count75.txt"; // "unicode-count-2012-July-21.txt";

	enum Columns {
		// 000009	ht	954857442	0	0	0	953577889	0	0	0	11182029595621	0	0	0	804363	56255	139	22	http://www.palmbeachschools.org/	71269	55048	139	22	http://www.palmbeachschools.org/jobs/	50871	54366	139	22	http://rtghaiti.com/
		codePoint,
		language,
		preHtmlCount1, postHtmlCount1, documentCount1, utf8DocumentCount1,
		preHtmlCount2, postHtmlCount2, documentCount2, utf8DocumentCount2,
		preHtmlCount3, postHtmlCount3, documentCount3, utf8DocumentCount3;
		static String[] parts;
		public static void set(String line) {
			parts = line.split("\t");
		}
		public String get() {
			return parts[ordinal()];
		}
		public String toString() {
		    return name() + "(" + get() + ")";
		}
	}

	private static Map<String,Counter<Integer>> lang2chars = new HashMap<String,Counter<Integer>>();
	private static Map<String,Counter<Integer>> lang2charsPageRank = new HashMap<String,Counter<Integer>>();

	public static void main(String[] args) throws IOException {
		doData();
		System.out.println("DONE");
	}

	static public void doData() throws IOException {
		BufferedReader in = BagFormatter.openUTF8Reader(
		        "/Users/markdavis/Documents/indigo/DATA/frequency/",
		        SOURCE_DATA);
		int lineCounter = 0;
		int zeroCountLines = 0;
		HashMap<String, String> langSeen = new HashMap<String,String>();
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			if ((++lineCounter % 100000) == 0) {
				System.out.println(lineCounter);
			}
			Columns.set(line);

			int codePoint = Integer.parseInt(Columns.codePoint.get(), 16);
			boolean debugCodepoint = DEBUG_SET != null && DEBUG_SET.contains(codePoint);
			if (debugCodepoint) {
				System.out.println(line + ",\t\t" + Arrays.asList(Columns.values()));
			}
			String lang0 = Columns.language.get();
			String lang = langSeen.get(lang0);
			if (lang == null) {
				lang = LanguageCodeConverter.fromGoogleLocaleId(lang0); 
				langSeen.put(lang0, lang);
				if (debugCodepoint) {
					System.out.println(lang0 + " => " + lang);
				}
			}
			

			long good = Long.parseLong(Columns.postHtmlCount2.get());
			addToCounter(lang2chars, lang, codePoint, good);
			addToCounter(lang2chars, "mul", codePoint, good);
			long rank = Long.parseLong(Columns.postHtmlCount3.get());
			addToCounter(lang2charsPageRank, lang, codePoint, rank);
			addToCounter(lang2charsPageRank, "mul", codePoint, rank);
		}
		in.close();
		System.out.println("Writing data");
		//System.out.println("zeroCountLines " + zeroCountLines);
		writeData(lang2chars, "/Users/markdavis/Documents/indigo/DATA/frequency/languages");
		System.out.println("Writing ranked data");
		writeData(lang2charsPageRank, "/Users/markdavis/Documents/indigo/DATA/frequency/languages-rank");
	}

	public static void writeData(Map<String, Counter<Integer>> map, String directory) throws IOException {
		Counter<String> totalLang = new Counter<String>();
		Counter<String> totalLangChars = new Counter<String>();
		for (Entry<String, Counter<Integer>> entry : map.entrySet()) {
			String lang = entry.getKey();
			Counter<Integer> counter = entry.getValue();
			PrintWriter out = BagFormatter.openUTF8Writer(directory, lang + ".txt");
			long totalCount = 0;
			long totalChars = 0;
			for (Integer cp : counter.getKeysetSortedByCount(false)) {
				long count = counter.getCount(cp);
				totalCount += count;
				totalChars += 1;
				out.println(com.ibm.icu.impl.Utility.hex(cp) + " ; " + count); //  + " # " + UCharacter.getExtendedName(cp));
			}
			totalLang.add(lang, totalCount);
			totalLangChars.add(lang, totalChars);
			out.println("# END");
			out.close();
		}
		
		for (String lang : totalLang.getKeysetSortedByCount(false)) {
			System.out.println(lang + "\t" + totalLang.get(lang) + "\t" + totalLangChars.get(lang));
		}
	}

	public static void addToCounter(Map<String, Counter<Integer>> map, String lang, int codePoint, long post) {
		if (post == 0) {
			return;
		}
		Counter<Integer> x = map.get(lang);
		if (x == null) {
			map.put(lang, x = new Counter<Integer>());
		}
		x.add(codePoint, post);
	}
}
