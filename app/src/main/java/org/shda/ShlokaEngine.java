package org.shda;

import java.util.Random;

public class ShlokaEngine {

    // 📚 THE MASTER VAULT: Add as many thousands of quotes here as you want!
    private static final String[] SHLOKAS = {

        // ════════════════════════════════════════
        // 🕉️  BHAGAVAD GITA
        // ════════════════════════════════════════
        "\"Dharmo Rakshati Rakshitah\"\n(Dharma protects those who protect it) - Manu Smriti 8.15",
        "\"Karmanye vadhikaraste Ma Phaleshu Kadachana.\"\n(You have the right to work, but never to the fruit of work) - Bhagavad Gita 2.47",
        "\"Whenever dharma declines and the purpose of life is forgotten, I manifest myself on earth.\"\n- Bhagavad Gita 4.7",
        "\"For the soul there is neither birth nor death at any time. He is unborn, eternal, ever-existing and primeval.\"\n- Bhagavad Gita 2.20",
        "\"As a person puts on new garments, giving up old ones, the soul similarly accepts new material bodies.\"\n- Bhagavad Gita 2.22",
        "\"He who has no attachments can really love others, for his love is pure and divine.\"\n- Bhagavad Gita",
        "\"A man is made by his belief. As he believes, so he is.\"\n- Bhagavad Gita 17.3",
        "\"There is nothing as purifying on earth as true knowledge.\"\n- Bhagavad Gita 4.38",
        "\"Yoga is the journey of the self, through the self, to the self.\"\n- Bhagavad Gita 6.20",
        "\"Whatever action is performed by a great man, common men follow in his footsteps.\"\n- Bhagavad Gita 3.21",
        "\"That which seems like poison at first, but tastes like nectar in the end, is the joy of Sattva.\"\n- Bhagavad Gita 18.37",
        "\"The soul is never born nor dies at any time. It is not slain when the body is slain.\"\n- Bhagavad Gita 2.19",
        "\"Let right deeds be thy motive, not the fruit which comes from them.\"\n- Bhagavad Gita 2.47",
        "\"Set thy heart upon thy work, but never on its reward.\"\n- Bhagavad Gita 2.47",
        "\"He who sees inaction in action and action in inaction is wise indeed.\"\n- Bhagavad Gita 4.18",
        "\"The wise man lets go of all results, whether good or bad, and is focused on the action alone.\"\n- Bhagavad Gita 12.12",
        "\"Fear not what is not real; never was and never will be. What is real, always was and cannot be destroyed.\"\n- Bhagavad Gita 2.16",
        "\"Through selfless service, you will always be fruitful and find the fulfillment of your desires.\"\n- Bhagavad Gita 3.10",
        "\"It is better to perform one's own duty imperfectly than to perform another's duty perfectly.\"\n- Bhagavad Gita 3.35",
        "\"No one who does good work will ever come to a bad end, either here or in the world to come.\"\n- Bhagavad Gita 6.40",
        "\"The peace of God is with them whose mind and soul are in harmony, who are free from desire and wrath.\"\n- Bhagavad Gita 5.29",
        "\"On this path effort never goes to waste, and there is no failure.\"\n- Bhagavad Gita 2.40",
        "\"When a man dwells in his mind on the object of sense, attachment to them is produced.\"\n- Bhagavad Gita 2.62",
        "\"Reshape yourself through the power of your will; never let yourself be degraded by self-will.\"\n- Bhagavad Gita 6.5",
        "\"The mind is restless and difficult to restrain, but it is subdued by practice.\"\n- Bhagavad Gita 6.35",
        "\"Calmness, gentleness, silence, self-restraint, and purity — these are the disciplines of the mind.\"\n- Bhagavad Gita 17.16",
        "\"Anger leads to clouding of judgment, which results in bewilderment of the memory.\"\n- Bhagavad Gita 2.63",
        "\"He who is not disturbed in mind even amidst the threefold miseries is the best among men.\"\n- Bhagavad Gita 2.56",
        "\"Those who eat too much or eat too little, who sleep too much or sleep too little, will not succeed in meditation.\"\n- Bhagavad Gita 6.16",
        "\"Abandoning all duties, come unto Me alone for shelter; sorrow not, I will liberate thee from all sins.\"\n- Bhagavad Gita 18.66",
        "\"God dwells in the heart of all beings, O Arjuna; and He is revolving all of them by His power.\"\n- Bhagavad Gita 18.61",
        "\"The self-controlled soul, who moves amongst sense objects, free from either attachment or repulsion, wins eternal peace.\"\n- Bhagavad Gita 2.64",
        "\"By devotion to one's own particular duty, everyone can attain perfection.\"\n- Bhagavad Gita 18.45",
        "\"A gift is pure when it is given from the heart to the right person at the right time and the right place.\"\n- Bhagavad Gita 17.20",

        // ════════════════════════════════════════
        // 🌿  UPANISHADS
        // ════════════════════════════════════════
        "\"Asato ma sadgamaya, Tamaso ma jyotirgamaya, Mrityorma amritam gamaya.\"\n(Lead me from falsehood to truth, from darkness to light, from death to immortality) - Brihadaranyaka Upanishad",
        "\"Arise, awake, and stop not till the goal is reached.\"\n- Katha Upanishad 1.3.14",
        "\"The mind is everything. What you think you become.\"\n- Upanishadic Thought",
        "\"Tat Tvam Asi.\"\n(Thou art That — You are the universe) - Chandogya Upanishad",
        "\"Aham Brahmasmi.\"\n(I am the infinite reality) - Brihadaranyaka Upanishad",
        "\"Ekam evadvitiyam.\"\n(He is One only, without a second) - Chandogya Upanishad",
        "\"The Self is not born and does not die. It was not produced from anyone; no one was produced from it.\"\n- Katha Upanishad 1.2.18",
        "\"He who knows Brahman becomes Brahman.\"\n- Mundaka Upanishad 3.2.9",
        "\"The Self is hidden in the hearts of all. He is subtler than the subtlest and greater than the greatest.\"\n- Katha Upanishad 1.2.20",
        "\"From joy springs all of creation; by joy it is sustained, towards joy it proceeds, and into joy it returns.\"\n- Taittiriya Upanishad",
        "\"Know the Self as lord of the chariot, the body as the chariot itself, the discriminating intellect as the charioteer.\"\n- Katha Upanishad 1.3.3",
        "\"In the beginning there was Existence alone — One only, without a second.\"\n- Chandogya Upanishad 6.2.1",
        "\"The wise man does not grieve for the living or the dead.\"\n- Katha Upanishad",
        "\"The goal of life is to make your heartbeat match the beat of the universe, to match your nature with Nature.\"\n- Upanishadic Wisdom",
        "\"Sarvam Khalvidam Brahma.\"\n(All this is indeed Brahman — the entire universe is divine) - Chandogya Upanishad 3.14.1",
        "\"Prajnanam Brahma.\"\n(Consciousness is Brahman) - Aitareya Upanishad 3.3",
        "\"The knower of Brahman becomes Brahman.\"\n- Mundaka Upanishad",
        "\"Neither by sight is It grasped, nor by speech, nor by the other senses; not by austerity nor by ritual action.\"\n- Mundaka Upanishad 3.1.8",
        "\"He who knows the bliss of Brahman, from which words and mind return without reaching it, fears nothing.\"\n- Taittiriya Upanishad 2.4",
        "\"This Self is Brahman, consisting of knowledge, mind, life, sight, hearing, earth, water, wind and space.\"\n- Brihadaranyaka Upanishad 4.4.5",

        // ════════════════════════════════════════
        // 🔥  RIG VEDA
        // ════════════════════════════════════════
        "\"Ekam Sat Vipra Bahudha Vadanti.\"\n(Truth is one, sages call it by various names) - Rig Veda 1.164.46",
        "\"Let noble thoughts come to us from every side.\"\n- Rig Veda 1.89.1",
        "\"May we be united in heart. May we be united in speech. May we be united in mind.\"\n- Rig Veda 10.191.2",
        "\"O Agni, lead us on the right path to prosperity, O God, who knows all the works.\"\n- Rig Veda 1.189.1",
        "\"Indra, give us best of treasures, a strong son and a brilliant one.\"\n- Rig Veda 2.21.6",
        "\"The generous person who helps those in need attains glory and obtains treasures.\"\n- Rig Veda 1.125.6",
        "\"May knowledge protect us. May we grow together in wisdom.\"\n- Rig Veda (Shanti Mantra)",
        "\"Move together, speak together, know your minds to be functioning together.\"\n- Rig Veda 10.191.2",
        "\"God is One, the wise call it by many names.\"\n- Rig Veda 1.164.46",
        "\"Like the sun, the truth cannot be concealed.\"\n- Rig Veda",
        "\"May all be happy, may all be free from disease. May all see what is auspicious; may none suffer.\"\n- Rig Veda",
        "\"Arise! Awake! Seek out the great ones and learn. Like the sharp edge of a razor is that path.\"\n- Rig Veda (echoed in Katha Upanishad 1.3.14)",
        "\"The one who helps a deserving person achieves both this world and the next.\"\n- Rig Veda 10.117.3",

        // ════════════════════════════════════════
        // 🌾  ATHARVA VEDA
        // ════════════════════════════════════════
        "\"Truth cannot be suppressed and always is the ultimate victor.\"\n- Yajur Veda",
        "\"O mind, be strong. What is destined to happen will happen. Do your duty with devotion.\"\n- Atharva Veda",
        "\"Do not be led by others; awaken your own mind, amass your own experience, and decide for yourself.\"\n- Atharva Veda",
        "\"May we be engaged in noble work and may our deeds be righteous.\"\n- Atharva Veda",
        "\"Prithivi Mata — Earth is our Mother; protect her, for she sustains all life.\"\n- Atharva Veda 12.1.1",
        "\"He who knows himself knows the universe.\"\n- Atharva Veda",
        "\"United your resolve, united your hearts, may your spirits be at one.\"\n- Atharva Veda 6.64.2",
        "\"Let the rivers flow, let the trees grow, let the air be pure, for in nature lies the divine.\"\n- Atharva Veda",
        "\"We must live in harmony with the laws of nature. This alone is true wisdom.\"\n- Atharva Veda",
        "\"A man who wastes food wastes himself. A man who honors food honors the divine.\"\n- Atharva Veda",
        "\"Sow the seeds of righteousness and you shall harvest a life of peace.\"\n- Atharva Veda",

        // ════════════════════════════════════════
        // 🏹  RAMAYANA & MAHABHARATA
        // ════════════════════════════════════════
        "\"Janani Janmabhoomischa Swargadapi Gariyasi.\"\n(Mother and motherland are superior even to heaven) - Ramayana",
        "\"We are kept from our goal not by obstacles, but by a clear path to a lesser goal.\"\n- Mahabharata (Wisdom of Vidura)",
        "\"There is no higher virtue than compassion, and no higher vice than cruelty.\"\n- Mahabharata",
        "\"Truth is one. It is the foundation of all righteousness.\"\n- Ramayana, Valmiki",
        "\"Even a drop of poison ruins the whole pot of milk. So does a single act of cowardice ruin a life of valor.\"\n- Mahabharata",
        "\"A true friend is one who walks in when the rest of the world walks out.\"\n- Mahabharata (Friendship of Krishna and Arjuna)",
        "\"Time is the root of all things. Respect it, for it waits for no one.\"\n- Mahabharata",
        "\"He who is wise in word but coward in deed teaches nothing but hypocrisy.\"\n- Mahabharata, Vidura Niti",
        "\"Do not speak harshly to anyone. Those who are thus addressed will retort.\"\n- Dhammapada (echoed in Mahabharata wisdom)",
        "\"The world does not pardon a weakling. Strength and dignity must be earned.\"\n- Mahabharata",
        "\"Patience is the greatest weapon of the righteous.\"\n- Ramayana, Valmiki",
        "\"He who has abandoned dharma has abandoned himself.\"\n- Mahabharata, Shanti Parva",
        "\"Even the mightiest warrior cannot conquer his own ego without wisdom.\"\n- Mahabharata, Udyoga Parva",
        "\"Words once spoken cannot be taken back. Guard your tongue as you guard your sword.\"\n- Valmiki Ramayana",

        // ════════════════════════════════════════
        // 👑  CHANAKYA NEETI
        // ════════════════════════════════════════
        "\"Knowledge is the ultimate wealth; it cannot be stolen, nor does it decrease when shared.\"\n- Chanakya Neeti",
        "\"Even if a snake is not poisonous, it should pretend to be venomous.\"\n- Chanakya Neeti",
        "\"A man is great by deeds, not by birth.\"\n- Chanakya Neeti",
        "\"Education is the best friend. An educated person is respected everywhere.\"\n- Chanakya Neeti",
        "\"Once you start working on something, don't be afraid of failure and don't abandon it.\"\n- Chanakya Neeti",
        "\"Before you start some work, always ask yourself three questions: Why am I doing it, what the results might be, and will I be successful.\"\n- Chanakya Neeti",
        "\"The biggest guru-mantra is: never share your secrets with anybody. It will destroy you.\"\n- Chanakya Neeti",
        "\"There is some self-interest behind every friendship. There is no friendship without self-interests. This is a bitter truth.\"\n- Chanakya Neeti",
        "\"As soon as the fear approaches near, attack and destroy it.\"\n- Chanakya Neeti",
        "\"A person should not be too honest. Straight trees are cut first and honest people are screwed first.\"\n- Chanakya Neeti",
        "\"The fragrance of flowers spreads only in the direction of the wind. But the goodness of a person spreads in all directions.\"\n- Chanakya Neeti",
        "\"Books are as useful to a stupid person as a mirror is useful to a blind person.\"\n- Chanakya Neeti",
        "\"Never make friends with people who are above or below you in status. Such friendships will never give you happiness.\"\n- Chanakya Neeti",
        "\"Time perfects men as well as destroys them.\"\n- Chanakya Neeti",
        "\"One who is in search of knowledge should give up the search of pleasure and one in search of pleasure should give up the search of knowledge.\"\n- Chanakya Neeti",
        "\"The world's biggest power is the youth and beauty of a woman.\"\n- Chanakya Neeti",
        "\"Do not reveal what you have thought upon doing, but by wise counsel keep it secret, being determined to carry it into execution.\"\n- Chanakya Neeti",
        "\"A man is born alone and dies alone; and he experiences the good and bad consequences of his karma alone.\"\n- Chanakya Neeti",
        "\"Test a servant while in the discharge of his duty, a relative in difficulty, a friend in adversity, and a wife in misfortune.\"\n- Chanakya Neeti",
        "\"The one excellent thing that can be learned from a lion is that whatever a man intends doing should be done by him with a whole-hearted effort.\"\n- Chanakya Neeti",
        "\"He who lives in our mind is near though he may actually be far away; but he who is not in our heart is far though he may really be nearby.\"\n- Chanakya Neeti",
        "\"Never settle for anything less than what you deserve. It is not pride, it is self-respect.\"\n- Chanakya Neeti",
        "\"Wealth, a friend, a wife, and a kingdom may be regained; but this body when lost may never be acquired again.\"\n- Chanakya Neeti",
        "\"There are three gems upon this earth: food, water, and pleasing words. Fools consider pieces of rocks as gems.\"\n- Chanakya Neeti",

        // ════════════════════════════════════════
        // ✨  YOGA SUTRAS & GENERAL VEDIC WISDOM
        // ════════════════════════════════════════
        "\"Yogas chitta vritti nirodhah.\"\n(Yoga is the cessation of the fluctuations of the mind) - Patanjali Yoga Sutras 1.2",
        "\"Sthira sukham asanam.\"\n(Posture should be steady and comfortable) - Patanjali Yoga Sutras 2.46",
        "\"Ahimsa Paramo Dharma.\"\n(Non-violence is the highest religion) - Mahabharata",
        "\"Satyameva Jayate.\"\n(Truth alone triumphs) - Mundaka Upanishad 3.1.6",
        "\"Vasudhaiva Kutumbakam.\"\n(The whole world is one family) - Maha Upanishad 6.71",
        "\"Om Shanti Shanti Shanti.\"\n(Peace in body, mind, and spirit) - Vedic Peace Mantra",
        "\"Sarve Bhavantu Sukhinah.\"\n(May all beings be happy; may all beings be free from suffering) - Vedic Prayer",
        "\"The universe is not outside of you. Look inside yourself; everything that you want, you already are.\"\n- Rumi (echoed in Vedanta wisdom)",
        "\"In the middle of difficulty lies opportunity. Seek wisdom in every trial.\"\n- Vedic Proverb",
        "\"Where the mind is without fear and the head is held high, where knowledge is free.\"\n- Inspired by Vedic ideals",
        "\"What you think, you become. What you feel, you attract. What you imagine, you create.\"\n- Ancient Vedic Thought",
        "\"Lokah Samastah Sukhino Bhavantu.\"\n(May all beings everywhere be happy and free) - Ancient Sanskrit Prayer",
        "\"Tameva Bhantam Anubhati Sarvam, Tasya Bhasa Sarvam Idam Vibhati.\"\n(By His light alone does everything shine; by His brilliance all this is illumined) - Mundaka Upanishad",
        "\"The aim of life is to live, and to live means to be aware — joyously, drunkenly, serenely, divinely aware.\"\n- Vedic Philosophy",
        "\"He who knows others is wise. He who knows himself is enlightened.\"\n- Ancient Vedic Wisdom",
    };

    private static final Random random = new Random();

    // Fetches a completely random verse every time it is called
    public static String getRandomShloka() {
        int randomIndex = random.nextInt(SHLOKAS.length);
        return SHLOKAS[randomIndex];
    }
}
