package dev.yellowpagetr.spam;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpamDbTest {
    @Test public void detectsBundledKnownNumberInCommonDialFormats() {
        assertTrue(SpamDb.contains("+902127053327"));
        assertTrue(SpamDb.contains("02127053327"));
        assertTrue(SpamDb.contains("90 212 705 33 27"));
        assertTrue(SpamDb.contains("(0212) 705-33-27"));
    }
    @Test public void rejectsNonTurkishOrInvalidNumbers() {
        assertNull(SpamDb.normalize(null));
        assertNull(SpamDb.normalize("+14155552671"));
        assertNull(SpamDb.normalize("112"));
        assertNull(SpamDb.normalize("not-a-number"));
        assertFalse(SpamDb.contains("+902127053328"));
    }
    @Test public void listHasBundledBaseline() {
        assertEquals(201,SpamDb.size());
    }
}
