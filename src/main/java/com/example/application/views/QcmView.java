package com.example.application.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "qcm", layout = MainLayout.class)
@PageTitle("QCM - Loi 2021-14")
public class QcmView extends VerticalLayout {

    private static final List<String> FICHIERS_CATEGORIES = Arrays.asList(
            "general.json",
            "departements.json",
            "communes-competences.json",
            "communes-organisation.json",
            "tutelle-finances.json"
    );

    private static final int NB_QUESTIONS_PAR_QUIZ = 10; // Maximum 10 questions par session

    private final Map<String, List<Question>> questionsParCategorie = new HashMap<>();
    private List<Question> questionsCourantes = new ArrayList<>();
    private int indexCourant = 0;

    private RadioButtonGroup<String> radioGroup;
    private H3 questionLabel;
    private ProgressBar progressBar;
    private Span progressionText;
    private Button btnPrecedent, btnSuivant, btnVoirReponses;

    public QcmView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        chargerToutesLesCategories();

        if (questionsParCategorie.isEmpty()) {
            removeAll();
            VerticalLayout error = new VerticalLayout();
            error.setSizeFull();
            error.setAlignItems(Alignment.CENTER);
            error.setJustifyContentMode(JustifyContentMode.CENTER);
            Span msg = new Span("Aucun fichier de questions trouvé dans src/main/resources/");
            msg.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.ERROR, LumoUtility.FontWeight.BOLD);
            error.add(msg);
            add(error);
            return;
        }

        if (questionsCourantes.isEmpty()) {
            montrerEcranSelection();
        } else {
            construireInterfaceQcm();
            afficherQuestionCourante();
        }
    }

    private void chargerToutesLesCategories() {
        ObjectMapper mapper = new ObjectMapper();
        for (String fichier : FICHIERS_CATEGORIES) {
            String nomCategorie = formatterNomCategorie(fichier);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fichier)) {
                if (is != null) {
                    List<Question> questions = mapper.readValue(is, new TypeReference<List<Question>>() {});
                    questionsParCategorie.put(nomCategorie, questions);
                }
            } catch (IOException e) {
                System.err.println("Erreur chargement " + fichier + " : " + e.getMessage());
            }
        }
    }

    private String formatterNomCategorie(String fichier) {
        return fichier.replace(".json", "")
                .replace("-", " ")
                .replace("general", "Dispositions générales")
                .replace("departements", "Départements")
                .replace("communes competences", "Compétences des communes")
                .replace("communes organisation", "Organisation des communes")
                .replace("tutelle finances", "Tutelle et finances")
                .toUpperCase();
    }

    private void montrerEcranSelection() {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Gap.XLARGE);

        H1 titre = new H1("Entraînement QCM");
        titre.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);

        H2 sousTitre = new H2("Loi n° 2021-14 – Administration Territoriale");
        sousTitre.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.TextColor.SECONDARY);

        ComboBox<String> combo = new ComboBox<>("Choisissez une catégorie");
        List<String> categories = new ArrayList<>(questionsParCategorie.keySet());
        categories.add(0, "TOUTES LES CATÉGORIES (mélangées)");
        combo.setItems(categories);
        combo.setWidth("600px");
        combo.addClassNames(LumoUtility.Margin.Top.XLARGE);

        Button demarrer = new Button("Démarrer le test (10 questions)", e -> {
            String choix = combo.getValue();
            if (choix == null || choix.isEmpty()) {
                Notification.show("Veuillez sélectionner une catégorie", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            List<Question> source;
            if (choix.equals("TOUTES LES CATÉGORIES (mélangées)")) {
                source = new ArrayList<>();
                questionsParCategorie.values().forEach(source::addAll);
            } else {
                source = new ArrayList<>(questionsParCategorie.get(choix));
            }

            Collections.shuffle(source);
            questionsCourantes = source.stream().limit(NB_QUESTIONS_PAR_QUIZ).collect(Collectors.toList());

            if (questionsCourantes.isEmpty()) {
                Notification.show("Pas assez de questions dans cette catégorie", 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            removeAll();
            construireInterfaceQcm();
            afficherQuestionCourante();
        });
        demarrer.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        layout.add(titre, sousTitre, combo, demarrer);
        add(layout);
    }

    private void construireInterfaceQcm() {
        removeAll();
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.MEDIUM, LumoUtility.Padding.LARGE, LumoUtility.Margin.LARGE);
        card.setMaxWidth("900px");
        card.setWidthFull();

        H2 titreCat = new H2("Quiz en cours – " + questionsCourantes.size() + " questions");
        titreCat.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Bottom.LARGE);

        progressBar = new ProgressBar();
        progressBar.setWidthFull();

        progressionText = new Span();
        progressionText.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.SECONDARY);

        VerticalLayout progressContainer = new VerticalLayout(progressBar, progressionText);
        progressContainer.setAlignItems(Alignment.CENTER);
        progressContainer.setSpacing(false);

        questionLabel = new H3();
        questionLabel.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Top.XLARGE);

        radioGroup = new RadioButtonGroup<>();
        radioGroup.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.Margin.Top.LARGE);
        radioGroup.setRenderer(new ComponentRenderer<>(prop -> {
            Div div = new Div();
            div.setText(prop);
            div.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Border.ALL,
                    LumoUtility.BorderRadius.MEDIUM, LumoUtility.Background.CONTRAST_5);
            div.getStyle().set("transition", "all 0.3s ease")
                    .set("font-size", "1.1em").set("font-weight", "500")
                    .set("cursor", "pointer").set("user-select", "none");

            radioGroup.addValueChangeListener(event -> {
                boolean selected = prop.equals(event.getValue());
                if (selected) {
                    div.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.TextColor.PRIMARY);
                    div.getStyle().set("border-color", "var(--lumo-primary-color)").set("border-width", "3px");
                } else {
                    div.removeClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.TextColor.PRIMARY);
                    div.getStyle().remove("border-color").remove("border-width");
                }
            });
            return div;
        }));
        radioGroup.addValueChangeListener(e -> sauvegarderReponse());

        btnPrecedent = new Button("Précédent", new Icon(VaadinIcon.ARROW_LEFT));
        btnPrecedent.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_CONTRAST);
        btnSuivant = new Button("Suivant", new Icon(VaadinIcon.ARROW_RIGHT));
        btnSuivant.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        btnVoirReponses = new Button("Voir réponses", new Icon(VaadinIcon.LIST));
        btnVoirReponses.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_LARGE);

        btnPrecedent.addClickListener(e -> allerPrecedent());
        btnSuivant.addClickListener(e -> allerSuivant());
        btnVoirReponses.addClickListener(e -> ouvrirRecapitulatif());

        HorizontalLayout nav = new HorizontalLayout(btnPrecedent, btnVoirReponses, btnSuivant);
        nav.setWidthFull();
        nav.setJustifyContentMode(JustifyContentMode.BETWEEN);
        nav.addClassNames(LumoUtility.Margin.Top.XLARGE);

        card.add(titreCat, progressContainer, questionLabel, radioGroup, nav);
        add(card);
    }

    private void afficherQuestionCourante() {
        Question q = questionsCourantes.get(indexCourant);
        questionLabel.setText((indexCourant + 1) + ". " + q.getTexte());
        radioGroup.setItems(q.getPropositions());

        if (q.getReponseChoisie() != null) {
            radioGroup.setValue(q.getPropositions().get(q.getReponseChoisie()));
        } else {
            radioGroup.setValue(null);
        }

        miseAJourProgression();
        btnPrecedent.setEnabled(indexCourant > 0);
        btnSuivant.setText(indexCourant == questionsCourantes.size() - 1 ? "Terminer" : "Suivant");
    }

    private void miseAJourProgression() {
        double progress = (indexCourant + 1.0) / questionsCourantes.size();
        progressBar.setValue(progress);
        int repondues = (int) questionsCourantes.stream().filter(q -> q.getReponseChoisie() != null).count();
        progressionText.setText(String.format("Question %d / %d • %d réponse%s",
                indexCourant + 1, questionsCourantes.size(), repondues, repondues > 1 ? "s" : ""));
    }

    private void sauvegarderReponse() {
        String valeur = radioGroup.getValue();
        if (valeur != null) {
            int index = questionsCourantes.get(indexCourant).getPropositions().indexOf(valeur);
            questionsCourantes.get(indexCourant).setReponseChoisie(index);
        } else {
            questionsCourantes.get(indexCourant).setReponseChoisie(null);
        }
        miseAJourProgression();
    }

    private void allerSuivant() {
        if (indexCourant < questionsCourantes.size() - 1) {
            indexCourant++;
            afficherQuestionCourante();
        } else {
            montrerResultatFinalAvecCorrection();
        }
    }

    private void allerPrecedent() {
        if (indexCourant > 0) {
            indexCourant--;
            afficherQuestionCourante();
        }
    }

    private void ouvrirRecapitulatif() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Récapitulatif rapide");
        dialog.setWidth("90%");
        dialog.setMaxWidth("900px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        for (int i = 0; i < questionsCourantes.size(); i++) {
            Question q = questionsCourantes.get(i);
            Icon icon = q.getReponseChoisie() == null ? VaadinIcon.CIRCLE.create()
                    : q.estCorrecte() ? VaadinIcon.CHECK_CIRCLE.create() : VaadinIcon.CLOSE_CIRCLE.create();
            icon.getStyle().set("color", q.getReponseChoisie() == null ? "gray"
                    : q.estCorrecte() ? "#2e7d32" : "#c62828");

            Span num = new Span((i + 1) + ". ");
            num.addClassName(LumoUtility.FontWeight.BOLD);

            String rep = q.getReponseChoisie() == null ? "Non répondu" : q.getReponseUtilisateur();
            Span texte = new Span(q.getTexte() + " → " + rep);
            texte.addClassNames(LumoUtility.FontSize.MEDIUM);

            HorizontalLayout ligne = new HorizontalLayout(icon, num, texte);
            ligne.setAlignItems(Alignment.CENTER);
            ligne.setWidthFull();
            content.add(ligne);
        }
        dialog.add(content);
        dialog.open();
    }

    private void montrerResultatFinalAvecCorrection() {
        int correctes = (int) questionsCourantes.stream().filter(Question::estCorrecte).count();
        double pourcentage = (double) correctes / questionsCourantes.size() * 100;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Résultat final");
        dialog.setWidth("95%");
        dialog.setMaxWidth("1000px");
        dialog.setHeight("90vh");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        H2 score = new H2(correctes + " / " + questionsCourantes.size() + " (" + String.format("%.0f", pourcentage) + " %)");
        score.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD,
                pourcentage >= 80 ? LumoUtility.TextColor.SUCCESS :
                        pourcentage >= 60 ? LumoUtility.TextColor.PRIMARY : LumoUtility.TextColor.ERROR);

        Span message = new Span(pourcentage >= 80 ? "Excellent ! 🎉" :
                pourcentage >= 60 ? "Bon résultat ! 👍" : "À réviser ! 💪");
        message.addClassNames(LumoUtility.FontSize.XLARGE);

        content.add(score, message);

        // Correction détaillée
        H3 correctionTitre = new H3("Correction détaillée");
        correctionTitre.addClassNames(LumoUtility.Margin.Top.XLARGE, LumoUtility.TextColor.HEADER);
        content.add(correctionTitre);

        for (int i = 0; i < questionsCourantes.size(); i++) {
            Question q = questionsCourantes.get(i);
            boolean correct = q.estCorrecte();

            Icon icon = correct ? VaadinIcon.CHECK_CIRCLE.create() : VaadinIcon.CLOSE_CIRCLE.create();
            icon.getStyle().set("color", correct ? "#2e7d32" : "#c62828");

            Span num = new Span((i + 1) + ". ");
            num.addClassName(LumoUtility.FontWeight.BOLD);

            Span questionTexte = new Span(q.getTexte());
            questionTexte.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.MEDIUM);

            Span bonne = new Span("Bonne réponse : " + q.getBonneReponse());
            bonne.addClassNames(LumoUtility.TextColor.SUCCESS, LumoUtility.FontWeight.BOLD);

            Span user = new Span("Votre réponse : " + (q.getReponseUtilisateur() == null ? "Aucune" : q.getReponseUtilisateur()));
            user.addClassNames(q.getReponseChoisie() == null ? LumoUtility.TextColor.SECONDARY :
                    correct ? LumoUtility.TextColor.SUCCESS : LumoUtility.TextColor.ERROR);

            Div explicationDiv = new Div();
            explicationDiv.setText(q.getExplication());
            explicationDiv.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.MEDIUM,
                    LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Top.SMALL);

            VerticalLayout bloc = new VerticalLayout(icon, num, questionTexte, bonne, user, explicationDiv);
            bloc.setSpacing(false);
            bloc.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Border.ALL,
                    LumoUtility.BorderRadius.LARGE, LumoUtility.Margin.Bottom.LARGE,
                    correct ? LumoUtility.Background.SUCCESS : LumoUtility.Background.ERROR);

            content.add(bloc);
        }

        Button recommencer = new Button("Nouveau quiz (10 questions)", e -> {
            dialog.close();
            questionsCourantes.clear();
            removeAll();
            montrerEcranSelection();
        });
        recommencer.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        Button fermer = new Button("Fermer", e -> dialog.close());
        fermer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(recommencer, fermer);
        actions.setJustifyContentMode(JustifyContentMode.CENTER);
        actions.addClassNames(LumoUtility.Margin.Top.XLARGE);

        content.add(actions);
        dialog.add(content);
        dialog.open();
    }
}