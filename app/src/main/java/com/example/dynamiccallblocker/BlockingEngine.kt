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
        return exactSet.any { exactMatches(incoming, it) } ||
            prefixSet.any { prefixMatches(incoming, it) }
    }

    private fun exactMatches(incoming: String, rule: String): Boolean {
        if (incoming == rule) return true

        val incomingNoCountryCode = stripNanpCountryCode(incoming)
        val ruleNoCountryCode = stripNanpCountryCode(rule)

        return (incomingNoCountryCode != null && incomingNoCountryCode == rule) ||
            (ruleNoCountryCode != null && incoming == ruleNoCountryCode) ||
            (incomingNoCountryCode != null && ruleNoCountryCode != null && incomingNoCountryCode == ruleNoCountryCode)
    }

    private fun prefixMatches(incoming: String, rule: String): Boolean {
        if (incoming.startsWith(rule)) return true

        val incomingNoCountryCode = stripNanpCountryCode(incoming)
        val ruleNoCountryCode = dropLeadingOne(rule)

        return (incomingNoCountryCode != null && incomingNoCountryCode.startsWith(rule)) ||
            (ruleNoCountryCode != null && incoming.startsWith(ruleNoCountryCode)) ||
            (incomingNoCountryCode != null && ruleNoCountryCode != null && incomingNoCountryCode.startsWith(ruleNoCountryCode))
    }

    private fun stripNanpCountryCode(number: String): String? {
        return if (number.length == 11 && number.startsWith("1")) number.drop(1) else null
    }

    private fun dropLeadingOne(number: String): String? {
        return if (number.length > 1 && number.startsWith("1")) number.drop(1) else null
    }
}
