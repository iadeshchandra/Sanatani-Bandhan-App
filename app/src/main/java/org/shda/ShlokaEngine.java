package org.shda;

import java.util.Random;

public class ShlokaEngine {

    // 📚 THE MASTER VAULT — Every shloka in English + বাংলা
    // Format per entry:
    //   Line 1 : Sanskrit / Original quote
    //   Line 2 : (English meaning) — Source
    //   blank line
    //   Line 4 : বাংলা অনুবাদ
    //   Line 5 : (বাংলা অর্থ)
    private static final String[] SHLOKAS = {

        // ════════════════════════════════════════
        // 🕉️  BHAGAVAD GITA
        // ════════════════════════════════════════

        "\"Dharmo Rakshati Rakshitah\"\n(Dharma protects those who protect it)\n— Manu Smriti 8.15\n\n\"ধর্মো রক্ষতি রক্ষিতঃ\"\n(যে ধর্মকে রক্ষা করে, ধর্মও তাকে রক্ষা করে)",

        "\"Karmanye vadhikaraste Ma Phaleshu Kadachana.\"\n(You have the right to work, but never to the fruit of work)\n— Bhagavad Gita 2.47\n\n\"কর্মণ্যেবাধিকারস্তে মা ফলেষু কদাচন।\"\n(তোমার অধিকার শুধু কর্মে, কখনো তার ফলে নয়)",

        "\"Whenever dharma declines and the purpose of life is forgotten, I manifest myself on earth.\"\n— Bhagavad Gita 4.7\n\n\"যখনই ধর্মের অবক্ষয় হয় এবং অধর্মের উত্থান হয়, তখনই আমি পৃথিবীতে আবির্ভূত হই।\"\n(ধর্ম রক্ষার্থে ঈশ্বর বারবার আসেন)",

        "\"For the soul there is neither birth nor death at any time. He is unborn, eternal, ever-existing and primeval.\"\n— Bhagavad Gita 2.20\n\n\"আত্মার কখনো জন্ম নেই, মৃত্যুও নেই। সে অজন্মা, নিত্য, চিরন্তন ও পুরাতন।\"\n(দেহের মৃত্যু হলেও আত্মা অমর)",

        "\"As a person puts on new garments, giving up old ones, the soul similarly accepts new material bodies.\"\n— Bhagavad Gita 2.22\n\n\"মানুষ যেমন পুরনো বস্ত্র ত্যাগ করে নতুন বস্ত্র ধারণ করে, তেমনি আত্মাও পুরনো দেহ ছেড়ে নতুন দেহ গ্রহণ করে।\"\n(পুনর্জন্মের অপূর্ব ব্যাখ্যা)",

        "\"He who has no attachments can really love others, for his love is pure and divine.\"\n— Bhagavad Gita\n\n\"যার কোনো আসক্তি নেই, সে-ই প্রকৃত ভালোবাসতে পারে — কারণ তার ভালোবাসা নিষ্কাম ও ঐশ্বরিক।\"\n(নিঃস্বার্থ প্রেমই সত্যিকারের ভালোবাসা)",

        "\"A man is made by his belief. As he believes, so he is.\"\n— Bhagavad Gita 17.3\n\n\"মানুষ তার বিশ্বাস দিয়ে গড়া। সে যা বিশ্বাস করে, সে তা-ই হয়ে ওঠে।\"\n(বিশ্বাসই মানুষের পরিচয় নির্মাণ করে)",

        "\"There is nothing as purifying on earth as true knowledge.\"\n— Bhagavad Gita 4.38\n\n\"এই পৃথিবীতে প্রকৃত জ্ঞানের মতো পবিত্র আর কিছু নেই।\"\n(জ্ঞানই সবচেয়ে বড় শুদ্ধিকারক)",

        "\"Yoga is the journey of the self, through the self, to the self.\"\n— Bhagavad Gita 6.20\n\n\"যোগ হলো নিজের মধ্য দিয়ে নিজেকে আবিষ্কারের যাত্রা।\"\n(আত্মজ্ঞানই যোগের চূড়ান্ত লক্ষ্য)",

        "\"Whatever action is performed by a great man, common men follow in his footsteps.\"\n— Bhagavad Gita 3.21\n\n\"একজন মহৎ মানুষ যা করেন, সাধারণ মানুষ তার পদাঙ্ক অনুসরণ করে।\"\n(নেতার আচরণই সমাজের আদর্শ)",

        "\"That which seems like poison at first, but tastes like nectar in the end, is the joy of Sattva.\"\n— Bhagavad Gita 18.37\n\n\"যা প্রথমে বিষের মতো মনে হয়, কিন্তু শেষে অমৃতের স্বাদ দেয় — সেটাই সাত্ত্বিক সুখ।\"\n(কঠিন পরিশ্রমের ফলই প্রকৃত আনন্দ)",

        "\"The soul is never born nor dies at any time. It is not slain when the body is slain.\"\n— Bhagavad Gita 2.19\n\n\"আত্মা কখনো জন্মগ্রহণ করে না বা মরে না। দেহ ধ্বংস হলেও আত্মা ধ্বংস হয় না।\"\n(দেহ ক্ষণিক, আত্মা চিরন্তন)",

        "\"Let right deeds be thy motive, not the fruit which comes from them.\"\n— Bhagavad Gita 2.47\n\n\"সৎকর্মই হোক তোমার প্রেরণা, ফল নয়।\"\n(ফলের আশা না করে কর্তব্য পালন করাই শ্রেষ্ঠ পথ)",

        "\"Set thy heart upon thy work, but never on its reward.\"\n— Bhagavad Gita 2.47\n\n\"কর্মে মন দাও, পুরস্কারে নয়।\"\n(নিষ্কাম কর্মই মুক্তির পথ)",

        "\"He who sees inaction in action and action in inaction is wise indeed.\"\n— Bhagavad Gita 4.18\n\n\"যে কর্মের মধ্যে নিষ্ক্রিয়তা এবং নিষ্ক্রিয়তার মধ্যে কর্ম দেখতে পায় — সে-ই প্রকৃত জ্ঞানী।\"\n(কর্ম ও অকর্মের গভীর সত্য বোঝাই জ্ঞান)",

        "\"The wise man lets go of all results, whether good or bad, and is focused on the action alone.\"\n— Bhagavad Gita 12.12\n\n\"জ্ঞানী ব্যক্তি ভালো-মন্দ সব ফল ত্যাগ করেন এবং শুধু কর্মেই মনোযোগ দেন।\"\n(ফলাফলের প্রতি উদাসীনতাই প্রজ্ঞার লক্ষণ)",

        "\"Fear not what is not real; never was and never will be. What is real, always was and cannot be destroyed.\"\n— Bhagavad Gita 2.16\n\n\"যা অবাস্তব তাকে ভয় পেও না — তা কখনো ছিল না এবং থাকবেও না। যা বাস্তব তা চিরকাল ছিল এবং ধ্বংস হবে না।\"\n(সত্যের অবিনাশিতা)",

        "\"Through selfless service, you will always be fruitful and find the fulfillment of your desires.\"\n— Bhagavad Gita 3.10\n\n\"নিঃস্বার্থ সেবার মাধ্যমে তুমি সর্বদা ফলবান হবে এবং তোমার আকাঙ্ক্ষা পূর্ণ হবে।\"\n(পরোপকারী জীবনই সর্বোত্তম জীবন)",

        "\"It is better to perform one's own duty imperfectly than to perform another's duty perfectly.\"\n— Bhagavad Gita 3.35\n\n\"নিজের ধর্ম অসম্পূর্ণভাবে পালন করা, অন্যের ধর্ম নিখুঁতভাবে পালনের চেয়ে উত্তম।\"\n(স্বধর্মই শ্রেষ্ঠ পথ)",

        "\"No one who does good work will ever come to a bad end, either here or in the world to come.\"\n— Bhagavad Gita 6.40\n\n\"যে সৎকর্ম করে, সে কখনো মন্দ পরিণতি পায় না — না এই জীবনে, না পরজীবনে।\"\n(সৎকর্মের ফল কখনো বিনষ্ট হয় না)",

        "\"On this path effort never goes to waste, and there is no failure.\"\n— Bhagavad Gita 2.40\n\n\"এই পথে কোনো প্রচেষ্টাই বিফলে যায় না এবং কোনো ব্যর্থতা নেই।\"\n(সৎ প্রচেষ্টা কখনো নিষ্ফল নয়)",

        "\"Reshape yourself through the power of your will; never let yourself be degraded by self-will.\"\n— Bhagavad Gita 6.5\n\n\"নিজের ইচ্ছাশক্তি দিয়ে নিজেকে পুনর্গঠিত করো; কখনো স্বেচ্ছাচারিতায় নিজেকে অধঃপতিত হতে দিও না।\"\n(ইচ্ছাশক্তিই মানুষের সবচেয়ে বড় সম্পদ)",

        "\"The mind is restless and difficult to restrain, but it is subdued by practice.\"\n— Bhagavad Gita 6.35\n\n\"মন চঞ্চল এবং নিয়ন্ত্রণ করা কঠিন, তবে অভ্যাসের মাধ্যমে তা বশে আনা সম্ভব।\"\n(মনকে বশ করতে নিরন্তর অভ্যাস চাই)",

        "\"Calmness, gentleness, silence, self-restraint, and purity — these are the disciplines of the mind.\"\n— Bhagavad Gita 17.16\n\n\"শান্তি, নম্রতা, মৌনতা, আত্মসংযম এবং পবিত্রতা — এগুলোই মনের তপস্যা।\"\n(মানসিক শৃঙ্খলার পাঁচটি স্তম্ভ)",

        "\"Anger leads to clouding of judgment, which results in bewilderment of the memory.\"\n— Bhagavad Gita 2.63\n\n\"ক্রোধ বিচারশক্তিকে আচ্ছন্ন করে, যা স্মৃতিবিভ্রম সৃষ্টি করে।\"\n(রাগ মানুষের সবচেয়ে বড় শত্রু)",

        "\"He who is not disturbed in mind even amidst the threefold miseries is the best among men.\"\n— Bhagavad Gita 2.56\n\n\"যে তিন ধরনের দুঃখের মধ্যেও মনে বিচলিত হয় না, সে-ই মানুষের মধ্যে শ্রেষ্ঠ।\"\n(কষ্টের মাঝেও স্থির থাকাই প্রকৃত শক্তি)",

        "\"The self-controlled soul who moves amongst sense objects, free from attachment or repulsion, wins eternal peace.\"\n— Bhagavad Gita 2.64\n\n\"যে আত্মসংযমী ব্যক্তি ইন্দ্রিয়ের বিষয়ের মধ্যে থেকেও আসক্তি ও বিদ্বেষ থেকে মুক্ত, সে চিরশান্তি পায়।\"\n(আসক্তিহীন জীবনই শান্তির জীবন)",

        "\"Abandoning all duties, come unto Me alone for shelter; sorrow not, I will liberate thee from all sins.\"\n— Bhagavad Gita 18.66\n\n\"সব ধর্ম পরিত্যাগ করে একমাত্র আমার শরণ লও। শোক করো না — আমি তোমাকে সব পাপ থেকে মুক্ত করব।\"\n(ঈশ্বরের সম্পূর্ণ শরণই মুক্তির পথ)",

        "\"A gift is pure when it is given from the heart to the right person at the right time and the right place.\"\n— Bhagavad Gita 17.20\n\n\"যে দান সঠিক সময়ে, সঠিক স্থানে, সঠিক মানুষকে হৃদয় থেকে দেওয়া হয় — সেটাই বিশুদ্ধ দান।\"\n(নিঃস্বার্থ দানই সর্বোচ্চ দান)",

        "\"By devotion to one's own particular duty, everyone can attain perfection.\"\n— Bhagavad Gita 18.45\n\n\"নিজের নির্দিষ্ট কর্তব্যের প্রতি নিষ্ঠার মাধ্যমে প্রত্যেকে পরিপূর্ণতা অর্জন করতে পারে।\"\n(নিজের কাজকে ভালোবেসে করাই পরিপূর্ণতার পথ)",

        "\"The peace of God is with them whose mind and soul are in harmony, who are free from desire and wrath.\"\n— Bhagavad Gita 5.29\n\n\"যার মন ও আত্মা সুসংগত, যে কামনা ও ক্রোধ থেকে মুক্ত — তার সাথেই ঈশ্বরের শান্তি বিরাজ করে।\"\n(মানসিক ভারসাম্যই ঐশ্বরিক শান্তির চাবিকাঠি)",

        // ════════════════════════════════════════
        // 🌿  UPANISHADS
        // ════════════════════════════════════════

        "\"Asato ma sadgamaya. Tamaso ma jyotirgamaya. Mrityorma amritam gamaya.\"\n(Lead me from falsehood to truth, from darkness to light, from death to immortality)\n— Brihadaranyaka Upanishad\n\n\"অসতো মা সদ্গময়। তমসো মা জ্যোতির্গময়। মৃত্যোর্মা অমৃতং গময়।\"\n(আমাকে মিথ্যা থেকে সত্যের দিকে, অন্ধকার থেকে আলোর দিকে, মৃত্যু থেকে অমরত্বের দিকে নিয়ে যাও)",

        "\"Arise, awake, and stop not till the goal is reached.\"\n— Katha Upanishad 1.3.14\n\n\"উত্তিষ্ঠত জাগ্রত প্রাপ্য বরান্নিবোধত।\"\n(জাগো, উঠে দাঁড়াও এবং লক্ষ্যে না পৌঁছানো পর্যন্ত থামো না)",

        "\"Tat Tvam Asi.\"\n(Thou art That — You are the universe)\n— Chandogya Upanishad 6.8.7\n\n\"তৎ ত্বম্ অসি।\"\n(সেটিই তুমি — তুমি নিজেই সেই পরম সত্তা, তুমিই ব্রহ্ম)",

        "\"Aham Brahmasmi.\"\n(I am the infinite reality)\n— Brihadaranyaka Upanishad 1.4.10\n\n\"অহম্ ব্রহ্মাস্মি।\"\n(আমিই ব্রহ্ম — আমি সেই অসীম, চিরন্তন সত্তা)",

        "\"Ekam evadvitiyam.\"\n(He is One only, without a second)\n— Chandogya Upanishad 6.2.1\n\n\"একম্ এব অদ্বিতীয়ম্।\"\n(সে একমাত্র, তাঁর দ্বিতীয় কেউ নেই — পরম সত্তা অদ্বয়)",

        "\"The Self is not born and does not die. It was not produced from anyone; no one was produced from it.\"\n— Katha Upanishad 1.2.18\n\n\"আত্মার জন্ম নেই, মৃত্যু নেই। সে কারো থেকে সৃষ্ট নয়, তার থেকেও কেউ সৃষ্ট নয়।\"\n(আত্মা অনন্ত ও স্বতন্ত্র)",

        "\"The Self is hidden in the hearts of all. He is subtler than the subtlest and greater than the greatest.\"\n— Katha Upanishad 1.2.20\n\n\"আত্মা সকলের হৃদয়ে লুকিয়ে আছেন। তিনি সূক্ষ্মের চেয়ে সূক্ষ্ম এবং বৃহতের চেয়ে বৃহৎ।\"\n(পরমাত্মা সর্বত্র বিরাজমান)",

        "\"From joy springs all of creation; by joy it is sustained, towards joy it proceeds, and into joy it returns.\"\n— Taittiriya Upanishad 3.6\n\n\"আনন্দ থেকেই সমস্ত সৃষ্টির উৎপত্তি; আনন্দেই তা টিকে থাকে, আনন্দের দিকেই চলে এবং আনন্দেই ফিরে যায়।\"\n(সমস্ত অস্তিত্বের উৎস ও গন্তব্য আনন্দ)",

        "\"Know the Self as lord of the chariot, the body as the chariot itself, the discriminating intellect as the charioteer.\"\n— Katha Upanishad 1.3.3\n\n\"আত্মাকে রথের মালিক হিসেবে জানো, দেহকে রথ হিসেবে এবং বিবেককে সারথি হিসেবে।\"\n(মানুষের অস্তিত্বের এক অপূর্ব রূপক)",

        "\"Sarvam Khalvidam Brahma.\"\n(All this is indeed Brahman — the entire universe is divine)\n— Chandogya Upanishad 3.14.1\n\n\"সর্বং খল্বিদং ব্রহ্ম।\"\n(এই সমগ্র বিশ্বব্রহ্মাণ্ডই ব্রহ্ম — প্রতিটি কণায় ঈশ্বর বিরাজমান)",

        "\"Prajnanam Brahma.\"\n(Consciousness is Brahman)\n— Aitareya Upanishad 3.3\n\n\"প্রজ্ঞানম্ ব্রহ্ম।\"\n(চেতনাই ব্রহ্ম — জ্ঞানই পরম সত্য)",

        "\"He who knows the bliss of Brahman, from which words and mind return without reaching it, fears nothing.\"\n— Taittiriya Upanishad 2.4\n\n\"যে ব্রহ্মের আনন্দ জানে — যেখানে বাণী ও মন পৌঁছাতে পারে না — সে কাউকে ভয় পায় না।\"\n(পরম আনন্দের অনুভবই ভয়মুক্তির পথ)",

        "\"Neither by sight is It grasped, nor by speech, nor by the other senses; not by austerity nor by ritual.\"\n— Mundaka Upanishad 3.1.8\n\n\"তাঁকে চোখে দেখা যায় না, বাণীতে বলা যায় না, কোনো ইন্দ্রিয় দিয়ে জানা যায় না; তপস্যা বা আচার দিয়েও নয়।\"\n(পরম সত্তা সকল ইন্দ্রিয়ের ঊর্ধ্বে)",

        "\"He who knows Brahman becomes Brahman.\"\n— Mundaka Upanishad 3.2.9\n\n\"যে ব্রহ্মকে জানে, সে নিজেই ব্রহ্ম হয়ে যায়।\"\n(জ্ঞানের চূড়ান্ত পরিণতিই পরমাত্মার সাথে একত্ব)",

        "\"Satyameva Jayate.\"\n(Truth alone triumphs)\n— Mundaka Upanishad 3.1.6\n\n\"সত্যমেব জয়তে।\"\n(একমাত্র সত্যেরই জয় হয় — মিথ্যার জয় ক্ষণিক)",

        "\"In the beginning there was Existence alone — One only, without a second.\"\n— Chandogya Upanishad 6.2.1\n\n\"শুরুতে শুধু সত্তা ছিল — একটিই, দ্বিতীয় কেউ ছিল না।\"\n(সৃষ্টির আগে একমাত্র পরম সত্তার অস্তিত্ব ছিল)",

        // ════════════════════════════════════════
        // 🔥  RIG VEDA
        // ════════════════════════════════════════

        "\"Ekam Sat Vipra Bahudha Vadanti.\"\n(Truth is one, sages call it by various names)\n— Rig Veda 1.164.46\n\n\"একং সৎ বিপ্রা বহুধা বদন্তি।\"\n(সত্য একটাই — জ্ঞানীরা তাকে বিভিন্ন নামে ডাকেন)",

        "\"Let noble thoughts come to us from every side.\"\n— Rig Veda 1.89.1\n\n\"আ নো ভদ্রা ক্রতবো যন্তু বিশ্বতঃ।\"\n(চারদিক থেকে মহৎ চিন্তা আমাদের কাছে আসুক)",

        "\"May we be united in heart. May we be united in speech. May we be united in mind.\"\n— Rig Veda 10.191.2\n\n\"সমানী ব আকূতিঃ সমানা হৃদয়ানি বঃ।\"\n(আমাদের হৃদয় এক হোক, বাণী এক হোক, মন এক হোক)",

        "\"The generous person who helps those in need attains glory and obtains treasures.\"\n— Rig Veda 1.125.6\n\n\"যে উদার মানুষ প্রয়োজনগ্রস্তকে সাহায্য করে, সে যশ ও সম্পদ লাভ করে।\"\n(পরোপকারই সত্যিকার সম্পদ অর্জনের পথ)",

        "\"May all be happy, may all be free from disease. May all see what is auspicious; may none suffer.\"\n— Rig Veda\n\n\"সর্বে ভবন্তু সুখিনঃ সর্বে সন্তু নিরাময়াঃ।\"\n(সবাই সুখী হোক, সবাই রোগমুক্ত হোক, কেউ দুঃখ না পাক)",

        "\"God is One, the wise call it by many names.\"\n— Rig Veda 1.164.46\n\n\"ঈশ্বর এক — জ্ঞানীরা তাঁকে বিভিন্ন নামে ডাকেন।\"\n(সব ধর্মের মূলে একই সত্য)",

        "\"Move together, speak together, know your minds to be functioning together.\"\n— Rig Veda 10.191.2\n\n\"একসাথে চলো, একসাথে কথা বলো, তোমাদের মন একই লক্ষ্যে কাজ করুক।\"\n(ঐক্যই শক্তি)",

        "\"Like the sun, the truth cannot be concealed.\"\n— Rig Veda\n\n\"সূর্যের মতো সত্যকেও কখনো আড়াল করা যায় না।\"\n(সত্য সর্বদা প্রকাশ পায়)",

        "\"The one who helps a deserving person achieves both this world and the next.\"\n— Rig Veda 10.117.3\n\n\"যোগ্য মানুষকে সাহায্য করে সে এই জীবনে এবং পরকালে উভয়েই সফল হয়।\"\n(সৎ দান-ধর্ম ইহলোক ও পরলোক উভয়ই সুন্দর করে)",

        "\"O Agni, lead us on the right path to prosperity, O God, who knows all the works.\"\n— Rig Veda 1.189.1\n\n\"হে অগ্নি, হে সর্বজ্ঞ ঈশ্বর, আমাদের সমৃদ্ধির সঠিক পথে নিয়ে চলো।\"\n(ঐশ্বরিক পথনির্দেশনার প্রার্থনা)",

        "\"Arise! Awake! Seek out the great ones and learn.\"\n— Rig Veda\n\n\"জাগো, উঠে দাঁড়াও! মহৎ জ্ঞানীদের কাছে যাও এবং শেখো।\"\n(জ্ঞান অর্জনের জন্য সক্রিয় হওয়াই বৈদিক আদেশ)",

        // ════════════════════════════════════════
        // 🌾  ATHARVA VEDA & YAJUR VEDA
        // ════════════════════════════════════════

        "\"Truth cannot be suppressed and always is the ultimate victor.\"\n— Yajur Veda\n\n\"সত্যকে দমন করা যায় না এবং সত্যই সর্বদা চূড়ান্ত বিজয়ী।\"\n(সত্যের শক্তি অপরাজেয়)",

        "\"O mind, be strong. What is destined to happen will happen. Do your duty with devotion.\"\n— Atharva Veda\n\n\"হে মন, দৃঢ় হও। যা ঘটার তা ঘটবেই। ভক্তি সহকারে তোমার কর্তব্য পালন করো।\"\n(নিয়তিকে মেনে নিয়ে কর্তব্যে মনোযোগ দাও)",

        "\"Do not be led by others; awaken your own mind, amass your own experience, and decide for yourself.\"\n— Atharva Veda\n\n\"অন্যের দ্বারা পরিচালিত হয়ো না; নিজের মন জাগাও, নিজের অভিজ্ঞতা সঞ্চয় করো এবং নিজেই সিদ্ধান্ত নাও।\"\n(স্বাধীন চিন্তাই মানুষের শ্রেষ্ঠ গুণ)",

        "\"Prithivi Mata — Earth is our Mother; protect her, for she sustains all life.\"\n— Atharva Veda 12.1.1\n\n\"পৃথিবী মাতা — তাঁকে রক্ষা করো, কারণ তিনিই সকল জীবনকে ধারণ করেন।\"\n(প্রকৃতির সংরক্ষণই সভ্যতার রক্ষা)",

        "\"United your resolve, united your hearts, may your spirits be at one.\"\n— Atharva Veda 6.64.2\n\n\"তোমাদের সংকল্প এক হোক, হৃদয় এক হোক, আত্মা এক হোক।\"\n(একতাই জাতির শক্তি)",

        "\"We must live in harmony with the laws of nature. This alone is true wisdom.\"\n— Atharva Veda\n\n\"আমাদের প্রকৃতির নিয়মের সাথে সামঞ্জস্য রেখে জীবন যাপন করতে হবে। এটাই প্রকৃত জ্ঞান।\"\n(প্রকৃতির সাথে মিলে চলাই সত্যিকারের প্রজ্ঞা)",

        "\"Sow the seeds of righteousness and you shall harvest a life of peace.\"\n— Atharva Veda\n\n\"ধার্মিকতার বীজ বপন করো — তুমি শান্তিময় জীবন ফসল হিসেবে পাবে।\"\n(সৎ জীবনের ফলই শান্তি)",

        "\"He who knows himself knows the universe.\"\n— Atharva Veda\n\n\"যে নিজেকে জানে, সে মহাবিশ্বকে জানে।\"\n(আত্মজ্ঞানই বিশ্বজ্ঞানের চাবিকাঠি)",

        "\"A man who honors food honors the divine.\"\n— Atharva Veda\n\n\"যে মানুষ অন্নকে সম্মান করে, সে ঈশ্বরকেই সম্মান করে।\"\n(অন্নব্রহ্ম — খাদ্যেই ঈশ্বরের প্রকাশ)",

        // ════════════════════════════════════════
        // 🏹  RAMAYANA & MAHABHARATA
        // ════════════════════════════════════════

        "\"Janani Janmabhoomischa Swargadapi Gariyasi.\"\n(Mother and motherland are superior even to heaven)\n— Valmiki Ramayana\n\n\"জননী জন্মভূমিশ্চ স্বর্গাদপি গরীয়সী।\"\n(জননী এবং জন্মভূমি স্বর্গের চেয়েও শ্রেষ্ঠ)",

        "\"We are kept from our goal not by obstacles, but by a clear path to a lesser goal.\"\n— Mahabharata (Wisdom of Vidura)\n\n\"আমরা বাধার কারণে লক্ষ্যে পৌঁছাতে পারি না — বরং ছোট লক্ষ্যের সুস্পষ্ট পথের কারণে।\"\n(বড় স্বপ্নে অবিচল থাকাই সাফল্যের চাবিকাঠি)",

        "\"There is no higher virtue than compassion, and no higher vice than cruelty.\"\n— Mahabharata\n\n\"দয়ার চেয়ে বড় কোনো পুণ্য নেই, এবং নিষ্ঠুরতার চেয়ে বড় কোনো পাপ নেই।\"\n(করুণাই মানবতার সর্বোচ্চ প্রকাশ)",

        "\"Truth is the foundation of all righteousness.\"\n— Valmiki Ramayana\n\n\"সত্যই সকল ধর্মের ভিত্তি।\"\n(সত্য ছাড়া কোনো ন্যায়ই টিকে থাকতে পারে না)",

        "\"Time is the root of all things. Respect it, for it waits for no one.\"\n— Mahabharata\n\n\"সময়ই সকল বস্তুর মূল। সময়কে সম্মান করো — কারণ সে কারো জন্য অপেক্ষা করে না।\"\n(সময়ের মূল্য না বুঝলে জীবন বিফল)",

        "\"Patience is the greatest weapon of the righteous.\"\n— Valmiki Ramayana\n\n\"ধৈর্য হলো ধার্মিকের সবচেয়ে বড় অস্ত্র।\"\n(সহনশীলতাই শেষ পর্যন্ত জয়ী হয়)",

        "\"Even the mightiest warrior cannot conquer his own ego without wisdom.\"\n— Mahabharata, Udyoga Parva\n\n\"সবচেয়ে শক্তিশালী যোদ্ধাও জ্ঞান ছাড়া নিজের অহংকার জয় করতে পারে না।\"\n(অহংকার জয়ই সবচেয়ে কঠিন যুদ্ধ)",

        "\"He who has abandoned dharma has abandoned himself.\"\n— Mahabharata, Shanti Parva\n\n\"যে ধর্ম ত্যাগ করেছে, সে নিজেকেই ত্যাগ করেছে।\"\n(ধর্মই মানুষের সত্যিকার পরিচয়)",

        "\"Words once spoken cannot be taken back. Guard your tongue as you guard your sword.\"\n— Valmiki Ramayana\n\n\"একবার বলা কথা ফেরানো যায় না। তরবারির মতো নিজের জিভও সংযত রাখো।\"\n(বাক্য সংযমই বুদ্ধিমানের লক্ষণ)",

        "\"A true friend is one who walks in when the rest of the world walks out.\"\n— Mahabharata\n\n\"প্রকৃত বন্ধু সেই যে দুঃসময়ে পাশে থাকে, যখন বাকি পৃথিবী সরে যায়।\"\n(সত্যিকার বন্ধুত্ব কষ্টের সময়েই পরীক্ষিত হয়)",

        "\"He who is wise in word but coward in deed teaches nothing but hypocrisy.\"\n— Mahabharata, Vidura Niti\n\n\"যে কথায় জ্ঞানী কিন্তু কাজে কাপুরুষ, সে শুধু ভণ্ডামিই শেখায়।\"\n(কথা ও কাজের মিল না থাকলে চরিত্র নষ্ট হয়)",

        "\"The world does not pardon a weakling. Strength and dignity must be earned.\"\n— Mahabharata\n\n\"দুর্বলকে পৃথিবী ক্ষমা করে না। শক্তি ও মর্যাদা অর্জন করে নিতে হয়।\"\n(শক্তিই পৃথিবীর ভাষা)",

        "\"Even a drop of poison ruins the whole pot of milk.\"\n— Mahabharata\n\n\"এক ফোঁটা বিষও পুরো দুধের পাত্র নষ্ট করে দেয়।\"\n(একটি মন্দ কাজ সমস্ত সুনামকে ধ্বংস করতে পারে)",

        // ════════════════════════════════════════
        // 👑  CHANAKYA NEETI
        // ════════════════════════════════════════

        "\"Knowledge is the ultimate wealth; it cannot be stolen, nor does it decrease when shared.\"\n— Chanakya Neeti\n\n\"জ্ঞানই সর্বোচ্চ সম্পদ — এটা চুরি করা যায় না, এবং ভাগ করে দিলেও কমে না।\"\n(জ্ঞানই একমাত্র সম্পদ যা বিতরণে বাড়ে)",

        "\"Even if a snake is not poisonous, it should pretend to be venomous.\"\n— Chanakya Neeti\n\n\"সাপ বিষাক্ত না হলেও তাকে বিষাক্ত বলে ভান করতে হয়।\"\n(দুর্বলতা দেখালে পৃথিবী সুযোগ নেয়; শক্তিশালী দেখানো বুদ্ধিমানের কাজ)",

        "\"A man is great by deeds, not by birth.\"\n— Chanakya Neeti\n\n\"মানুষ কর্মে মহান হয়, জন্মে নয়।\"\n(বংশ নয়, কর্মই প্রকৃত পরিচয় তৈরি করে)",

        "\"Education is the best friend. An educated person is respected everywhere.\"\n— Chanakya Neeti\n\n\"শিক্ষাই সর্বোত্তম বন্ধু। শিক্ষিত মানুষ সর্বত্র সম্মান পায়।\"\n(জ্ঞানই সবচেয়ে বিশ্বস্ত সঙ্গী)",

        "\"Once you start working on something, don't be afraid of failure and don't abandon it.\"\n— Chanakya Neeti\n\n\"একবার কোনো কাজ শুরু করলে ব্যর্থতাকে ভয় পেও না এবং সেটা ছেড়ে দিও না।\"\n(অধ্যবসায়ই সাফল্যের রহস্য)",

        "\"Before you start some work, always ask yourself: Why am I doing it, what might the results be, and will I be successful?\"\n— Chanakya Neeti\n\n\"কোনো কাজ শুরু করার আগে সর্বদা নিজেকে জিজ্ঞেস করো: কেন করছি, ফল কী হতে পারে, এবং আমি কি সফল হব?\"\n(পরিকল্পিত কর্মই সঠিক কর্ম)",

        "\"The biggest guru-mantra is: never share your secrets with anybody. It will destroy you.\"\n— Chanakya Neeti\n\n\"সবচেয়ে বড় গুরুমন্ত্র হলো: কখনো কাউকে তোমার গোপন কথা বলো না — এটা তোমাকে ধ্বংস করে দেবে।\"\n(গোপনীয়তাই আত্মরক্ষার প্রথম শর্ত)",

        "\"There is some self-interest behind every friendship. There is no friendship without self-interests.\"\n— Chanakya Neeti\n\n\"প্রতিটি বন্ধুত্বের পেছনে কিছু না কিছু স্বার্থ থাকে। স্বার্থহীন বন্ধুত্ব বলে কিছু নেই — এটা একটা তিক্ত সত্য।\"\n(বাস্তব জীবনের কঠোর সত্য বোঝাই বুদ্ধিমত্তা)",

        "\"As soon as the fear approaches near, attack and destroy it.\"\n— Chanakya Neeti\n\n\"ভয় কাছে আসামাত্র সেটাকে আক্রমণ করো এবং ধ্বংস করো।\"\n(ভয়কে এড়ানো নয়, মোকাবেলাই সাহসিকতা)",

        "\"The fragrance of flowers spreads only in the direction of the wind. But the goodness of a person spreads in all directions.\"\n— Chanakya Neeti\n\n\"ফুলের সুগন্ধ শুধু বাতাসের দিকে ছড়ায়। কিন্তু একজন ভালো মানুষের সুনাম সব দিকেই ছড়িয়ে পড়ে।\"\n(সুচরিত্রের প্রভাব সীমানা মানে না)",

        "\"Books are as useful to a stupid person as a mirror is useful to a blind person.\"\n— Chanakya Neeti\n\n\"মূর্খের কাছে বইয়ের যতটুকু উপকারিতা, একজন অন্ধের কাছে আয়নার ততটুকুই উপকারিতা।\"\n(জ্ঞান কাজে না লাগালে তা অর্থহীন)",

        "\"Time perfects men as well as destroys them.\"\n— Chanakya Neeti\n\n\"সময় মানুষকে পরিপূর্ণ করে, আবার ধ্বংসও করে।\"\n(সময়ের সদ্ব্যবহারই জীবনের পার্থক্য তৈরি করে)",

        "\"A man is born alone and dies alone; and he experiences the good and bad consequences of his karma alone.\"\n— Chanakya Neeti\n\n\"মানুষ একা জন্মায় এবং একাই মরে; এবং সে তার কর্মের ভালো-মন্দ পরিণতিও একাই ভোগ করে।\"\n(কর্মের ফল নিজেই বহন করতে হয়)",

        "\"Wealth, a friend, a wife, and a kingdom may be regained; but this body when lost may never be acquired again.\"\n— Chanakya Neeti\n\n\"সম্পদ, বন্ধু, স্ত্রী, রাজ্য — সব ফিরে পাওয়া সম্ভব; কিন্তু এই দেহ একবার হারালে আর পাওয়া যায় না।\"\n(স্বাস্থ্যই সবচেয়ে বড় সম্পদ)",

        "\"There are three gems upon this earth: food, water, and pleasing words. Fools consider pieces of rocks as gems.\"\n— Chanakya Neeti\n\n\"পৃথিবীতে তিনটি রত্ন আছে: খাদ্য, জল এবং মধুর বাণী। মূর্খরা পাথরের টুকরোকে রত্ন মনে করে।\"\n(জীবনের আসল মূল্যবান জিনিস চেনাই প্রজ্ঞা)",

        "\"One who is in search of knowledge should give up the search of pleasure, and one in search of pleasure should give up knowledge.\"\n— Chanakya Neeti\n\n\"যে জ্ঞান খোঁজে সে সুখের সন্ধান ছেড়ে দিক; যে সুখ খোঁজে সে জ্ঞানের সন্ধান ছেড়ে দিক।\"\n(জ্ঞান ও ভোগের একসাথে চলা সম্ভব নয়)",

        "\"Do not reveal what you have thought upon doing; by wise counsel keep it secret and be determined to carry it into execution.\"\n— Chanakya Neeti\n\n\"তুমি কী করতে চাইছ তা প্রকাশ করো না; বিজ্ঞ পরামর্শে তা গোপন রাখো এবং তা সম্পন্ন করার সংকল্প ধরে রাখো।\"\n(পরিকল্পনা প্রকাশ না করে নীরবে বাস্তবায়ন করো)",

        "\"The one excellent thing that can be learned from a lion is that whatever a man intends doing should be done with whole-hearted effort.\"\n— Chanakya Neeti\n\n\"সিংহের কাছ থেকে যে একটি শ্রেষ্ঠ গুণ শেখার আছে তা হলো: মানুষ যা করতে চায় তা পূর্ণ মনোযোগ দিয়ে করতে হয়।\"\n(পরিপূর্ণ মনোযোগেই সাফল্য আসে)",

        "\"Never settle for anything less than what you deserve. It is not pride, it is self-respect.\"\n— Chanakya Neeti\n\n\"নিজের প্রাপ্যের চেয়ে কম কখনো মেনে নিও না। এটা অহংকার নয়, এটা আত্মসম্মান।\"\n(আত্মসম্মানবোধই ব্যক্তিত্বের ভিত্তি)",

        "\"Test a servant while in the discharge of his duty, a relative in difficulty, a friend in adversity, and a wife in misfortune.\"\n— Chanakya Neeti\n\n\"সেবককে কাজের সময়ে, আত্মীয়কে বিপদে, বন্ধুকে দুর্দশায় এবং স্ত্রীকে দুর্ভাগ্যে পরীক্ষা করো।\"\n(কঠিন সময়েই মানুষের আসল চেহারা বেরিয়ে আসে)",

        // ════════════════════════════════════════
        // ✨  YOGA SUTRAS & GENERAL VEDIC WISDOM
        // ════════════════════════════════════════

        "\"Yogas chitta vritti nirodhah.\"\n(Yoga is the cessation of the fluctuations of the mind)\n— Patanjali Yoga Sutras 1.2\n\n\"যোগশ্চিত্তবৃত্তিনিরোধঃ।\"\n(যোগ হলো মনের চঞ্চলতার নিরোধ — মনকে স্থির করাই যোগের সারকথা)",

        "\"Sthira sukham asanam.\"\n(Posture should be steady and comfortable)\n— Patanjali Yoga Sutras 2.46\n\n\"স্থিরসুখম্ আসনম্।\"\n(আসন হওয়া উচিত স্থির ও সুখকর — শরীর ও মনের সামঞ্জস্যই যোগের শুরু)",

        "\"Ahimsa Paramo Dharma.\"\n(Non-violence is the highest religion)\n— Mahabharata\n\n\"অহিংসা পরমো ধর্মঃ।\"\n(অহিংসাই সর্বোচ্চ ধর্ম — হিংসা যেকোনো ধর্মের বিরুদ্ধে)",

        "\"Vasudhaiva Kutumbakam.\"\n(The whole world is one family)\n— Maha Upanishad 6.71\n\n\"বসুধৈব কুটুম্বকম্।\"\n(সমগ্র পৃথিবী একটি পরিবার — সকল মানুষ আপনজন)",

        "\"Sarve Bhavantu Sukhinah.\"\n(May all beings be happy; may all beings be free from suffering)\n— Ancient Vedic Prayer\n\n\"সর্বে ভবন্তু সুখিনঃ সর্বে সন্তু নিরাময়াঃ।\"\n(সকল প্রাণী সুখী হোক, সকলে রোগমুক্ত হোক — এটাই বৈদিক প্রার্থনার সারকথা)",

        "\"Lokah Samastah Sukhino Bhavantu.\"\n(May all beings everywhere be happy and free)\n— Ancient Sanskrit Prayer\n\n\"লোকাঃ সমস্তাঃ সুখিনো ভবন্তু।\"\n(পৃথিবীর সকল প্রাণী সুখী ও মুক্ত হোক — সার্বজনীন কল্যাণের প্রার্থনা)",

        "\"What you think, you become. What you feel, you attract. What you imagine, you create.\"\n— Ancient Vedic Thought\n\n\"তুমি যা ভাবো তা হয়ে ওঠো। যা অনুভব করো তা আকর্ষণ করো। যা কল্পনা করো তা সৃষ্টি হয়।\"\n(মনের শক্তিই বাস্তবতা নির্মাণ করে)",

        "\"He who knows others is wise. He who knows himself is enlightened.\"\n— Ancient Vedic Wisdom\n\n\"যে অন্যকে জানে সে জ্ঞানী। যে নিজেকে জানে সে আলোকিত।\"\n(আত্মজ্ঞানই সর্বোচ্চ জ্ঞান)",

        "\"Om Shanti Shanti Shanti.\"\n(Peace in body, mind, and spirit)\n— Vedic Peace Mantra\n\n\"ওম শান্তি শান্তি শান্তি।\"\n(দেহে শান্তি, মনে শান্তি, আত্মায় শান্তি — ত্রিস্তরীয় শান্তির প্রার্থনা)",

        "\"Tameva Bhantam Anubhati Sarvam, Tasya Bhasa Sarvam Idam Vibhati.\"\n(By His light alone does everything shine; by His brilliance all this is illumined)\n— Mundaka Upanishad\n\n\"তমেব ভান্তম অনুভাতি সর্বম্, তস্য ভাসা সর্বম্ ইদং বিভাতি।\"\n(তাঁর আলোতেই সব কিছু আলোকিত; তাঁর দ্যুতিতেই এই সমগ্র বিশ্ব উদ্ভাসিত)",

    };

    private static final Random random = new Random();

    // Fetches a completely random verse every time it is called
    public static String getRandomShloka() {
        int randomIndex = random.nextInt(SHLOKAS.length);
        return SHLOKAS[randomIndex];
    }
}
