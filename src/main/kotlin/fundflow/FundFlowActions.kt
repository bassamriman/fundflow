package fundflow

import common.Describable
import common.Identifiable
import common.Nameable
import java.util.*

data class FundFlowAction(
    override val name: String,
    override val description: String,
    override val id: String = UUID.randomUUID().toString()
) : Identifiable, Nameable, Describable

data class FundFlowActionAcknowledgement<out Input, out Output>(
    val fundFlowAction: FundFlowAction,
    val input: Input,
    val output: Output,
    override val id: String
) : Identifiable