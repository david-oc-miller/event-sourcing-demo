package com.davidocmiller.eventsourcingdemo.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive JUnit tests for JsonSchemaValidator
 */
class JsonSchemaValidatorTest
{

    private JsonSchemaValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new JsonSchemaValidator();
    }

    @Nested
    @DisplayName("Document Validation Tests")
    class DocumentValidationTests
    {

        private final String validSchema = """
                {
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string",
                      "minLength": 1,
                      "maxLength": 50
                    },
                    "age": {
                      "type": "integer",
                      "minimum": 0,
                      "maximum": 150
                    },
                    "email": {
                      "type": "string",
                      "format": "email",
                      "pattern": "^[^@]+@[^@]+\\\\.[^@]+$"
                    }
                  },
                  "required": ["name", "age"],
                  "additionalProperties": false
                }
                """;

        @Test
        @DisplayName("Should validate a correct JSON document")
        void shouldValidateCorrectDocument()
        {
            String validDocument = """
                    {
                      "name": "John Doe",
                      "age": 30,
                      "email": "john@example.com"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, validDocument);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("Should reject document missing required field")
        void shouldRejectDocumentMissingRequiredField()
        {
            String invalidDocument = """
                    {
                      "name": "John Doe"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, invalidDocument);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).contains(": missing required property 'age'");
        }

        @Test
        @DisplayName("Should reject document with wrong type")
        void shouldRejectDocumentWithWrongType()
        {
            String invalidDocument = """
                    {
                      "name": "John Doe",
                      "age": "thirty"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, invalidDocument);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(error -> error.contains("expected type 'integer'"));
        }

        @Test
        @DisplayName("Should reject document with additional properties when not allowed")
        void shouldRejectDocumentWithAdditionalProperties()
        {
            String invalidDocument = """
                    {
                      "name": "John Doe",
                      "age": 30,
                      "extraField": "not allowed"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, invalidDocument);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(error -> error.contains("additional property 'extraField'"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0", // minimum boundary
                "75", // middle value
                "150" // maximum boundary
        })
        @DisplayName("Should accept valid age values")
        void shouldAcceptValidAgeValues(String age)
        {
            String document = String.format("""
                    {
                      "name": "Test User",
                      "age": %s
                    }
                    """, age);

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, document);

            assertThat(result.isValid()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "-1", // below minimum
                "151" // above maximum
        })
        @DisplayName("Should reject invalid age values")
        void shouldRejectInvalidAgeValues(String age)
        {
            String document = String.format("""
                    {
                      "name": "Test User",
                      "age": %s
                    }
                    """, age);

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, document);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should validate string length constraints")
        void shouldValidateStringLengthConstraints()
        {
            String tooShortName = """
                    {
                      "name": "",
                      "age": 30
                    }
                    """;

            String tooLongName = """
                    {
                      "name": "%s",
                      "age": 30
                    }
                    """.formatted("a".repeat(51));

            JsonSchemaValidator.ValidationResult shortResult = validator.validate(validSchema, tooShortName);
            JsonSchemaValidator.ValidationResult longResult = validator.validate(validSchema, tooLongName);

            assertThat(shortResult.isValid()).isFalse();
            assertThat(shortResult.errors()).anyMatch(error -> error.contains("string length must be at least 1"));

            assertThat(longResult.isValid()).isFalse();
            assertThat(longResult.errors()).anyMatch(error -> error.contains("string length must be at most 50"));
        }

        @Test
        @DisplayName("Should validate pattern matching")
        void shouldValidatePatternMatching()
        {
            String invalidEmail = """
                    {
                      "name": "John Doe",
                      "age": 30,
                      "email": "invalid-email"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, invalidEmail);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(error -> error.contains("does not match pattern"));
        }

        @Test
        @DisplayName("Should validate date format")
        void shouldValidateDateFormat()
        {
            String dateSchema = """
                    {
                      "type": "object",
                      "properties": {
                        "eventDate": {
                          "type": "string",
                          "format": "date"
                        }
                      }
                    }
                    """;

            String validDate = """
                    {
                      "eventDate": "2024-03-15"
                    }
                    """;

            String invalidDate = """
                    {
                      "eventDate": "15/03/2024"
                    }
                    """;

            JsonSchemaValidator.ValidationResult validResult = validator.validate(dateSchema, validDate);
            JsonSchemaValidator.ValidationResult invalidResult = validator.validate(dateSchema, invalidDate);

            assertThat(validResult.isValid()).isTrue();
            assertThat(invalidResult.isValid()).isFalse();
            assertThat(invalidResult.errors()).anyMatch(error -> error.contains("not a valid date format"));
        }
    }

    @Nested
    @DisplayName("Array Validation Tests")
    class ArrayValidationTests
    {

        private final String arraySchema = """
                {
                  "type": "object",
                  "properties": {
                    "tags": {
                      "type": "array",
                      "items": {
                        "type": "string",
                        "minLength": 1
                      },
                      "minItems": 1,
                      "maxItems": 5
                    },
                    "numbers": {
                      "type": "array",
                      "items": {
                        "type": "number",
                        "minimum": 0
                      }
                    }
                  }
                }
                """;

        @Test
        @DisplayName("Should validate array with valid items")
        void shouldValidateArrayWithValidItems()
        {
            String validDocument = """
                    {
                      "tags": ["java", "json", "validation"],
                      "numbers": [1, 2.5, 10]
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(arraySchema, validDocument);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should reject array with invalid items")
        void shouldRejectArrayWithInvalidItems()
        {
            String invalidDocument = """
                    {
                      "tags": ["java", "", "validation"],
                      "numbers": [1, -5, 10]
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validate(arraySchema, invalidDocument);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(error -> error.contains("tags[1]") && error.contains("string length must be at least 1"));
            assertThat(result.errors()).anyMatch(error -> error.contains("numbers[1]") && error.contains("number must be at least 0"));
        }

        @Test
        @DisplayName("Should validate array size constraints")
        void shouldValidateArraySizeConstraints()
        {
            String tooFewItems = """
                    {
                      "tags": []
                    }
                    """;

            String tooManyItems = """
                    {
                      "tags": ["a", "b", "c", "d", "e", "f"]
                    }
                    """;

            JsonSchemaValidator.ValidationResult fewResult = validator.validate(arraySchema, tooFewItems);
            JsonSchemaValidator.ValidationResult manyResult = validator.validate(arraySchema, tooManyItems);

            assertThat(fewResult.isValid()).isFalse();
            assertThat(fewResult.errors()).anyMatch(error -> error.contains("array must have at least 1 items"));

            assertThat(manyResult.isValid()).isFalse();
            assertThat(manyResult.errors()).anyMatch(error -> error.contains("array must have at most 5 items"));
        }
    }

    @Nested
    @DisplayName("File Operations Tests")
    class FileOperationsTests
    {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should validate document against schema from file")
        void shouldValidateDocumentAgainstSchemaFromFile() throws IOException
        {
            Path schemaFile = tempDir.resolve("schema.json");
            String schema = """
                    {
                      "type": "object",
                      "properties": {
                        "id": {"type": "integer"},
                        "name": {"type": "string"}
                      },
                      "required": ["id"]
                    }
                    """;
            Files.writeString(schemaFile, schema);

            String validDocument = """
                    {
                      "id": 123,
                      "name": "Test"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validateWithSchemaFile(schemaFile, validDocument);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should validate both schema and document from files")
        void shouldValidateBothSchemaAndDocumentFromFiles() throws IOException
        {
            Path schemaFile = tempDir.resolve("schema.json");
            Path documentFile = tempDir.resolve("document.json");

            String schema = """
                    {
                      "type": "object",
                      "properties": {
                        "value": {"type": "string"}
                      }
                    }
                    """;

            String document = """
                    {
                      "value": "test"
                    }
                    """;

            Files.writeString(schemaFile, schema);
            Files.writeString(documentFile, document);

            JsonSchemaValidator.ValidationResult result = validator.validateFiles(schemaFile, documentFile);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should handle file read errors gracefully")
        void shouldHandleFileReadErrorsGracefully()
        {
            Path nonExistentFile = tempDir.resolve("nonexistent.json");

            String document = """
                    {
                      "value": "test"
                    }
                    """;

            JsonSchemaValidator.ValidationResult result = validator.validateWithSchemaFile(nonExistentFile, document);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("Failed to read schema file");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests
    {

        @Test
        @DisplayName("Should handle invalid JSON syntax in schema")
        void shouldHandleInvalidJsonSyntaxInSchema()
        {
            String invalidSchema = "{ invalid json }";
            String validDocument = "{}";

            JsonSchemaValidator.ValidationResult result = validator.validate(invalidSchema, validDocument);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("Failed to parse JSON");
        }

        @Test
        @DisplayName("Should handle invalid JSON syntax in document")
        void shouldHandleInvalidJsonSyntaxInDocument()
        {
            String validSchema = """
                    {
                      "type": "object"
                    }
                    """;
            String invalidDocument = "{ invalid json }";

            JsonSchemaValidator.ValidationResult result = validator.validate(validSchema, invalidDocument);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("Failed to parse JSON");
        }

        @Test
        @DisplayName("Should handle null inputs gracefully")
        void shouldHandleNullInputsGracefully()
        {
            JsonSchemaValidator.ValidationResult result = validator.validate(null, "{}");
            assertThat(result.errorMessage()).contains("Cannot invoke \"String.length()\" because \"s\" is null");
            assertThat(result.isValid()).isFalse();
            
            result = validator.validate("{}", null);
            assertThat(result.errorMessage()).contains("Cannot invoke \"String.length()\" because \"s\" is null");
            assertThat(result.isValid()).isFalse();
            
        }
    }

    @Nested
    @DisplayName("Complex Schema Tests")
    class ComplexSchemaTests
    {

        @Test
        @DisplayName("Should validate nested object structures")
        void shouldValidateNestedObjectStructures()
        {
            String nestedSchema = """
                    {
                      "type": "object",
                      "properties": {
                        "user": {
                          "type": "object",
                          "properties": {
                            "profile": {
                              "type": "object",
                              "properties": {
                                "name": {"type": "string"},
                                "age": {"type": "integer"}
                              },
                              "required": ["name"]
                            }
                          },
                          "required": ["profile"]
                        }
                      }
                    }
                    """;

            String validDocument = """
                    {
                      "user": {
                        "profile": {
                          "name": "John",
                          "age": 30
                        }
                      }
                    }
                    """;

            String invalidDocument = """
                    {
                      "user": {
                        "profile": {
                          "age": 30
                        }
                      }
                    }
                    """;

            JsonSchemaValidator.ValidationResult validResult = validator.validate(nestedSchema, validDocument);
            JsonSchemaValidator.ValidationResult invalidResult = validator.validate(nestedSchema, invalidDocument);

            assertThat(validResult.isValid()).isTrue();
            assertThat(invalidResult.isValid()).isFalse();
            assertThat(invalidResult.errors())
                    .anyMatch(error -> error.contains("user.profile") && error.contains("missing required property 'name'"));
        }

        @Test
        @DisplayName("Should validate multiple type constraints")
        void shouldValidateMultipleTypeConstraints()
        {
            String multipleTypeSchema = """
                    {
                      "type": "object",
                      "properties": {
                        "value": {
                          "type": ["string", "number"]
                        }
                      }
                    }
                    """;

            String stringDocument = """
                    {
                      "value": "hello"
                    }
                    """;

            String numberDocument = """
                    {
                      "value": 42
                    }
                    """;

            String booleanDocument = """
                    {
                      "value": true
                    }
                    """;

            JsonSchemaValidator.ValidationResult stringResult = validator.validate(multipleTypeSchema, stringDocument);
            JsonSchemaValidator.ValidationResult numberResult = validator.validate(multipleTypeSchema, numberDocument);
            JsonSchemaValidator.ValidationResult booleanResult = validator.validate(multipleTypeSchema, booleanDocument);

            assertThat(stringResult.isValid()).isTrue();
            assertThat(numberResult.isValid()).isTrue();
            assertThat(booleanResult.isValid()).isFalse();
            assertThat(booleanResult.errors()).anyMatch(error -> error.contains("expected type one of ['string', 'number']") &&
                    error.contains("got 'boolean'"));
        }
    }

    @Nested
    @DisplayName("ValidationResult Tests")
    class ValidationResultTests
    {

        @Test
        @DisplayName("Should create success result correctly")
        void shouldCreateSuccessResultCorrectly()
        {
            JsonSchemaValidator.ValidationResult success = JsonSchemaValidator.ValidationResult.success();

            assertThat(success.isValid()).isTrue();
            assertThat(success.errors()).isEmpty();
            assertThat(success.errorMessage()).isNull();
        }

        @Test
        @DisplayName("Should create error result correctly")
        void shouldCreateErrorResultCorrectly()
        {
            String errorMessage = "Test error message";
            JsonSchemaValidator.ValidationResult error = JsonSchemaValidator.ValidationResult.error(errorMessage);

            assertThat(error.isValid()).isFalse();
            assertThat(error.errors()).isEmpty();
            assertThat(error.errorMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("Should print results without throwing exceptions")
        void shouldPrintResultsWithoutThrowingExceptions()
        {
            JsonSchemaValidator.ValidationResult success = JsonSchemaValidator.ValidationResult.success();
            JsonSchemaValidator.ValidationResult error = JsonSchemaValidator.ValidationResult.error("Test error");

            assertThatCode(success::printResult).doesNotThrowAnyException();
            assertThatCode(error::printResult).doesNotThrowAnyException();
        }
    }
}

/**
 * Tests for JsonSchemaDefinitionValidator
 */
class JsonSchemaDefinitionValidatorTest
{

    private JsonSchemaDefinitionValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new JsonSchemaDefinitionValidator();
    }

    @Nested
    @DisplayName("Schema Definition Validation Tests")
    class SchemaDefinitionValidationTests
    {

        @Test
        @DisplayName("Should validate a correct JSON schema definition")
        void shouldValidateCorrectSchemaDefinition()
        {
            String validSchema = """
                    {
                      "$schema": "http://json-schema.org/draft-07/schema#",
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string",
                          "minLength": 1,
                          "maxLength": 100
                        },
                        "age": {
                          "type": "integer",
                          "minimum": 0,
                          "maximum": 150
                        }
                      },
                      "required": ["name"],
                      "additionalProperties": false
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchema(validSchema);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should reject schema with invalid type")
        void shouldRejectSchemaWithInvalidType()
        {
            String invalidSchema = """
                    {
                      "type": "invalid_type"
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchema(invalidSchema);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).anyMatch(error -> error.contains("invalid type 'invalid_type'"));
        }

        @ParameterizedTest
        @CsvSource({
                "'null', true",
                "'boolean', true",
                "'object', true",
                "'array', true",
                "'number', true",
                "'string', true",
                "'integer', true",
                "'invalid', false"
        })
        @DisplayName("Should validate type values correctly")
        void shouldValidateTypeValuesCorrectly(String type, boolean shouldBeValid)
        {
            String schema = String.format("""
                    {
                      "type": "%s"
                    }
                    """, type);

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchema(schema);

            assertThat(result.isValid()).isEqualTo(shouldBeValid);
        }

        @Test
        @DisplayName("Should validate array of types")
        void shouldValidateArrayOfTypes()
        {
            String validArraySchema = """
                    {
                      "type": ["string", "number"]
                    }
                    """;

            String invalidArraySchema = """
                    {
                      "type": ["string", "invalid_type"]
                    }
                    """;

            String duplicateArraySchema = """
                    {
                      "type": ["string", "string"]
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult validResult = validator.validateSchema(validArraySchema);
            JsonSchemaDefinitionValidator.ValidationResult invalidResult = validator.validateSchema(invalidArraySchema);
            JsonSchemaDefinitionValidator.ValidationResult duplicateResult = validator.validateSchema(duplicateArraySchema);

            assertThat(validResult.isValid()).isTrue();
            assertThat(invalidResult.isValid()).isFalse();
            assertThat(invalidResult.errors()).anyMatch(error -> error.contains("invalid type 'invalid_type'"));
            assertThat(duplicateResult.isValid()).isFalse();
            assertThat(duplicateResult.errors()).anyMatch(error -> error.contains("duplicate type 'string'"));
        }

        @Test
        @DisplayName("Should validate string constraints in schema")
        void shouldValidateStringConstraintsInSchema()
        {
            String invalidMinLengthSchema = """
                    {
                      "type": "string",
                      "minLength": -1
                    }
                    """;

            String invalidMaxLengthSchema = """
                    {
                      "type": "string",
                      "maxLength": "not_a_number"
                    }
                    """;

            String inconsistentLengthSchema = """
                    {
                      "type": "string",
                      "minLength": 10,
                      "maxLength": 5
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult minResult = validator.validateSchema(invalidMinLengthSchema);
            JsonSchemaDefinitionValidator.ValidationResult maxResult = validator.validateSchema(invalidMaxLengthSchema);
            JsonSchemaDefinitionValidator.ValidationResult inconsistentResult = validator.validateSchema(inconsistentLengthSchema);

            assertThat(minResult.isValid()).isFalse();
            assertThat(minResult.errors()).anyMatch(error -> error.contains("minLength") && error.contains("non-negative integer"));

            assertThat(maxResult.isValid()).isFalse();
            assertThat(maxResult.errors()).anyMatch(error -> error.contains("maxLength") && error.contains("non-negative integer"));

            assertThat(inconsistentResult.isValid()).isFalse();
            assertThat(inconsistentResult.errors())
                    .anyMatch(error -> error.contains("minLength") && error.contains("cannot be greater than maxLength"));
        }

        @Test
        @DisplayName("Should validate pattern in schema")
        void shouldValidatePatternInSchema()
        {
            String validPatternSchema = """
                    {
                      "type": "string",
                      "pattern": "^[a-zA-Z]+$"
                    }
                    """;

            String invalidPatternSchema = """
                    {
                      "type": "string",
                      "pattern": "[invalid regex"
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult validResult = validator.validateSchema(validPatternSchema);
            JsonSchemaDefinitionValidator.ValidationResult invalidResult = validator.validateSchema(invalidPatternSchema);

            assertThat(validResult.isValid()).isTrue();
            assertThat(invalidResult.isValid()).isFalse();
            assertThat(invalidResult.errors()).anyMatch(error -> error.contains("invalid regular expression pattern"));
        }

        @Test
        @DisplayName("Should validate required array in schema")
        void shouldValidateRequiredArrayInSchema()
        {
            String validRequiredSchema = """
                    {
                      "type": "object",
                      "required": ["name", "age"]
                    }
                    """;

            String invalidRequiredSchema = """
                    {
                      "type": "object",
                      "required": [123, "age"]
                    }
                    """;

            String duplicateRequiredSchema = """
                    {
                      "type": "object",
                      "required": ["name", "name"]
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult validResult = validator.validateSchema(validRequiredSchema);
            JsonSchemaDefinitionValidator.ValidationResult invalidResult = validator.validateSchema(invalidRequiredSchema);
            JsonSchemaDefinitionValidator.ValidationResult duplicateResult = validator.validateSchema(duplicateRequiredSchema);

            assertThat(validResult.isValid()).isTrue();
            assertThat(invalidResult.isValid()).isFalse();
            assertThat(invalidResult.errors()).anyMatch(error -> error.contains("required array items must be strings"));
            assertThat(duplicateResult.isValid()).isFalse();
            assertThat(duplicateResult.errors()).anyMatch(error -> error.contains("duplicate required property"));
        }

        @Test
        @DisplayName("Should validate number constraints in schema")
        void shouldValidateNumberConstraintsInSchema()
        {
            String validNumberSchema = """
                    {
                      "type": "number",
                      "minimum": 0,
                      "maximum": 100,
                      "multipleOf": 5
                    }
                    """;

            String invalidMultipleOfSchema = """
                    {
                      "type": "number",
                      "multipleOf": 0
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult validResult = validator.validateSchema(validNumberSchema);
            JsonSchemaDefinitionValidator.ValidationResult invalidResult = validator.validateSchema(invalidMultipleOfSchema);

            assertThat(validResult.isValid()).isTrue();
            assertThat(invalidResult.isValid()).isFalse();
            assertThat(invalidResult.errors()).anyMatch(error -> error.contains("multipleOf") && error.contains("must be greater than 0"));
        }

        @Test
        @DisplayName("Should validate enum in schema")
        void shouldValidateEnumInSchema()
        {
            String validEnumSchema = """
                    {
                      "enum": ["red", "green", "blue"]
                    }
                    """;

            String emptyEnumSchema = """
                    {
                      "enum": []
                    }
                    """;

            String duplicateEnumSchema = """
                    {
                      "enum": ["red", "red", "blue"]
                    }
                    """;

            JsonSchemaDefinitionValidator.ValidationResult validResult = validator.validateSchema(validEnumSchema);
            JsonSchemaDefinitionValidator.ValidationResult emptyResult = validator.validateSchema(emptyEnumSchema);
            JsonSchemaDefinitionValidator.ValidationResult duplicateResult = validator.validateSchema(duplicateEnumSchema);

            assertThat(validResult.isValid()).isTrue();
            assertThat(emptyResult.isValid()).isFalse();
            assertThat(emptyResult.errors()).anyMatch(error -> error.contains("enum array cannot be empty"));
            assertThat(duplicateResult.isValid()).isFalse();
            assertThat(duplicateResult.errors()).anyMatch(error -> error.contains("duplicate enum value"));
        }
    }

    @Nested
    @DisplayName("Boolean Schema Tests")
    class BooleanSchemaTests
    {

        @Test
        @DisplayName("Should accept boolean true schema")
        void shouldAcceptBooleanTrueSchema()
        {
            String booleanSchema = "true";

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchema(booleanSchema);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should accept boolean false schema")
        void shouldAcceptBooleanFalseSchema()
        {
            String booleanSchema = "false";

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchema(booleanSchema);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should reject non-boolean, non-object schemas")
        void shouldRejectInvalidSchemaTypes()
        {
            String stringSchema = "\"not a schema\"";
            String numberSchema = "42";
            String arraySchema = "[1, 2, 3]";

            JsonSchemaDefinitionValidator.ValidationResult stringResult = validator.validateSchema(stringSchema);
            JsonSchemaDefinitionValidator.ValidationResult numberResult = validator.validateSchema(numberSchema);
            JsonSchemaDefinitionValidator.ValidationResult arrayResult = validator.validateSchema(arraySchema);

            assertThat(stringResult.isValid()).isFalse();
            assertThat(stringResult.errors()).anyMatch(error -> error.contains("schema must be an object or boolean"));

            assertThat(numberResult.isValid()).isFalse();
            assertThat(numberResult.errors()).anyMatch(error -> error.contains("schema must be an object or boolean"));

            assertThat(arrayResult.isValid()).isFalse();
            assertThat(arrayResult.errors()).anyMatch(error -> error.contains("schema must be an object or boolean"));
        }
    }

    @Nested
    @DisplayName("File Operations Tests")
    class FileOperationsTests
    {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should validate schema definition from file")
        void shouldValidateSchemaDefinitionFromFile() throws IOException
        {
            Path schemaFile = tempDir.resolve("schema.json");
            String schema = """
                    {
                      "type": "object",
                      "properties": {
                        "name": {"type": "string"}
                      }
                    }
                    """;
            Files.writeString(schemaFile, schema);

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchemaFile(schemaFile);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should handle file read errors")
        void shouldHandleFileReadErrors()
        {
            Path nonExistentFile = tempDir.resolve("nonexistent.json");

            JsonSchemaDefinitionValidator.ValidationResult result = validator.validateSchemaFile(nonExistentFile);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("Failed to read schema file");
        }
    }
}
