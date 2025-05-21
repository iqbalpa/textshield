package org.textshield.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform