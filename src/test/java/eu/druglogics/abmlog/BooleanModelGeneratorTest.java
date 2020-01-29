package eu.druglogics.abmlog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BooleanModelGeneratorTest {

	@Test
	void test_get_binary_representation() throws Exception {
		BooleanModelGenerator bmg = new BooleanModelGenerator();

		assertThrows(Exception.class, () -> bmg.getBinaryRepresentation(-1, 0));
		assertThrows(Exception.class, () -> bmg.getBinaryRepresentation(0, 0));
		assertThrows(Exception.class, () -> bmg.getBinaryRepresentation(3, 1));
		assertThrows(Exception.class, () -> bmg.getBinaryRepresentation(7, 2));
		assertThrows(Exception.class, () -> bmg.getBinaryRepresentation(16, 4));

		assertEquals(bmg.getBinaryRepresentation(0, 1), "0");
		assertEquals(bmg.getBinaryRepresentation(0, 3), "000");
		assertEquals(bmg.getBinaryRepresentation(0, 10), "0000000000");

		assertEquals(bmg.getBinaryRepresentation(5, 3), "101");
		assertEquals(bmg.getBinaryRepresentation(5, 4), "0101");
		assertEquals(bmg.getBinaryRepresentation(5, 5), "00101");

		assertEquals(bmg.getBinaryRepresentation(7, 3), "111");
		assertEquals(bmg.getBinaryRepresentation(7, 6), "000111");

		assertEquals(bmg.getBinaryRepresentation(0, 4), "0000");
		assertEquals(bmg.getBinaryRepresentation(1, 4), "0001");
		assertEquals(bmg.getBinaryRepresentation(3, 4), "0011");
		assertEquals(bmg.getBinaryRepresentation(11, 4), "1011");
		assertEquals(bmg.getBinaryRepresentation(11, 8), "00001011");
	}
}
