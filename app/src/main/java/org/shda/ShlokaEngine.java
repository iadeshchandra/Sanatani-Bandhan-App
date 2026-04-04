package org.shda;

import java.util.Calendar;

public class ShlokaEngine {
    private static final String[] SHLOKAS = {
        "“Dharmo Rakshati Rakshitah”\n(Dharma protects those who protect it) - Manu Smriti",
        "“You have the right to work, but for the work's sake only.”\n- Bhagavad Gita 2.47",
        "“Truth is one, sages call it by various names.”\n- Rig Veda",
        "“Arise, awake, and stop not till the goal is reached.”\n- Katha Upanishad",
        "“Whenever dharma declines and the purpose of life is forgotten, I manifest myself.”\n- Bhagavad Gita 4.7",
        "“He who has no attachments can really love others.”\n- Swami Vivekananda",
        "“A man is made by his belief. As he believes, so he is.”\n- Bhagavad Gita",
        "“The mind is everything. What you think you become.”\n- Upanishads",
        "“Let noble thoughts come to us from every side.”\n- Rig Veda 1.89.1",
        "“There is nothing purifying on earth as knowledge.”\n- Bhagavad Gita 4.38"
    };

    // Returns a dynamic Shloka that changes every single day based on the calendar
    public static String getDailyShloka() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return SHLOKAS[dayOfYear % SHLOKAS.length];
    }
}
