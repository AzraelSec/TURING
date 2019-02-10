package it.azraelsec;

public enum Commands {
    REGISTER,
    LOGIN,
    LOGOUT,
    CREATE,
    EDIT,
    EDIT_END,
    SHOW_SECTION,
    SHOW_DOCUMENT,
    LIST,
    CHAT_SEND,
    CHAT_RECV;

    public int getCode() {
        return this.ordinal();
    }

    public static Commands getCommand(int code) {
        try{
            return values()[code];
        }
        catch(IndexOutOfBoundsException ex){
            throw new IllegalArgumentException("Invalid Operation Code");
        }
    }
}
