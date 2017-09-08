# Unsupervised Knowledge Free Word Sense Disambiguation

A software to construct and visualize Word Sense Disambiguation models based on [JoBimText models](http://ltmaggie.informatik.uni-hamburg.de/jobimtext/). This project implements the method described in the following paper, please cite it if you use the paper in a research project:

* Panchenko A., Marten F., Ruppert E.,  Faralli S., Ustalov D., Ponzetto S.P., Biemann C. [Unsupervised, Knowledge-Free, and Interpretable Word Sense Disambiguation](https://arxiv.org/abs/1707.06878). In Proceedings of the the Conference on Empirical Methods on Natural Language Processing (EMNLP 2017). 2017. Copenhagen, Denmark. Association for Computational Linguistics

```bibtex
@inproceedings{Panchenko:17:emnlp,
  author    = {Panchenko, Alexander and Marten, Fide and Ruppert, Eugen and Faralli, Stefano  and Ustalov, Dmitry and Ponzetto, Simone Paolo and Biemann, Chris},
  title     = {{Unsupervised, Knowledge-Free, and Interpretable Word Sense Disambiguation}},
  booktitle = {In Proceedings of the the Conference on Empirical Methods on Natural Language Processing (EMNLP 2017)},
  year      = {2017},
  address   = {Copenhagen, Denmark},
  publisher = {Association for Computational Linguistics},
  language  = {english}
}
```

# Prerequisites

- Java 1.8
- Docker Engine (1.13.0+), see [Docker installation guide](https://docs.docker.com/engine/installation/)
- Docker Compose (1.10.0+), see [Compose installation guide](https://docs.docker.com/compose/install/)
- (Spark 2.0+, to [build your own model](#build-your-own-db))


# Serving the WSD model

[Online demo](http://ltbev.informatik.uni-hamburg.de/wsd)

## Download precalculated DB and pictures

We provide a ready for use database and a dump of pictures for all senses in the database.
To download and prepare the project with those two artifacts, you can use the following command:

To download and untar it, you will need 300 GB of free disk space!

```bash
./wsd model:donwload
```

Note: For instructions on how to rebuild the DB with the model, please see below: [Build your own DB](#build-your-own-db)

## Start the web application

To start the application:

```bash
./wsd web-app:start
```

The web application runs with Docker Compose. To customize your installation adjust `docker-compose.override.yml`. See the [official documentation](https://docs.docker.com/compose/compose-file/) for general information on this file.

To get further information on the running containers you can use all Docker Compose commands, such as `docker-compose ps` and `docker-compose logs`.

# Build your own DB

First set the `$SPARK_HOME` environment variable or provide `spark-submit` on your path. TODO link

By modifying the script `scripts/spark_submit_jar.sh` you can adjust the amount of memory used by Spark (consider changing `--conf 'spark.driver.memory=4g'` and `--conf 'spark.executor.memory=1g'`).

We recommend to first use a **toy training data set** to build a toy model within a few minutes.

## Build small toy model

```bash
./wsd model:build-toy
```

This model only provides senses for the word "Python" but is fully functional and should be used during the initial setup of the web application.

## Build full model

Building the full model will take nearly 11 hours on an eight core machine with 30 GB of memory and needs around 300 GB of free disk space. It will also download 4 GB of training data.

# See also

```bash
./wsd --help
```
