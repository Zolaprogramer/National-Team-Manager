package com.nationalteammanager.exception;

/**
 * Traženi podatak ne postoji ili pripada drugom korisniku. Ta dva slučaja se namjerno ne razlikuju,
 * kako korisnik ne bi mogao saznati koji ID-evi postoje kod drugih korisnika.
 * Poruka izuzetka je ključ iz messages_*.properties.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String messageCode) {
        super(messageCode);
    }
}
