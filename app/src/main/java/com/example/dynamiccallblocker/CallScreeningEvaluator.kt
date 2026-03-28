package com.example.dynamiccallblocker

data class CallScreeningEvaluation(
    val normalizedIncoming: String,
    val isContact: Boolean,
    val shouldBlock: Boolean,
    val blockExactCount: Int,
    val blockPrefixCount: Int,
    val allowExactCount: Int,
    val allowPrefixCount: Int,
    val failure: Throwable? = null
)

object CallScreeningEvaluator {
    suspend fun evaluate(
        rawNumber: String,
        loadRules: suspend () -> BlockRules,
        isInContacts: suspend (String) -> Boolean
    ): CallScreeningEvaluation {
        val normalizedIncoming = BlockRepository.normalizeForRule(rawNumber)

        return try {
            val rules = loadRules()
            val inContacts = isInContacts(rawNumber)

            CallScreeningEvaluation(
                normalizedIncoming = normalizedIncoming,
                isContact = inContacts,
                shouldBlock = BlockingEngine.shouldBlock(rawNumber, rules, inContacts),
                blockExactCount = rules.blockExact.size,
                blockPrefixCount = rules.blockPrefix.size,
                allowExactCount = rules.allowExact.size,
                allowPrefixCount = rules.allowPrefix.size
            )
        } catch (failure: Throwable) {
            CallScreeningEvaluation(
                normalizedIncoming = normalizedIncoming,
                isContact = false,
                shouldBlock = false,
                blockExactCount = 0,
                blockPrefixCount = 0,
                allowExactCount = 0,
                allowPrefixCount = 0,
                failure = failure
            )
        }
    }
}
