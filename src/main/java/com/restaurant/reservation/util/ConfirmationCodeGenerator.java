package com.restaurant.reservation.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Makes confirmation codes like "K7QM2P" (FR-6).
 *
 * The alphabet leaves out look-alike characters (0/O, 1/I/L) so
 * customers don't misread their code.
 *
 * SecureRandom is used instead of Random because codes protect bookings
 * (NFR-5). Random codes are much harder to guess than 1, 2, 3...
 */
public final class ConfirmationCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ConfirmationCodeGenerator() { }   // only static methods, never "new" this

    /** One random code. It might (very rarely) already be in use. */
    public static String generate() {
        StringBuilder code = new StringBuilder(LENGTH);   // StringBuilder: efficient way to build a String
        for (int i = 0; i < LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());  // random position 0 .. alphabet length - 1
            code.append(ALPHABET.charAt(index));
        }
        return code.toString();
    }

    /**
     * A code guaranteed not to be in use yet.
     *
     * @param isTaken a yes/no test that says whether a code is already used.
     *                Usually: data.reservations()::confirmationCodeExists
     */
    public static String generateUnique(Predicate<String> isTaken) {
        String code;
        do {
            code = generate();
        } while (isTaken.test(code));   // try again until we get an unused one
        return code;
    }
}
