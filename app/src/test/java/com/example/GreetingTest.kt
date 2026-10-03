package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test fun greetingFollowsTheHour() {
        assertEquals("Good morning", greetingForHour(8))
        assertEquals("Good afternoon", greetingForHour(14))
        assertEquals("Good evening", greetingForHour(19))
    }

    @Test fun lateNightAndEarlyMorningSayHello() {
        assertEquals("Hello", greetingForHour(23))
        assertEquals("Hello", greetingForHour(2))
    }
}
