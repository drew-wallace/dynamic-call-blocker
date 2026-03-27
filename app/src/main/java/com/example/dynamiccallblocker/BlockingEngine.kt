package com.example.dynamiccallblocker

object BlockingEngine {
    fun shouldBlock(rawNumber: String, rules: BlockRules, isContact: Boolean): Boolean {
        if (!rules.blockingEnabled) return false

        val incoming = BlockRepository.normalizeForRule(rawNumber)
        if (incoming.isEmpty()) return false
        if (rules.allowContacts && isContact) return false

        val blocked = matches(incoming, rules.blockExact, rules.blockPrefix)
        if (!blocked) return false

        val allowed = matches(incoming, rules.allowExact, rules.allowPrefix)
        return !allowed
    }

    private fun matches(incoming: String, exactSet: Set<String>, prefixSet: Set<String>): Boolean {
        val incomingVariants = numberVariants(incoming)
        val exactMatched = exactSet.any { rule ->
            val ruleVariants = numberVariants(rule)
            incomingVariants.any { it in ruleVariants }
        }
        if (exactMatched) return true

        return prefixSet.any { rule ->
            val ruleVariants = numberVariants(rule)
            incomingVariants.any { incomingVariant ->
                ruleVariants.any { ruleVariant -> incomingVariant.startsWith(ruleVariant) }
            }
        }
    }

    private fun numberVariants(number: String): Set<String> {
        if (number.isEmpty()) return emptySet()

        return buildSet {
            add(number)
            if (number.startsWith("1") && number.length > 10) {
                add(number.drop(1))
            }
            if (!number.startsWith("1")) {
                add("1$number")
            }
        }
    }
}
