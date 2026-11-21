package com.indra.krishiscan

/**
 * Builds the grounded prompt for Llama and the static advisory text in
 * English, Hindi and Bengali.
 *
 * Every fact comes from the verified advisory database — the same content as
 * the web POC's advisories.js (symptoms + actions per disease). The Bengali
 * and Hindi texts are direct translations of those same verified facts,
 * written for farmers (simple words, no invented treatments or doses).
 *
 * LANGUAGE ROUTING (decided by the on-device test, 20 Nov 2026):
 * - English → Llama generates the reply (grounded prompt, facts only).
 * - Bengali / Hindi → the verified TRANSLATED static advice is shown
 *   directly, no generation. Llama 1B's Bengali came out garbled in testing,
 *   and Bengali text burns tokens faster, so generated replies also got cut
 *   off before the prevention tip. Static text in these languages is instant
 *   and always correct.
 *
 * Region default: West Bengal. The region only changes the prompt framing —
 * the verified facts stay identical, by design (grounding).
 *
 * Token budget (English generation): replies are capped at 120 new tokens in
 * MainActivity — at the measured 9.4 tok/s on the Redmi 15 5G that is ~13 s
 * worst case, so the prompt asks for under 60 words.
 */
object PromptBuilder {

    private data class AdvisoryEntry(
        val severity: String,
        val symptoms: String,
        val actions: List<String>,
    )

    // ---- English (same verified facts as advisories.js) ----
    private val ADVISORIES = mapOf(
        "Tomato___Bacterial_spot" to AdvisoryEntry(
            severity = "High",
            symptoms = "Small water-soaked spots on leaves that turn dark brown/black, often with a yellow halo. Can spread to stems and fruit.",
            actions = listOf(
                "Use certified disease-free seeds and seedlings.",
                "Avoid overhead irrigation — water at the base of the plant.",
                "Spray a copper-based bactericide at first sign; repeat every 7–10 days.",
                "Remove and destroy infected leaves (do not compost).",
                "Rotate crops for 2–3 seasons; avoid planting tomato/pepper in the same plot.",
            ),
        ),
        "Tomato___Early_blight" to AdvisoryEntry(
            severity = "Medium",
            symptoms = "Dark concentric 'target' rings on older, lower leaves; leaves yellow and drop. Caused by Alternaria fungus.",
            actions = listOf(
                "Mulch around plants to stop soil splashing onto leaves.",
                "Prune off the lowest infected leaves and destroy them.",
                "Spray mancozeb or chlorothalonil fungicide every 10–14 days.",
                "Space plants well for airflow; stake to keep foliage off soil.",
                "Rotate with non-solanaceous crops next season.",
            ),
        ),
        "Tomato___Late_blight" to AdvisoryEntry(
            severity = "Critical",
            symptoms = "Pale water-soaked lesions that turn brown; white moldy growth under leaves in humid weather. Spreads extremely fast.",
            actions = listOf(
                "Act immediately — uproot and destroy badly infected plants (do not compost).",
                "Never wet the foliage; water early morning at the base.",
                "Apply copper or metalaxyl-based fungicide preventively in humid weather.",
                "Plant resistant varieties next season.",
                "Clear all crop debris after harvest.",
            ),
        ),
        "Tomato___Leaf_Mold" to AdvisoryEntry(
            severity = "Medium",
            symptoms = "Pale yellow spots on upper leaf surface; olive-green to brown velvety mold underneath. Thrives in humid, crowded conditions.",
            actions = listOf(
                "Improve air circulation — thin crowded foliage, space plants wider.",
                "Avoid wetting leaves; ventilate greenhouses/polyhouses.",
                "Water in the morning so foliage dries by evening.",
                "Remove severely infected leaves; fungicide only if spreading fast.",
            ),
        ),
        "Tomato___Septoria_leaf_spot" to AdvisoryEntry(
            severity = "Medium",
            symptoms = "Small circular spots with dark borders and tan/grey centers, starting on lower leaves and moving upward.",
            actions = listOf(
                "Pick off affected lower leaves at first sight and destroy them.",
                "Mulch to prevent rain splash from soil.",
                "Avoid overhead watering; stake plants upright.",
                "Rotate crops; spray fungicide if spots keep climbing the plant.",
            ),
        ),
        "Tomato___Spider_mites Two-spotted_spider_mite" to AdvisoryEntry(
            severity = "Medium",
            symptoms = "Fine yellow stippling/speckling on leaves, bronzing, and delicate webbing under leaves. Tiny moving dots visible with a lens.",
            actions = listOf(
                "Blast undersides of leaves with a strong jet of water.",
                "Spray neem oil (azadirachtin) covering leaf undersides; repeat weekly.",
                "Encourage predatory mites — avoid broad-spectrum insecticides.",
                "Remove heavily infested leaves; keep plants well-watered (mites love dry stress).",
            ),
        ),
        "Tomato___Target_Spot" to AdvisoryEntry(
            severity = "Medium",
            symptoms = "Brown lesions with concentric rings ('target' pattern) on leaves and stems; can cause heavy defoliation.",
            actions = listOf(
                "Practice field sanitation — clear fallen leaves and debris.",
                "Rotate crops; avoid planting tomato in the same soil consecutively.",
                "Avoid overhead irrigation; improve drainage.",
                "Apply mancozeb or chlorothalonil fungicide if lesions spread.",
            ),
        ),
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to AdvisoryEntry(
            severity = "Critical",
            symptoms = "Upward curling of leaves, yellowing between veins, stunted bushy growth, poor fruit set. Spread by whiteflies. No cure.",
            actions = listOf(
                "There is no cure — uproot and destroy infected plants immediately to protect neighbours.",
                "Control whiteflies: yellow sticky traps + neem oil sprays.",
                "Use reflective silver mulch to repel whiteflies.",
                "Plant TYLCV-resistant varieties next season.",
                "Remove weed hosts around the field.",
            ),
        ),
        "Tomato___Tomato_mosaic_virus" to AdvisoryEntry(
            severity = "High",
            symptoms = "Mottled light/dark green mosaic pattern, leaf curling and distortion. Spreads by touch, tools, and hands. No cure.",
            actions = listOf(
                "No cure — remove and destroy infected plants at once.",
                "Disinfect tools, stakes and hands (wash with soap); avoid handling plants when wet.",
                "Do not smoke or handle tobacco near tomato plants.",
                "Control weeds that host the virus; plant resistant varieties.",
            ),
        ),
        "Tomato___healthy" to AdvisoryEntry(
            severity = "None — healthy",
            symptoms = "No disease symptoms detected. Leaf looks healthy.",
            actions = listOf(
                "Keep up the good work — inspect plants weekly.",
                "Maintain even watering and good spacing for airflow.",
                "A preventive neem-oil spray every 2 weeks keeps pests away.",
                "Remove weeds regularly to deny pests a hiding place.",
            ),
        ),
    )

