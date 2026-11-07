package com.nationalteammanager.enums;

/**
 * Enum čiji se naziv prikazuje korisniku. Tekst se nalazi u messages_*.properties pod ključem
 * NazivEnuma.VRIJEDNOST, a Thymeleaf ga čita izrazom #{${vrijednost.messageKey}}.
 */
public interface TranslatableEnum {

    String name();

    default String getMessageKey() {
        return getClass().getSimpleName() + "." + name();
    }
}
