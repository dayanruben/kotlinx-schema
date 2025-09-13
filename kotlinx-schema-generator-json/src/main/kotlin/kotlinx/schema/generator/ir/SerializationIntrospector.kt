package kotlinx.schema.generator.ir

import kotlinx.schema.generator.ir.DefaultPresence.Absent
import kotlinx.schema.generator.ir.DefaultPresence.Required
import kotlinx.schema.generator.json.internal.getPolymorphicDescriptors
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.descriptors.PrimitiveKind as SerialPrimitiveKind

/**
 * Introspects kotlinx.serialization descriptors into Schema IR.
 */
public class SerializationIntrospector(
    private val json: Json,
) : SchemaIntrospector<KSerializer<*>> {
    override fun introspect(root: KSerializer<*>): TypeGraph {
        val nodes = LinkedHashMap<TypeId, TypeNode>()
        val visiting = HashSet<SerialDescriptor>()
        val cache = HashMap<SerialDescriptor, TypeRef>()

        fun descriptorId(d: SerialDescriptor): TypeId = TypeId(d.serialName)

        fun primitiveFor(d: SerialDescriptor): PrimitiveNode? =
            when (d.kind) {
                SerialPrimitiveKind.STRING -> PrimitiveNode(PrimitiveKind.STRING)
                SerialPrimitiveKind.BOOLEAN -> PrimitiveNode(PrimitiveKind.BOOLEAN)
                SerialPrimitiveKind.BYTE, SerialPrimitiveKind.SHORT, SerialPrimitiveKind.INT ->
                    PrimitiveNode(
                        PrimitiveKind.INT,
                    )
                SerialPrimitiveKind.LONG -> PrimitiveNode(PrimitiveKind.LONG)
                SerialPrimitiveKind.FLOAT -> PrimitiveNode(PrimitiveKind.FLOAT)
                SerialPrimitiveKind.DOUBLE -> PrimitiveNode(PrimitiveKind.DOUBLE)
                SerialPrimitiveKind.CHAR -> PrimitiveNode(PrimitiveKind.STRING)
                else -> null
            }

        fun SerialDescriptor.readDescription(): String? =
            annotations.filterIsInstance<kotlinx.schema.Description>().firstOrNull()?.value

        fun elementDescription(
            parent: SerialDescriptor,
            index: Int,
        ): String? =
            parent
                .getElementAnnotations(index)
                .filterIsInstance<kotlinx.schema.Description>()
                .firstOrNull()
                ?.value

        fun toRef(d: SerialDescriptor): TypeRef {
            cache[d]?.let { return it }

            val nullable = d.isNullable

            // Primitives inline
            primitiveFor(d)?.let { prim ->
                val ref = TypeRef.Inline(prim, nullable)
                cache[d] = ref
                return ref
            }

            return when (d.kind) {
                is SerialKind.ENUM -> {
                    val id = descriptorId(d)
                    if (id !in nodes.keys && d !in visiting) {
                        visiting += d
                        val entries = (0 until d.elementsCount).map { d.getElementName(it) }
                        val node = EnumNode(name = d.serialName, entries = entries, description = d.readDescription())
                        nodes[id] = node
                        visiting -= d
                    }
                    val ref = TypeRef.Ref(id, nullable)
                    cache[d] = ref
                    ref
                }

                is StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP -> {
                    // Map is treated as object with additionalProperties semantics in emitters; keep as MapNode inline
                    if (d.kind == StructureKind.MAP) {
                        val keyDesc = d.getElementDescriptor(0)
                        val valDesc = d.getElementDescriptor(1)
                        val node = MapNode(key = toRef(keyDesc), value = toRef(valDesc))
                        val ref = TypeRef.Inline(node, nullable)
                        cache[d] = ref
                        ref
                    } else {
                        val id = descriptorId(d)
                        if (id !in nodes.keys && d !in visiting) {
                            visiting += d
                            val props = ArrayList<Property>()
                            val required = LinkedHashSet<String>()
                            for (i in 0 until d.elementsCount) {
                                val name = d.getElementName(i)
                                val ed = d.getElementDescriptor(i)
                                val pDesc = elementDescription(d, i)
                                val typeRef = toRef(ed)
                                val hasDefault = d.isElementOptional(i)
                                val presence = if (hasDefault) Absent else Required
                                if (!hasDefault) required += name
                                props +=
                                    Property(
                                        name = name,
                                        type = typeRef,
                                        description = pDesc,
                                        defaultPresence = presence,
                                    )
                            }
                            val node =
                                ObjectNode(
                                    name = d.serialName,
                                    properties = props,
                                    required = required,
                                    description = d.readDescription(),
                                )
                            nodes[id] = node
                            visiting -= d
                        }
                        val ref = TypeRef.Ref(id, nullable)
                        cache[d] = ref
                        ref
                    }
                }

                is StructureKind.LIST -> {
                    val elem = d.getElementDescriptor(0)
                    val node = ListNode(element = toRef(elem))
                    val ref = TypeRef.Inline(node, nullable)
                    cache[d] = ref
                    ref
                }

                is PolymorphicKind -> {
                    val id = descriptorId(d)
                    if (id !in nodes.keys && d !in visiting) {
                        visiting += d
                        val subDescs = d.getPolymorphicDescriptors(json)
                        val subtypes = subDescs.map { sd -> SubtypeRef(TypeId(sd.serialName)) }
                        val mode = json.configuration.classDiscriminatorMode
                        val discName = json.configuration.classDiscriminator
                        val required =
                            mode in listOf(ClassDiscriminatorMode.ALL_JSON_OBJECTS, ClassDiscriminatorMode.POLYMORPHIC)
                        val disc = Discriminator(name = discName, required = required, mapping = null)
                        val node =
                            PolymorphicNode(
                                baseName = d.serialName,
                                subtypes = subtypes,
                                discriminator = disc,
                                description = d.readDescription(),
                            )
                        nodes[id] = node
                        // also ensure each subtype object node is discovered
                        subDescs.forEach { sd -> toRef(sd) }
                        visiting -= d
                    }
                    val ref = TypeRef.Ref(id, nullable)
                    cache[d] = ref
                    ref
                }

                else -> {
                    // Fallback: treat unknown structured kinds as object without properties
                    val id = descriptorId(d)
                    if (id !in nodes.keys) {
                        nodes[id] =
                            ObjectNode(
                                name = d.serialName,
                                properties = emptyList(),
                                required = emptySet(),
                                description = d.readDescription(),
                            )
                    }
                    val ref = TypeRef.Ref(id, nullable)
                    cache[d] = ref
                    ref
                }
            }
        }

        val rootRef = toRef(root.descriptor)
        return TypeGraph(root = rootRef, nodes = nodes)
    }
}