    // ---- Bengali: direct translation of the same verified facts ----
    private val NAMES_BN = mapOf(
        "Tomato___Bacterial_spot" to "টমেটো ব্যাকটেরিয়াল স্পট",
        "Tomato___Early_blight" to "টমেটো আর্লি ব্লাইট",
        "Tomato___Late_blight" to "টমেটো লেট ব্লাইট",
        "Tomato___Leaf_Mold" to "টমেটো লিফ মোল্ড",
        "Tomato___Septoria_leaf_spot" to "টমেটো সেপ্টোরিয়া পাতার দাগ",
        "Tomato___Spider_mites Two-spotted_spider_mite" to "টমেটো স্পাইডার মাইট",
        "Tomato___Target_Spot" to "টমেটো টার্গেট স্পট",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to "টমেটো ইয়েলো লিফ কার্ল ভাইরাস",
        "Tomato___Tomato_mosaic_virus" to "টমেটো মোজাইক ভাইরাস",
        "Tomato___healthy" to "সুস্থ টমেটো পাতা",
    )

    private val ADVISORIES_BN = mapOf(
        "Tomato___Bacterial_spot" to AdvisoryEntry(
            severity = "বেশি",
            symptoms = "পাতায় ছোট ছোট জল-ভেজা দাগ হয়, পরে তা গাঢ় বাদামি/কালো হয়ে যায়, অনেক সময় হলুদ বলয় থাকে। কাণ্ড ও ফলেও ছড়াতে পারে।",
            actions = listOf(
                "প্রত্যয়িত রোগমুক্ত বীজ ও চারা ব্যবহার করুন।",
                "উপরে থেকে জল দেবেন না — গাছের গোড়ায় জল দিন।",
                "প্রথম লক্ষণ দেখা দিলে তামা-ভিত্তিক ব্যাকটেরিয়ানাশক স্প্রে করুন; প্রতি ৭–১০ দিনে পুনরাবৃত্তি করুন।",
                "আক্রান্ত পাতা তুলে নষ্ট করুন (কম্পোস্টে দেবেন না)।",
                "২–৩ মরশুম শস্য পরিবর্তন করুন; একই জমিতে টমেটো/লঙ্কা লাগাবেন না।",
            ),
        ),
        "Tomato___Early_blight" to AdvisoryEntry(
            severity = "মাঝারি",
            symptoms = "নিচের পুরনো পাতায় গাঢ় গোলাকার 'টার্গেট'-আকৃতির দাগ; পাতা হলুদ হয়ে ঝরে যায়। অল্টারনারিয়া ছত্রাকের কারণে হয়।",
            actions = listOf(
                "মাটি ছিটকে পাতায় না লাগে, সেজন্য গাছের গোড়ায় মালচ দিন।",
                "নিচের আক্রান্ত পাতা কেটে নষ্ট করুন।",
                "প্রতি ১০–১৪ দিনে ম্যানকোজেব বা ক্লোরোথালোনিল ছত্রাকনাশক স্প্রে করুন।",
                "গাছের মধ্যে ফাঁক রাখুন যাতে বাতাস চলাচল করে; পাতা যাতে মাটি না ছোঁয়, সেজন্য খুঁটি দিন।",
                "পরের মরশুমে সোলানেসি ছাড়া অন্য ফসল লাগান।",
            ),
        ),
        "Tomato___Late_blight" to AdvisoryEntry(
            severity = "মারাত্মক",
            symptoms = "ফ্যাকাশে জল-ভেজা দাগ যা বাদামি হয়ে যায়; আর্দ্র আবহাওয়ায় পাতার নিচে সাদা ছত্রাক দেখা যায়। খুব দ্রুত ছড়ায়।",
            actions = listOf(
                "এখনই ব্যবস্থা নিন — বেশি আক্রান্ত গাছ উপড়ে নষ্ট করুন (কম্পোস্টে দেবেন না)।",
                "পাতা কখনো ভেজাবেন না; সকালে গোড়ায় জল দিন।",
                "আর্দ্র আবহাওয়ায় আগেভাগে তামা বা মেটালাক্সিল-ভিত্তিক ছত্রাকনাশক দিন।",
                "পরের মরশুমে রোগ-প্রতিরোধী জাত লাগান।",
                "ফসল তোলার পর সব গাছের অবশেষ পরিষ্কার করুন।",
            ),
        ),
        "Tomato___Leaf_Mold" to AdvisoryEntry(
            severity = "মাঝারি",
            symptoms = "পাতার উপরে ফ্যাকাশে হলুদ দাগ; নিচে জলপাই-সবুজ থেকে বাদামি মখমলের মতো ছত্রাক। আর্দ্র ও ঘন জায়গায় বেশি হয়।",
            actions = listOf(
                "বাতাস চলাচল বাড়ান — ঘন পাতা পাতলা করুন, গাছের ফাঁক বাড়ান।",
                "পাতা ভেজাবেন না; গ্রিনহাউস/পলিহাউসে বাতাস চলাচলের ব্যবস্থা করুন।",
                "সকালে জল দিন যাতে সন্ধ্যার মধ্যে পাতা শুকিয়ে যায়।",
                "বেশি আক্রান্ত পাতা তুলে ফেলুন; দ্রুত ছড়ালে তবেই ছত্রাকনাশক দিন।",
            ),
        ),
        "Tomato___Septoria_leaf_spot" to AdvisoryEntry(
            severity = "মাঝারি",
            symptoms = "ছোট গোলাকার দাগ, গাঢ় কিনারা ও হালকা বাদামি/ধূসর মাঝখান; নিচের পাতা থেকে শুরু হয়ে উপরে ওঠে।",
            actions = listOf(
                "প্রথম দেখাতেই নিচের আক্রান্ত পাতা তুলে নষ্ট করুন।",
                "বৃষ্টির ছিটে মাটি থেকে পাতায় না লাগে, সেজন্য মালচ দিন।",
                "উপরে থেকে জল দেবেন না; গাছ খাড়া রাখতে খুঁটি দিন।",
                "শস্য পরিবর্তন করুন; দাগ উপরে উঠতে থাকলে ছত্রাকনাশক স্প্রে করুন।",
            ),
        ),
        "Tomato___Spider_mites Two-spotted_spider_mite" to AdvisoryEntry(
            severity = "মাঝারি",
            symptoms = "পাতায় সূক্ষ্ম হলুদ ছিট, বাদামি ভাব, পাতার নিচে সূক্ষ্ম জাল। লেন্স দিয়ে ছোট চলমান বিন্দু দেখা যায়।",
            actions = listOf(
                "পাতার নিচে জোরে জলের ধারা দিন।",
                "নিম তেল (অ্যাজাডিরাকটিন) পাতার নিচে ভালো করে স্প্রে করুন; সপ্তাহে একবার পুনরাবৃত্তি করুন।",
                "উপকারী মাইট বাঁচাতে ব্যাপক-ক্রিয়াশীল কীটনাশক এড়িয়ে চলুন।",
                "বেশি আক্রান্ত পাতা তুলে ফেলুন; গাছে ঠিকমতো জল দিন (শুকনো গাছে মাইট বেশি হয়)।",
            ),
        ),
        "Tomato___Target_Spot" to AdvisoryEntry(
            severity = "মাঝারি",
            symptoms = "পাতা ও কাণ্ডে গোলাকার বলয়যুক্ত বাদামি দাগ ('টার্গেট' আকৃতি); বেশি হলে অনেক পাতা ঝরে যায়।",
            actions = listOf(
                "মাঠ পরিষ্কার রাখুন — পড়ে থাকা পাতা ও আবর্জনা সরান।",
                "শস্য পরিবর্তন করুন; একই মাটিতে বারবার টমেটো লাগাবেন না।",
                "উপরে থেকে জল দেবেন না; জল নিকাশি ভালো রাখুন।",
                "দাগ ছড়ালে ম্যানকোজেব বা ক্লোরোথালোনিল ছত্রাকনাশক দিন।",
            ),
        ),
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to AdvisoryEntry(
            severity = "মারাত্মক",
            symptoms = "পাতা উপরের দিকে কুঁকড়ে যায়, শিরার মাঝে হলুদ হয়, গাছ বেঁটে ও ঝোপালো হয়, ফল কম ধরে। সাদা মাছি ছড়ায়। এর কোনো ওষুধ নেই।",
            actions = listOf(
                "এর কোনো ওষুধ নেই — আক্রান্ত গাছ এখনই উপড়ে নষ্ট করুন, যাতে পাশের গাছ বাঁচে।",
                "সাদা মাছি দমন করুন: হলুদ আঠালো ফাঁদ + নিম তেল স্প্রে।",
                "সাদা মাছি তাড়াতে রুপালি প্রতিফলিত মালচ ব্যবহার করুন।",
                "পরের মরশুমে TYLCV-প্রতিরোধী জাত লাগান।",
                "ক্ষেতের আশপাশের আগাছা পরিষ্কার করুন।",
            ),
        ),
        "Tomato___Tomato_mosaic_virus" to AdvisoryEntry(
            severity = "বেশি",
            symptoms = "পাতায় হালকা/গাঢ় সবুজ মোজাইক দাগ, পাতা কুঁকড়ে ও বেঁকে যায়। ছোঁয়া, যন্ত্রপাতি ও হাতের মাধ্যমে ছড়ায়। এর কোনো ওষুধ নেই।",
            actions = listOf(
                "কোনো ওষুধ নেই — আক্রান্ত গাছ এখনই তুলে নষ্ট করুন।",
                "যন্ত্রপাতি, খুঁটি ও হাত সাবান দিয়ে ধুয়ে জীবাণুমুক্ত করুন; ভেজা গাছ ধরবেন না।",
                "টমেটো গাছের কাছে ধূমপান করবেন না বা তামাক ধরবেন না।",
                "ভাইরাস বহনকারী আগাছা দমন করুন; প্রতিরোধী জাত লাগান।",
            ),
        ),
        "Tomato___healthy" to AdvisoryEntry(
            severity = "নেই — সুস্থ",
            symptoms = "রোগের কোনো লক্ষণ পাওয়া যায়নি। পাতা সুস্থ দেখাচ্ছে।",
            actions = listOf(
                "ভালো কাজ চালিয়ে যান — প্রতি সপ্তাহে গাছ পরীক্ষা করুন।",
                "সমানভাবে জল দিন ও বাতাস চলাচলের জন্য ফাঁক রাখুন।",
                "প্রতি ২ সপ্তাহে একবার নিম তেল স্প্রে করলে পোকা দূরে থাকে।",
                "পোকা লুকাতে না পারে, সেজন্য আগাছা নিয়মিত পরিষ্কার করুন।",
            ),
        ),
    )

