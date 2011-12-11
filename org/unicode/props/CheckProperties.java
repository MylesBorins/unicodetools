package org.unicode.props;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Timer;
import org.unicode.props.IndexUnicodeProperties.PropertyParsingInfo;
import org.unicode.props.PropertyNames.NameMatcher;
import org.unicode.props.PropertyNames.PropertyType;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.Tabber.MonoTabber;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeMap.EntryRange;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodeProperty.UnicodeMapProperty;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CheckProperties {
    static LinkedHashSet<String> PROPNAMEDIFFERENCES = new LinkedHashSet<String>();
    static LinkedHashSet<String> SKIPPING = new LinkedHashSet<String>();
    static LinkedHashSet<String> NOT_IN_ICU = new LinkedHashSet<String>();

    enum Action {SHOW, COMPARE, CHECK, EMPTY, INFO, SPACES, DETAILS}
    enum Extent {SOME, ALL}

    public static void main(String[] args) throws Exception {
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        EnumSet<UcdProperty> properties = EnumSet.noneOf(UcdProperty.class);
        Extent extent = null;
        for (String arg : args) {
            try {
                actions.add(Action.valueOf(arg.toUpperCase()));
                continue;
            } catch (Exception e) {}
            try {
                extent = Extent.valueOf(arg.toUpperCase());
                continue;
            } catch (Exception e) {}
            try {
                properties.add(UcdProperty.forString(arg));
                continue;
            } catch (Exception e) {}
            throw new IllegalArgumentException(arg);
        }
        if (actions.size() == 0) actions = EnumSet.of(Action.CHECK);

        Timer total = new Timer();
        for (Entry<String, PropertyParsingInfo> entry : IndexUnicodeProperties.getFile2PropertyInfoSet().keyValueSet()) {
            if (IndexUnicodeProperties.SHOW_PROP_INFO) System.out.println(entry.getKey() + " ; " + entry.getValue());
        }
        IndexUnicodeProperties last = IndexUnicodeProperties.make("6.0.0");
        UnicodeMap<String> gcLast = showValue(last, UcdProperty.General_Category, '\u00A7');
        //        showValue(last, UcdProperty.kMandarin, '\u5427');
        //        showValue(last, UcdProperty.General_Category, '\u5427');

        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        //        showValue(latest, UcdProperty.General_Category, '\u00A7');
        //        showValue(latest, UcdProperty.kMandarin, '\u5427');

        UnicodeSet ignore = new UnicodeSet();
        addAll(ignore, gcLast.getSet(null)); // separate for debugging
        addAll(ignore, gcLast.getSet(PropertyValues.General_Category_Values.Unassigned.toString()));
        addAll(ignore, gcLast.getSet(PropertyValues.General_Category_Values.Private_Use.toString()));
        addAll(ignore, gcLast.getSet(PropertyValues.General_Category_Values.Surrogate.toString()));
        //addAll(ignore, gcLast.getSet("Cc"));

        UnicodeSet retain = new UnicodeSet(ignore).complement().freeze();

        //        compare(UcdProperty.General_Category, last, latest, retain);
        //
        //        latest.show(UcdProperty.General_Category);

        List<UcdProperty> values = 
            extent == null ? new ArrayList(properties)
        : extent == Extent.ALL ? Arrays.asList(UcdProperty.values()) 
                : Arrays.asList(
                        //UcdProperty.General_Category,
                        UcdProperty.CJK_Radical,
                        UcdProperty.Indic_Matra_Category,
                        UcdProperty.Indic_Syllabic_Category,
                        UcdProperty.Jamo_Short_Name
                        // Bidi_Mirroring_Glyph
                        //                    UcdProperty.CJK_Radical, 
                        //                    UcdProperty.Script_Extensions,
                        //                    UcdProperty.Emoji_DoCoMo,
                        //                    UcdProperty.Emoji_KDDI,
                        //                    UcdProperty.Emoji_SoftBank,
                        //                    UcdProperty.Name_Alias_Prov,
                        //                    UcdProperty.Named_Sequences,
                        //                    UcdProperty.Named_Sequences_Prov
                );
        for (Action action : actions) {
            switch(action) { 
            case SHOW:
                for (UcdProperty prop : values) {
                    show(latest, prop, actions.contains(Action.SPACES), false);
                }
                break;
            case SPACES:
                break;
            case DETAILS:
                for (UcdProperty prop : values) {
                    show(latest, prop, actions.contains(Action.SPACES), true);
                }
                break;
            case COMPARE:
                for (UcdProperty prop : values) {
                    compare(prop, last, latest, retain);
                }
                break;
            case CHECK:
                System.out.println("Property\tICU-Value\tDirect-Value\tChars-Affected");
                for (UcdProperty prop : values) {
                    compareICU(prop, last);
                }
                break;
            case EMPTY:
                for (UcdProperty prop : values) {
                    checkEmpty(latest, prop);
                }
                break;
            case INFO:
                Tabber tabber = new Tabber.MonoTabber()
                .add(30, MonoTabber.LEFT)
                .add(30, MonoTabber.LEFT)
                .add(30, MonoTabber.LEFT);
                Relation<String, String> sorted = Relation.of(new TreeMap<String,Set<String>>(), LinkedHashSet.class);
                Set<UcdProperty> missingRegex = EnumSet.noneOf(UcdProperty.class);
                for (UcdProperty prop : UcdProperty.values()) {
                    PropertyParsingInfo propInfo = IndexUnicodeProperties.getPropertyInfo(prop);
                    if (propInfo.originalRegex == null) {
                        continue;
                    }
                    String line = tabber.process(propInfo.property + " ;\t" + propInfo.multivalued + " ;\t" + propInfo.originalRegex);
                    sorted.put(propInfo.originalRegex, line);
                }
                for (Entry<String, String> regexLine : sorted.keyValueSet()) {
                    System.out.println(regexLine.getValue());
                }

                for (UcdProperty prop : UcdProperty.values()) {
                    PropertyParsingInfo propInfo = IndexUnicodeProperties.getPropertyInfo(prop);
                    System.out.println(propInfo);
                    if (propInfo.regex == null || !propInfo.emptyValue.isEmpty())
                        switch (prop.getType()) {
                        case Binary: case Catalog: case Enumerated: break;
                        default: missingRegex.add(prop);
                        }
                }
                System.out.println("\nMissing Regex");
                for (UcdProperty prop : missingRegex) {
                    PropertyParsingInfo propInfo = IndexUnicodeProperties.getPropertyInfo(prop);
                    System.out.println(
                            prop + " ;\t"
                            + propInfo.multivalued + " ;\t"
                            + propInfo.regex
                    );
                }
                break;
            }
        }
        System.out.println("Property Enum Canonical Form wrong");
        for (String s : PROPNAMEDIFFERENCES) {
            System.out.println("\t" + s);
        }

        System.out.println("No Differences");
        for (String s : SKIPPING) {
            System.out.println("\t" + s);
        }

        System.out.println("Not In ICU");
        for (String s : NOT_IN_ICU) {
            System.out.println("\t" + s);
        }

        for (Entry<UcdProperty, Set<String>> s : IndexUnicodeProperties.DATA_LOADING_ERRORS.keyValuesSet()) {
            System.out.println("\t" + s.getKey());   
            int max = 100;
            for (String value : s.getValue()) {
                System.out.println("\t\t" + value);   
                if (--max < 0) {
                    System.out.println("…");
                    break;
                }
            }
        }
        Set<String> latestFiles = latest.fileNames;
        File dir = new File("/Users/markdavis/Documents/workspace/DATA/UCD/6.1.0-Update");
        checkFiles(latestFiles, dir);
        total.stop();
        System.out.println(total.toString());
    }

    public static void checkEmpty(IndexUnicodeProperties latest, UcdProperty prop) {
        UnicodeMap<String> map = latest.load(prop);
        String defaultValue = IndexUnicodeProperties.getDefaultValue(prop);
        UnicodeSet nullElements = map.getSet(null);

        UnicodeSet empty = map.getSet("");
        if (defaultValue.isEmpty()) {
            empty.addAll(nullElements);
        }
        if (empty.size() != 0) {
            System.out.println("Empty: " + prop + "\t" + abbreviate(empty, 100, false));
        }

        final String no_value_constant = IndexUnicodeProperties.SpecialValue.NO_VALUE.toString();
        UnicodeSet no_value = map.getSet(no_value_constant);
        if (no_value_constant.equals(defaultValue)) {
            no_value.addAll(nullElements);
        }
        if (no_value.size() != 0) {
            System.out.println("No_Value: " + prop + "\t" + abbreviate(no_value, 100, false));
        }
        //        if (nullElements.size() != 0 && (defaultValue == null || defaultValue.equals(no_value_constant) || defaultValue.isEmpty())) {
        //            System.out.println("Null: " + prop + "\t" + defaultValue + "\t" + abbreviate(nullElements, 100, false));
        //        }
    }

    private static void compareICU(UcdProperty prop, IndexUnicodeProperties direct) {
        ICUPropertyFactory propFactory = ICUPropertyFactory.make();
        UnicodeProperty icuProp = propFactory.getProperty(prop.toString());
        if (icuProp == null) {
            NOT_IN_ICU.add(prop.toString());
            return;
        }
        final UnicodeMap<String> icuMap = icuProp.getUnicodeMap();
        UnicodeMap<String> directMap = direct.load(prop);
        showChanges(prop, new UnicodeSet("[^[:cn:][:co:][:cs:]]"), null, icuMap, direct, directMap);
    }

    private static void addAll(UnicodeSet toSet, UnicodeSet set) {
        if (set.contains('\u5427')) {
            int y = 3;
        }
        toSet.addAll(set);
    }

    public static UnicodeMap<String> showValue(IndexUnicodeProperties last, UcdProperty ucdProperty, int codePoint) {
        UnicodeMap<String> gcLast = last.load(ucdProperty);
        System.out.println(last.ucdVersion + ", " + ucdProperty + "(" + Utility.hex(codePoint) + ")=" + gcLast.get(codePoint));
        return gcLast;
    }

    public static void checkFiles(Set<String> latestFiles, File dir) throws IOException {
        for (File file : dir.listFiles()) {
            String canonical = file.getCanonicalPath();
            if (file.isDirectory()) {
                checkFiles(latestFiles, file);
                continue;
            } else {
                final String fileName = file.toString();
                if (latestFiles.contains(canonical) 
                        || !canonical.endsWith(".txt") 
                        || fileName.contains("Test")
                        || fileName.contains("NamesList")
                        || fileName.contains("NormalizationCorrections")
                        || fileName.contains("PropertyValueAliases")
                        || fileName.contains("PropertyAliases")
                        || fileName.contains("ReadMe")
                        || fileName.contains("Index")
                        || fileName.contains("Derived")
                ) {
                    continue;
                }
            }
            System.out.println("Not read for properties: " + file);
        }
    }

    private static void compare(UcdProperty prop, IndexUnicodeProperties last, IndexUnicodeProperties latest, UnicodeSet retain) {
        UnicodeMap<String> lastMap = last.load(prop);
        UnicodeMap<String> latestMap = latest.load(prop);
        showChanges(prop, retain, last, lastMap, latest, latestMap);
    }

    public static void showChanges(UcdProperty prop, UnicodeSet retain, 
            IndexUnicodeProperties last, UnicodeMap<String> lastMap, 
            IndexUnicodeProperties latest, UnicodeMap<String> latestMap) {
        // TODO handle strings in maps
        UnicodeMap<String> changes = new UnicodeMap<String>();
        for (UnicodeSetIterator it = new UnicodeSetIterator(retain); it.next();) {
            String lastValue = lastMap.get(it.codepoint);
            String latestValue = latestMap.get(it.codepoint);
            captureChanges(prop, it.codepoint, last, lastValue, latest, latestValue, changes);
        }
        if (changes.size() == 0) {
            SKIPPING.add(prop.toString());
            return;
        }
        int limit = 30;
        for (String value : new TreeSet<String>(changes.values())) {
            final UnicodeSet chars = changes.getSet(value);
            System.out.println(prop + "\t" + value 
                    //+ "\t" + FIX_INVISIBLES.transform(chars.toPattern(false))
                    + "\t" + abbreviate(chars, 50, false));
            if (--limit < 0) {
                System.out.println("\t\tand more");
                break;
            }
        }
    }

    public static void captureChanges(UcdProperty prop, int codepoint, 
            IndexUnicodeProperties last, String lastValue, IndexUnicodeProperties latest, String latestValue, UnicodeMap<String> changes) {
        lastValue = IndexUnicodeProperties.getResolvedValue(last, prop, codepoint, lastValue);
        latestValue = IndexUnicodeProperties.getResolvedValue(latest, prop, codepoint, latestValue);
        if (UnicodeProperty.equals(lastValue, latestValue)) {
            return;
        }
        switch (prop.getType()) {
        case Numeric:
            if (approximatelyEqual(numericValue(lastValue), numericValue(latestValue), 0.0000001d)) {
                return;
            }
            break;
        case Catalog: case Enumerated: 
            if (PropertyNames.NameMatcher.matches(lastValue, latestValue)) {
                PROPNAMEDIFFERENCES.add(prop + "\t" + abbreviate(lastValue, 50, true) + "\t≠\t" + abbreviate(latestValue, 50, true));
                return;
            }
            break;

        }
        changes.put(codepoint, abbreviate(lastValue, 50, true) + "\t≠\t" + abbreviate(latestValue, 50, true));
    }

    private static Double numericValue(String a) {
        int slashPos = a.indexOf('/');
        if (slashPos >= 0) {
            return Double.parseDouble(a.substring(0,slashPos)) / Double.parseDouble(a.substring(slashPos+1));
        }
        return Double.parseDouble(a);
    }

    private static boolean approximatelyEqual(Double a, Double b, Double epsilon) {
        if (a == b) return true;
        return (a >= b - epsilon || a <= b + epsilon);
    }

    public static String getDisplayValue(String value) {
        return (value == null || value.isEmpty()) ? "∅" : value;
    }

    static final Transliterator FIX_INVISIBLES = Transliterator.createFromRules("ID", "([[:c:][:di:]]) > ❮&hex/plain($1)❯ ;", Transliterator.FORWARD);
    static final Transliterator FIX_NON_ASCII8 = Transliterator.createFromRules("ID", 
            "([[:c:][:di:]]) > ❮&hex/plain($1)❯ ;" +
            "([^\\u0000-\\u00FF]) > $1❮&hex/plain($1)❯ ;",
            Transliterator.FORWARD);

    public static String abbreviate(UnicodeSet chars, int maxLength, boolean showNonAscii) {
        return abbreviate(chars.toPattern(false), maxLength, showNonAscii);
    }

    public static String abbreviate(String charString, int maxLength, boolean showNonAscii) {
        charString = getDisplayValue(charString);
        if (charString.length() > maxLength) {
            charString = charString.substring(0,maxLength) + "…";
        }
        if (showNonAscii) {
            String alt = FIX_NON_ASCII8.transform(charString);
            if (!alt.equals(charString)) {
                charString = alt;
            }
        }
        return charString;
    }

    public static void show(IndexUnicodeProperties iup, UcdProperty prop, boolean onlySpaces, boolean details) {
        Timer timer = new Timer();
        System.out.println(prop);
        timer.start();
        UnicodeMap<String> map = iup.load(prop);
        timer.stop();
        Collection<String> values = map.values();
        if (onlySpaces) {
            LinkedHashSet<String> spaceValues = new LinkedHashSet();
            for (String value : values) {
                if (value.contains(" ")) {
                    spaceValues.add(value);
                }
            }
            if (spaceValues.size() == 0) {
                return;
            }
            values = spaceValues;
        }
        String sample = abbreviate(values.toString(), 150, false);
        System.out.println(prop + "\ttime:\t" + timer.getDuration() + "\tcodepoints:\t" + map.size() + "\tvalues:\t" + values.size() + "\tsample:\t" + sample);
        if (details) {
            //            UnicodeMapProperty ump = new UnicodeMapProperty().set(map);
            //            ump.addName(prop.toString());
            //            BagFormatter bf = new BagFormatter()
            //            .setMergeRanges(true)
            //            .setValueSource(ump)
            //            .setLabelSource(null)
            //            .setNameSource(null)
            //            ;
            //            PrintWriter out = new PrintWriter(System.out);
            //            bf.showSetNames(out, map.keySet());
            //            out.flush();
            int maxCodepointLength = 0;
            List<R2<String, String>> list = new ArrayList<R2<String, String>>();
            String defaultValue = IndexUnicodeProperties.getDefaultValue(prop);
            for (EntryRange<String> entryRange : map.entryRanges()) {
                if (entryRange.value == defaultValue) {
                    continue;
                }
                String codepoints = null;
                if (entryRange.string != null) {
                    codepoints = Utility.hex(entryRange.string);
                } else {
                    codepoints = Utility.hex(entryRange.codepoint);
                    if (entryRange.codepoint != entryRange.codepointEnd) {
                        codepoints = codepoints + ".." + Utility.hex(entryRange.codepointEnd);
                    }
                }
                maxCodepointLength = Math.max(maxCodepointLength, codepoints.length());
                String value = entryRange.value;
                Enum item = prop.getEnum(value);
                if (item != null) {
                    NameMatcher x = PropertyNames.getNameToEnums(item.getClass());
                    PropertyNames enumNames = x.getNames();
                    value = enumNames.getShortName();
                }
                R2<String, String> row = Row.of(codepoints, value);
                list.add(row);
            }
            final String shortName = prop.getNames().getShortName();
            System.out.println("# @missing 0000..10FFFF; " + shortName + "; " + defaultValue);
            for (R2<String, String> entry : list) {
                final String codepoints = entry.get0();
                System.out.println(
                        codepoints
                        + "; "
                        + Utility.repeat(" ", maxCodepointLength - codepoints.length())
                        + shortName
                        + "; "
                        + entry.get1());
            }
        }
        //        for (String value : map.getAvailableValues()) {
        //            System.out.println("\t" + value + " " + map.getSet(value));
        //        }
    }
//    public PropertyNames getEnumNames() {
//        return name2enum == null ? null : name2enum.getNames();
//    }

}
