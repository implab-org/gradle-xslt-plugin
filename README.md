# Gradle XSLT Plugin

Gradle plugin with a small `XsltTask` task type for applying one XSLT stylesheet
to one principal XML document.

```groovy
plugins {
    id "org.implab.gradle-xslt" version "0.1.0"
}

tasks.register("transform", XsltTask) {
    source = file("src/main/xml/input.xml")
    stylesheet = file("src/main/xslt/model-java.xsl")
    output = layout.buildDirectory.file("generated/model.java")

    includes.from("src/main/xslt/common.xsl")
    indent = 4

    parameters {
        pkg = "org.example.model"
        annotation = nodeSet {
            text("jakarta.annotation.Generated")
            text("javax.annotation.processing.Generated")
        }
    }
}
```

## Task Properties

- `source`: principal XML input document.
- `stylesheet`: principal XSLT stylesheet.
- `output`: transformation result file. Parent directories are created by the
  task.
- `includes`: additional files that affect transformation, typically
  `xsl:include` and `xsl:import` dependencies. The task declares them as Gradle
  inputs; URI resolution itself is still handled by the XSLT processor.
- `indent`: optional output indentation size. When set, the task enables XSLT
  output indentation and passes the value as Xalan `indent-amount`.
- `parameters`: XSLT parameters. Values are converted to an internal
  configuration-cache friendly representation before execution.

## Parameters

Groovy DSL assignment is the intended concise syntax:

```groovy
parameters {
    pkg = "org.example.model"
    debug = false
}
```

A map can be passed directly:

```groovy
parameters([
    pkg: "org.example.model",
    version: providers.gradleProperty("modelVersion")
])
```

Simple parameter values are passed through to `Transformer#setParameter` as-is.
Use stable, serializable scalar values when relying on Gradle input snapshots or
configuration cache. XML node values created with `nodeSet` and `fragment` are
converted to an internal serializable representation.

`XsltTask` is configuration-cache compatible and supports Gradle up-to-date
checks, but it is not build-cacheable by default. Stylesheets can resolve
external resources that are invisible to Gradle unless they are explicitly
declared as inputs.

Build cache can be enabled for concrete task instances when all external XSLT
inputs are declared:

```groovy
tasks.withType(XsltTask).configureEach {
    outputs.cacheIf("all external XSLT inputs are declared") {
        true
    }
}
```

Use `includes` for stylesheet dependencies and any other files that affect the
result but are not visible through the main `source` and `stylesheet` inputs.

For Java or Kotlin oriented configuration, `parameters(Action<? super
Map<String, Object>>)` accepts values through a temporary map and converts them
to the internal parameter model. Kotlin DSL example:

```kotlin
parameters {
    put("pkg", "org.example.model")
}
```

`nodeSet` creates a Xalan `NodeSet` parameter:

```groovy
parameters {
    annotations = nodeSet {
        text("Generated")
        text("Stable")
    }

    metadata = nodeSet {
        "meta:info"("xmlns:meta": "urn:example:meta") {
            "meta:item"(name: "source", value: "model.xml")
        }
    }
}
```

For complex structured values, prefer an explicit wrapper element:

```groovy
parameters {
    options = nodeSet {
        options {
            option(name: "nullable", value: "true")
            option(name: "immutable", value: "false")
        }
    }
}
```

When the stylesheet expects a single XML fragment parameter rather than a
node-set, use `fragment`:

```groovy
parameters {
    items = fragment {
        item("one")
        item("two")
    }
}
```

## Chaining Transformations

Keep each `XsltTask` atomic and compose chains through Gradle providers:

```groovy
def normalize = tasks.register("normalizeModel", XsltTask) {
    source = file("src/main/xml/model.xml")
    stylesheet = file("src/main/xslt/normalize.xsl")
    output = layout.buildDirectory.file("tmp/model-normalized.xml")
}

tasks.register("generateJavaModel", XsltTask) {
    source.set(normalize.flatMap { it.output })
    stylesheet = file("src/main/xslt/model-java.xsl")
    output = layout.buildDirectory.file("generated/sources/model/java/Model.java")
}
```

This lets Gradle infer task dependencies from the `Provider<RegularFile>`
connection without manual `dependsOn`.

## Publishing

Publication coordinates and plugin portal metadata are configured in
`gradle.properties` and `xslt-plugin/build.gradle`.

Local Maven publication smoke test:

```sh
./gradlew :xslt-plugin:publishAllPublicationsToStagingRepository
```

Gradle Plugin Portal validation without uploading:

```sh
./gradlew :xslt-plugin:publishPlugins --validate-only
```

Actual Plugin Portal publication:

```sh
./gradlew :xslt-plugin:publishPlugins
```

Plugin Portal credentials should stay outside the repository. Use
`GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`, or put
`gradle.publish.key` and `gradle.publish.secret` into
`~/.gradle/gradle.properties`.

## Notes

`XsltTask` intentionally models a single principal XML document and a single
output file. Batch transformations and pipeline DSLs can be built later as a
separate layer over this task type if repeated real-world scenarios justify it.