    // ---- Hindi: direct translation of the same verified facts ----
    private val NAMES_HI = mapOf(
        "Tomato___Bacterial_spot" to "टमाटर बैक्टीरियल स्पॉट",
        "Tomato___Early_blight" to "टमाटर अर्ली ब्लाइट",
        "Tomato___Late_blight" to "टमाटर लेट ब्लाइट",
        "Tomato___Leaf_Mold" to "टमाटर लीफ़ मोल्ड",
        "Tomato___Septoria_leaf_spot" to "टमाटर सेप्टोरिया पत्ती धब्बा",
        "Tomato___Spider_mites Two-spotted_spider_mite" to "टमाटर स्पाइडर माइट",
        "Tomato___Target_Spot" to "टमाटर टारगेट स्पॉट",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to "टमाटर येलो लीफ़ कर्ल वायरस",
        "Tomato___Tomato_mosaic_virus" to "टमाटर मोज़ेक वायरस",
        "Tomato___healthy" to "स्वस्थ टमाटर पत्ता",
    )

    private val ADVISORIES_HI = mapOf(
        "Tomato___Bacterial_spot" to AdvisoryEntry(
            severity = "ज्यादा",
            symptoms = "पत्तियों पर छोटे पानी-भरे धब्बे जो गहरे भूरे/काले हो जाते हैं, अक्सर पीले घेरे के साथ। तने और फल तक फैल सकता है।",
            actions = listOf(
                "प्रमाणित रोग-मुक्त बीज और पौधे इस्तेमाल करें।",
                "ऊपर से सिंचाई न करें — पौधे की जड़ में पानी दें।",
                "पहला लक्षण दिखते ही ताँबा-आधारित जीवाणुनाशक छिड़कें; हर 7–10 दिन दोहराएँ।",
                "संक्रमित पत्तियाँ हटाकर नष्ट करें (खाद में न डालें)।",
                "2–3 मौसम फसल बदलें; उसी खेत में टमाटर/मिर्च न लगाएँ।",
            ),
        ),
        "Tomato___Early_blight" to AdvisoryEntry(
            severity = "मध्यम",
            symptoms = "नीचे की पुरानी पत्तियों पर गहरे गोल 'लक्ष्य' जैसे छल्ले; पत्तियाँ पीली होकर गिर जाती हैं। अल्टरनेरिया फफूँद से होता है।",
            actions = listOf(
                "मिट्टी के छींटे पत्तियों पर न पड़ें, इसलिए पौधों के आसपास मल्च डालें।",
                "नीचे की संक्रमित पत्तियाँ काटकर नष्ट करें।",
                "हर 10–14 दिन मैंकोज़ेब या क्लोरोथैलोनिल फफूँदनाशक छिड़कें।",
                "हवा के लिए पौधों में दूरी रखें; पत्तियाँ मिट्टी न छुएँ, इसलिए सहारा दें।",
                "अगले मौसम में सोलेनेसी कुल के अलावा कोई फसल लगाएँ।",
            ),
        ),
        "Tomato___Late_blight" to AdvisoryEntry(
            severity = "गंभीर",
            symptoms = "पीले पानी-भरे धब्बे जो भूरे हो जाते हैं; नमी वाले मौसम में पत्तियों के नीचे सफ़ेद फफूँद। बहुत तेज़ी से फैलता है।",
            actions = listOf(
                "तुरंत कार्रवाई करें — बुरी तरह संक्रमित पौधे उखाड़कर नष्ट करें (खाद में न डालें)।",
                "पत्तियाँ कभी गीली न करें; सुबह जड़ में पानी दें।",
                "नमी वाले मौसम में पहले से ताँबा या मेटालैक्सिल-आधारित फफूँदनाशक दें।",
                "अगले मौसम में प्रतिरोधी किस्में लगाएँ।",
                "कटाई के बाद खेत का सारा अवशेष साफ़ करें।",
            ),
        ),
        "Tomato___Leaf_Mold" to AdvisoryEntry(
            severity = "मध्यम",
            symptoms = "पत्ती की ऊपरी सतह पर हल्के पीले धब्बे; नीचे जैतूनी-हरे से भूरे मखमली फफूँद। नमी और भीड़-भाड़ में ज़्यादा होता है।",
            actions = listOf(
                "हवा का संचार बढ़ाएँ — घनी पत्तियाँ छाँटें, पौधों में दूरी बढ़ाएँ।",
                "पत्तियाँ गीली न करें; ग्रीनहाउस/पॉलीहाउस में हवा का प्रबंध करें।",
                "सुबह पानी दें ताकि शाम तक पत्तियाँ सूख जाएँ।",
                "बुरी तरह संक्रमित पत्तियाँ हटाएँ; तेज़ी से फैले तभी फफूँदनाशक दें।",
            ),
        ),
        "Tomato___Septoria_leaf_spot" to AdvisoryEntry(
            severity = "मध्यम",
            symptoms = "छोटे गोल धब्बे, गहरी किनारी और हल्के भूरे/स्लेटी बीच वाला भाग; नीचे की पत्तियों से शुरू होकर ऊपर बढ़ता है।",
            actions = listOf(
                "पहली बार दिखते ही नीचे की संक्रमित पत्तियाँ तोड़कर नष्ट करें।",
                "बारिश के छींटे मिट्टी से पत्तियों तक न पहुँचें, इसलिए मल्च डालें।",
                "ऊपर से पानी न दें; पौधे सीधे रखने के लिए सहारा दें।",
                "फसल बदलें; धब्बे ऊपर चढ़ते रहें तो फफूँदनाशक छिड़कें।",
            ),
        ),
        "Tomato___Spider_mites Two-spotted_spider_mite" to AdvisoryEntry(
            severity = "मध्यम",
            symptoms = "पत्तियों पर बारीक पीली चित्तियाँ, भूरापन, और पत्तियों के नीचे बारीक जाले। लेंस से छोटे चलते बिंदु दिखते हैं।",
            actions = listOf(
                "पत्तियों के नीचे तेज़ पानी की धार मारें।",
                "नीम तेल (एज़ाडिरैक्टिन) पत्तियों के नीचे अच्छी तरह छिड़कें; हफ़्ते में एक बार दोहराएँ।",
                "शिकारी घुन बचाने के लिए व्यापक कीटनाशक न इस्तेमाल करें।",
                "बुरी तरह संक्रमित पत्तियाँ हटाएँ; पौधों को ठीक से पानी दें (सूखे पौधों पर घुन ज़्यादा होते हैं)।",
            ),
        ),
        "Tomato___Target_Spot" to AdvisoryEntry(
            severity = "मध्यम",
            symptoms = "पत्तियों और तनों पर गोल छल्लों वाले भूरे धब्बे ('लक्ष्य' जैसा पैटर्न); ज़्यादा हो तो बहुत पत्तियाँ झड़ जाती हैं।",
            actions = listOf(
                "खेत की सफ़ाई रखें — गिरी पत्तियाँ और कचरा हटाएँ।",
                "फसल बदलें; एक ही मिट्टी में लगातार टमाटर न लगाएँ।",
                "ऊपर से सिंचाई न करें; जल-निकासी ठीक रखें।",
                "धब्बे फैलें तो मैंकोज़ेब या क्लोरोथैलोनिल फफूँदनाशक दें।",
            ),
        ),
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to AdvisoryEntry(
            severity = "गंभीर",
            symptoms = "पत्तियाँ ऊपर की ओर मुड़ जाती हैं, नसों के बीच पीलापन, पौधा बौना और झाड़ीनुमा, फल कम लगते हैं। सफ़ेद मक्खी फैलाती है। इसका कोई इलाज नहीं।",
            actions = listOf(
                "कोई इलाज नहीं — संक्रमित पौधे तुरंत उखाड़कर नष्ट करें ताकि पड़ोसी पौधे बचें।",
                "सफ़ेद मक्खी रोकें: पीले चिपचिपे जाल + नीम तेल छिड़काव।",
                "सफ़ेद मक्खी भगाने के लिए चाँदी जैसी परावर्तक मल्च इस्तेमाल करें।",
                "अगले मौसम में TYLCV-प्रतिरोधी किस्में लगाएँ।",
                "खेत के आसपास के खरपतवार हटाएँ।",
            ),
        ),
        "Tomato___Tomato_mosaic_virus" to AdvisoryEntry(
            severity = "ज्यादा",
            symptoms = "हल्के/गहरे हरे मोज़ेक धब्बे, पत्तियाँ मुड़ना और टेढ़ी होना। छूने, औज़ारों और हाथों से फैलता है। कोई इलाज नहीं।",
            actions = listOf(
                "कोई इलाज नहीं — संक्रमित पौधे तुरंत हटाकर नष्ट करें।",
                "औज़ार, सहारे और हाथ साबुन से धोकर साफ़ करें; गीले पौधे न छुएँ।",
                "टमाटर के पौधों के पास धूम्रपान न करें और तंबाकू न छुएँ।",
                "वायरस वाले खरपतवार हटाएँ; प्रतिरोधी किस्में लगाएँ।",
            ),
        ),
        "Tomato___healthy" to AdvisoryEntry(
            severity = "नहीं — स्वस्थ",
            symptoms = "रोग का कोई लक्षण नहीं मिला। पत्ता स्वस्थ दिख रहा है।",
            actions = listOf(
                "अच्छा काम जारी रखें — हर हफ़्ते पौधे जाँचें।",
                "बराबर पानी दें और हवा के लिए दूरी रखें।",
                "हर 2 हफ़्ते नीम तेल छिड़कने से कीट दूर रहते हैं।",
                "कीटों को छिपने की जगह न मिले, इसलिए खरपतवार हटाते रहें।",
            ),
        ),
    )

