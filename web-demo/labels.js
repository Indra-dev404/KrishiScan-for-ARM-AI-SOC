/* Verify these against: print(test_dl.dataset.classes) in your Kaggle notebook.
   Order must match the model's training order (ImageFolder alphabetical). */
const LABELS = [
  "Tomato___Bacterial_spot",
  "Tomato___Early_blight",
  "Tomato___Late_blight",
  "Tomato___Leaf_Mold",
  "Tomato___Septoria_leaf_spot",
  "Tomato___Spider_mites Two-spotted_spider_mite",
  "Tomato___Target_Spot",
  "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
  "Tomato___Tomato_mosaic_virus",
  "Tomato___healthy",
];

function prettyName(folder) {
  return folder.replace("Tomato___", "").replace(/_/g, " ");
}
