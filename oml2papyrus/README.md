# Oml2Papyrus

A tool that translates [OML](https://opencaesar.github.io/oml) ontologies to [Papyrus](https://www.eclipse.org/papyrus/) models

## Run as CLI

MacOS/Linux
```
    ./gradlew oml2papyrus:run --args="..."
```
Windows
```
    gradlew.bat oml2papyrus:run --args="..."
```
Args
```
--input-ontology-path, -i path/to/input/oml/file [Required]
--input-profile-path, -p path/to/input/profile/file [Optional]
--output-folder-path | -i path/to/output/papyrus/folder [Required]
```

## Run with Gradle
```
buildscript {
	repositories {
		mavenLocal()
		maven { url 'https://dl.bintray.com/opencaesar/papyrus-adapter' }
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.adapters:oml2papyrus-gradle:+'
	}
}
task oml2papyrus(type:io.opencaesar.oml2papyrus.Oml2PapryusTask) {
	inputCatalogPath = file('path/to/input/oml/catalog.xml') [Required]
	inputOntologyIri = 'iri' [Required]
	outputFolderPath = file('path/to/output/papyrus/folder') [Required]
}               
```
