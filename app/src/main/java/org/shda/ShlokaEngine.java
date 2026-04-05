package org.shda;

import java.util.Random;

public class ShlokaEngine {
    
    // 📚 THE MASTER VAULT: Add as many thousands of quotes here as you want!
    private static final String[] SHLOKAS = {
        // Bhagavad Gita
        "“Dharmo Rakshati Rakshitah”\n(Dharma protects those who protect it) - Manu Smriti 8.15",
        "“Karmanye vadhikaraste Ma Phaleshu Kadachana.”\n(You have the right to work, but never to the fruit of work) - Bhagavad Gita 2.47",
        "“Whenever dharma declines and the purpose of life is forgotten, I manifest myself on earth.”\n- Bhagavad Gita 4.7",
        "“For the soul there is neither birth nor death at any time. He is unborn, eternal, ever-existing and primeval.”\n- Bhagavad Gita 2.20",
        "“As a person puts on new garments, giving up old ones, the soul similarly accepts new material bodies.”\n- Bhagavad Gita 2.22",
        "“He who has no attachments can really love others, for his love is pure and divine.”\n- Bhagavad Gita",
        "“A man is made by his belief. As he believes, so he is.”\n- Bhagavad Gita 17.3",
        "“There is nothing as purifying on earth as true knowledge.”\n- Bhagavad Gita 4.38",
        "“Yoga is the journey of the self, through the self, to the self.”\n- Bhagavad Gita 6.20",
        "“Whatever action is performed by a great man, common men follow in his footsteps.”\n- Bhagavad Gita 3.21",
        "“That which seems like poison at first, but tastes like nectar in the end, is the joy of Sattva.”\n- Bhagavad Gita 18.37",
        
        // Upanishads
        "“Asato ma sadgamaya, Tamaso ma jyotirgamaya, Mrityorma amritam gamaya.”\n(Lead me from falsehood to truth, from darkness to light, from death to immortality) - Brihadaranyaka Upanishad",
        "“Arise, awake, and stop not till the goal is reached.”\n- Katha Upanishad 1.3.14",
        "“The mind is everything. What you think you become.”\n- Upanishadic Thought",
        "“Tat Tvam Asi.”\n(Thou art That - You are the universe) - Chandogya Upanishad",
        "“Aham Brahmasmi.”\n(I am the infinite reality) - Brihadaranyaka Upanishad",
        "“Ekam evadvitiyam.”\n(He is One only, without a second) - Chandogya Upanishad",
        
        // Vedas
        "“Ekam Sat Vipra Bahudha Vadanti.”\n(Truth is one, sages call it by various names) - Rig Veda 1.164.46",
        "“Let noble thoughts come to us from every side.”\n- Rig Veda 1.89.1",
        "“Truth cannot be suppressed and always is the ultimate victor.”\n- Yajur Veda",
        "“O mind, be strong. What is destined to happen will happen. Do your duty with devotion.”\n- Atharva Veda",
        "“Do not be led by others, awaken your own mind, amass your own experience, and decide for yourself.”\n- Atharva Veda",
        
        // Ramayana & Mahabharata
        "“Janani Janmabhoomischa Swargadapi Gariyasi.”\n(Mother and motherland are superior even to heaven) - Ramayana",
        "“We are kept from our goal not by obstacles, but by a clear path to a lesser goal.”\n- Mahabharata (Wisdom of Vidura)",
        "“There is no higher virtue than compassion, and no higher vice than cruelty.”\n- Mahabharata",
        
        // Chanakya & General Wisdom
        "“Knowledge is the ultimate wealth; it cannot be stolen, nor does it decrease when shared.”\n- Chanakya Neeti",
        "“Even if a snake is not poisonous, it should pretend to be venomous.”\n- Chanakya Neeti",
        "“A man is great by deeds, not by birth.”\n- Chanakya Neeti",
        "“Education is the best friend. An educated person is respected everywhere.”\n- Chanakya Neeti",
        "“Once you start working on something, don't be afraid of failure and don't abandon it. People who work sincerely are the happiest.”\n- Chanakya Neeti"
    };

    private static final Random random = new Random();

    // Fetches a completely random verse every time it is called
    public static String getRandomShloka() {
        int randomIndex = random.nextInt(SHLOKAS.length);
        return SHLOKAS[randomIndex];
    }
}
