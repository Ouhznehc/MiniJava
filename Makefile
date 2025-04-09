# Maven command
MVN = mvn

# Arguments for Maven's exec plugin
MAIN_CLASS = Main
SOURCE = test/basic_expression.mj


# Compile the project using Maven
compile:
	@$(MVN) compile

# Run the project using Maven's exec plugin with the specified main class
run:
	@$(MVN) exec:java -Dexec.mainClass=$(MAIN_CLASS) -Dexec.args="$(SOURCE)" -e -q

# Clean the project (remove generated files)
clean:
	@$(MVN) clean

# Count the number of lines of code in the project
count:
	@scc .

.PHONY: compile run clean count