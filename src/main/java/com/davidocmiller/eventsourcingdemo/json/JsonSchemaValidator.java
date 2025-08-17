package com.davidocmiller.eventsourcingdemo.json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public class JsonSchemaValidator
{

    public JsonSchemaValidator()
    {
        // Constructor for consistency
    }

    /**
     * Validates a JSON document against a JSON schema
     * 
     * @param schemaJson
     *            The JSON schema as a string
     * @param documentJson
     *            The JSON document to validate as a string
     * @return ValidationResult containing validation outcome and any errors
     */
    public ValidationResult validate(String schemaJson, String documentJson)
    {
        try
        {
            // Parse the schema and document using built-in JSON-P
            JsonReader schemaReader = Json.createReader(new StringReader(schemaJson));
            JsonObject schema = schemaReader.readObject();
            schemaReader.close();

            JsonReader documentReader = Json.createReader(new StringReader(documentJson));
            JsonValue document = documentReader.read();
            documentReader.close();

            // Perform validation
            List<String> errors = new ArrayList<>();
            validateValue(document, schema, "", errors);

            return new ValidationResult(errors.isEmpty(), errors);

        }
        catch (JsonException e)
        {
            return ValidationResult.error("Failed to parse JSON: " + e.getMessage());
        }
        catch (Exception e)
        {
            return ValidationResult.error("Validation error: " + e.getMessage());
        }
    }

    /**
     * Validates a JSON document against a JSON schema loaded from file
     * 
     * @param schemaPath
     *            Path to the JSON schema file
     * @param documentJson
     *            The JSON document to validate as a string
     * @return ValidationResult containing validation outcome and any errors
     */
    public ValidationResult validateWithSchemaFile(Path schemaPath, String documentJson)
    {
        try
        {
            String schemaJson = Files.readString(schemaPath);
            return validate(schemaJson, documentJson);
        }
        catch (IOException e)
        {
            return ValidationResult.error("Failed to read schema file: " + e.getMessage());
        }
    }

    /**
     * Validates a JSON document file against a JSON schema file
     * 
     * @param schemaPath
     *            Path to the schema file
     * @param documentPath
     *            Path to the document file
     * @return ValidationResult containing validation outcome and any errors
     */
    public ValidationResult validateFiles(Path schemaPath, Path documentPath)
    {
        try
        {
            String schemaJson = Files.readString(schemaPath);
            String documentJson = Files.readString(documentPath);
            return validate(schemaJson, documentJson);
        }
        catch (IOException e)
        {
            return ValidationResult.error("Failed to read files: " + e.getMessage());
        }
    }

    /**
     * Core validation logic
     */
    private void validateValue(JsonValue value, JsonObject schema, String path, List<String> errors)
    {
        // Check type
        JsonValue typeValue = schema.get("type");
        if (typeValue != null)
        {
            if (!isValidType(value, typeValue))
            {
                String expectedTypes = getExpectedTypesString(typeValue);
                errors.add(path + ": expected type " + expectedTypes + " but got '" + getActualType(value) + "'");
                return;
            }
        }

        // Validate based on type
        switch (value.getValueType())
        {
        case OBJECT -> validateObject(value.asJsonObject(), schema, path, errors);
        case ARRAY -> validateArray(value.asJsonArray(), schema, path, errors);
        case STRING -> validateString(((JsonString) value).getString(), schema, path, errors);
        case NUMBER -> validateNumber((JsonNumber) value, schema, path, errors);
        case TRUE, FALSE -> validateBoolean(value, schema, path, errors);
        case NULL -> validateNull(value, schema, path, errors);
        }
    }

    private void validateObject(JsonObject obj, JsonObject schema, String path, List<String> errors)
    {
        // Check required properties
        JsonArray required = schema.getJsonArray("required");
        if (required != null)
        {
            for (JsonValue req : required)
            {
                String reqField = ((JsonString) req).getString();
                if (!obj.containsKey(reqField))
                {
                    errors.add(path + ": missing required property '" + reqField + "'");
                }
            }
        }

        // Check additional properties
        boolean additionalProperties = schema.getBoolean("additionalProperties", true);
        JsonObject properties = schema.getJsonObject("properties");

        if (!additionalProperties && properties != null)
        {
            for (String key : obj.keySet())
            {
                if (!properties.containsKey(key))
                {
                    errors.add(path + ": additional property '" + key + "' is not allowed");
                }
            }
        }

        // Validate each property
        if (properties != null)
        {
            for (Map.Entry<String, JsonValue> entry : obj.entrySet())
            {
                String propertyName = entry.getKey();
                JsonValue propertyValue = entry.getValue();
                JsonObject propertySchema = properties.getJsonObject(propertyName);

                if (propertySchema != null)
                {
                    String newPath = path.isEmpty() ? propertyName : path + "." + propertyName;
                    validateValue(propertyValue, propertySchema, newPath, errors);
                }
            }
        }
    }

    private void validateArray(JsonArray arr, JsonObject schema, String path, List<String> errors)
    {
        // Validate array items if schema is provided
        JsonObject items = schema.getJsonObject("items");
        if (items != null)
        {
            for (int i = 0; i < arr.size(); i++)
            {
                validateValue(arr.get(i), items, path + "[" + i + "]", errors);
            }
        }

        // Check minItems and maxItems
        if (schema.containsKey("minItems"))
        {
            int minItems = schema.getInt("minItems");
            if (arr.size() < minItems)
            {
                errors.add(path + ": array must have at least " + minItems + " items");
            }
        }

        if (schema.containsKey("maxItems"))
        {
            int maxItems = schema.getInt("maxItems");
            if (arr.size() > maxItems)
            {
                errors.add(path + ": array must have at most " + maxItems + " items");
            }
        }
    }

    private void validateString(String str, JsonObject schema, String path, List<String> errors)
    {
        // Check minLength
        if (schema.containsKey("minLength"))
        {
            int minLength = schema.getInt("minLength");
            if (str.length() < minLength)
            {
                errors.add(path + ": string length must be at least " + minLength);
            }
        }

        // Check maxLength
        if (schema.containsKey("maxLength"))
        {
            int maxLength = schema.getInt("maxLength");
            if (str.length() > maxLength)
            {
                errors.add(path + ": string length must be at most " + maxLength);
            }
        }

        // Check pattern
        if (schema.containsKey("pattern"))
        {
            String patternStr = schema.getString("pattern");
            try
            {
                Pattern pattern = Pattern.compile(patternStr);
                if (!pattern.matcher(str).matches())
                {
                    errors.add(path + ": string does not match pattern '" + patternStr + "'");
                }
            }
            catch (Exception e)
            {
                errors.add(path + ": invalid pattern in schema: " + e.getMessage());
            }
        }

        // Check format
        if (schema.containsKey("format"))
        {
            String format = schema.getString("format");
            if ("date".equals(format))
            {
                if (!isValidDate(str))
                {
                    errors.add(path + ": string is not a valid date format (expected YYYY-MM-DD)");
                }
            }
            // Add more format validations as needed
        }
    }

    private void validateNumber(JsonNumber num, JsonObject schema, String path, List<String> errors)
    {
        double value = num.doubleValue();

        // Check minimum
        if (schema.containsKey("minimum"))
        {
            double minimum = schema.getJsonNumber("minimum").doubleValue();
            if (value < minimum)
            {
                errors.add(path + ": number must be at least " + minimum);
            }
        }

        // Check maximum
        if (schema.containsKey("maximum"))
        {
            double maximum = schema.getJsonNumber("maximum").doubleValue();
            if (value > maximum)
            {
                errors.add(path + ": number must be at most " + maximum);
            }
        }
    }

    private void validateBoolean(JsonValue bool, JsonObject schema, String path, List<String> errors)
    {
        // Boolean validation is typically just type checking, which is already done
    }

    private void validateNull(JsonValue nullValue, JsonObject schema, String path, List<String> errors)
    {
        // Null validation is typically just type checking, which is already done
    }

    private boolean isValidType(JsonValue value, JsonValue typeValue)
    {
        if (typeValue.getValueType() == JsonValue.ValueType.STRING)
        {
            // Single type
            String expectedType = ((JsonString) typeValue).getString();
            return isValidSingleType(value, expectedType);
        }
        else if (typeValue.getValueType() == JsonValue.ValueType.ARRAY)
        {
            // Multiple types - value must match at least one
            JsonArray typeArray = typeValue.asJsonArray();
            for (JsonValue type : typeArray)
            {
                if (type.getValueType() == JsonValue.ValueType.STRING)
                {
                    String expectedType = ((JsonString) type).getString();
                    if (isValidSingleType(value, expectedType))
                    {
                        return true; // Found a matching type
                    }
                }
            }
            return false; // No matching type found
        }
        return false; // Invalid type specification
    }

    private boolean isValidSingleType(JsonValue value, String expectedType)
    {
        return switch (expectedType.toLowerCase())
        {
        case "object" -> value.getValueType() == JsonValue.ValueType.OBJECT;
        case "array" -> value.getValueType() == JsonValue.ValueType.ARRAY;
        case "string" -> value.getValueType() == JsonValue.ValueType.STRING;
        case "number" -> value.getValueType() == JsonValue.ValueType.NUMBER;
        case "integer" -> value.getValueType() == JsonValue.ValueType.NUMBER &&
                ((JsonNumber) value).isIntegral();
        case "boolean" -> value.getValueType() == JsonValue.ValueType.TRUE ||
                value.getValueType() == JsonValue.ValueType.FALSE;
        case "null" -> value.getValueType() == JsonValue.ValueType.NULL;
        default -> false;
        };
    }

    private String getExpectedTypesString(JsonValue typeValue)
    {
        if (typeValue.getValueType() == JsonValue.ValueType.STRING)
        {
            return "'" + ((JsonString) typeValue).getString() + "'";
        }
        else if (typeValue.getValueType() == JsonValue.ValueType.ARRAY)
        {
            JsonArray typeArray = typeValue.asJsonArray();
            List<String> types = new ArrayList<>();
            for (JsonValue type : typeArray)
            {
                if (type.getValueType() == JsonValue.ValueType.STRING)
                {
                    types.add("'" + ((JsonString) type).getString() + "'");
                }
            }
            return "one of [" + String.join(", ", types) + "]";
        }
        return "unknown";
    }

    private String getActualType(JsonValue value)
    {
        return switch (value.getValueType())
        {
        case OBJECT -> "object";
        case ARRAY -> "array";
        case STRING -> "string";
        case NUMBER -> ((JsonNumber) value).isIntegral() ? "integer" : "number";
        case TRUE, FALSE -> "boolean";
        case NULL -> "null";
        };
    }

    private boolean isValidDate(String dateStr)
    {
        try
        {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        }
        catch (DateTimeParseException e)
        {
            return false;
        }
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
                System.out.println("✓ JSON document is valid!");
            }
            else
            {
                System.out.println("✗ JSON document is invalid:");
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
