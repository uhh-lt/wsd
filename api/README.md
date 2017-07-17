# API Documentation

## Calling the API

When using the below examples, please set `$YOUR_API_SERVER` appropriately.
Per default the API runs under port `9000`, i.e run
 
```bash
 YOUR_API_SERVER="http://your-host.com:9000"
 ```


### Predict single word sense enpoint

Endpoint: `/predictSense`

#### Example request


```bash
curl -H "Content-Type: application/json" \
  -X POST \
  -d '{"context":"Java is an island.","word":"Java", "model": "simwords"}' \
  $YOUR_API_SERVER/predictWordSense
```

The `modelName` selects the prediction model. Currently the following
 models are implemented: `simwords`, `clusterwords` ,`hybridwords` and `cosets`.
  

#### Example response

```json
{
  "context": "Java is an island.",
  "contextFeatures": [
    "be",
    "a",
    "island",
    "."
  ],
  "word": "java",
  "predictions": [
    {
      "senseCluster": {
        "id": 5360,
        "lemma": "Java",
        "hypernyms": [
          [
            "country"
          ]
        ],
        "words": [
          "sumatra",
          "indonesia"
        ],
        "sampleSentences": [
          {
            "text": "Java is an island.",
            "position": {
              "start": 0,
              "end": 4
            }
          }
        ]
      },
      "confidenceProb": 0.7334364458614677,
      "contextFeatures": [
        {
          "label": "island",
          "weight": 1
        }
      ],
      "mutualFeatures": [
        {
          "label": "island",
          "weight": 0.010348378920223236
        }
      ],
      "numClusterFeatures": 5718,
      "rank": "0",
      "simScore": 0.010348378920223236,
      "top20ClusterFeatures": [
        {
          "label": "banda",
          "weight": 0.07481755537999021
        }
      ]
    }
  ]
}

```