package common

import arrow.core.None
import arrow.core.Some
import fundflow.FundRef
import fundflow.ledgers.BalanceTransactionOps
import graph.HierarchicalElementAPI
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import java.math.BigDecimal
import ledger.Sign
import ledger.Transaction
import ledger.TransactionApi

typealias TestTransaction = Transaction<BigDecimal, BigDecimal, FundRef>

class BigDecimalGen : Gen<BigDecimal> {
    override fun constants(): Iterable<BigDecimal> = emptyList()

    override fun random(): Sequence<BigDecimal> = generateSequence { BigDecimal(Math.random()) }
}

class NegativeBigDecimalGen : Gen<BigDecimal> {
    override fun constants(): Iterable<BigDecimal> = emptyList()

    override fun random(): Sequence<BigDecimal> = generateSequence { BigDecimal(-Math.random()) }
}

class PositiveTestTransactionGen : Gen<TestTransaction> {
    override fun constants(): Iterable<TestTransaction> = emptyList()
    override fun random(): Sequence<TestTransaction> = generateSequence {
        TestTransaction(
            BigDecimalGen().random().first(),
            BigDecimalGen().random().first(),
            TestTransactionCoordinatesGen().random().first()
        )
    }
}

class NegativeTestTransactionGen : Gen<TestTransaction> {
    override fun constants(): Iterable<TestTransaction> = emptyList()
    override fun random(): Sequence<TestTransaction> = generateSequence {
        TestTransaction(
            NegativeBigDecimalGen().random().first(),
            NegativeBigDecimalGen().random().first(),
            TestTransactionCoordinatesGen().random().first()
        )
    }
}

class TransactionTests : StringSpec({

    "fixOrientation : Should swap source and destination and quantification is be positive " {
        forAll(NegativeTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val newTransaction = transaction.fixOrientation(BalanceTransactionOps)
                newTransaction.quantification >= BigDecimal.ZERO && newTransaction == TestTransaction(
                    -transaction.quantification,
                    transaction.details,
                    TestTransactionCoordinates(
                        transaction.transactionCoordinates.destination,
                        transaction.transactionCoordinates.source
                    )
                )
            }
        }
    }

    "fixOrientation : Should return the same transaction with no change " {
        forAll(PositiveTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val newTransaction = transaction.fixOrientation(BalanceTransactionOps)
                newTransaction.quantification >= BigDecimal.ZERO && newTransaction == TestTransaction(
                    transaction.quantification,
                    transaction.details,
                    TestTransactionCoordinates(
                        transaction.transactionCoordinates.source,
                        transaction.transactionCoordinates.destination
                    )
                )
            }
        }
    }

    "negate : should negate the quantity of the transaction to become negative given a positive input" {
        forAll(PositiveTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val newTransaction = transaction.negate(BalanceTransactionOps)
                newTransaction.quantification <= BigDecimal.ZERO && newTransaction == TestTransaction(
                    -transaction.quantification,
                    transaction.details,
                    TestTransactionCoordinates(
                        transaction.transactionCoordinates.source,
                        transaction.transactionCoordinates.destination
                    )
                )
            }
        }
    }

    "negate : should negate the quantity of the transaction to become positive given a negative input" {
        forAll(NegativeTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val newTransaction = transaction.negate(BalanceTransactionOps)
                newTransaction.quantification >= BigDecimal.ZERO && newTransaction == TestTransaction(
                    -transaction.quantification,
                    transaction.details,
                    TestTransactionCoordinates(
                        transaction.transactionCoordinates.source,
                        transaction.transactionCoordinates.destination
                    )
                )
            }
        }
    }

    "forcePositive : Should return the same transaction given the fund is the destination " {
        forAll(PositiveTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val maybeNewTransaction =
                    transaction.forcePositive(transaction.transactionCoordinates.destination, BalanceTransactionOps)
                maybeNewTransaction == Some(transaction)
            }
        }
    }

    "forcePositive : Should swap the transaction orientation and negate the quantity " {
        forAll(PositiveTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val maybeNewTransaction =
                    transaction.forcePositive(transaction.transactionCoordinates.source, BalanceTransactionOps)
                maybeNewTransaction == Some(
                    TestTransaction(
                        -transaction.quantification,
                        transaction.details,
                        TestTransactionCoordinates(
                            transaction.transactionCoordinates.source,
                            transaction.transactionCoordinates.destination
                        )
                    )
                )
            }
        }
    }

    "forcePositive : Should give nothing since input fund is not part of transaction " {
        forAll(PositiveTestTransactionGen(), FundRefGen()) { transaction, fund ->
            TransactionApi.run {
                val maybeNewTransaction =
                    transaction.forcePositive(fund, BalanceTransactionOps)
                maybeNewTransaction == None
            }
        }
    }

    "Sign : Should return negative given the input fund is the source " {
        forAll(PositiveTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val maybeSign =
                    transaction.sign(
                        HierarchicalElementAPI.run {
                            empty(transaction.transactionCoordinates.source)
                        }
                    )
                maybeSign == Some(Sign.NEGATIVE)
            }
        }
    }

    "Sign : Should return positive given the input fund is the source " {
        forAll(PositiveTestTransactionGen()) { transaction ->
            TransactionApi.run {
                val maybeSign =
                    transaction.sign(
                        HierarchicalElementAPI.run {
                            empty(transaction.transactionCoordinates.destination)
                        }
                    )
                maybeSign == Some(Sign.POSITIVE)
            }
        }
    }

    "Sign : Should give nothing since input fund is not part of transaction" {
        forAll(PositiveTestTransactionGen(), FundRefGen()) { transaction, fund ->
            TransactionApi.run {
                val maybeSign =
                    transaction.sign(
                        HierarchicalElementAPI.run {
                            empty(fund)
                        }
                    )
                maybeSign == None
            }
        }
    }
})
