package fundflow

import common.unit.Amount
import common.unit.Daily
import common.unit.TimeFrequency
import common.unit.TimeFrequencyOps
import java.math.BigDecimal

typealias Flow = Amount<TimeFrequency>
typealias DailyFlow = Amount<Daily>

object DailyFlowOps {
    val ZERO: DailyFlow = Amount.zero(Daily)
    operator fun Daily.invoke(value: BigDecimal): DailyFlow = Amount(
        value,
        Daily
    )
}

object FlowOps {
    fun Flow.toDailyFlow(): DailyFlow =
        this.let {
            TimeFrequencyOps.run {
                it convertTo Daily
            }
        }
}
