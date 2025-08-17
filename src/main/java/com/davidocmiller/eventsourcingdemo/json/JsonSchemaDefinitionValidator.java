package com.davidocmiller.eventsourcingdemo.json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Validates whether a JSON document is a valid JSON Schema according to JSON Schema Draft 7
 */
public class JsonSchemaDefinitionValidator
{

    // Valid types in JSON Schema
    private static final Set<String> VALID_TYPES = Set.of(
            "null", "boolean", "object", "array", "number", "string", "integer");

    // Valid formats for string type
    private static final Set<String> VALID_STRING_FORMATS = Set.of(
            "date-time", "date", "time", "email", "idn-email", "hostname", "idn-hostname",
            "ipv4", "ipv6", "uri", "uri-reference", "iri", "iri-reference", "uri-template",
            "json-pointer", "relative-json-pointer", "regex");

    public JsonSchemaDefinitionValidator()
    {
        // Constructor for consistency
    }

    /**
     * Validates whether a JSON string is a valid JSON Schema
     * 
     * @param schemaJson
     *            The JSON schema definition as a string
     * @return ValidationResult containing validation outcome and any errors
     */
    public ValidationResult validateSchema(String schemaJson)
    {
        try
        {
            // Handle boolean schemas (true/false) which are valid but not parsed as JSON objects
            String trimmedSchema = schemaJson.trim();
            if ("true".equals(trimmedSchema) || "false".equals(trimmedSchema))
            {
                // Boolean schemas are always valid according to JSON Schema spec
                return new ValidationResult(true, List.of());
            }

            // Handle null schema
            if ("null".equals(trimmedSchema))
            {
                List<String> errors = List.of(": null is not a valid JSON schema (must be object or boolean)");
                return new ValidationResult(false, errors);
            }

            // Try to parse as JSON object/array/value
            JsonValue schema;

            try (JsonReader reader = Json.createReader(new StringReader(schemaJson)))
            {
                schema = reader.read();
            }
            catch (JsonException e)
            {
                // If it fails to parse as a complete JSON document, it might be a primitive value
                // Try wrapping it and parsing again to validate JSON syntax
                try (JsonReader wrappedReader = Json.createReader(new StringReader("[" + schemaJson + "]")))
                {
                    JsonArray wrapped = wrappedReader.readArray();
                    schema = wrapped.get(0); // Extract the actual value
                }
                catch (JsonException e2)
                {
                    return ValidationResult.error("Invalid JSON syntax: " + e.getMessage());
                }
            }

            List<String> errors = new ArrayList<>();
            validateSchemaValue(schema, "", errors);

            return new ValidationResult(errors.isEmpty(), errors);

        }
        catch (JsonException e)
        {
            return ValidationResult.error("Invalid JSON syntax: " + e.getMessage());
        }
        catch (Exception e)
        {
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * Validates a JSON Schema file
     * 
     * @param schemaPath
     *            Path to the JSON schema file
     * @return ValidationResult containing validation outcome and any errors
     */
    public ValidationResult validateSchemaFile(Path schemaPath)
    {
        try
        {
            String schemaJson = Files.readString(schemaPath);
            return validateSchema(schemaJson);
        }
        catch (IOException e)
        {
            return ValidationResult.error("Failed to read schema file: " + e.getMessage());
        }
    }

    /**
     * Core validation logic for schema definitions
     */
    private void validateSchemaValue(JsonValue schema, String path, List<String> errors)
    {
        // Schema can be boolean or object
        if (schema.getValueType() == JsonValue.ValueType.TRUE ||
                schema.getValueType() == JsonValue.ValueType.FALSE)
        {
            // Boolean schemas are always valid (true = accept all, false = reject all)
            return;
        }

        if (schema.getValueType() != JsonValue.ValueType.OBJECT)
        {
            errors.add(path + ": schema must be an object or boolean");
            return;
        }

        JsonObject schemaObj = schema.asJsonObject();

        // Validate schema keywords
        validateSchemaKeywords(schemaObj, path, errors);
    }

    private void validateSchemaKeywords(JsonObject schema, String path, List<String> errors)
    {
        // Validate type keyword
        if (schema.containsKey("type"))
        {
            validateTypeKeyword(schema.get("type"), path + ".type", errors);
        }

        // Validate properties (for object type)
        if (schema.containsKey("properties"))
        {
            validatePropertiesKeyword(schema.get("properties"), path + ".properties", errors);
        }

        // Validate required (for object type)
        if (schema.containsKey("required"))
        {
            validateRequiredKeyword(schema.get("required"), path + ".required", errors);
        }

        // Validate additionalProperties
        if (schema.containsKey("additionalProperties"))
        {
            validateAdditionalPropertiesKeyword(schema.get("additionalProperties"), path + ".additionalProperties", errors);
        }

        // Validate items (for array type)
        if (schema.containsKey("items"))
        {
            validateItemsKeyword(schema.get("items"), path + ".items", errors);
        }

        // Validate string constraints
        validateStringConstraints(schema, path, errors);

        // Validate number constraints
        validateNumberConstraints(schema, path, errors);

        // Validate array constraints
        validateArrayConstraints(schema, path, errors);

        // Validate format
        if (schema.containsKey("format"))
        {
            validateFormatKeyword(schema.get("format"), path + ".format", errors);
        }

        // Validate pattern
        if (schema.containsKey("pattern"))
        {
            validatePatternKeyword(schema.get("pattern"), path + ".pattern", errors);
        }

        // Validate enum
        if (schema.containsKey("enum"))
        {
            validateEnumKeyword(schema.get("enum"), path + ".enum", errors);
        }

        // Validate const
        if (schema.containsKey("const"))
        {
            // const can be any JSON value, so it's always valid
        }

        // Check for unknown keywords (optional - could be removed for less strict validation)
        validateUnknownKeywords(schema, path, errors);
    }

    private void validateTypeKeyword(JsonValue typeValue, String path, List<String> errors)
    {
        if (typeValue.getValueType() == JsonValue.ValueType.STRING)
        {
            String type = ((JsonString) typeValue).getString();
            if (!VALID_TYPES.contains(type))
            {
                errors.add(path + ": invalid type '" + type + "'. Valid types are: " + VALID_TYPES);
            }
        }
        else if (typeValue.getValueType() == JsonValue.ValueType.ARRAY)
        {
            JsonArray typeArray = typeValue.asJsonArray();
            if (typeArray.isEmpty())
            {
                errors.add(path + ": type array cannot be empty");
                return;
            }

            Set<String> seenTypes = new HashSet<>();
            for (JsonValue type : typeArray)
            {
                if (type.getValueType() != JsonValue.ValueType.STRING)
                {
                    errors.add(path + ": type array must contain only strings");
                    continue;
                }

                String typeStr = ((JsonString) type).getString();
                if (!VALID_TYPES.contains(typeStr))
                {
                    errors.add(path + ": invalid type '" + typeStr + "' in array");
                }

                if (seenTypes.contains(typeStr))
                {
                    errors.add(path + ": duplicate type '" + typeStr + "' in array");
                }
                seenTypes.add(typeStr);
            }
        }
        else
        {
            errors.add(path + ": type must be a string or array of strings");
        }
    }

    private void validatePropertiesKeyword(JsonValue propertiesValue, String path, List<String> errors)
    {
        if (propertiesValue.getValueType() != JsonValue.ValueType.OBJECT)
        {
            errors.add(path + ": properties must be an object");
            return;
        }

        JsonObject properties = propertiesValue.asJsonObject();
        for (Map.Entry<String, JsonValue> entry : properties.entrySet())
        {
            String propertyName = entry.getKey();
            JsonValue propertySchema = entry.getValue();
            validateSchemaValue(propertySchema, path + "." + propertyName, errors);
        }
    }

    private void validateRequiredKeyword(JsonValue requiredValue, String path, List<String> errors)
    {
        if (requiredValue.getValueType() != JsonValue.ValueType.ARRAY)
        {
            errors.add(path + ": required must be an array");
            return;
        }

        JsonArray required = requiredValue.asJsonArray();
        Set<String> seenProperties = new HashSet<>();

        for (int i = 0; i < required.size(); i++)
        {
            JsonValue item = required.get(i);
            if (item.getValueType() != JsonValue.ValueType.STRING)
            {
                errors.add(path + "[" + i + "]: required array items must be strings");
                continue;
            }

            String propertyName = ((JsonString) item).getString();
            if (seenProperties.contains(propertyName))
            {
                errors.add(path + "[" + i + "]: duplicate required property '" + propertyName + "'");
            }
            seenProperties.add(propertyName);
        }
    }

    private void validateAdditionalPropertiesKeyword(JsonValue additionalPropsValue, String path, List<String> errors)
    {
        if (additionalPropsValue.getValueType() == JsonValue.ValueType.TRUE ||
                additionalPropsValue.getValueType() == JsonValue.ValueType.FALSE)
        {
            // Boolean is valid
            return;
        }

        // Must be a schema (object)
        validateSchemaValue(additionalPropsValue, path, errors);
    }

    private void validateItemsKeyword(JsonValue itemsValue, String path, List<String> errors)
    {
        // items can be a schema or array of schemas
        if (itemsValue.getValueType() == JsonValue.ValueType.ARRAY)
        {
            JsonArray items = itemsValue.asJsonArray();
            for (int i = 0; i < items.size(); i++)
            {
                validateSchemaValue(items.get(i), path + "[" + i + "]", errors);
            }
        }
        else
        {
            // Single schema
            validateSchemaValue(itemsValue, path, errors);
        }
    }

    private void validateStringConstraints(JsonObject schema, String path, List<String> errors)
    {
        // minLength
        if (schema.containsKey("minLength"))
        {
            JsonValue minLength = schema.get("minLength");
            if (!isNonNegativeInteger(minLength))
            {
                errors.add(path + ".minLength: must be a non-negative integer");
            }
        }

        // maxLength
        if (schema.containsKey("maxLength"))
        {
            JsonValue maxLength = schema.get("maxLength");
            if (!isNonNegativeInteger(maxLength))
            {
                errors.add(path + ".maxLength: must be a non-negative integer");
            }
        }

        // Check minLength <= maxLength
        if (schema.containsKey("minLength") && schema.containsKey("maxLength"))
        {
            JsonValue minLength = schema.get("minLength");
            JsonValue maxLength = schema.get("maxLength");
            if (isNonNegativeInteger(minLength) && isNonNegativeInteger(maxLength))
            {
                int min = ((JsonNumber) minLength).intValue();
                int max = ((JsonNumber) maxLength).intValue();
                if (min > max)
                {
                    errors.add(path + ": minLength (" + min + ") cannot be greater than maxLength (" + max + ")");
                }
            }
        }
    }

    private void validateNumberConstraints(JsonObject schema, String path, List<String> errors)
    {
        // minimum
        if (schema.containsKey("minimum"))
        {
            JsonValue minimum = schema.get("minimum");
            if (minimum.getValueType() != JsonValue.ValueType.NUMBER)
            {
                errors.add(path + ".minimum: must be a number");
            }
        }

        // maximum
        if (schema.containsKey("maximum"))
        {
            JsonValue maximum = schema.get("maximum");
            if (maximum.getValueType() != JsonValue.ValueType.NUMBER)
            {
                errors.add(path + ".maximum: must be a number");
            }
        }

        // exclusiveMinimum
        if (schema.containsKey("exclusiveMinimum"))
        {
            JsonValue exclusiveMinimum = schema.get("exclusiveMinimum");
            if (exclusiveMinimum.getValueType() != JsonValue.ValueType.NUMBER)
            {
                errors.add(path + ".exclusiveMinimum: must be a number");
            }
        }

        // exclusiveMaximum
        if (schema.containsKey("exclusiveMaximum"))
        {
            JsonValue exclusiveMaximum = schema.get("exclusiveMaximum");
            if (exclusiveMaximum.getValueType() != JsonValue.ValueType.NUMBER)
            {
                errors.add(path + ".exclusiveMaximum: must be a number");
            }
        }

        // multipleOf
        if (schema.containsKey("multipleOf"))
        {
            JsonValue multipleOf = schema.get("multipleOf");
            if (multipleOf.getValueType() != JsonValue.ValueType.NUMBER)
            {
                errors.add(path + ".multipleOf: must be a number");
            }
            else
            {
                double value = ((JsonNumber) multipleOf).doubleValue();
                if (value <= 0)
                {
                    errors.add(path + ".multipleOf: must be greater than 0");
                }
            }
        }
    }

    private void validateArrayConstraints(JsonObject schema, String path, List<String> errors)
    {
        // minItems
        if (schema.containsKey("minItems"))
        {
            JsonValue minItems = schema.get("minItems");
            if (!isNonNegativeInteger(minItems))
            {
                errors.add(path + ".minItems: must be a non-negative integer");
            }
        }

        // maxItems
        if (schema.containsKey("maxItems"))
        {
            JsonValue maxItems = schema.get("maxItems");
            if (!isNonNegativeInteger(maxItems))
            {
                errors.add(path + ".maxItems: must be a non-negative integer");
            }
        }

        // uniqueItems
        if (schema.containsKey("uniqueItems"))
        {
            JsonValue uniqueItems = schema.get("uniqueItems");
            if (uniqueItems.getValueType() != JsonValue.ValueType.TRUE &&
                    uniqueItems.getValueType() != JsonValue.ValueType.FALSE)
            {
                errors.add(path + ".uniqueItems: must be a boolean");
            }
        }
    }

    private void validateFormatKeyword(JsonValue formatValue, String path, List<String> errors)
    {
        if (formatValue.getValueType() != JsonValue.ValueType.STRING)
        {
            errors.add(path + ": format must be a string");
            return;
        }

        String format = ((JsonString) formatValue).getString();
        if (!VALID_STRING_FORMATS.contains(format))
        {
            // Note: This is a warning rather than an error since custom formats are allowed
            // You might want to make this configurable
            errors.add(path + ": unknown format '" + format + "' (custom formats are allowed but not validated)");
        }
    }

    private void validatePatternKeyword(JsonValue patternValue, String path, List<String> errors)
    {
        if (patternValue.getValueType() != JsonValue.ValueType.STRING)
        {
            errors.add(path + ": pattern must be a string");
            return;
        }

        String pattern = ((JsonString) patternValue).getString();
        try
        {
            Pattern.compile(pattern);
        }
        catch (Exception e)
        {
            errors.add(path + ": invalid regular expression pattern: " + e.getMessage());
        }
    }

    private void validateEnumKeyword(JsonValue enumValue, String path, List<String> errors)
    {
        if (enumValue.getValueType() != JsonValue.ValueType.ARRAY)
        {
            errors.add(path + ": enum must be an array");
            return;
        }

        JsonArray enumArray = enumValue.asJsonArray();
        if (enumArray.isEmpty())
        {
            errors.add(path + ": enum array cannot be empty");
            return;
        }

        // Check for duplicate values
        Set<String> seenValues = new HashSet<>();
        for (int i = 0; i < enumArray.size(); i++)
        {
            String valueStr = enumArray.get(i).toString();
            if (seenValues.contains(valueStr))
            {
                errors.add(path + "[" + i + "]: duplicate enum value");
            }
            seenValues.add(valueStr);
        }
    }

    private void validateUnknownKeywords(JsonObject schema, String path, List<String> errors)
    {
        Set<String> knownKeywords = Set.of(
                "type", "properties", "required", "additionalProperties", "items",
                "minLength", "maxLength", "pattern", "format",
                "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
                "minItems", "maxItems", "uniqueItems",
                "enum", "const",
                "$schema", "$id", "$ref", "$comment", "title", "description", "default", "examples",
                "allOf", "anyOf", "oneOf", "not", "if", "then", "else");

        for (String key : schema.keySet())
        {
            if (!knownKeywords.contains(key))
            {
                // This is just a warning - unknown keywords are technically allowed
                errors.add(path + ": unknown keyword '" + key + "' (may be custom extension)");
            }
        }
    }

    private boolean isNonNegativeInteger(JsonValue value)
    {
        if (value.getValueType() != JsonValue.ValueType.NUMBER)
        {
            return false;
        }

        JsonNumber number = (JsonNumber) value;
        if (!number.isIntegral())
        {
            return false;
        }

        return number.intValue() >= 0;
    }

    // Record class for validation results
    public record ValidationResult(
            boolean isValid,
            List<String> errors,
            String errorMessage)
    {
        public static ValidationResult success()
        {
            return new ValidationResult(true, List.of(), null);
        }

        public static ValidationResult error(String message)
        {
            return new ValidationResult(false, List.of(), message);
        }

        public ValidationResult(boolean isValid, List<String> errors)
        {
            this(isValid, errors, null);
        }

        public void printResult()
        {
            if (isValid)
            {
                System.out.println("✓ JSON Schema definition is valid!");
            }
            else
            {
                System.out.println("✗ JSON Schema definition is invalid:");
                if (errorMessage != null)
                {
                    System.out.println("  Error: " + errorMessage);
                }
                else
                {
                    errors.forEach(error -> System.out.println("  - " + error));
                }
            }
        }
    }
}
