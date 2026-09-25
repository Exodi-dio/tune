package com.exodidio.tune

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform