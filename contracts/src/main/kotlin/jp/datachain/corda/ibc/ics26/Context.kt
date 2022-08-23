package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.states.IbcFungibleState
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party

class Context(val inStates: Collection<ContractState>, val refStates: Collection<ContractState>) {
    val outStates = mutableSetOf<ContractState>()

    private val inIbcStates get() = inStates.mapNotNull{it as? IbcState}
    private val refIbcStates get() = refStates.mapNotNull{it as? IbcState}
    private val outIbcStates get() = outStates.mapNotNull{it as? IbcState}

    val inNotary: Party get() = (inIbcStates + refIbcStates).map{it.notary}.distinct().single()
    val inValidators: List<Party> get() = (inIbcStates + refIbcStates).map{it.validators}.distinct().single()
    val inBaseId: StateRef get() = (inIbcStates + refIbcStates).map{it.baseId}.distinct().single()

    inline fun <reified T: ContractState> getInputs(): List<T> {
        return inStates.filterIsInstance<T>()
    }
    inline fun <reified T: ContractState> getInput() = getInputs<T>().single()

    inline fun <reified T: ContractState> getReferences(): List<T> {
        return refStates.filterIsInstance<T>()
    }
    inline fun <reified T: ContractState> getReference() = getReferences<T>().single()

    inline fun <reified T: ContractState> addOutput(state: T) {
        if (state is IbcState) {
            require(outStates.none { it is T }) { "At most one IBC state can be contained in a transaction"}
        }
        outStates.add(state)
    }

    fun verifyResults(expectedOutputStates: Collection<ContractState>) {
        // Confirm that output states are expected ones
        require(expectedOutputStates.size == outStates.size)
        expectedOutputStates.forEach{require(outStates.contains(it)){"$it is not included in outStates"}}

        // Confirm that all input states are included in output states
        require(outIbcStates.filter{it !is IbcFungibleState<*>}.map{it.linearId}.containsAll(
                inIbcStates.filter{it !is IbcFungibleState<*>}.map{it.linearId}))

        // Confirm all baseIds in states are same
        val outNotary = outIbcStates.map{it.notary}.distinct().single()
        require(inNotary == outNotary)
        val outValidators = outIbcStates.map{it.validators}.distinct().single()
        require(inValidators == outValidators)
        val outBaseId = outIbcStates.map{it.baseId}.distinct().single()
        require(inBaseId == outBaseId)
    }
}