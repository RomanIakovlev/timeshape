# Makefile for Timeshape data processing
# Handles timezone data download and resource generation for Maven build

# Variables
DATA_VERSION ?= 2025b
BUILDER_JAR = builder/target/timeshape-builder.jar
OUTPUT_DIR = core/target/classes
OUTPUT_FILE = $(OUTPUT_DIR)/data.tar.zstd
TIMEZONE_URL = https://github.com/evansiroky/timezone-boundary-builder/releases/download/$(DATA_VERSION)/timezones-with-oceans.geojson.zip
CACHE_DIR = .cache
CACHED_DATA = $(CACHE_DIR)/timezones-$(DATA_VERSION).zip

# Use Maven to execute Java commands (same JVM as Maven uses)
MVN = mvn

# Ensure directories exist
$(OUTPUT_DIR):
	@mkdir -p $(OUTPUT_DIR)

$(CACHE_DIR):
	@mkdir -p $(CACHE_DIR)

# Build the builder JAR if it doesn't exist or is out of date
$(BUILDER_JAR): builder/src/main/java/net/iakovlev/timeshape/*.java geojson-proto/src/main/protobuf/geojson.proto
	@echo "Building timeshape-builder and dependencies..."
	@echo "  - Compiling protobuf and installing geojson-proto..."
	@cd geojson-proto && $(MVN) -q compile install
	@echo "  - Compiling builder..."
	@cd builder && $(MVN) -q compile
	@echo "  - Creating builder assembly..."
	@cd builder && $(MVN) -q assembly:single

# Download and cache timezone data
$(CACHED_DATA): $(CACHE_DIR)
	@echo "Downloading timezone data version: $(DATA_VERSION)"
	@if curl -L -f -s -o $(CACHED_DATA) "$(TIMEZONE_URL)"; then \
		echo "Downloaded timezone data to $(CACHED_DATA)"; \
	else \
		echo "Failed to download timezone data from $(TIMEZONE_URL)"; \
		exit 1; \
	fi

# Download timezone data (public target)
download-data: $(CACHED_DATA)

# Execute builder using Maven (ensures same Java as Maven)
run-builder:
	@if [ -f "$(CACHED_DATA)" ]; then \
		echo "Using cached timezone data from $(CACHED_DATA)"; \
		cd builder && $(MVN) -q exec:java -Dexec.mainClass="net.iakovlev.timeshape.Main" -Dexec.args="$(CACHED_DATA) ../$(OUTPUT_FILE)"; \
	elif [ -f "/tmp/timezones-$(DATA_VERSION).zip" ]; then \
		echo "Using timezone data from /tmp/timezones-$(DATA_VERSION).zip"; \
		cd builder && $(MVN) -q exec:java -Dexec.mainClass="net.iakovlev.timeshape.Main" -Dexec.args="/tmp/timezones-$(DATA_VERSION).zip ../$(OUTPUT_FILE)"; \
	else \
		echo "Downloading and processing timezone data directly"; \
		cd builder && $(MVN) -q exec:java -Dexec.mainClass="net.iakovlev.timeshape.Main" -Dexec.args="$(DATA_VERSION) ../$(OUTPUT_FILE)"; \
	fi

# Generate data.tar.zstd resource file
generate-data: $(OUTPUT_DIR) $(BUILDER_JAR)
	@if [ -f "$(OUTPUT_FILE)" ]; then \
		echo "Timeshape resource exists at $(OUTPUT_FILE), skipping creation"; \
	else \
		echo "Timeshape resource doesn't exist, creating it now"; \
		echo "Generating timezone data with version: $(DATA_VERSION)"; \
		$(MAKE) run-builder; \
		if [ -f "$(OUTPUT_FILE)" ]; then \
			echo "Successfully generated $(OUTPUT_FILE)"; \
		else \
			echo "Failed to generate $(OUTPUT_FILE)"; \
			exit 1; \
		fi; \
	fi

# Clean generated files
clean-data:
	@echo "Cleaning generated data files..."
	@rm -f $(OUTPUT_FILE)
	@rm -f /tmp/timezones-*.zip

# Clean cache
clean-cache:
	@echo "Cleaning cache directory..."
	@rm -rf $(CACHE_DIR)

# Clean all build artifacts
clean: clean-data
	@echo "Cleaning all build artifacts..."
	@mvn clean

# Clean everything including cache
clean-all: clean clean-cache
	@echo "Cleaned all artifacts and cache"

# Build all modules
build: generate-data
	@echo "Building all modules..."
	@mvn compile

# Package all modules
package: generate-data
	@echo "Packaging all modules..."
	@mvn package

# Test all modules
test: generate-data
	@echo "Running tests..."
	@mvn test

# Install to local repository
install: generate-data
	@echo "Installing to local Maven repository..."
	@mvn install

# Deploy to remote repository (requires release profile)
deploy: generate-data
	@echo "Deploying to remote repository..."
	@PROJECT_VERSION=$$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout); \
	if [[ "$$PROJECT_VERSION" == *"-SNAPSHOT" ]]; then \
		echo "Deploying snapshot version $$PROJECT_VERSION to snapshots repository"; \
		mvn deploy -DskipTests; \
	else \
		echo "Deploying release version $$PROJECT_VERSION to Maven Central"; \
		mvn deploy -Prelease -DskipTests; \
	fi

# Force regenerate data (useful for development)
force-generate-data: clean-data generate-data

# Dry-run deployment (shows what would be deployed)
deploy-dry-run:
	@echo "Dry-run deployment check..."
	@PROJECT_VERSION=$$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout); \
	echo "Current version: $$PROJECT_VERSION"; \
	if [[ "$$PROJECT_VERSION" == *"-SNAPSHOT" ]]; then \
		echo "-> Would deploy SNAPSHOT to snapshots repository"; \
		echo "-> Command: mvn deploy -DskipTests"; \
	else \
		echo "-> Would deploy RELEASE to Maven Central"; \
		echo "-> Command: mvn deploy -Prelease -DskipTests"; \
	fi; \
	echo "Modules that would be deployed:"; \
	mvn help:evaluate -Dexpression=project.modules -q -DforceStdout | grep -v "maven.deploy.skip=true" || true

# Show current configuration
show-config:
	@echo "Current configuration:"
	@echo "  DATA_VERSION: $(DATA_VERSION)"
	@echo "  BUILDER_JAR: $(BUILDER_JAR)"
	@echo "  OUTPUT_FILE: $(OUTPUT_FILE)"
	@echo "  TIMEZONE_URL: $(TIMEZONE_URL)"
	@echo "  MVN: $(MVN)"

# Help target
help:
	@echo "Available targets:"
	@echo "  generate-data     - Generate data.tar.zstd resource file"
	@echo "  download-data     - Download timezone data to cache"
	@echo "  run-builder       - Execute builder using Maven"
	@echo "  build             - Compile all modules"
	@echo "  package           - Package all modules"
	@echo "  test              - Run all tests"
	@echo "  install           - Install to local Maven repository"
	@echo "  deploy            - Deploy to remote repository (snapshots or Maven Central)"
	@echo "  deploy-dry-run    - Show what would be deployed without deploying"
	@echo "  clean             - Clean build artifacts and generated data"
	@echo "  clean-data        - Clean only generated data files"
	@echo "  clean-cache       - Clean only cache directory"
	@echo "  clean-all         - Clean everything including cache"
	@echo "  force-generate-data - Force regenerate data file"
	@echo "  show-config       - Show current configuration"
	@echo "  help              - Show this help message"
	@echo ""
	@echo "Variables:"
	@echo "  DATA_VERSION      - Timezone data version (default: $(DATA_VERSION))"

.PHONY: run-builder generate-data download-data build package test install deploy deploy-dry-run clean clean-data clean-cache clean-all force-generate-data show-config help