package org.example.pdfextractor.service;

/**
 * Service for translating text between languages.
 */
public interface TranslationService {

    /**
     * Translates the given text from source language to target language.
     *
     * @param text       the text to translate
     * @param sourceLang source language code (e.g. "en")
     * @param targetLang target language code (e.g. "ru")
     * @return translated text, or the original text if translation fails
     */
    String translate(String text, String sourceLang, String targetLang);
}
