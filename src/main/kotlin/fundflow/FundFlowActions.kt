package fundflow

import common.Describable
import common.Identifiable
import common.Nameable
import java.util.*

data class FundFlowAction(
    override val name: String,
    override val description: String,
    override val id: UUID = UUID.randomUUID()
) : Identifiable, Nameable, Describable

data class FundFlowActionAcknowledgement<out Input, out Output>(
    val fundFlowAction: FundFlowAction,
    val input: Input,
    val output: Output,
    override val id: UUID
) : Identifiable