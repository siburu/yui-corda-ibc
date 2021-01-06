package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.channel.v1.QueryGrpc
import ibc.core.channel.v1.QueryOuterClass
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort

class ChannelQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryGrpc.QueryImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun channel(request: QueryOuterClass.QueryChannelRequest, responseObserver: StreamObserver<QueryOuterClass.QueryChannelResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryChannelResponse.newBuilder()
                .setChannel(stateAndRef.state.data.end)
                .setProof(proof.toByteString())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}