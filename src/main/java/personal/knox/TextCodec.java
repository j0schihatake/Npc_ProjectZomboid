package personal.knox;

import java.text.Normalizer;
import java.util.Locale;

/** Display-safe text for the bitmap chat font, which has no Cyrillic glyphs. */
public final class TextCodec {
    private static final String RU = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String EN = "ABVGDEYOZHZIYKLMNOPRSTUFHTSCHSHYEEYUAabvgdeyozhziyklmnoprstufhtschshyeyua";
    // Explicit mapping is safer than index maths for the mixed case alphabet.
    private static final String[] PAIRS = {
            "А","A","Б","B","В","V","Г","G","Д","D","Е","E","Ё","Yo","Ж","Zh","З","Z","И","I","Й","Y",
            "К","K","Л","L","М","M","Н","N","О","O","П","P","Р","R","С","S","Т","T","У","U","Ф","F",
            "Х","H","Ц","Ts","Ч","Ch","Ш","Sh","Щ","Sch","Ъ","","Ы","Y","Ь","","Э","E","Ю","Yu","Я","Ya",
            "а","a","б","b","в","v","г","g","д","d","е","e","ё","yo","ж","zh","з","z","и","i","й","y",
            "к","k","л","l","м","m","н","n","о","o","п","p","р","r","с","s","т","t","у","u","ф","f",
            "х","h","ц","ts","ч","ch","ш","sh","щ","sch","ъ","","ы","y","ь","","э","e","ю","yu","я","ya"
    };

    private TextCodec() {}

    public static String normalize(String text) {
        if (text == null) return "";
        String value = Normalizer.normalize(text, Normalizer.Form.NFC);
        if (value.contains("Ð") || value.contains("Ñ") || value.contains("â€")) {
            try {
                String repaired = new String(value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                        java.nio.charset.StandardCharsets.UTF_8);
                if (cyrillic(repaired) > cyrillic(value)) value = repaired;
            } catch (RuntimeException ignored) { }
        }
        return value;
    }

    public static String forChat(String text) {
        String value = normalize(text);
        var out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String mapped = null;
            for (int p = 0; p < PAIRS.length; p += 2) {
                if (PAIRS[p].charAt(0) == c) { mapped = PAIRS[p + 1]; break; }
            }
            out.append(mapped == null ? String.valueOf(c) : mapped);
        }
        return out.toString();
    }

    private static int cyrillic(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) if (Character.UnicodeBlock.of(value.charAt(i)) == Character.UnicodeBlock.CYRILLIC) count++;
        return count;
    }
}
