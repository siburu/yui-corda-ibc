package jp.datachain.corda.ibc.flows.ics24

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Genesis
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcGenesisCreateFlow(private val validators: List<Party>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        require(validators.contains(ourIdentity)){"Flow initiator must be one of validators"}

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        builder.addCommand(Ibc.Commands.GenesisCreate(), ourIdentity.owningKey)
                .addOutputState(Genesis(validators))

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (validators - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcGenesisCreateFlow::class)
class IbcGenesisCreateResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}