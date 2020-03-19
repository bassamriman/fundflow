package common

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

interface Nameable {
    val name: String
}

interface Describable {
    val description: String
}

interface Identifiable {
    val id: String
}

interface Temporal {
    val timestamp: LocalDateTime
}

interface hasBalance {
    val balance: BigDecimal
}
