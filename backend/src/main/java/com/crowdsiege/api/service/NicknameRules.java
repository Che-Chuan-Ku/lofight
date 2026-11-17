package com.crowdsiege.api.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The single nickname comparison rule shared by the rename uniqueness check and the donation matcher:
 * NFKC, strip, then lower-case (so full-width "Ｂｏｂ" equals "Bob"). Also the rule for characters a nickname may not
 * contain, which keeps look-alike names (zero-width characters, non-breaking spaces) from dodging the comparison.
 */
public final class NicknameRules {

    private NicknameRules() {
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT);
    }

    /**
     * True when the text holds a control (Cc), format (Cf, incl. U+200B/C/D and U+FEFF), line / paragraph separator
     * (Zl, Zp), any space separator (Zs) other than a plain U+0020 (incl. U+00A0, U+3000), or a private-use
     * code point.
     */
    public static boolean hasIllegalCharacter(String value) {
        return value.codePoints().anyMatch(cp -> {
            int type = Character.getType(cp);
            if (type == Character.SPACE_SEPARATOR) {
                return cp != ' ';
            }
            return type == Character.CONTROL || type == Character.FORMAT || type == Character.LINE_SEPARATOR
                    || type == Character.PARAGRAPH_SEPARATOR
                    || type == Character.PRIVATE_USE || type == Character.SURROGATE;
        });
    }
}
