package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

interface IbcState : LinearState {
    val notary: Party
    val validators: List<Party>
    val baseId: StateRef
    val id: Identifier

    override val participants: List<AbstractParty> get() = validators

    override val linearId: UniqueIdentifier get() = UniqueIdentifier(baseId.toString(), id.toUUID())
}