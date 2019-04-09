package it.azraelsec.Protocol;

/**
 * The {@code Commands} enumeration encapsulates all the commands that {@code Client} and {@code Server}
 * will exchange between them via their main TCP connection.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public enum Commands {
    LOGIN,
    LOGOUT,
    CREATE,
    EDIT,
    EDIT_END,
    SHOW_SECTION,
    SHOW_DOCUMENT,
    LIST,
    SHARE,
    SUCCESS,
    FAILURE,
    NEW_NOTIFICATIONS,
    EXIT;

    /**
     * Gets the integer corresponding to the instance {@code Commands} kind.
     *
     * @return code integer representation
     */
    public int getCode() {
        return this.ordinal();
    }

    /**
     * Returns the {@code Commands} value related to the input code value.
     *
     * @param code  code value
     * @return  related command
     */
    public static Commands getCommand(int code) {
        try{
            return values()[code];
        }
        catch(IndexOutOfBoundsException ex){
            throw new IllegalArgumentException("Invalid Operation Code");
        }
    }
}
