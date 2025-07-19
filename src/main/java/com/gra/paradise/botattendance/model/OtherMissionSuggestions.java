package com.gra.paradise.botattendance.model;

import java.util.Arrays;
import java.util.List;

public class OtherMissionSuggestions {
    public static final List<String> SUGGESTIONS = Arrays.asList(
            "Recrutamento",
            "Recrutamento GTM",
            "Recrutamento GOT",
            "Recrutamento GRA",
            "Recrutamento Pericia",
            "Recrutamento GIC",
            "Formacao Semanal",
            "Curso"
    );

    public static List<String> getSuggestions() {
        return SUGGESTIONS;
    }
}