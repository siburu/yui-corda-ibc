package jp.datachain.corda.ibc.contracts

import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20cash.CashBank
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.DatagramHandler
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class Ibc : Contract {
    override fun verify(tx: LedgerTransaction) {
        for (commandSigners in tx.commands) {
            val command = commandSigners.value
            val signers = commandSigners.signers
            when (command) {
                is Commands -> {
                    command.verify(tx, signers)
                }
                is DatagramHandler -> {
                    val ctx = Context(tx.inputsOfType(), tx.referenceInputsOfType())
                    require(tx.notary == ctx.inNotary)
                    require(signers.contains(ctx.inNotary.owningKey))
                    require(signers.containsAll(ctx.inValidators.map{it.owningKey}))
                    command.execute(ctx, signers)
                    ctx.verifyResults(tx.outputsOfType())
                }
            }
        }
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class GenesisCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) = requireThat {
                "No state should be consumed" using (tx.inputs.isEmpty())
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val genesis = tx.outputsOfType<Genesis>().single()
                "Tx must be signed by all validators" using (signers.containsAll(genesis.validators.map{it.owningKey}))
            }
        }

        class HostCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val genesis = tx.inRefsOfType<Genesis>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val expectedHost = Host(genesis)
                "Output should be expected states" using (newHost == expectedHost)
                "Tx must be signed by all validators" using (signers.containsAll(newHost.validators.map{it.owningKey}))
            }
        }

        class BankCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val host = tx.inRefsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newBank = tx.outputsOfType<Bank>().single()
                val expectedBank = Bank(host.state.data)
                val expectedHost = host.state.data.addBank(expectedBank.id)
                "Output should be expected states" using (Pair(newHost, newBank) == Pair(expectedHost, expectedBank))
                "Tx must be signed by all validators" using (signers.containsAll(newHost.validators.map{it.owningKey}))
            }
        }

        data class CashBankCreate(val owner: Party) :  Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val host = tx.inRefsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newBank = tx.outputsOfType<CashBank>().single()
                val expectedBank = CashBank(host.state.data, owner)
                val expectedHost = host.state.data.addBank(expectedBank.id)
                "Output should be expected states" using (Pair(newHost, newBank) == Pair(expectedHost, expectedBank))
                "Tx must be signed by bank owner" using (signers.contains(owner.owningKey))
                "Tx must be signed by all validators" using (signers.containsAll(newHost.validators.map{it.owningKey}))
            }
        }

        data class FundAllocate(val owner: Address, val denom: Denom, val amount: Amount): Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val bank = tx.inputsOfType<Bank>().single()
                val newBank = tx.outputsOfType<Bank>().single()
                val expectedBank = bank.allocate(owner, denom, amount)
                "Output should be expected state" using (newBank == expectedBank)
                "Tx must be signed by bank owner" using (signers.contains(owner.toPublicKey()))
            }
        }

        data class SendPacket(val packet: ChannelOuterClass.Packet) : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                val ctx = Context(tx.inputsOfType(), tx.referenceInputsOfType())
                Handler.sendPacket(ctx, packet)
                ctx.verifyResults(tx.outputsOfType())
            }
        }
    }
}