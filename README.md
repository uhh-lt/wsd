# Unsupervised Knowledge Free Word Sense Disambiguation

A software to construct and visualize Word Sense Disambiguation models based on [JoBimText models](http://ltmaggie.informatik.uni-hamburg.de/jobimtext/). This project implements the method described in the following paper, please cite it if you use the paper in a research project:

* Panchenko A., Marten F., Ruppert E.,  Faralli S., Ustalov D., Ponzetto S.P., Biemann C. [Unsupervised, Knowledge-Free, and Interpretable Word Sense Disambiguation](https://arxiv.org/abs/1707.06878). In Proceedings of the the Conference on Empirical Methods on Natural Language Processing (EMNLP 2017). 2017. Copenhagen, Denmark. Association for Computational Linguistics

```latex
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

This project consists of multiple subprojects: 

- A REST **api** project
- A ReactJS **web** frontend project
- A **spark** project for calculating the model and exporting it to a DB

This README covers how to deploy the Web frontend together with the API.
For topics on the other subprojects you will find READMEs within their folders.

# Deployment of Web frontend and API

## Prerequisites

- Docker Engine (1.13.0+), see [Docker installation guide](https://docs.docker.com/engine/installation/)
- Docker Compose (1.10.0+), see [Compose installation guide](https://docs.docker.com/compose/install/)

## Instructions

Start by checking out this repository.

```bash
git clone https://github.com/uhh-lt/wsd
cd wsd
```

### 1. Download precalculated DB and images

We provide a ready for use database. To download and prepare the project with this database, you can use the following instructions:

```bash
wget http://ltdata1.informatik.uni-hamburg.de/joint/wsd/20170831_wsd_db.tar
tar -xf 20170719_wsd_db.tar
# Adjust UID, 999 is the postgres user in the wsd_db docker container
docker run -v "$(pwd)/pgdata:/pgdata" alpine chown -R 999:999 /pgdata/data
```

NOTE: The Postgres data is currently around 120 GB!

For instructions on how to rebuild the DB with the model, please see below: [Build your own DB](#build-your-own-db)

Additionally we provide an archive with images for most senses.

```bash
wget http://ltdata1.informatik.uni-hamburg.de/joint/wsd/20170831_all_senses_imgdata.tgz
tar -xzf 20170721_wsd_images.tgz
# Adjust UID, 1 is the daemon user in the wsd_api docker container
docker run -v "$(pwd)/imgdata:/imgdata" alpine chown -R 1:1 /imgdata/bing
```

NOTE: The image data is currently around 7 GB!

### 2. Start the web application

The web application useses Docker Compose to manage three services: DB, API and Website.

To start the application:

- First copy the configuration file: `cp sample-docker-compose.override.yml docker-compose.override.yml`.
- By changing `docker-compose.override.yml` you can customize the deployment.
See the [official documentation](https://docs.docker.com/compose/compose-file/) for detailed explanation of this file.
- Then start the web application with: `./scripts/start_web_app.sh`

# Build your own DB

In case you want to build the model on your own, we recommend to first use a toy training dataset to build a toy model within a few minutes. Building the full model is the most time intensive part and also currently least documented. That is why we thought you might not want to start with it, but first finish the installation of the web application and then come back to build the full model in seciton 1.2.

## Build small toy model
```bash
./scripts/build_toy_model.sh
```
This model only provides senses for the word "Python" but is fully functional and should be used during the initial setup of the web application.

## Build full model

Once you are ready to build the full model, here is how to do it. It will take nearly 11 hours on an eight core machine with 30GB of memory and needs 120GB of free disk space.

- First follow the instruction in `data/training/README.md` to download the training data into the same folder.
- Then take a lookt at the script `scripts/spark_submit_jar.sh` and adjust the amount of memory used to whatever you want to provide to Spark.
- Optionally you can copy `sample-app.conf` to `app.conf` and include the commented out params in `scripts/spark_submit_jar.sh` and further configure the software by changing `app.conf`. Take a look at `sample-app.conf` to get an idea of what can be configured.
- Finally use the following script to compute the model: `scripts/build_model.sh`

# Maintenance related commands

You can get informations about the running containers with:
```bash
docker-compose ps
docker-compose logs
docker-compose top
```

Shut the container down (i.e. removing them) with:
```bash
docker-compose down
```

For further information on Compose consult the [documentation](https://docs.docker.com/compose/)
