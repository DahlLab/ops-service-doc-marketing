package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Ein einzelner Punkt innerhalb einer Checkliste (z.B. "USV geprüft").
// Ich bilde das bewusst als eingebettetes Unterobjekt ab, nicht als
// eigene MongoDB-Collection, weil ein Item nie unabhängig von seiner
// Checkliste existiert oder abgefragt wird.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChecklistItem {

    // Eigene ID pro Item (nicht die Mongo-ObjectId der Checkliste),
    // damit ich ein einzelnes Item beim Aktualisieren gezielt
    // wiedererkennen kann.
    private String id;
    private String beschreibung;
    private boolean erledigt;
}
