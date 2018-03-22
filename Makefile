.PHONY: test clean start-db

docker_sbt := scripts/utils/docker_sbt.sh

test: start-db
	docker-compose exec db dropdb -U postgres wsp_development_default --if-exists
	docker-compose exec db createdb -U postgres -T wsp_default wsp_development_default
	./sbt/sbt -Dspark.master="local[*]" test -ivy .ivy2

start-db: api/target/docker/stage
	docker-compose up -d db
	scripts/utils/wait_for_db.sh

api/target/docker/stage:
	./sbt/sbt api/docker:stage

clean:
	rm -rd api/target
	rm -rd common/target
	rm -rd spark/target
