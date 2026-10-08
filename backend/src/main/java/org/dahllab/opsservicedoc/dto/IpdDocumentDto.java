package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;
import org.dahllab.opsservicedoc.model.IpdDocumentStatus;
import org.dahllab.opsservicedoc.model.SzenarioTyp;

import java.time.LocalDateTime;

// DTO für ein IPD-Dokument (API-Ebene). Feldreihenfolge entspricht
// exakt der Reihenfolge in IpdDocument.java, damit ich in Service und
// Mapper nicht durcheinanderkomme, welches Feld an welcher Stelle
// steht.
public record IpdDocumentDto(

        String id,
        String ticketId,
        IpdDocumentStatus status,
        @NotBlank(message = "Titel darf nicht leer sein")
        String titel,
        String techniker,
        SzenarioTyp szenarioTyp,
        String kunde,
        String ansprechpartnerKunde,
        String zeitraum,
        String ausgangslage,
        String anforderungen,
        String infrastrukturUebersicht,
        String serverUndVms,
        String netzwerk,
        String rollenUndVerantwortlichkeiten,
        String backupKonzept,
        String securityUeberlegungen,
        String durchgefuehrteSchritte,
        String entscheidungen,
        String risikenUndAnnahmen,
        String rollbackPlan,
        boolean qualitaetssicherungAbgeschlossen,
        LocalDateTime erstelltAm,
        LocalDateTime aktualisiertAm
) {
}
