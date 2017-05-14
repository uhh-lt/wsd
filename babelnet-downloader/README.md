# A short guide

## Configuration

Copy `sample_env` to `.env` in this directory.

Within this file you will have to fill the `BABELNET_KEY` variable.
And also put a file with a list of words and provide a relative path with the `WORDLIST_FILE` variable.

Note: Please provide only relative paths if you want to work with Docker.

## With Docker (recommended)

Just run the `./run-docker.sh` script.

## Without Docker

### Installation requirments

For details please consult the Dockerfile.

However in general you will need:

- Composer (+ Git)
- PHP (with curl support)

And run `composer install`.

### Run

To start the download:
 
 `php -f download.sh`

And to show some statistics for the downloaded data:
 
 `./print-statistics.sh`.