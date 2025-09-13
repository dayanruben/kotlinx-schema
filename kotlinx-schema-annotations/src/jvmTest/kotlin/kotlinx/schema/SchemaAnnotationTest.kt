package kotlinx.schema

import io.kotest.matchers.collections.shouldHaveSize
import kotlin.test.Test

class SchemaAnnotationTest {
    @Test
    fun `@Schema annotation should be erased`() {
        val classAnnotations =
            Person::class
                .annotations
                .filterIsInstance<Schema>()
        classAnnotations shouldHaveSize 0
    }
}