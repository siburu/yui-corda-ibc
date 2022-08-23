package jp.datachain.corda.ibc.ics24

import ibc.core.client.v1.Client.Height
import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.corda.PREFIX
import jp.datachain.corda.ibc.clients.corda.VERSION
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.states.IbcState
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party

@BelongsToContract(Ibc::class)
data class Host constructor (
        override val notary: Party,
        override val validators: List<Party>,
        override val baseId: StateRef,
        val nextClientSequence: Long,
        val nextConnectionSequence: Long,
        val nextChannelSequence: Long,
        val bankIds: List<Identifier>
) : IbcState {
    override val id = Identifier("host")

    constructor(genesisAndRef: StateAndRef<Genesis>) : this(
            genesisAndRef.state.notary,
            genesisAndRef.state.data.validators,
            genesisAndRef.ref,
            0,
            0,
            0,
            emptyList()
    )

    fun getCurrentHeight() = HEIGHT

    fun getConsensusState(height: Height) : CordaConsensusState {
        require(height == getCurrentHeight())
        return CordaConsensusState(baseId, notary.owningKey)
    }

    fun getCommitmentPrefix() = PREFIX

    fun currentTimestamp() = Timestamp(0)

    fun getCompatibleVersions(): List<Connection.Version> = listOf(VERSION)
    fun pickVersion(supportedVersions: Collection<Connection.Version>, counterpartyVersions: Collection<Connection.Version>): Connection.Version {
        return supportedVersions.intersect(counterpartyVersions).first()
    }

    fun generateClientIdentifier(clientType: ClientType) = Pair(
            copy(nextClientSequence = nextClientSequence + 1),
            Identifier("$clientType-$nextClientSequence")
    )

    fun parseClientIdentifier(id: Identifier): Pair<ClientType, Long> {
        val lastIndex = id.id.lastIndexOf('-')
        val clientTypePart = id.id.substring(0, lastIndex)
        val sequencePart = id.id.substring(lastIndex + 1)
        val clientType = ClientType.fromString(clientTypePart)
        val sequence = sequencePart.toLong()
        return Pair(clientType, sequence)
    }

    fun generateConnectionIdentifier() = Pair(
            copy(nextConnectionSequence = nextConnectionSequence + 1),
            Identifier("connection-$nextConnectionSequence")
    )

    fun generateChannelIdentifier() = Pair(
            copy(nextChannelSequence = nextChannelSequence + 1),
            Identifier("channel-$nextChannelSequence")
    )

    fun addBank(id: Identifier) : Host {
        require(!bankIds.contains(id))
        return copy(bankIds = bankIds + id)
    }
}
