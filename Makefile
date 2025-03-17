# Maven command
MVN = mvn

# Arguments for Maven's exec plugin
MAIN_CLASS = Main
SOURCE = test/basic_expression.mj
BYTECODE = test/basic_expression.bc


# Compile the project using Maven
compile:
	$(MVN) clean compile

# Run the project using Maven's exec plugin with the specified main class
run: compile
	$(MVN) exec:java -Dexec.mainClass=$(MAIN_CLASS) -Dexec.args="$(SOURCE) $(BYTECODE)"-e

# Clean the project (remove generated files)
clean:
	$(MVN) clean

.PHONY: compile run clean