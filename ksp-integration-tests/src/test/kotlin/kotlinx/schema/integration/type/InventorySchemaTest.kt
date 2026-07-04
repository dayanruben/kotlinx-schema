package kotlinx.schema.integration.type

import io.kotest.assertions.json.shouldEqualJson
import kotlin.test.Test

/**
 * Tests for Inventory schema generation - unsigned integer types and unsigned arrays.
 */
class InventorySchemaTest {
    @Test
    fun `generates integer schema with minimum 0 for unsigned types`() {
        val schema = Inventory::class.jsonSchemaString

        // language=json
        schema shouldEqualJson
            $$"""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "kotlinx.schema.integration.type.Inventory",
              "type": "object",
              "properties": {
                "quantity": {
                  "type": "integer",
                  "description": "Number of items currently in stock",
                  "minimum": 0
                },
                "capacity": {
                  "type": "integer",
                  "description": "Maximum storage capacity",
                  "minimum": 0
                },
                "reorderLevel": {
                  "type": ["integer", "null"],
                  "description": "Optional reorder threshold",
                  "minimum": 0
                },
                "codes": {
                  "type": "array",
                  "description": "Item codes",
                  "items": {
                    "type": "integer",
                    "minimum": 0
                  }
                }
              },
              "required": ["quantity", "capacity", "reorderLevel", "codes"],
              "additionalProperties": false,
              "description": "Inventory record using unsigned numeric types."
            }
            """.trimIndent()
    }
}
