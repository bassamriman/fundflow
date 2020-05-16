import common.*
import common.unit.Amount
import common.unit.BiWeekly
import common.unit.Monthly
import fundflow.Fund
import fundflow.FundHierarchy
import fundflow.FundRef
import fundflow.FundRelation
import fundflow.ledgers.*
import graph.HierarchicalTree
import graph.HierarchicalTreeApi
import ledger.LedgerContextAPI
import ledger.TransactionCoordinates
import java.time.LocalDateTime

fun main(args: Array<String>) {

    val income = Fund(
        name = "Income",
        description = "income",
        reference = FundRef("Income")
    )

    val tfsa = Fund(
        name = "TFSAFund",
        description = "This is the TFSA Fund",
        reference = FundRef("TFSAFund")
    )

    val savingTangerine = Fund(
        name = "SavingTangerineFund",
        description = "This is the SavingTangerineFund",
        reference = FundRef("SavingTangerineFund")
    )
    val checkingTangerine =
        Fund(
            name = "CheckingTangerineFund",
            description = "This is the CheckingTangerineFund",
            reference = FundRef("CheckingTangerineFund")
        )

    val house = Fund(
        name = "HouseFund",
        description = "This is a house fund",
        reference = FundRef("HouseFund")
    )

    val car = Fund(
        name = "CarFund",
        description = "This is a Car fund",
        reference = FundRef("CarFund")
    )

    val travel = Fund(
        name = "TravelFund",
        description = "This is a Travel fund",
        reference = FundRef("TravelFund")
    )

    val relations = listOf(
        FundRelation(tfsa.reference, savingTangerine.reference),
        FundRelation(savingTangerine.reference, travel.reference),
        FundRelation(checkingTangerine.reference, house.reference)
    )

    val fundHierarchy: ValueWithError<HierarchicalTree<FundRef>> =
        HierarchicalTreeApi.run {
            FundHierarchy.empty<FundRef>() + relations
        }

    val dateTimeInterval = DateTimeInterval(LocalDateTime.now(), LocalDateTime.now().plusDays(100))

    val carLeasePayment =
        RecurrentTransaction(
            quantification = RecurrentTransactionQuantification(
                Amount(
                    200.bd,
                    BiWeekly
                )
            ),
            details = RecurrentTransactionDetail(dateTimeInterval),
            transactionCoordinates = TransactionCoordinates(income.reference, car.reference)
        )
    val rentPayment =
        RecurrentTransaction(
            quantification = RecurrentTransactionQuantification(
                Amount(
                    1200.bd,
                    Monthly
                )
            ),
            details = RecurrentTransactionDetail(dateTimeInterval),
            transactionCoordinates = TransactionCoordinates(income.reference, house.reference)
        )

    val travelPayment =
        RecurrentTransaction(
            quantification = RecurrentTransactionQuantification(
                Amount(
                    100.bd,
                    BiWeekly
                )
            ),
            details = RecurrentTransactionDetail(dateTimeInterval),
            transactionCoordinates = TransactionCoordinates(income.reference, travel.reference)
        )

    val recurrentTransactionLedgerContext =
        RecurrentTransactionLedgerContext(
            funds = listOf(
                income,
                tfsa,
                savingTangerine,
                checkingTangerine,
                house,
                car,
                travel
            ),
            fundHierarchy = fundHierarchy.v,
            recurrentTransactions = listOf(carLeasePayment, rentPayment, travelPayment)
        )

    val recurrentBalanceTransactionLedgerContext =
        RecurrentTransactionLedgerContextAPI.run {
            recurrentTransactionLedgerContext.rollOutBalanceTransactionsIn(dateTimeInterval)
        }
    val rt =
        RecurrentTransactionLedgerContextAPI.run { recurrentTransactionLedgerContext.viewAll() }

    val rbt = LedgerContextAPI.run {
        recurrentBalanceTransactionLedgerContext.viewAll(
            fundView = RecurrentBalanceTransactionFundViewFactory
        )
    }

    println("$rbt")

/*
    val fundHierarchy: FundHierarchy = HierarchicalTree.empty()

    val TFSAFund: FundRef = FundRef.create("TFSAFund", "This is the TFSAFund")

    val savingTangerineFund: FundRef = FundRef.create("SavingTangerineFund", "This is the SavingTangerineFund")
    val checkingTangerineFund: FundRef = FundRef.create("CheckingTangerineFund", "This is the CheckingTangerineFund")

    val savingRBCFund: FundRef = FundRef.create("RBCSavingFund", "This is the SavingRBCFund")
    val checkingRBCFund: FundRef = FundRef.create("CheckingRBCFund", "This is the CheckingRBCFund")

    val houseFund: FundRef = FundRef.create("HouseFund", "This is a house fund")
    val travelFund: FundRef = FundRef.create("TravelFund", "This is a Travel fund")




    HierarchicalTreeApi.run {
        val relations = listOf(
            ParentChild(TFSAFund, savingTangerineFund),
            ParentChild(savingTangerineFund, travelFund),
            ParentChild(TFSAFund, savingRBCFund),
            ParentChild(checkingTangerineFund, houseFund)
        )

        val fundHierarchy = fundHierarchy + relations

        println("TFSAFund.children(fundHierarchy): " + TFSAFund.children(fundHierarchy))
        println("travelFund.childOf(TFSAFund): " + travelFund.childOf(TFSAFund, fundHierarchy))
        println("checkingRBCFund.childOf(TFSAFund): " + checkingRBCFund.childOf(TFSAFund, fundHierarchy))

        println(
            "checkingTangerineFund.parentOf(houseFund): " + checkingTangerineFund.parentOf(
                houseFund,
                fundHierarchy
            )
        )

    }


    val savingTangerineFund: FundRef = FundRef.create("SavingTangerineFund", "This is the SavingTangerineFund")
    val savingRBCFund: FundRef = FundRef.create("RBCSavingFund", "This is the SavingRBCFund")

    val carFund: FundRef = FundRef.create("CarFund", "This is a car fund")
    val houseFund: FundRef = FundRef.create("HouseFund", "This is a house fund")
    val sourceFund: FundRef = FundRef.create("SourceFund", "This is a source fund")

    val relations = listOf(
        ParentChild(savingRBCFund, sourceFund),
        ParentChild(savingTangerineFund, houseFund),
        ParentChild(savingTangerineFund, carFund)
    )

    val fundHierarchy: Try<HierarchicalTree<FundRef>> = HierarchicalTreeApi.run {
        FundHierarchy.empty<FundRef>().add(relations)
    }

    val now = LocalDateTime.now()


    val carLeasePayment =
        FlowTransaction(
            RecurrentTransaction(now, Amount(200.bd, BiWeekly)),
            FlowTransactionDetail(now),
            TransactionCoordinates(sourceFund, carFund)
        )
    val rentPayment =
        FlowTransaction(
            RecurrentTransaction(now, Amount(1200.bd, Monthly)),
            FlowTransactionDetail(now),
            TransactionCoordinates(sourceFund, houseFund)
        )

    val flowLedger: RecurrentTransactionLedger = RecurrentTransactionLedger(listOf(carLeasePayment, rentPayment))

    val balanceLedger: BalanceLedger = RecurrentTransactionLedgerAPI.run {
        flowLedger.toBalanceLedger(now.plusDays(1), now.plusMonths(5))
    }

    val dailyFlowLedger: DailyFlowLedger = RecurrentTransactionLedgerAPI.run {
        flowLedger.toDailyFlow()
    }

    LedgerApi.run {
        //val perFundBalanceLedger = balanceLedger.splitByFund()
        //val perFundDailyFlowLedger = dailyFlowLedger.splitByFund()


        SingleFundLedgerAPI.run {
            //val balances = perFundBalanceLedger.map { it.fundFlow(BalanceTransactionOps) }
            //val dailyFlows = perFundDailyFlowLedger.map { it.fundFlow(DailyFlowQuantificationOps) }

            fundHierarchy.fold(
                { t -> FundHierarchy.empty<FundRef>() },
                { fundHierarchy ->
                    val savingTangerineFundFlow = flowLedger.ledgerOfWithChild(savingTangerineFund, fundHierarchy)
                    val savingRBCFundFlows = flowLedger.ledgerOfWithChild(savingRBCFund, fundHierarchy)

                    val savingTangerineFundBalanceLedger: Option<SingleFundLedger<BigDecimal>> =
                        balanceLedger.ledgerOfWithChild(savingTangerineFund, fundHierarchy)
                    val savingRBCFundFlowsBalanceLedger: Option<SingleFundLedger<BigDecimal>> =
                        balanceLedger.ledgerOfWithChild(savingRBCFund, fundHierarchy)
                    val savingTangerineFundBalanceLedgerBalance =
                        savingTangerineFundBalanceLedger.map { it.fundFlow(BalanceTransactionOps, fundHierarchy) }
                    val savingRBCFundFlowsBalanceLedgerBalance =
                        savingRBCFundFlowsBalanceLedger.map { it.fundFlow(BalanceTransactionOps, fundHierarchy) }

                    //println(balances)
                    println(savingTangerineFundBalanceLedgerBalance)
                    println(savingRBCFundFlowsBalanceLedgerBalance)
                })
        }
    }

*/
}