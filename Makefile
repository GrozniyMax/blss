clean-java:
	./gradlew clean

down-docker:
	docker compose down -v --remove-orphans

clean: clean-java down-docker

build-java:
	./gradlew bootJar

up-docker:
	docker compose up -d --build

up: build-java up-docker

down: down-docker

rebuild: down-docker clean-java up

restart:
	docker compose down -v
	docker compose up -d


