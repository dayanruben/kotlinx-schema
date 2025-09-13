
.PHONY: build
build:
	./gradlew  build koverLog koverXmlReport

.PHONY: test
test:
	./gradlew test

.PHONY: clean
clean:
	./gradlew clean
