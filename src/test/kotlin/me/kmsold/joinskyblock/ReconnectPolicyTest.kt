package me.kmsold.joinskyblock

import me.kmsold.joinskyblock.connect.ReconnectPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReconnectPolicyTest {

    private val kick = "Connection Lost\nYou were kicked while joining that server!"

    @Test
    fun `reconnects after a hypixel kick when enabled`() {
        val policy = ReconnectPolicy()
        policy.onJoin(isHypixel = true)
        assertTrue(policy.shouldReconnect(enabled = true, reason = kick))
    }

    @Test
    fun `stays put when the option is off`() {
        val policy = ReconnectPolicy()
        policy.onJoin(isHypixel = true)
        assertFalse(policy.shouldReconnect(enabled = false, reason = kick))
    }

    @Test
    fun `ignores other servers`() {
        val policy = ReconnectPolicy()
        policy.onJoin(isHypixel = false)
        assertFalse(policy.shouldReconnect(enabled = true, reason = kick))
    }

    @Test
    fun `never fights a ban or a login elsewhere`() {
        for (reason in listOf(
            "You are temporarily banned for 29d from this server!",
            "You logged in from another location!",
        )) {
            val policy = ReconnectPolicy()
            policy.onJoin(isHypixel = true)
            assertFalse(policy.shouldReconnect(enabled = true, reason = reason), reason)
        }
    }

    @Test
    fun `a session is only used once`() {
        val policy = ReconnectPolicy()
        policy.onJoin(isHypixel = true)
        assertTrue(policy.shouldReconnect(enabled = true, reason = kick))
        // A later failure on some other server must not look like a Hypixel kick.
        assertFalse(policy.shouldReconnect(enabled = true, reason = kick))
    }

    @Test
    fun `a failed reconnect is retried, but only up to the limit`() {
        val policy = ReconnectPolicy()
        policy.onJoin(isHypixel = true)
        assertTrue(policy.shouldReconnect(enabled = true, reason = kick))
        repeat(ReconnectPolicy.MAX_ATTEMPTS - 1) {
            policy.onReconnectStarted()
            assertTrue(policy.shouldReconnect(enabled = true, reason = "Connection refused"))
        }
        policy.onReconnectStarted()
        assertFalse(policy.shouldReconnect(enabled = true, reason = "Connection refused"))
    }

    @Test
    fun `a successful join resets the attempts`() {
        val policy = ReconnectPolicy()
        policy.onJoin(isHypixel = true)
        repeat(ReconnectPolicy.MAX_ATTEMPTS) {
            policy.shouldReconnect(enabled = true, reason = kick)
            policy.onReconnectStarted()
        }
        policy.onJoin(isHypixel = true)
        assertTrue(policy.shouldReconnect(enabled = true, reason = kick))
    }
}
