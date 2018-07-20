package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser.Fields;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.EmojiData.VariantFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Minimize;

/**
 * To generate emoji frequency data:
 * <ul>
 * <li>Add new data to DATA directory</li>
 * <li>Run this program, and paste files into spreadsheet.</li>
 * <li>
 * </ul>
 * @author markdavis
 *
 */
public class ListEmojiGroups {
    private static final boolean DEBUG = false;

    static final EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);
    static final UnicodeSet SKIP = new UnicodeSet("[© ® ™]").freeze();

    private static final String OUTDIR = "/Users/markdavis/Google Drive/workspace/Generated/emoji/frequency";

    public static void main(String[] args) {
        //        System.out.println("\n\n***MAIN***\n");
        //        showCounts("gboardMainRaw.tsv", GBoardCounts.countsRaw, null);
        //
        //        System.out.println("\n\n***W/O FE0F***\n");
        //        showCounts("gboardNoFE0F.tsv", GBoardCounts.countsWithoutFe0f, GBoardCounts.countsRaw);
        //        
        System.out.println("\n\n***RawSequencesToCount***\n");
        showSequencesToCount("RawSequencesToCount.tsv");

        System.out.println("\n\n***MAIN***\n");
        showCounts("gboardMain.tsv", GBoardCounts.localeToCountInfo.get("001").keyToCount, null);
        showCounts("gboardDE.tsv", GBoardCounts.localeToCountInfo.get("de").keyToCount, null);

        System.out.println("\n\n***EmojiTracker***\n");
        showCounts("emojiTracker.tsv", EmojiTracker.countInfo.keyToCount, null);

        System.out.println("\n\n***Twitter***\n");
        showCounts("twitter.tsv", Twitter.countInfo.keyToCount, null);

        System.out.println("\n\n***Facebook***\n");
        showCounts("facebook.tsv", Facebook.countInfo.keyToCount, null);

        System.out.println("\n\n***INFO***\n");
        showInfo("emojiInfo.tsv");

        showTextEmoji("emojiText.tsv");
    }

    private static void showTextEmoji(String filename) {
        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {
            UnicodeSet Android_Chrome_TP = new UnicodeSet("[☹ ☠  ❣ ⛑ ☘ ⛰ ⛩ ♨ ⛴ ✈ ⏱ ⏲ ⛈ ☂ ⛱ ☃ ☄ ⛸  ⌨  ✉ ✏ ⛏ ⚒ ⚔ ⚙ ⚗ ⚖ ⛓ ⚰ ⚱ ⚠ ☢ ☣ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ⚛ ✡ ☸ ☯ ✝ ☦ ☪ ☮ ▶ ⏭ ⏯ ◀ ⏮  ⏏ ♀ ♂ ⚕ ♻ ⚜ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_Chrome_TP = new UnicodeSet("[☺ ❤ ❣ 🗨 ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ☎ ✉ ✏ ✒ ✂ ⚠ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_TextMate_TP = new UnicodeSet("[☺☝ ✌✍ ❤ ❣ ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ✉ ✏ ✒ ✂ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ #⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_Notes_TP = new UnicodeSet("[☝ ✌ ✍ ❤ ❣ ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ✉ ✏ ✒ ✂ ⚠ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ⚜ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ #⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_Safari_TP = new UnicodeSet("[☺☝ ✌ ✍ ❤ ❣♨✈☀ ☁☂❄♠ ♥ ♦ ♣☎✉✏ ✒✂⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖↕ ↔ ↩ ↪ ⤴ ⤵✡☯ ✝▶◀⏏ ♀ ♂ ⚕ ♻☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™#⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗㊙ ▪ ▫ ◻ ◼﻿]");
            out.println("Hex\tEmoji\tAndroid Chrome\tMac Chrome\tMac Safari\tMac TextMate\tMac Notes");
            for (String s : EmojiMatcher.nopres) {
                out.println(
                        hex(s)
                        + "\t" + s
                        + "\t" + (Android_Chrome_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_Chrome_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_Safari_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_TextMate_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_Notes_TP.contains(s) ? "text" : "emoji")
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static void showSequencesToCount(String outputFileName) {
        Set<String> sorted = EmojiData.of(Emoji.VERSION_LAST_RELEASED).getAllEmojiWithDefectives().addAllTo(new TreeSet<>(order.codepointCompareSeparateDefects));
        VariantFactory vf = order.emojiData.new VariantFactory();
        for (String s : EmojiData.EMOJI_DATA.getAllEmojiWithDefectives()) {
            if (s.equals("\u263A")) {
                int debug = 0;
            }
            vf.set(s);
            for (String cp : vf.getCombinations()) {
                sorted.add(cp);
            }
        }
        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, outputFileName)) {
            out.println("Hex\tEmoji\tCLDR Name");
            for (String s : sorted) {
                out.println(hex(s,4) 
                        + "\t" + s
                        + "\t" + EmojiData.EMOJI_DATA.getName(s)
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    static final Set<String> SORTED;
    static {
        Set<String> SORTED2 = new TreeSet<>(order.codepointCompare);
        System.out.println(order.codepointCompare.compare("😀", "#️⃣"));
        for (String s : EmojiData.EMOJI_DATA.getAllEmojiWithDefectives()) {
            String norm = normalizeEmoji(s);
            if (!norm.isEmpty()) {
                SORTED2.add(norm);
            }

            //            if (Emoji.isSingleCodePoint(s)) {
            //                String ex = EmojiData.EMOJI_DATA.addEmojiVariants(s);
            //                if (!ex.equals(s)) {
            //                    sorted.add(ex);
            //                }
            //            }
        }
        SORTED = ImmutableSet.copyOf(SORTED2);
        //        for (String s : SORTED) {
        //            System.out.println(s + "\t" + EmojiData.EMOJI_DATA.getName(s));
        //        }
    }

    private static void showInfo(String filename) {
        int sortOrder = 0;

        //try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {

        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {
            out.println("Hex\tEmoji\tGroup\tSubgroup\tName (cldr)\tNorm?\tSort Order\tType\tYear");
            for (String s : SORTED) {
                String subcategory = order.getCategory(s);
                if (subcategory == null) {
                    subcategory = order.getCategory(UTF16.valueOf(s.codePointAt(0)));
                    if (subcategory == null) {
                        continue;
                    }
                }
                String ep = EmojiData.EMOJI_DATA.addEmojiVariants(s).equals(s) ? "" : "Defect";
                out.println(
                        hex(s)
                        + "\t" + s 
                        + "\t" + order.getMajorGroupFromCategory(subcategory).toPlainString()
                        + "\t" + subcategory.toString()
                        + "\t" + EmojiData.EMOJI_DATA.getName(s)
                        + "\t" + ep
                        + "\t" + sortOrder++
                        + "\t" + Category.getBucket(s).toStringPlain()
                        + "\t" + EmojiData.getYear(s)
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    static final UnicodeSet HACK_FE0F = new UnicodeSet("[©®™✔]").freeze();

    private static void showCounts(String filename, Map<String,Long> x, Map<String,Long> withFe0f) {

        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {
            boolean normal = withFe0f == null;
            out.println("Hex\tCount"
                    + (normal ? "\tRank" : "\tGB-Data\tto add to GB-Data")
                    + "\tEmoji");
            int rank = 0;
            for (Entry<String, Long> entry : x.entrySet()) {
                String term = entry.getKey();
                try {
                    int cp = term.codePointAt(0);
                } catch (Exception e) {
                    continue;
                }
                Long count = entry.getValue();
                Long countWithFe0f = normal ? 0 : withFe0f.get(term + Emoji.EMOJI_VARIANT);
                Long adjusted = GBoardCounts.toAddAdjusted(term, countWithFe0f, count);
                out.println(hex(term)
                        + "\t" + count
                        + "\t" + (normal ? ++rank : countWithFe0f)
                        + (normal ? "" : "\t" + adjusted)
                        + "\t" + term
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }



    static int matches(UnicodeSet unicodeSet, String input, int offset) {
        SortedSet<String> items = (SortedSet<String>) unicodeSet.strings();
        int cp = input.codePointAt(offset);
        SortedSet<String> subset = items.subSet(UTF16.valueOf(cp), UTF16.valueOf(cp+1));
        int bestLength = -1;
        int inputLength = input.length();
        int allowedLength = inputLength - offset;
        if (!subset.isEmpty()) {
            for (String trial : subset) {
                // see if the trial matches the characters in input starting at offset
                int trialLength = trial.length();
                if (bestLength >= trialLength) { // when we start to contract, stop
                    break;
                }
                //            if (trialLength > allowedLength) { // trial is too big, stop
                //                break; // can't match and nothing else will
                //            }
                if (input.regionMatches(offset, trial, 0, trialLength)) {
                    bestLength = trialLength;
                }
            }
        }
        if (bestLength >= 0) {
            return offset + bestLength;
        }
        if (unicodeSet.contains(cp)) {
            return offset + Character.charCount(cp);
        }
        return -1;
    }

    static class CountInfo {
        public static final double SCALE = 1000000000.0;
        final long rawTotal;
        final Map<String,Long> keyToCount;
        final Map<String,Integer> keyToRank;
        
        public long getRaw(String key) {
            Long raw = keyToCount.get(key);
            return raw == null ? 0 : (long)(raw * rawTotal / SCALE);
        }
        public CountInfo(Counter<String> inputCounter) {
            inputCounter.remove("");
            rawTotal = inputCounter.getTotal();
            Map<String,Long> _keyToCount = new LinkedHashMap();
            Map<String,Integer> _keyToRank = new LinkedHashMap();

            double factor = SCALE/rawTotal;
            int rank = 0;
            for (R2<Long, String> entry : inputCounter.getEntrySetSortedByCount(false, null)) {
                long count = entry.get0();
                String codes = entry.get1();
//                if (factor < 0) {
//                    factor = 1000000000.0/rawTotal;
//                }
                _keyToCount.put(codes, Math.round(factor*count));
                _keyToRank.put(codes, ++rank);
            }
            keyToCount = ImmutableMap.copyOf(_keyToCount);
            keyToRank = ImmutableMap.copyOf(_keyToRank);
//            for (String s : SORTED) {
//                if (!outputCounter.containsKey(s)) {
//
//                }
//            }
        }
    }

    static class GBoardCounts {
        private static final String FREQ_SOURCE = "/Users/markdavis/Google Drive/workspace/DATA/frequency/emoji/";
        //static Counter<String> counts = new Counter<>();
        static Map<String, CountInfo> localeToCountInfo = new LinkedHashMap<>();
        //        static Counter<String> countsRaw = new Counter<>();
        //        static Counter<String> countsWithoutFe0f = new Counter<>();
        private static long toAddAdjusted(String term, Long countWithFe0f, Long countWithoutFe0f) {
            return HACK_FE0F.contains(term) ? countWithFe0f * 4 : countWithoutFe0f;
        }
        enum Type {
            global, 
            locale;

            public String getFile() {
                switch(this) {
                case global: return "emoji_frequency_";
                case locale: return "emoji_frequency_by_locale_";
                }
                throw new IllegalArgumentException();
            }
            static final int rankIndex = 0, emojiIndex=1, decIndex=2, countIndex=3, hexIndex=4, limitIndex=5;
            //global: 1,    ߘ  ,[128514] ,3354042, ['0x1F602'] 
            //locale: ab_GE,    ߘ£  ,[128547]   ,24, ['0x1F623']

            public int getRankIndex() {
                return rankIndex;
            }
            public int getEmojiIndex() {
                return emojiIndex;
            }
            public int getCountIndex() {
                return countIndex;
            }
            public int getLocaleIndex() {
                return rankIndex;
            }
            static public int size() {
                return limitIndex;
            }
        }
        static {
            Map<String, Counter<String>> _counts = new LinkedHashMap<>();
            //Counter<String> _counts = new Counter<>();

            List<String> emojiSet = new ArrayList<>();
            List<String> nonPresSet = new ArrayList<>();
            List<String> nonEmojiSet = new ArrayList<>();
            //,text,decimal_code_points,count,hex_code_points
            // 8,❤️,"[10084, 65039]",705086,"['0x2764', '0xFE0F']"
            CSVParser csvParser = new CSVParser();
            for (Type type : Type.values()) {
                for (String id : Arrays.asList(
                        /*"20171031_20171113", "20171115_20171128", */
                        "20180608_20180621", "20180624_20180707")) { // "20171031_20171113", "20171115_20171128"
                    String filename = type.getFile() + id + ".csv";
                    int offset = 0;
                    for (String line : FileUtilities.in(FREQ_SOURCE + "/emoji_freqs_" + id, filename)) {
                        if (line.isEmpty() || line.startsWith(",text") || line.startsWith("locale")) {
                            continue;
                        } else if (line.startsWith(",locale")) {
                            offset = 1;
                            continue;
                        }
                        csvParser.set(line);
                        if (csvParser.size() != Type.size() + offset) {
                            System.out.println(filename + "\tSkipping short line: " + csvParser);
                            continue;
                        }
                        String emojiString = csvParser.get(type.getEmojiIndex() + offset);

                        String rankString = csvParser.get(type.getRankIndex() + offset);
                        long rank = type == Type.global ? Long.parseLong(rankString) : -1;

                        String locale = type == Type.global ? "001" : normalizeLocale(csvParser.get(type.getLocaleIndex() + offset));
                        if (locale == null) {
                            continue;
                        }
                        String countString = csvParser.get(type.getCountIndex() + offset);
                        long count = Long.parseLong(countString);

                        emojiSet.clear();
                        nonEmojiSet.clear();
                        nonPresSet.clear();
                        EmojiMatcher.parse(emojiString, emojiSet, nonPresSet, nonEmojiSet);
                        if (DEBUG) System.out.println(rank
                                + "\t" + count
                                + "\t" + emojiString 
                                + "\t" + hex(emojiString)
                                + "\t" + emojiSet
                                + "\t" + nonPresSet
                                + "\t" + nonEmojiSet
                                );
                        Counter<String> c = _counts.get(locale);
                        if (c == null) _counts.put(locale, c = new Counter<>());

                        for (String s : emojiSet) {
                            c.add(normalizeEmoji(s), count);
                        }
                        for (String s : nonPresSet) {
                            c.add(normalizeEmoji(s), count);
                        }
                    }
                }
            }
            localeToCountInfo = normalizeLocaleCounts(_counts);
            //            counts.addAll(countsRaw);
            //            for (R2<Long, String> entry : countsWithoutFe0f.getEntrySetSortedByCount(false, null)) {
            //                long countWithoutFe0f = entry.get0();
            //                String term = entry.get1();
            //                long countWithFe0f = counts.get(term);
            //                counts.add(term + Emoji.EMOJI_VARIANT, toAddAdjusted(term, countWithFe0f, countWithoutFe0f));
            //            }
        }
        private static Map<String, CountInfo> normalizeLocaleCounts(Map<String, Counter<String>> _counts) {
            Map<String, CountInfo> counts2 = new LinkedHashMap<>();
            for (String locale : _counts.keySet()) {
                Counter<String> c = _counts.get(locale);
                CountInfo outputCounter = new CountInfo(c);
                counts2.put(locale, outputCounter);
            }
            return counts2;
        }
        private static String normalizeLocale(String string) {
            ULocale ulocale = new ULocale(string);
            String country = ulocale.getCountry();
            if (country.equals("XA")) {
                return null;
            }
            if (country.equals("HK")) {
                int debug = 0;
            }
            ULocale max = ULocale.addLikelySubtags(ulocale);
            ULocale noCountry = new ULocale.Builder().setLanguage(max.getLanguage()).setScript(max.getScript()).build();
            return ULocale.minimizeSubtags(noCountry).toLanguageTag();
        }
    }



    public static String hex(String string) {
        return hex(string, 1);
    }

    private static String hex(String string, int minLen) {
        return "\\x{" + Utility.hex(string, minLen, " ") + "}";
    }

    public static class CSVParser {
        enum State {start, quote}
        // ab,cd => -1,2,5 that is, point before each comma
        private String line;
        private List<Integer> commaPoints = new ArrayList<>();

        public String get(int item) {
            return line.substring(commaPoints.get(item)+1, commaPoints.get(item+1));
        }

        public int size() {
            return commaPoints.size() - 1;
        }

        public CSVParser set(String line) {
            this.line = line;
            commaPoints.clear();
            commaPoints.add(-1);
            State state = State.start;
            int i = 0;
            for (; i < line.length(); ++i) {
                int ch = line.charAt(i);
                switch(state) {
                case start: {
                    switch(ch) {
                    case ',': commaPoints.add(i); break;
                    case '"': state = State.quote; break;
                    }
                    break;
                }
                case quote: {
                    switch(ch) {
                    case '"': state = State.start; break;
                    }
                    break;
                }
                }
            }
            commaPoints.add(i);
            return this;
        }
        public List<String> toList() {
            Builder<String> builder = ImmutableList.builder();
            for (int i = 0; i < size(); ++i) {
                builder.add(get(i));
            }
            return builder.build();
        }
        @Override
        public String toString() {
            return toList().toString();
        }
    }

    static class EmojiTracker {
        static CountInfo countInfo;
        static {
            Counter<String> _counts = new Counter<>();

            Matcher m = Pattern.compile("id=\"score-([A-F0-9]+)\">\\s*(\\d+)\\s*</span>").matcher("");
            // <span class="score" id="score-1F602">1872748264</span>
            try (BufferedReader in = FileUtilities.openFile(GenerateEmojiFrequency.class, "emojitracker.txt")) {
                String lastBuffer = "";
                double factor = 0;

                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    line = lastBuffer+line;
                    m.reset(line);
                    int pos = 0;

                    while (true) {
                        boolean found = m.find(pos);
                        if (!found) break;
                        int cp = Integer.parseInt(m.group(1),16);
                        String str = UTF16.valueOf(cp);
                        long count = Long.parseLong(m.group(2));
                        if (factor == 0) {
                            factor = 1_000_000_000.0/count;
                        }
                        _counts.add(normalizeEmoji(str), count);
                        pos = m.end();
                    }
                    lastBuffer = line.substring(pos);
                }
                countInfo = new CountInfo(_counts);
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
    }

    static class Twitter {
        static CountInfo countInfo;
        static {
            Counter<String> _counts = new Counter<>();

            try (BufferedReader in = FileUtilities.openFile("/Users/markdavis/Google Drive/workspace/DATA/frequency/emoji/", "twitterRaw.tsv")) {
                int lineCount = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    ++lineCount;
                    String[] parts = line.split("\t");
                    String rawCodes = parts[0];
                    String codes = normalizeEmoji(rawCodes);
                    long count = Long.parseLong(parts[2].replace(",",""));
                    _counts.add(codes, count);
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
            countInfo = new CountInfo(_counts);
        }
    }

    static class Facebook {
        static CountInfo countInfo;
        static {
            Counter<String> _counts = new Counter<>();

            int lineCount = 0;
            try (BufferedReader in = FileUtilities.openFile("/Users/markdavis/Google Drive/workspace/DATA/frequency/emoji/", "facebookRaw.tsv")) {
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    ++lineCount;
                    String[] parts = line.split("\t");
                    String hexCodes = parts[1];
                    String codes = normalizeHexEmoji(hexCodes);
                    long count = Math.round(Double.parseDouble(parts[2].replace(",","")));
                    _counts.add(codes, count);
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException("Bad hex at " + lineCount, e);
            }
            countInfo = new CountInfo(_counts);
        }
    }

    private static String normalizeEmoji(String rawCodes) {
        // remove skin tones
        if (SKIP.containsSome(rawCodes)) {
            return "";
        }

        String result1 = EmojiData.EMOJI_DATA.MODIFIERS.stripFrom(rawCodes, true);
        // remove gender
        if (result1.contains("\u2642")) {
            int debug = 0;
        }
        String result = stripFrom(Emoji.ZWJ_GENDER_MARKERS, result1, true);
        if (!result.equals(result1)) {
            int debug = 0;
        }
        Category cat = Category.getBucket(result);
        if (cat == Category.zwj_seq_role) {
            if (result.startsWith(Emoji.MAN_STR)) {
                result = Emoji.MAN_STR + result.substring(Emoji.MAN_STR.length());
            }
        }
        if (result.isEmpty()) {
            int debug = 0;
        }
        return EmojiData.EMOJI_DATA.addEmojiVariants(result);
    }

    public static String stripFrom(UnicodeSet uset, CharSequence source, boolean matches) {
        StringBuilder result = new StringBuilder(); // could optimize to only allocate when needed
        SpanCondition toKeep = matches ? SpanCondition.NOT_CONTAINED : SpanCondition.CONTAINED;
        SpanCondition toSkip = matches ? SpanCondition.CONTAINED : SpanCondition.NOT_CONTAINED;
        for (int pos = 0; pos < source.length();) {
            int inside = uset.span(source, pos, toKeep);
            result.append(source.subSequence(pos, inside));
            pos = uset.span(source, inside, toSkip); // get next start
        }
        return result.toString();
    }

    private static String normalizeHexEmoji(String rawCodes) {
        if (rawCodes.startsWith("\\x{") && rawCodes.endsWith("}")) {
            rawCodes = rawCodes.substring(3, rawCodes.length()-1);
        }
        // hack
        String[] parts = rawCodes.split("\\s+");
        if (parts[0].length() == 1) {
            parts[0] = Utility.hex(parts[0]);
            rawCodes = CollectionUtilities.join(parts, " ");
        } else if (parts[0].startsWith("\\X")) {
            parts[0] = parts[0].substring(2);
            rawCodes = CollectionUtilities.join(parts, " ");
        }
        if (rawCodes.contains("1F647")) {
            int debug = 0;
        }
        return normalizeEmoji(Utility.fromHex(rawCodes, false, 2));
    }
}
