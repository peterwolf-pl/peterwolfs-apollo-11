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

    public LandingOutcome resolveLanding(boolean manualBurnApplied) {
        return switch (this) {
            case EASY -> LandingOutcome.AUTOPILOT;
            case MID -> manualBurnApplied ? LandingOutcome.ASSISTED_MANUAL : LandingOutcome.ASSISTED_AUTOPILOT;
            case PROFESSIONAL -> manualBurnApplied ? LandingOutcome.MANUAL : LandingOutcome.ABORTED;
        };
    }

    public enum LandingOutcome {
        AUTOPILOT,
        ASSISTED_AUTOPILOT,
        ASSISTED_MANUAL,
        MANUAL,
        ABORTED
    }
}
