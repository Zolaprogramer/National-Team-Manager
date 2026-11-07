package com.nationalteammanager.exception;

/**
 * Akcija krši poslovno pravilo. Izuzetak nosi ključ iz messages_*.properties i argumente poruke,
 * a tekst na jeziku korisnika se bira tek pri prikazu.
 */
public class BusinessRuleException extends RuntimeException {

    private final transient Object[] arguments;

    public BusinessRuleException(String messageCode, Object... arguments) {
        super(messageCode);
        this.arguments = arguments;
    }

    public String getCode() {
        return getMessage();
    }

    public Object[] getArguments() {
        return arguments;
    }
}
