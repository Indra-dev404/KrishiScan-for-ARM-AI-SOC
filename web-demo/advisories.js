/* Advisory database: keyed by exact class folder name.
   Keep entries short: what it looks like + what to do now. */
const ADVISORIES = {
  "Tomato___Bacterial_spot": {
    severity: "High",
    symptoms: "Small water-soaked spots on leaves that turn dark brown/black, often with a yellow halo. Can spread to stems and fruit.",
    actions: [
      "Use certified disease-free seeds and seedlings.",
      "Avoid overhead irrigation — water at the base of the plant.",
      "Spray a copper-based bactericide at first sign; repeat every 7–10 days.",
      "Remove and destroy infected leaves (do not compost).",
      "Rotate crops for 2–3 seasons; avoid planting tomato/pepper in the same plot.",
    ],
  },
  "Tomato___Early_blight": {
    severity: "Medium",
    symptoms: "Dark concentric 'target' rings on older, lower leaves; leaves yellow and drop. Caused by Alternaria fungus.",
    actions: [
      "Mulch around plants to stop soil splashing onto leaves.",
      "Prune off the lowest infected leaves and destroy them.",
      "Spray mancozeb or chlorothalonil fungicide every 10–14 days.",
      "Space plants well for airflow; stake to keep foliage off soil.",
      "Rotate with non-solanaceous crops next season.",
    ],
  },
  "Tomato___Late_blight": {
    severity: "Critical",
    symptoms: "Pale water-soaked lesions that turn brown; white moldy growth under leaves in humid weather. Spreads extremely fast.",
    actions: [
      "Act immediately — uproot and destroy badly infected plants (do not compost).",
      "Never wet the foliage; water early morning at the base.",
      "Apply copper or metalaxyl-based fungicide preventively in humid weather.",
      "Plant resistant varieties next season.",
      "Clear all crop debris after harvest.",
    ],
  },
  "Tomato___Leaf_Mold": {
    severity: "Medium",
    symptoms: "Pale yellow spots on upper leaf surface; olive-green to brown velvety mold underneath. Thrives in humid, crowded conditions.",
    actions: [
      "Improve air circulation — thin crowded foliage, space plants wider.",
      "Avoid wetting leaves; ventilate greenhouses/polyhouses.",
      "Water in the morning so foliage dries by evening.",
      "Remove severely infected leaves; fungicide only if spreading fast.",
    ],
  },
  "Tomato___Septoria_leaf_spot": {
    severity: "Medium",
    symptoms: "Small circular spots with dark borders and tan/grey centers, starting on lower leaves and moving upward.",
    actions: [
      "Pick off affected lower leaves at first sight and destroy them.",
      "Mulch to prevent rain splash from soil.",
      "Avoid overhead watering; stake plants upright.",
      "Rotate crops; spray fungicide if spots keep climbing the plant.",
    ],
  },
  "Tomato___Spider_mites Two-spotted_spider_mite": {
    severity: "Medium",
    symptoms: "Fine yellow stippling/speckling on leaves, bronzing, and delicate webbing under leaves. Tiny moving dots visible with a lens.",
    actions: [
      "Blast undersides of leaves with a strong jet of water.",
      "Spray neem oil (azadirachtin) covering leaf undersides; repeat weekly.",
      "Encourage predatory mites — avoid broad-spectrum insecticides.",
      "Remove heavily infested leaves; keep plants well-watered (mites love dry stress).",
    ],
  },
  "Tomato___Target_Spot": {
    severity: "Medium",
    symptoms: "Brown lesions with concentric rings ('target' pattern) on leaves and stems; can cause heavy defoliation.",
    actions: [
      "Practice field sanitation — clear fallen leaves and debris.",
      "Rotate crops; avoid planting tomato in the same soil consecutively.",
      "Avoid overhead irrigation; improve drainage.",
      "Apply mancozeb or chlorothalonil fungicide if lesions spread.",
    ],
  },
  "Tomato___Tomato_Yellow_Leaf_Curl_Virus": {
    severity: "Critical",
    symptoms: "Upward curling of leaves, yellowing between veins, stunted bushy growth, poor fruit set. Spread by whiteflies. No cure.",
    actions: [
      "There is no cure — uproot and destroy infected plants immediately to protect neighbours.",
      "Control whiteflies: yellow sticky traps + neem oil sprays.",
      "Use reflective silver mulch to repel whiteflies.",
      "Plant TYLCV-resistant varieties next season.",
      "Remove weed hosts around the field.",
    ],
  },
  "Tomato___Tomato_mosaic_virus": {
    severity: "High",
    symptoms: "Mottled light/dark green mosaic pattern, leaf curling and distortion. Spreads by touch, tools, and hands. No cure.",
    actions: [
      "No cure — remove and destroy infected plants at once.",
      "Disinfect tools, stakes and hands (wash with soap); avoid handling plants when wet.",
      "Do not smoke or handle tobacco near tomato plants.",
      "Control weeds that host the virus; plant resistant varieties.",
    ],
  },
  "Tomato___healthy": {
    severity: "None — healthy",
    symptoms: "No disease symptoms detected. Leaf looks healthy.",
    actions: [
      "Keep up the good work — inspect plants weekly.",
      "Maintain even watering and good spacing for airflow.",
      "A preventive neem-oil spray every 2 weeks keeps pests away.",
      "Remove weeds regularly to deny pests a hiding place.",
    ],
  },
};
