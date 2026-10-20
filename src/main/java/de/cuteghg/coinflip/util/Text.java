package de.cuteghg.coinflip.util;

public final class Text {
    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return input.replace('&', '§');
    }
}
