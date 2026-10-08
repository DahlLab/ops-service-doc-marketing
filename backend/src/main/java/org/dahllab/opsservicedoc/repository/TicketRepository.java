package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

// Repository-Interface für den Datenbankzugriff auf Tickets.
// MongoRepository<Ticket, String> gibt uns automatisch CRUD-Methoden
// (save, findById, findAll, delete, ...), OHNE dass ich sie selbst
// implementieren muss. Spring Data generiert die Implementierung
// zur Laufzeit (DRY; kein manueller Boilerplate-Code für Standard-Datenbankzugriffe).
//
// Generic-Parameter: <Ticket, String>
// - Ticket: die Entity-Klasse, die verwaltet wird
// - String: der Datentyp der ID (siehe @Id-Feld in Ticket.java)
public interface TicketRepository extends MongoRepository<Ticket, String> {

    // Spring Data generiert die Implementierung automatisch aus dem
    // Methodennamen (Derived Query): sucht ein Ticket anhand seiner
    // glpiTicketId. Brauche ich für die Upsert-Logik beim GLPI-Sync -
    // prüfen, ob ein Ticket mit dieser GLPI-ID schon existiert, bevor
    // ich ein neues anlege.
    Optional<Ticket> findByGlpiTicketId(String glpiTicketId);
}
