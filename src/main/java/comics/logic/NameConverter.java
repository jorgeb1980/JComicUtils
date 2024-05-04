package comics.logic;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

public class NameConverter {

    private String replaceNumbers(String baseValue, String patternString, String replacementString) {
        var pattern = compile(patternString);
        var matcher = pattern.matcher(baseValue);
        if (matcher.find()) return matcher.replaceFirst(format(replacementString, matcher.group(1), matcher.group(2)));
        else return baseValue;
    }

    private String checkForHyphen(String name, String searchPattern) {
        var ret = name;
        if (compile("[^-] " + searchPattern).matcher(name).find()) {
            var matcher = compile(searchPattern).matcher(name);
            if (matcher.find()) ret = matcher.replaceFirst("- " + matcher.group(1));
        }
        return ret;
    }

    public String normalizeFileName(String fileName) {
        var name = fileName;
        var extension = "";
        if (fileName.contains(".")) {
            int pointIndex = fileName.lastIndexOf('.');
            name = fileName.substring(0, pointIndex).trim();
            extension = "." + fileName.substring(pointIndex + 1).trim().toLowerCase();
        }
        // Remove everything between [], ()
        name = name.replaceAll("\\[[^\\]]*\\]", "").replaceAll("\\([^\\)]*\\)", "");
        // Remove excess blank characters
        name = name.replaceAll("\\s{2,}", " ");
        // Deal with caps
        name = name.toLowerCase();
        name = Arrays.stream(name.replaceAll("\\.", " ").split("\\s+"))
            // Quite unintended - however lib-cli-base brings it
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" ")).trim();
        // Deal with numbers
        // Is this a collection with a limited length (like in: each item has 'X of Y' at the end of the file name)?
        var isLimited = compile("(\\d+) (De|Of) (\\d+)$").matcher(name).find();
        if (isLimited) {
            // Look for x de y, x of y
            name = replaceNumbers(name, "(\\d+) [Dd][Ee] (\\d+)$", "%s de %s");
            name = replaceNumbers(name, "(\\d+) [Oo][Ff] (\\d+)$", "%s of %s");
            // Is there a ' - ' in front of the 'x of y'?
            name = checkForHyphen(name, "(\\d+ de \\d+)$");
            name = checkForHyphen(name, "(\\d+ of \\d+)$");
        } else {
            // Remove '#' in the numbers
            var numberSignMatcher = compile("#(\\d+)$").matcher(name);
            if (numberSignMatcher.find()) {
                name = numberSignMatcher.replaceFirst(numberSignMatcher.group(1));
            }
            // make it sure there is a ' - ' in front of the number
            name = checkForHyphen(name, "(\\d+)$");
        }

        return name + extension;
    }
}
