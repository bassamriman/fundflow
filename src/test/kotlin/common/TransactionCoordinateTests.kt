package common

import arrow.core.None
import arrow.core.Some
import fundflow.FundRef
import graph.HierarchicalElementAPI
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import java.util.UUID
import ledger.Position
import ledger.TransactionCoordinates
import ledger.TransactionCoordinatesAPI

class FundRefGen : Gen<FundRef> {
    override fun constants(): Iterable<FundRef> = emptyList()
    override fun random(): Sequence<FundRef> = generateSequence {
        FundRef(UUID.randomUUID().toString())
    }
}

typealias TestTransactionCoordinates = TransactionCoordinates<FundRef>

class TestTransactionCoordinatesGen : Gen<TestTransactionCoordinates> {
    override fun constants(): Iterable<TestTransactionCoordinates> = emptyList()
    override fun random(): Sequence<TestTransactionCoordinates> = generateSequence {
        TestTransactionCoordinates(FundRefGen().random().first(), FundRefGen().random().first())
    }
}

class TransactionCoordinateTests : StringSpec({

    "POSITION: Should give nothing since the given account is not equivalent nor the parent of the source nor the destination account" {
        forAll(
            TestTransactionCoordinatesGen(),
            FundRefGen(),
            FundRefGen()
        ) { transactionCoordinate: TestTransactionCoordinates, fund1: FundRef, fund2: FundRef ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.position(
                    HierarchicalElementAPI.run {
                        empty(fund1).addChild(fund2)
                    }
                ) == None
            }
        }
    }

    "POSITION: Should give the position of the given account as Source" {
        forAll(
            TestTransactionCoordinatesGen()
        ) { transactionCoordinate: TestTransactionCoordinates ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.position(
                    HierarchicalElementAPI.run {
                        empty(transactionCoordinate.source)
                    }
                ) == Some(
                    Position.SOURCE
                )
            }
        }
    }

    "POSITION: Should give the position of the given account as Destination" {
        forAll(
            TestTransactionCoordinatesGen()
        ) { transactionCoordinate: TestTransactionCoordinates ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.position(
                    HierarchicalElementAPI.run {
                        empty(transactionCoordinate.destination)
                    }
                ) == Some(
                    Position.DESTINATION
                )
            }
        }
    }

    "POSITION: Should give the position of the child account given a parent account as Source " {
        forAll(
            TestTransactionCoordinatesGen(),
            FundRefGen()
        ) { transactionCoordinate: TestTransactionCoordinates, fund: FundRef ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.position(
                    HierarchicalElementAPI.run {
                        empty(fund).addChild(transactionCoordinate.source)
                    }
                ) == Some(
                    Position.SOURCE
                )
            }
        }
    }

    "POSITION: Should give the position of the child account given a parent account as Destination " {
        forAll(
            TestTransactionCoordinatesGen(),
            FundRefGen()
        ) { transactionCoordinate: TestTransactionCoordinates, fund: FundRef ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.position(
                    HierarchicalElementAPI.run {
                        empty(fund).addChild(transactionCoordinate.destination)
                    }
                ) == Some(
                    Position.DESTINATION
                )
            }
        }
    }

    "CONTAINS: Should return true since the given account is source" {
        forAll(
            TestTransactionCoordinatesGen()
        ) { transactionCoordinate: TestTransactionCoordinates ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.source in transactionCoordinate
            }
        }
    }

    "CONTAINS: Should return true since the given account is destination" {
        forAll(
            TestTransactionCoordinatesGen()
        ) { transactionCoordinate: TestTransactionCoordinates ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.destination in transactionCoordinate
            }
        }
    }

    "CONTAINS: Should return false since the given account is not source or destination" {
        forAll(
            TestTransactionCoordinatesGen(),
            FundRefGen()
        ) { transactionCoordinate: TestTransactionCoordinates, fund: FundRef ->
            TransactionCoordinatesAPI.run {
                fund !in transactionCoordinate
            }
        }
    }

    "TOLIST: Should return source and destination in a list" {
        forAll(
            TestTransactionCoordinatesGen()
        ) { transactionCoordinate: TestTransactionCoordinates ->
            TransactionCoordinatesAPI.run {
                transactionCoordinate.toList() == listOf(
                    transactionCoordinate.source,
                    transactionCoordinate.destination
                )
            }
        }
    }

    "UNARYMINUS: Should swap source and destination" {
        forAll(
            TestTransactionCoordinatesGen()
        ) { transactionCoordinate: TestTransactionCoordinates ->
            TransactionCoordinatesAPI.run {
                -transactionCoordinate ==
                    TransactionCoordinates(
                        transactionCoordinate.destination,
                        transactionCoordinate.source
                    )
            }
        }
    }
})
