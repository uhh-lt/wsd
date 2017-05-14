const data = {
  "context": "Java is an island.",
  "contextFeatures": [
    "be",
    "a",
    "island",
    "."
  ],
  "word": "java",
  "modelName": "cosets",
  "predictions": [
    {
      "senseCluster": {
        "id": "5360",
        "lemma": "Java",
        "hypernyms": ["country"],
        "words": [
          "sumatra",
          "indonesia"
        ]
      },
      "confidenceProb": 0.7334364458614677,
      "contextFeatures": [{
          "label": "island",
          "weight": 1
      }],
      "mutualFeatures": [{
          "label": "island",
          "weight": 0.010348378920223236
      }],
      "numClusterFeatures": 5718,
      "rank": "0",
      "simScore": 0.010348378920223236,
      "top20ClusterFeatures": [{
          "label": "banda",
          "weight": 0.07481755537999021
      }]
    }
  ]
};

export default data