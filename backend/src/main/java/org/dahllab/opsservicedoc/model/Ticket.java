package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// @Document: markiert diese Klasse als MongoDB-Dokument.
// "collection = tickets" legt fest, in welcher Collection (vergleichbar mit
// einer Tabelle in relationalen DBs) die Objekte gespeichert werden.
@Document(collection = "tickets")
// @Data (Lombok): erzeugt automatisch Getter, Setter, toString(), equals() und hashCode(),
// damit wir das nicht alles von Hand schreiben müssen.
@Data
// @NoArgsConstructor: erzeugt einen leeren Konstruktor (Ticket()) - wird von
// MongoDB/Jackson beim Auslesen der Datenbank benötigt.
@NoArgsConstructor
// @AllArgsConstructor: erzeugt einen Konstruktor mit ALLEN Feldern als Parameter -
// praktisch, wenn wir z.B. in Tests oder im Service schnell ein komplettes Ticket bauen wollen.
@AllArgsConstructor
public class Ticket {

    // @Id: markiert dieses Feld als die eindeutige Datenbank-ID.
    // MongoDB generiert diesen Wert automatisch, wenn wir ihn beim Anlegen leer lassen.
    @Id
    private String id;

    // Referenz auf die ECHTE Ticket-ID in GLPI. Wird gebraucht, sobald wir später
    // den GLPI-Connector bauen, um unser eigenes Ticket mit dem GLPI-Original zu verknüpfen.
    // Kann anfangs null sein, solange wir nur mit Mock-Daten arbeiten.
    private String glpiTicketId;

    // Titel/Betreff des Tickets (entspricht dem Feld "name" in GLPI).
    private String title;

    // Ausführliche Beschreibung des Tickets (entspricht dem Feld "content" in GLPI).
    private String description;

    // Aktueller Bearbeitungsstatus, als Enum statt als rohe Zahl (siehe TicketStatus.java).
    private TicketStatus status;

    // Name oder Kennung des zuständigen Technikers/der Technikerin.
    private String technician;

    // Art des Wartungsszenarios, siehe SzenarioTyp.java.
    private ScenarioType scenarioType;

    // Zeitpunkt, an dem das Ticket bei uns erstellt wurde.
    // LocalDateTime speichert Datum UND Uhrzeit, ohne Zeitzonen-Informationen -
    // passt für unseren Anwendungsfall (ein Server, eine Zeitzone).
    private LocalDateTime createdAt;
}
