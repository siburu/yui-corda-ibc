package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.Any
import ibc.lightclients.corda.v1.Corda
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import net.corda.core.contracts.StateRef
import java.security.PublicKey

data class CordaConsensusState(val cordaConsensusState: Corda.ConsensusState) : ConsensusState {
    override val consensusState get() = Any.pack(cordaConsensusState, "")!!
    val baseId: StateRef get() = cordaConsensusState.baseId.into()
    val notaryKey: PublicKey get() = cordaConsensusState.notaryKey.into()
    val validatorKeys: List<PublicKey> get() = cordaConsensusState.validatorKeysList.map{it.into()}

    constructor(baseId: StateRef, notaryKey: PublicKey): this(Corda.ConsensusState.newBuilder()
            .setBaseId(baseId.into())
            .setNotaryKey(notaryKey.into())
            .build())

    override fun clientType() = ClientType.CordaClient
    override fun getRoot() = throw NotImplementedError()
    override fun getTimestamp() = throw NotImplementedError()
    override fun validateBasic() = throw NotImplementedError()
}