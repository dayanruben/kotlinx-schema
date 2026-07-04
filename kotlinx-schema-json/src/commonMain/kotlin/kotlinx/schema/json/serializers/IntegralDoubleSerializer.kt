package kotlinx.schema.json.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializer for [Double] constraint values (`minimum`, `maximum`, etc.) that renders
 * whole numbers as JSON integer literals.
 *
 * Numeric constraint fields are modeled as [Double], but for integer schemas the values are
 * whole numbers (e.g. `minimum: 0` for unsigned types). Without this serializer, such values
 * would be encoded as `0.0` on JVM/Native targets (and `0` on JS), producing inconsistent and
 * unexpected output for integer schemas.
 *
 * This serializer's behavior:
 * - During serialization: values that are exactly representable as [Long] are encoded as
 *   integer literals (`0` instead of `0.0`); all other values keep the standard [Double]
 *   encoding (`0.5` stays `0.5`).
 * - During deserialization: standard [Double] decoding, which accepts both integer and
 *   decimal JSON literals.
 */
internal object IntegralDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntegralDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()

    override fun serialize(
        encoder: Encoder,
        value: Double,
    ) {
        val asLong = value.toLong()
        if (asLong.toDouble() == value) {
            encoder.encodeLong(asLong)
        } else {
            encoder.encodeDouble(value)
        }
    }
}
