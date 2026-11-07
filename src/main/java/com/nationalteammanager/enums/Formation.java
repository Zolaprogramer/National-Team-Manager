package com.nationalteammanager.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formacija se opisuje brojem igrača po linijama, a raspored na terenu se iz toga izračunava.
 * Pozicija s indeksom 0 je uvijek golmanska.
 */
public enum Formation {
    F_4_3_3("4-3-3", 4, 3, 3),
    F_4_4_2("4-4-2", 4, 4, 2),
    F_3_5_2("3-5-2", 3, 5, 2),
    F_4_2_3_1("4-2-3-1", 4, 2, 3, 1),
    F_3_4_3("3-4-3", 3, 4, 3),
    F_5_3_2("5-3-2", 5, 3, 2);

    public static final int GOALKEEPER_SLOT = 0;

    private static final double GOALKEEPER_Y = 90;
    private static final double OUTFIELD_HEIGHT = 79;

    private final String label;
    private final List<FormationSlot> slots;

    Formation(String label, int... playersPerLine) {
        this.label = label;
        this.slots = buildSlots(playersPerLine);
    }

    private static List<FormationSlot> buildSlots(int[] playersPerLine) {
        List<FormationSlot> slots = new ArrayList<>();
        slots.add(new FormationSlot(GOALKEEPER_SLOT, PlayerPosition.GOALKEEPER.getShortLabel(), 50, GOALKEEPER_Y));

        int lines = playersPerLine.length;
        for (int line = 0; line < lines; line++) {
            double y = GOALKEEPER_Y - (line + 1) * OUTFIELD_HEIGHT / lines;
            for (int player = 0; player < playersPerLine[line]; player++) {
                double x = (player + 1) * 100.0 / (playersPerLine[line] + 1);
                slots.add(new FormationSlot(slots.size(), lineLabel(line, lines), x, y));
            }
        }
        return List.copyOf(slots);
    }

    private static String lineLabel(int line, int lines) {
        if (line == 0) {
            return PlayerPosition.DEFENDER.getShortLabel();
        }
        return line == lines - 1 ? PlayerPosition.FORWARD.getShortLabel() : PlayerPosition.MIDFIELDER.getShortLabel();
    }

    /** Raspored pozicija po formaciji, koji JavaScript koristi za crtanje terena. */
    public static Map<String, List<FormationSlot>> slotsByName() {
        return Arrays.stream(values()).collect(Collectors.toMap(Formation::name, Formation::getSlots));
    }

    public String getLabel() {
        return label;
    }

    public List<FormationSlot> getSlots() {
        return slots;
    }
}
