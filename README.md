# Unsupervised Knowledge Free Word Sense Disambiguation

A software to construct and visualize Word Sense Disambiguation models based on [JoBimText models](http://ltmaggie.informatik.uni-hamburg.de/jobimtext/).


This project consists of multiple subprojects: 

- A REST **api** project
- A ReactJS **web** frontend project
- A **spark** project for calculating the model and exporting it to a DB
- A **babelnet-downloader** to retrieve test data

This README covers how to deploy the Web frontend together with the API.
For topics on the other subprojects you will find READMEs within their folders.

# Deployment of Web frontend and API

## Prerequists

- Docker Engine (1.13.0+), see [Docker installation guide](https://docs.docker.com/engine/installation/)
- Docker Compose (1.10.0+), see [Compose installation guide](https://docs.docker.com/compose/install/)

## Instructions

Start by checking out this repository.

```bash
git clone https://github.com/uhh-lt/wsd
cd wsd
```

### 1. Prepare the postgres DB

We recommend to first use a toy training dataset to build a toy model within a few minutes. Building the full model is the most time intensive part and also currently least documented. That is why we thought you might not want to start with it, but first finish the installation of the web application and then come back to build the full model in seciton 1.2.

#### 1.1 Build small toy model
```bash
./scripts/build_toy_model.sh
```
This model only provides senses for the word "Python" but is fully functional and should be used during the initial setup of the web application.

#### 1.2 Build full model

Once you are ready to build the full model, here is how to do it. It will take nearly 11 hours on an eight core machine with 30GB of memory and needs 120GB of free disk space.

- First follow the instruction in `data/training/README.md` to download the training data into the same folder.
- Than take a lookt at the script `scripts/spark_submit_jar.sh` and adjust the amount of memory used to whatever you want to provide to Spark.
- Optionally you can copy `sample-app.conf` to `app.conf` and include the commented out params in `scripts/spark_submit_jar.sh` and further configure the software by changing `app.conf`. Take a look at `sample-app.conf` to get an idea of what can be configured.
- Finally use the following script to compute the model: `scripts/build_model.sh`

### 2. Start the web application

The web application useses Docker Compose to manage three services: DB, API and Website.

To start the application:

- First copy the configuration file: `cp sample-docker-compose.override.yml docker-compose.override.yml`.
- By changing `docker-compose.override.yml` you can customize the deployment.
See the [official documentation](https://docs.docker.com/compose/compose-file/) for detailed explanation of this file.
- Then start the web application with: `./scripts/web_app/start.sh`

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
