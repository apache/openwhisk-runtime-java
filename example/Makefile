IMG=actionloop-java-v8:latest
ACT=hello-java
PREFIX=docker.io/openwhisk
GSON=gson-2.8.5.jar
SRC=Main.java action/Hello.java action/Main.java 

main.jar:  $(SRC) | $(GSON)
	javac -cp $(GSON) $?
	jar cvf main.jar *.class action/*.class

$(GSON):
	curl -LO "https://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.8.5/gson-2.8.5.jar"

src.zip: $(SRC)
	zip src.zip $?

bin.zip: src.zip
	docker run -i $(IMG) -compile main <src.zip >bin.zip

test-single:
	wsk action update $(ACT) hello.py --docker $(PREFIX)/$(IMG) --main hello
	wsk action invoke $(ACT) -r
	wsk action invoke $(ACT) -p name Mike -r

test-src-zip: src.zip
	wsk action update $(ACT) $< --docker $(PREFIX)/$(IMG)
	wsk action invoke $(ACT) -r
	wsk action invoke $(ACT) -p name Mike -r

test-bin-zip: bin.zip
	wsk action update $(ACT) $< --docker $(PREFIX)/$(IMG)
	wsk action invoke $(ACT) -r
	wsk action invoke $(ACT) -p name Mike -r

.PHONE: test-single test-src-zip test-bin-zip

