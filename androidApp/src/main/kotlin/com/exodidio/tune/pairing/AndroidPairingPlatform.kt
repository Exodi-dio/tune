package com.exodidio.tune.pairing

import java.util.UUID
import com.exodidio.tune.pairing.PairingClock
import com.exodidio.tune.pairing.PairingIdGenerator

object AndroidPairingClock : PairingClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

object AndroidPairingIdGenerator : PairingIdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
