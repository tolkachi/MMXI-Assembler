package cse560;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArgFormatTest {

    //-----------------------------------------------------------------------
    // Test isValid
    //-----------------------------------------------------------------------

    // Test register detection //

    /**
     * Checking: Any valid hexadecimal number works with lowercase letters.
     * <p>
     * Input: "x0" through "xffff"
     * <p>
     * Expected Output: ArgType.BAD
     */
    @Test
    public void isValidAllLowerHexImmediates() {
        String hexPrefix = "x";

        for (int i = 0; i < 0x10000; ++i) {
            String arg = hexPrefix + Integer.toString(i, 16);

            assertEquals(
                    String.format("isValid(\"%s\") == ArgType.IMMEDIATE", arg),
                    ArgType.IMMEDIATE, ArgFormat.isValid(arg));
        }
    }

    /**
     * Checking: Any valid hexadecimal number works with upper case letters.
     * <p>
     * Input: "x0" through "xffff"
     * <p>
     * Expected Output: ArgType.IMMEDIATE
     */
    @Test
    public void isValidAllUpperHexImmediates() {
        String hexPrefix = "x";

        for (int i = 0; i < 0x10000; ++i) {
            String arg = hexPrefix + Integer.toString(i, 16).toUpperCase();

            assertEquals(
                    String.format("isValid(\"%s\") == ArgType.IMMEDIATE", arg),
                    ArgType.IMMEDIATE, ArgFormat.isValid(arg));
        }
    }

    /**
     * Inputs "RN" where N is not in [0, 7] returns ArgType.BAD.
     */
    @Test
    public void isValidBadRegisterIndex() {
        StringBuilder register = new StringBuilder("R0");

        for (int i = 8; i < 20; ++i) {
            register.setCharAt(1, Character.forDigit(i, 10));

            assertEquals(register.toString() + " is invalid", ArgType.BAD,
                    ArgFormat.isValid(register.toString()));
        }
    }

    /**
     * Checking: Decimal immediates work in general.
     * <p>
     * Input: "#-100" through "#499"
     * <p>
     * Expected Output: ArgType.IMMEDIATE
     */
    @Test
    public void isValidDecimalImmediates() {
        String hexPrefix = "#";

        for (int i = -0x8000; i < 0x7fff; ++i) {
            String arg = hexPrefix + Integer.toString(i, 10);

            assertEquals(
                    String.format("isValid(\"%s\") == ArgType.IMMEDIATE", arg),
                    ArgType.IMMEDIATE, ArgFormat.isValid(arg));
        }
    }

    // Test immediate detection

    /**
     * Inputs "R0" through "R7" all return ArgType.REGISTER.
     */
    @Test
    public void isValidGoodRegister() {
        StringBuilder register = new StringBuilder("R0");

        for (int i = 0; i < 8; ++i) {
            register.setCharAt(1, Character.forDigit(i, 10));

            assertEquals(register.toString() + " is valid", ArgType.REGISTER,
                    ArgFormat.isValid(register.toString()));
        }
    }

    /**
     * Checking: A hex immediate with non-hexy letters in it returns
     * ArgType.BAD.
     * <p>
     * Input: "x2zoo"
     * <p>
     * Expected Output: ArgType.BAD
     */
    @Test
    public void isValidHexImmediateBadLetters() {
        assertEquals("isValid(\"x2zoo\") == ArgType.BAD", ArgType.BAD,
                ArgFormat.isValid("x2zoo"));
    }

    /**
     * Checking: "R" alone should be detected as a bad format.
     * <p>
     * Input: "R"
     * <p>
     * Expected Output: ArgType.BAD
     */
    @Test
    public void isValidNoRegisterIndex() {
        assertEquals("isValid(\"R\") == ArgType.BAD", ArgType.BAD,
                ArgFormat.isValid("R"));
    }

    /**
     * Checking: "R" followed by non-digit characters should return ArgType.BAD
     * <p>
     * Input: "Rfoo"
     * <p>
     * Expected Output: ArgType.BAD
     */
    @Test
    public void isValidRegisterWithLetters() {
        assertEquals("isValid(\"Rfoo\") == ArgType.BAD", ArgType.BAD,
                ArgFormat.isValid("Rfoo"));
    }

    // Test literal detection

    // Test string detection

    // Test symbol detection
}
