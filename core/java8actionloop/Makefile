IMG=actionloop-java-v8:latest
PREFIX=docker.io/openwhisk
INVOKE=python ../../tools/invoke.py
MAIN_JAR=../../example/main.jar

build:
	docker build -t $(IMG) .

push: build
	docker login
	docker tag $(IMG) $(PREFIX)/$(IMG) 
	docker push $(PREFIX)/$(IMG)

clean:
	docker rmi -f $(IMG)

start: build
	docker run -p 8080:8080 -ti -v $(PWD):/proxy $(IMG)

debug: build
	docker run -p 8080:8080 -ti --entrypoint=/bin/sh -v $(PWD):/proxy $(IMG)

.PHONY: build push clean start debug

$(MAIN_JAR):
	$(MAKE) $< -C ../../example main.jar

## You need to execute make start in another terminal 
test-jar: $(MAIN_JAR)
	$(INVOKE) init Hello#hello $(MAIN_JAR)
	$(INVOKE) run '{}'
	$(INVOKE) run '{"name":"Mike"}'

test-single:
	$(INVOKE) init hello ../example/hello.py
	$(INVOKE) run '{}'
	$(INVOKE) run '{"name":"Mike"}'

test-src-zip:
	$(MAKE) -C ../example src.zip
	$(INVOKE) init ../example/src.zip
	$(INVOKE) run '{}'
	$(INVOKE) run '{"name":"Mike"}'

test-bin-zip:
	$(MAKE) -C ../example bin.zip
	$(INVOKE) init ../example/bin.zip
	$(INVOKE) run '{}'
	$(INVOKE) run '{"name":"Mike"}'
