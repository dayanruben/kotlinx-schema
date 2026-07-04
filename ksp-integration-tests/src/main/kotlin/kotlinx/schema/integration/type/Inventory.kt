package kotlinx.schema.integration.type

import kotlinx.schema.Description
import kotlinx.schema.Schema

/**
 * Model with unsigned integer types
 */
@Description("Inventory record using unsigned numeric types.")
@Schema
data class Inventory(
    @Description("Number of items currently in stock")
    val quantity: UInt,
    @Description("Maximum storage capacity")
    val capacity: ULong,
    @Description("Optional reorder threshold")
    val reorderLevel: UShort?,
    @Description("Item codes")
    val codes: UIntArray,
)
