package com.example.dynamiccallblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingEngineTest {
    @Test
    fun blocksPrefix919WhenIncomingHasCountryCodeAndContactsBypassDisabled() {
        val rules = BlockRules(
            blockPrefix = setOf("919"),
            allowContacts = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "+19195551234",
            rules = rules,
            isContact = true
        )

        assertTrue(shouldBlock)
    }

    @Test
    fun blocksPrefix1919WhenIncomingHasNoCountryCode() {
        val rules = BlockRules(
            blockPrefix = setOf("1919"),
            allowContacts = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "9195551234",
            rules = rules,
            isContact = false
        )

        assertTrue(shouldBlock)
    }

    @Test
    fun supportsRulesEnteredWithPlusCountryCodeAfterNormalization() {
        val rules = BlockRules(
            blockPrefix = setOf(BlockRepository.normalizeForRule("+1919")),
            allowContacts = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "9195551234",
            rules = rules,
            isContact = false
        )

        assertTrue(shouldBlock)
    }

    @Test
    fun blocksExactMatchWithOptionalNanpCountryCode() {
        val rules = BlockRules(
            blockExact = setOf("9195551234"),
            allowContacts = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "+19195551234",
            rules = rules,
            isContact = false
        )

        assertTrue(shouldBlock)
    }

    @Test
    fun allowListOverridesBlockList() {
        val rules = BlockRules(
            blockPrefix = setOf("919"),
            allowPrefix = setOf("919555"),
            allowContacts = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "+19195551234",
            rules = rules,
            isContact = false
        )

        assertFalse(shouldBlock)
    }

    @Test
    fun contactBypassPreventsBlockWhenEnabled() {
        val rules = BlockRules(
            blockPrefix = setOf("919"),
            allowContacts = true
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "+19195551234",
            rules = rules,
            isContact = true
        )

        assertFalse(shouldBlock)
    }

    @Test
    fun globalToggleDisablesBlockingCompletely() {
        val rules = BlockRules(
            blockPrefix = setOf("919"),
            allowContacts = false,
            blockingEnabled = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "9195551234",
            rules = rules,
            isContact = false
        )

        assertFalse(shouldBlock)
    }

    @Test
    fun doesNotMatchDifferentAreaCode() {
        val rules = BlockRules(
            blockPrefix = setOf("919"),
            allowContacts = false
        )

        val shouldBlock = BlockingEngine.shouldBlock(
            rawNumber = "+12025551234",
            rules = rules,
            isContact = false
        )

        assertFalse(shouldBlock)
    }
}
