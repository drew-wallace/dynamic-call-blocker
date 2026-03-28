package com.example.dynamiccallblocker

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallScreeningEvaluatorTest {
    @Test
    fun evaluateReturnsBlockingDecisionAndRuleCounts() = runBlocking {
        val rules = BlockRules(
            blockExact = setOf("12025551234"),
            blockPrefix = setOf("919"),
            allowExact = setOf("12025550000"),
            allowPrefix = setOf("1202"),
            allowContacts = false
        )

        val evaluation = CallScreeningEvaluator.evaluate(
            rawNumber = "+19195551234",
            loadRules = { rules },
            isInContacts = { false }
        )

        assertEquals("19195551234", evaluation.normalizedIncoming)
        assertTrue(evaluation.shouldBlock)
        assertFalse(evaluation.isContact)
        assertEquals(1, evaluation.blockExactCount)
        assertEquals(1, evaluation.blockPrefixCount)
        assertEquals(1, evaluation.allowExactCount)
        assertEquals(1, evaluation.allowPrefixCount)
        assertNull(evaluation.failure)
    }

    @Test
    fun evaluateFallsBackToAllowWhenRuleLoadingFails() = runBlocking {
        val evaluation = CallScreeningEvaluator.evaluate(
            rawNumber = "+19195551234",
            loadRules = { error("boom") },
            isInContacts = { true }
        )

        assertEquals("19195551234", evaluation.normalizedIncoming)
        assertFalse(evaluation.shouldBlock)
        assertFalse(evaluation.isContact)
        assertEquals(0, evaluation.blockExactCount)
        assertEquals(0, evaluation.blockPrefixCount)
        assertEquals(0, evaluation.allowExactCount)
        assertEquals(0, evaluation.allowPrefixCount)
        assertNotNull(evaluation.failure)
    }
}
