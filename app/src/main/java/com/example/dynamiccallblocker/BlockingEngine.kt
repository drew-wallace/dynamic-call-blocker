package com.example.dynamiccallblocker

object BlockingEngine {
    fun shouldBlock(rawNumber: String, rules: BlockRules, isContact: Boolean): Boolean {
        val incoming = BlockRepository.normalizeForRule(rawNumber)
        if (incoming.isEmpty()) return false
        if (rules.allowContacts && isContact) return false

        val blocked = matches(incoming, rules.blockExact, rules.blockPrefix)
        if (!blocked) return false

        val allowed = matches(incoming, rules.allowExact, rules.allowPrefix)
        return !allowed
    }

    private fun matches(incoming: String, exactSet: Set<String>, prefixSet: Set<String>): Boolean {
        return incoming in exactSet || prefixSet.any { incoming.startsWith(it) }
    }
}
