package com.example.application.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Question {
    private final String texte;
    private final List<String> propositions;
    private final int bonneReponseIndex;
    private final String explication;  // Nouveau champ
    private Integer reponseChoisie = null;

    @JsonCreator
    public Question(@JsonProperty("texte") String texte,
                    @JsonProperty("propositions") List<String> propositions,
                    @JsonProperty("bonneReponseIndex") int bonneReponseIndex,
                    @JsonProperty("explication") String explication) {
        this.texte = texte;
        this.propositions = propositions;
        this.bonneReponseIndex = bonneReponseIndex;
        this.explication = explication;
    }

    public String getTexte() { return texte; }
    public List<String> getPropositions() { return propositions; }
    public int getBonneReponseIndex() { return bonneReponseIndex; }
    public String getExplication() { return explication; }
    public Integer getReponseChoisie() { return reponseChoisie; }
    public void setReponseChoisie(Integer reponseChoisie) { this.reponseChoisie = reponseChoisie; }

    public boolean estCorrecte() {
        return reponseChoisie != null && reponseChoisie == bonneReponseIndex;
    }

    public String getBonneReponse() {
        return propositions.get(bonneReponseIndex);
    }

    public String getReponseUtilisateur() {
        return reponseChoisie == null ? null : propositions.get(reponseChoisie);
    }
}