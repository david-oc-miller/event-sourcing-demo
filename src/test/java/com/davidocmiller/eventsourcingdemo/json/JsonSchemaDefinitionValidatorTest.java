package com.davidocmiller.eventsourcingdemo.json;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * Tests for JsonSchemaDefinitionValidator
 */
public class JsonSchemaDefinitionValidatorTest
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
