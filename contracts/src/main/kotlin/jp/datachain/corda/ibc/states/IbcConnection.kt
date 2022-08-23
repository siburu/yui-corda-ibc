package jp.datachain.corda.ibc.states

import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party

@BelongsToContract(Ibc::class)
data class IbcConnection private constructor (
        override val notary: Party,
        override val validators: List<Party>,
        override val baseId: StateRef,
        override val id: Identifier,
        val end: Connection.ConnectionEnd
) : IbcState {
    constructor(host: Host, id: Identifier, connectionEnd: Connection.ConnectionEnd)
            : this(host.notary, host.validators, host.baseId, id, connectionEnd)
}
