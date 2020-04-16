package eu.druglogics.abmlog;

import org.junit.jupiter.api.Test;

import static eu.druglogics.abmlog.Util.getBinaryRepresentation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilTest {

	@Test
	void test_get_binary_representation() throws Exception {
		assertThrows(Exception.class, () -> getBinaryRepresentation(-1, 0));
		assertThrows(Exception.class, () -> getBinaryRepresentation(0, 0));
		assertThrows(Exception.class, () -> getBinaryRepresentation(3, 1));
		assertThrows(Exception.class, () -> getBinaryRepresentation(7, 2));
		assertThrows(Exception.class, () -> getBinaryRepresentation(16, 4));

		assertEquals(getBinaryRepresentation(0, 1), "0");
		assertEquals(getBinaryRepresentation(0, 3), "000");
		assertEquals(getBinaryRepresentation(0, 10), "0000000000");

		assertEquals(getBinaryRepresentation(5, 3), "101");
		assertEquals(getBinaryRepresentation(5, 4), "0101");
		assertEquals(getBinaryRepresentation(5, 5), "00101");

		assertEquals(getBinaryRepresentation(7, 3), "111");
		assertEquals(getBinaryRepresentation(7, 6), "000111");

		assertEquals(getBinaryRepresentation(0, 4), "0000");
		assertEquals(getBinaryRepresentation(1, 4), "0001");
		assertEquals(getBinaryRepresentation(3, 4), "0011");
		assertEquals(getBinaryRepresentation(11, 4), "1011");
		assertEquals(getBinaryRepresentation(11, 8), "00001011");
	}
}
