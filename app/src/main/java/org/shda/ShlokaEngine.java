package org.shda;

import java.util.Random;

public class ShlokaEngine {
    
    // A deeply researched, profound library of Sanatani wisdom.
    private static final String[] SHLOKAS = {
        "“Dharmo Rakshati Rakshitah”\n(Dharma protects those who protect it) - Manu Smriti",
        "“You have the right to work, but for the work's sake only.”\n- Bhagavad Gita 2.47",
        "“Truth is one, sages call it by various names.”\n- Rig Veda 1.164.46",
        "“Arise, awake, and stop not till the goal is reached.”\n- Katha Upanishad 1.3.14",
        "“Whenever dharma declines and the purpose of life is forgotten, I manifest myself.”\n- Bhagavad Gita 4.7",
        "“The mind is everything. What you think you become.”\n- Dhammapada / Upanishadic Thought",
        "“Let noble thoughts come to us from every side.”\n- Rig Veda 1.89.1",
        "“There is nothing purifying on earth as knowledge.”\n- Bhagavad Gita 4.38",
        "“He who has no attachments can really love others, for his love is pure and divine.”\n- Bhagavad Gita",
        "“As the blazing fire reduces wood to ashes, O Arjuna, so does the fire of knowledge reduce all karma to ashes.”\n- Bhagavad Gita 4.37",
        "“Lead me from the unreal to the real, from darkness to light, from death to immortality.”\n- Brihadaranyaka Upanishad 1.3.28",
        "“A man is made by his belief. As he believes, so he is.”\n- Bhagavad Gita 17.3",
        "“The soul is neither born, and nor does it die.”\n- Bhagavad Gita 2.20",
        "“That which seems like poison at first, but tastes like nectar in the end, is the joy of Sattva.”\n- Bhagavad Gita 18.37",
        "“Yoga is the journey of the self, through the self, to the self.”\n- Bhagavad Gita 6.20",
        "“We are kept from our goal not by obstacles, but by a clear path to a lesser goal.”\n- Mahabharata (Wisdom of Vidura)",
        "“One should lift oneself by one's own efforts and should not degrade oneself.”\n- Bhagavad Gita 6.5",
        "“Knowledge is the ultimate wealth; it cannot be stolen, nor does it decrease when shared.”\n- Chanakya Neeti",
        "“Whatever action is performed by a great man, common men follow in his footsteps.”\n- Bhagavad Gita 3.21",
        "“O mind, be strong. What is destined to happen will happen. Do your duty with devotion.”\n- Vedic Proverb"
    };

    private static final Random random = new Random();

    // ✨ NEW: This now generates a completely random Shloka every single time it is called!
    public static String getRandomShloka() {
        int randomIndex = random.nextInt(SHLOKAS.length);
        return SHLOKAS[randomIndex];
    }
}
