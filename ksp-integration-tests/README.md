# KSP Integration Tests

This module provides comprehensive integration testing for the kotlinx-schema KSP processor.

## Purpose

Battle-test the KSP processor by:
1. **Real-world usage**: Apply KSP to actual classes with `@Schema` annotations
2. **Code generation verification**: Ensure generated extension properties work correctly
3. **Performance testing**: Verify KSP processing is efficient and scalable
4. **Edge case handling**: Test unusual class names, complex nested structures, and generic types

## Structure

- `src/main/kotlin/` - Test data models with various `@Schema` annotations
- `src/test/kotlin/` - Integration tests verifying generated code functionality

## Test Classes

### Main Test Models (`TestModels.kt`)
- `Person` - Basic data class
- `Address` - Nested properties
- `Product` - Optional fields and collections
- `Container<T>` - Generic classes
- `Status` - Enum classes
- `Order` - Complex nested structures
- `NonAnnotatedClass` - Negative test case

### Test Suites

#### `KspIntegrationTest`
- Verifies all `@Schema` annotated classes generate `jsonSchemaString` extensions
- Ensures non-annotated classes don't generate extensions
- Validates generated JSON schema format and uniqueness

#### `KspPerformanceTest`
- Performance benchmarks for schema access
- Scalability tests with multiple schema classes
- Edge case handling (special characters, nested types)
- Consistency verification across multiple invocations

## Running Tests
