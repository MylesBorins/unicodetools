package org.unicode.text.tools;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.CountryCodeConverter;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class EmojiAnnotations extends Birelation<String,String> {

    static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("the", "of", "for", "a", "and", "state", 
            "c�te", "verde▪cape", "dhekelia", "akrotiri", "comros", "pdr", "jamahiriya", "part",
            "yugoslav", "tfyr", "autonomous", "rawanda", "da", "rb", "yugoslavia",
            "states", "sar", "people's", "minor",
            "sts."));

    public EmojiAnnotations(Comparator codepointCompare, String filename) {
        super(new TreeMap(codepointCompare), 
                new TreeMap(codepointCompare), 
                TreeSet.class, 
                TreeSet.class, 
                codepointCompare, 
                codepointCompare);

        Output<Set<String>> lastLabel = new Output(new TreeSet<String>(codepointCompare));
        for (String line : FileUtilities.in(EmojiAnnotations.class, filename)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.contains("closed")) {
                int debug = 0;
            }
            line = Emoji.getLabelFromLine(lastLabel, line);
            for (int i = 0; i < line.length();) {
                String string = Emoji.getEmojiSequence(line, i);
                if (Emoji.ASCII_LETTERS.containsSome(string)) {
                    throw new IllegalArgumentException("Strange line with ASCII emoji: " + line);
                }
                i += string.length();
                if (Emoji.skipEmojiSequence(string)) {
                    continue;
                }
                for (String item : lastLabel.value) {
                    add(item, string);
                }
            }
        }
        //        for (String s : FITZ_MINIMAL) {
        //            ANNOTATIONS_TO_CHARS.add("fitz-minimal", s);
        //        }
        //        for (String s : FITZ_OPTIONAL) {
        //            ANNOTATIONS_TO_CHARS.add("fitz-optional", s);
        //        }
        // for programmatic additions, take this and modify
        //        for (String s : Emoji.EMOJI_CHARS) {
        //            String charName = UCharacter.getName(s.codePointAt(0));
        //            if (charName.contains("MARK")) {
        //                ANNOTATIONS_TO_CHARS.add("mark", s);
        //            }
        //        }
        for (int cp1 = Emoji.FIRST_REGIONAL; cp1 <= Emoji.LAST_REGIONAL; ++cp1) {
            for (int cp2 = Emoji.FIRST_REGIONAL; cp2 <= Emoji.LAST_REGIONAL; ++cp2) {
                String emoji = new StringBuilder().appendCodePoint(cp1).appendCodePoint(cp2).toString();
                if (Emoji.EMOJI_CHARS.contains(emoji)) {
                    add("flag", emoji);
                }
                //String regionCode = GenerateEmoji.getFlagCode(emoji);
            }
        }
        // get extra names
        for (String name : CountryCodeConverter.names()) {
            String regionCode = CountryCodeConverter.getCodeFromName(name);
            if (regionCode == null || regionCode.length() != 2) {
                continue;
            }
            if (regionCode.equals("RS") 
                    && name.contains("montenegro")) {
                continue;
            }
            String emoji = Emoji.getEmojiFromRegionCode(regionCode);
            //System.out.println(regionCode + "=>" + name);
            addParts(emoji, name);
        }
        freeze();
        UnicodeSet annotationCharacters = new UnicodeSet().addAll(valuesSet());
        if (!annotationCharacters.containsAll(Emoji.EMOJI_CHARS)) {
            UnicodeSet missing = new UnicodeSet().addAll(Emoji.EMOJI_CHARS).removeAll(annotationCharacters);
            throw new IllegalArgumentException("Missing annotations: " + missing.toPattern(false));
        }
    }

    public void addParts(String emoji, String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        for (String namePart : name.split("[- ,&\\(\\)]+")) {
            if (STOP_WORDS.contains(namePart)) {
                continue;
            }
            if (namePart.startsWith("d’") || namePart.startsWith("d'")) {
                namePart = namePart.substring(2);
            }
            add(namePart, emoji);
        }
    }
}