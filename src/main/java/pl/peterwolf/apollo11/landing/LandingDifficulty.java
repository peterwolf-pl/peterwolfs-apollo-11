package pl.peterwolf.apollo11.landing;

import java.util.Locale;

public enum LandingDifficulty {
    EASY,
    MID,
    PROFESSIONAL;

    public static LandingDifficulty parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
