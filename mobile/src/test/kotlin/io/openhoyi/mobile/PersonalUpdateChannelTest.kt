package io.openhoyi.mobile

import org.junit.Assert.*
import org.junit.Test

class PersonalUpdateChannelTest {
    @Test fun personalIdentityAndItsTestVariantsCannotUseOfficialUpdates() {
        assertTrue(PersonalUpdateChannel.isPersonal("io.openhoyi.mobile.personal"))
        assertFalse(PersonalUpdateChannel.officialUpdatesAllowed("io.openhoyi.mobile.personal"))
        assertFalse(PersonalUpdateChannel.officialUpdatesAllowed("io.openhoyi.mobile.personal.test"))
        assertTrue(PersonalUpdateChannel.officialUpdatesAllowed("io.openhoyi.mobile"))
        assertTrue(PersonalUpdateChannel.officialUpdatesAllowed("io.openhoyi.mobile.mock"))
        assertEquals("https://github.com/wwh0188/future68/tree/feature/control-modes-curve-notes/downloads",
            PersonalUpdateChannel.DOWNLOADS_URL)
    }
}