    /** Grounded prompt (English generation only): diagnosis + verified facts.
     * Pass severityOverride = SeverityEngine result's bandForPrompt (e.g.
     * "Moderate (proposed, 15.6% leaf coverage)") once severity is wired in;
     * null keeps the fixed per-disease label as before. */
    fun buildPrompt(
        label: String,
        confidence: Float,
        region: String = "West Bengal",
        language: String = "English",
        severityOverride: String? = null,
    ): String {
        val entry = ADVISORIES[label]
            ?: return "The leaf looks healthy. Give the farmer one short tip to keep the crop healthy, in plain simple $language."
        return buildString {
            appendLine("You are a crop-disease adviser for smallholder farmers in $region. Answer ONLY using the facts below. Keep the whole reply under 60 words, in plain simple $language. Never invent treatments, chemicals or doses. If a fact is not listed, say you don't know.")
            appendLine()
            appendLine("DISEASE: ${label.replace("___", " — ").replace('_', ' ')} (confidence ${(confidence * 100).toInt()}%)")
            appendLine("SEVERITY: ${severityOverride ?: entry.severity}")
            appendLine("SYMPTOMS: ${entry.symptoms}")
            appendLine("VERIFIED ACTIONS:")
            entry.actions.forEachIndexed { i, a -> appendLine("${i + 1}. $a") }
            appendLine()
            appendLine("Reply with: (1) what this disease is, in one line; (2) the 3 most urgent things to do now; (3) one prevention tip for next season.")
        }
    }

