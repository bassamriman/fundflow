package fundflow

import common.Amount
import common.Daily
import common.TimeFrequency
import common.TimeFrequencyOps
import java.math.BigDecimal

typealias Flow = Amount<TimeFrequency>
typealias DailyFlow = Amount<Daily>

object DailyFlowOps {
    val ZERO: DailyFlow = Amount.zero(Daily)
    operator fun Flow.invoke(value: BigDecimal): DailyFlow = Amount(value, Daily)
}


object FlowOps {
    fun Flow.toDailyFlow(): DailyFlow =
        this.let {
            TimeFrequencyOps.run {
                it convertTo Daily
            }
        }
}
