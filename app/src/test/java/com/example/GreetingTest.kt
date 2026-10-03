package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test fun greetingFollowsTheHour() {
        assertEquals("GOOD MORNING,", greetingForHour(8))
        assertEquals("GOOD AFTERNOON,", greetingForHour(14))
        assertEquals("GOOD EVENING,", greetingForHour(19))
    }

    @Test fun lateNightAndEarlyMorningSayHello() {
        assertEquals("HELLO,", greetingForHour(23))
        assertEquals("HELLO,", greetingForHour(2))
    }
}