    /**
     * Verified advice in the requested language. Bengali/Hindi use the
     * translated database; English is the generation fallback.
     */
    fun staticAdvisory(label: String, language: String = "English"): String {
        return when (language) {
            "Bengali" -> {
                val entry = ADVISORIES_BN[label] ?: return "এই ফলাফলের জন্য যাচাই করা পরামর্শ এখনো নেই।"
                val name = NAMES_BN[label] ?: label
                buildString {
                    appendLine("$name — তীব্রতা: ${entry.severity}")
                    appendLine(entry.symptoms)
                    appendLine()
                    entry.actions.forEachIndexed { i, a -> appendLine("${i + 1}. $a") }
                }.trim()
            }
            "Hindi" -> {
                val entry = ADVISORIES_HI[label] ?: return "इस परिणाम के लिए सत्यापित सलाह अभी नहीं है।"
                val name = NAMES_HI[label] ?: label
                buildString {
                    appendLine("$name — गंभीरता: ${entry.severity}")
                    appendLine(entry.symptoms)
                    appendLine()
                    entry.actions.forEachIndexed { i, a -> appendLine("${i + 1}. $a") }
                }.trim()
            }
            else -> {
                val entry = ADVISORIES[label] ?: return "No verified advice for this result yet."
                buildString {
                    appendLine("${label.replace("___", " — ").replace('_', ' ')} — severity: ${entry.severity}")
                    appendLine(entry.symptoms)
                    appendLine()
                    entry.actions.forEachIndexed { i, a -> appendLine("${i + 1}. $a") }
                }.trim()
            }
        }
    }
}
