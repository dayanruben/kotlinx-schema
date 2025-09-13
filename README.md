[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# kotlinx-schema

Kotlin Multiplatform library that generates JSON Schemas from your data classes at compile time using Kotlin Symbol Processing (KSP).

- Zero reflection at runtime
- Works across JVM, JS, iOS, macOS, Wasm (multiplatform)
- Supports enums, collections, maps, nested objects, nullability, and generics (with star-projection)
- Preserves @Description on classes and properties in the emitted schema

## Quick start

### 1) Add dependencies and apply KSP

Kotlin Multiplatform (Kotlin DSL shown):

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.google.ksp)
}

dependencies {
    // Apply KSP processor to commonMain metadata only
    add("kspCommonMainMetadata", project(":kotlinx-schema-ksp"))
}

kotlin {
    compilerOptions {
        // Apply bare annotations to both constructor params and properties (recommended)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    // your targets ...
    jvm()
    js { nodejs() }
    wasmJs { browser(); nodejs() }
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlinx-schema-annotations"))
                implementation(libs.kotlinx.serialization.json)
                // Make generated sources visible to compilation (metadata)
                kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.assertions.json)
            }
        }
    }
}

// Ensure KSP runs before all compilations that need metadata
afterEvaluate {
    tasks.findByName("compileKotlinWasmJs")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinIosArm64")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinIosSimulatorArm64")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinJs")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinJvm")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinLinuxArm64")?.dependsOn("kspCommonMainKotlinMetadata")
    tasks.findByName("kspKotlinMacosArm64")?.dependsOn("kspCommonMainKotlinMetadata")
}
```

Single target (JVM) setup example:

```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.google.ksp)
}

dependencies {
    implementation(project(":kotlinx-schema-annotations"))
    ksp(project(":kotlinx-schema-ksp"))
    implementation(libs.kotlinx.serialization.json)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

// Include generated sources in compilation
sourceSets.main { kotlin.srcDir("build/generated/ksp/main/kotlin") }
```

### 2) Annotate your models

```kotlin
@Description("A postal address for deliveries and billing.")
@Schema
data class Address(
    @Description("Street address, including house number") val street: String,
    @Description("City or town name") val city: String,
    @Description("Postal or ZIP code") val zipCode: String,
    @Description("Two-letter ISO country code; defaults to US") val country: String = "US",
)
```

### 3) Use the generated extensions

```kotlin
val schemaString: String = Address::class.jsonSchemaString
val schemaObject: kotlinx.serialization.json.JsonObject = Address::class.jsonSchema
```

## What gets generated

Schemas follow a $id/$defs/$ref layout. Example (pretty-printed):

```json
{
  "$id": "com.example.Address",
  "$defs": {
    "com.example.Address": {
      "type": "object",
      "properties": {
        "street": { "type": "string", "description": "Street address, including house number" },
        "city":   { "type": "string", "description": "City or town name" },
        "zipCode":{ "type": "string", "description": "Postal or ZIP code" },
        "country":{ "type": "string", "description": "Two-letter ISO country code; defaults to US" }
      },
      "required": ["street", "city", "zipCode"],
      "additionalProperties": false,
      "description": "A postal address for deliveries and billing."
    }
  },
  "$ref": "#/$defs/com.example.Address"
}
```

- Enums are `type: string` with `enum: [...]` and carry `@Description` as `description`.
- Object properties include their inferred type schema and, when present, property-level `@Description` as `description`.
- Nullable properties are emitted as a union including `null`.
- Collections: `List<T>`/`Set<T>` → `{ "type":"array", "items": T }`; `Map<String, V>` → `{ "type":"object", "additionalProperties": V }`.
- Unknown/generic type parameters resolve to `kotlin.Any` with a minimal definition in `$defs`.

## Descriptions

Use `@Description` on classes and properties. With the recommended compiler flag, a bare `@Description` on a primary constructor parameter also applies to the property. If you do not enable the flag, prefer `@param:Description` for constructor-declared properties.

```kotlin
@Schema
data class Product(
    @Description("Unique identifier for the product") val id: Long,
    @Description("Human-readable product name") val name: String,
    @Description("Optional detailed description of the product") val description: String?,
    @Description("Unit price expressed as a decimal number") val price: Double,
)
```

## Project modules

- `kotlinx-schema-annotations`: `@Schema`, `@Description`
- `kotlinx-schema-ksp`: KSP processor producing extension properties
- `kotlinx-schema-generator-json`: IR and JSON Schema emitter
- `ksp-integration-tests`: Multiplatform tests verifying KSP generation
- (Disabled) `kotlinx-schema-compiler-plugin`, `integration-tests`

## Building and testing

- Build all: `./gradlew build`
- Run all tests: `./gradlew test`
- KSP integration tests: `./gradlew :ksp-integration-tests:test`

Generated code for tests can be found under:
- `ksp-integration-tests/build/generated/ksp/metadata/commonMain/kotlin`

## Requirements

- Kotlin 2.2.20+
- KSP compatible with your Kotlin version
- kotlinx.serialization-json for JsonObject support

## Contributing

Contributions are welcome! Please ensure:
- All tests pass: `./gradlew test`
- Code generation works: `./gradlew :ksp-integration-tests:build`
- Generated code is clean and well-documented
- Integration tests cover new functionality

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.