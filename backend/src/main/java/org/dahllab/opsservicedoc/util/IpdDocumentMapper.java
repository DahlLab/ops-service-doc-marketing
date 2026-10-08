package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.IpdDocumentDto;
import org.dahllab.opsservicedoc.model.IpdDocument;

// Wandelt IpdDocument (Mongo-Dokument) in IpdDocumentDto (API-Ebene)
// um, gleiches Einweg-Mapping-Muster wie TicketMapper, TaskMapper und
// ChecklistMapper. Die Rückrichtung baut der Service selbst
// zusammen, weil dort noch Zusatzlogik reinspielt (automatische
// Felder neu berechnen).
public class IpdDocumentMapper {

    private IpdDocumentMapper() {
        // Utility-Klasse, keine Instanzen nötig.
    }

    public static IpdDocumentDto toDto(IpdDocument dokument) {
        return new IpdDocumentDto(
                dokument.getId(),
                dokument.getTicketId(),
                dokument.getStatus(),
                dokument.getTitel(),
                dokument.getTechniker(),
                dokument.getSzenarioTyp(),
                dokument.getKunde(),
                dokument.getAnsprechpartnerKunde(),
                dokument.getZeitraum(),
                dokument.getAusgangslage(),
                dokument.getAnforderungen(),
                dokument.getInfrastrukturUebersicht(),
                dokument.getServerUndVms(),
                dokument.getNetzwerk(),
                dokument.getRollenUndVerantwortlichkeiten(),
                dokument.getBackupKonzept(),
                dokument.getSecurityUeberlegungen(),
                dokument.getDurchgefuehrteSchritte(),
                dokument.getEntscheidungen(),
                dokument.getRisikenUndAnnahmen(),
                dokument.getRollbackPlan(),
                dokument.isQualitaetssicherungAbgeschlossen(),
                dokument.getErstelltAm(),
                dokument.getAktualisiertAm()
        );
    }
}
