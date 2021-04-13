# Papyrus2Oml

[![Release](https://img.shields.io/github/v/tag/opencaesar/papyrus-adapter?label=release)](https://github.com/opencaesar/papyrus-adapter/releases/latest)

A tool that translates [Papyrus](https://www.eclipse.org/papyrus/) models to [OML](https://opencaesar.github.io/oml) ontologies

## Run as CLI

MacOS/Linux
```
    ./gradlew papyrus2oml:run --args="..."
```
Windows
```
    gradlew.bat papyrus2oml:run --args="..."
```
Args
```
--input-folder-path | -i path/to/input/papyrus/folder [Required]
--output-catalog-path | -o path/to/output/oml/catalog.oml [Required]
```

## Run with Gradle
```
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.adapters:papyrus2oml-gradle:+'
	}
}
task papyrus2oml(type:io.opencaesar.papyrus2oml.Papyrus2OmlTask) {
	inputFolderPath = file('path/to/input/papyrus/folder') [Required]
	outputCatalogPath = file('path/to/output/oml/catalog.xml') [Required]
}               
```