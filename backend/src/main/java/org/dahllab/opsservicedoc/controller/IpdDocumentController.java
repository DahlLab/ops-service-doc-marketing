package org.dahllab.opsservicedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dahllab.opsservicedoc.dto.IpdDocumentDto;
import org.dahllab.opsservicedoc.service.IpdDocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST-Schnittstelle für IPD-Dokumente. Pfadpräfix "/api/ipd" (nicht
// "/api/ipd-documents"), weil ich diesen Pfad schon vorab in der
// SecurityConfig als geschützten Bereich eingetragen hatte.
//
// @Tag: fasst alle Endpoints dieses Controllers in der Swagger-UI unter
// einer gemeinsamen Überschrift zusammen, statt sie nur nach
// Klassennamen zu sortieren.
@Tag(name = "IPD-Dokumente", description = "Erstellen, Pflegen und Exportieren von IPD-Dokumenten (Infrastructure Planning & Design)")
@RestController
@RequestMapping("/api/ipd")
public class IpdDocumentController {

    private final IpdDocumentService ipdDocumentService;

    public IpdDocumentController(IpdDocumentService ipdDocumentService) {
        this.ipdDocumentService = ipdDocumentService;
    }

    // GET /api/ipd - liefert alle IPD-Dokumente, optional gefiltert
    // nach ticketId (gleiches Muster wie bei Task/Checklist).
    @Operation(
            summary = "Alle IPD-Dokumente abrufen",
            description = "Liefert alle IPD-Dokumente. Wird der optionale Parameter ticketId mitgegeben, " +
                    "werden nur die IPD-Dokumente zu genau diesem Ticket zurückgegeben."
    )
    @ApiResponse(responseCode = "200", description = "Liste der IPD-Dokumente (kann leer sein)")
    @GetMapping
    public List<IpdDocumentDto> getAllIpdDocuments(
            @Parameter(description = "Optionale Ticket-ID zum Filtern der Ergebnisse")
            @RequestParam(required = false) String ticketId) {
        if (ticketId != null) {
            return ipdDocumentService.getIpdDocumentsByTicketId(ticketId);
        }
        return ipdDocumentService.getAllIpdDocuments();
    }

    // GET /api/ipd/{id} - liefert genau ein IPD-Dokument.
    @Operation(summary = "Ein IPD-Dokument anhand seiner ID abrufen")
    @ApiResponse(responseCode = "200", description = "IPD-Dokument gefunden",
            content = @Content(schema = @Schema(implementation = IpdDocumentDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein IPD-Dokument mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}")
    public IpdDocumentDto getIpdDocumentById(
            @Parameter(description = "ID des IPD-Dokuments") @PathVariable String id) {
        return ipdDocumentService.getIpdDocumentById(id);
    }

    // POST /api/ipd/from-ticket/{ticketId} - erzeugt einen neuen
    // IPD-Entwurf automatisch aus einem bestehenden Ticket. Alle
    // restlichen Abschnitte muss ich danach per PUT noch von Hand
    // ergänzen.
    @Operation(
            summary = "Neuen IPD-Entwurf aus einem Ticket erzeugen",
            description = "Legt automatisch einen neuen IPD-Entwurf (Status DRAFT) an und übernimmt " +
                    "Titel, Techniker und Szenariotyp aus dem angegebenen Ticket. Zusätzlich werden " +
                    "die durchgeführten Schritte aus den erledigten Tasks sowie der Qualitätssicherungs-" +
                    "Status aus den Checklisten dieses Tickets automatisch ermittelt. Alle übrigen " +
                    "Abschnitte (Kunde, Infrastruktur, Risiken usw.) müssen danach per PUT ergänzt werden."
    )
    @ApiResponse(responseCode = "201", description = "IPD-Entwurf wurde erstellt",
            content = @Content(schema = @Schema(implementation = IpdDocumentDto.class)))
    @ApiResponse(responseCode = "404", description = "Kein Ticket mit dieser ID vorhanden", content = @Content)
    @PostMapping("/from-ticket/{ticketId}")
    @ResponseStatus(HttpStatus.CREATED)
    public IpdDocumentDto createIpdDocumentFromTicket(
            @Parameter(description = "ID des Tickets, aus dem der Entwurf erzeugt wird") @PathVariable String ticketId) {
        return ipdDocumentService.createIpdDocumentFromTicket(ticketId);
    }

    // PUT /api/ipd/{id} - aktualisiert die manuell gepflegten
    // Abschnitte eines bestehenden IPD-Dokuments, z.B. um es von
    // ENTWURF auf ABGESCHLOSSEN zu setzen, bevor es an den Kunden
    // rausgeht.
    @Operation(
            summary = "Ein IPD-Dokument aktualisieren",
            description = "Aktualisiert die manuell gepflegten Abschnitte eines bestehenden IPD-Dokuments " +
                    "(z.B. Kunde, Ansprechpartner, Infrastruktur, Risiken) und erlaubt den Statuswechsel " +
                    "von DRAFT auf COMPLETED. Die durchgeführten Schritte sowie der Qualitätssicherungs-" +
                    "Status werden dabei serverseitig neu berechnet, nicht aus dem Request übernommen."
    )
    @ApiResponse(responseCode = "200", description = "IPD-Dokument wurde aktualisiert",
            content = @Content(schema = @Schema(implementation = IpdDocumentDto.class)))
    @ApiResponse(responseCode = "400", description = "Request-Body ist ungültig oder unvollständig", content = @Content)
    @ApiResponse(responseCode = "404", description = "Kein IPD-Dokument mit dieser ID vorhanden", content = @Content)
    @PutMapping("/{id}")
    public IpdDocumentDto updateIpdDocument(
            @Parameter(description = "ID des zu aktualisierenden IPD-Dokuments") @PathVariable String id,
            @Valid @RequestBody IpdDocumentDto ipdDocumentDto) {
        return ipdDocumentService.updateIpdDocument(id, ipdDocumentDto);
    }

    // DELETE /api/ipd/{id} - löscht ein IPD-Dokument.
    @Operation(summary = "Ein IPD-Dokument löschen")
    @ApiResponse(responseCode = "204", description = "IPD-Dokument wurde gelöscht", content = @Content)
    @ApiResponse(responseCode = "404", description = "Kein IPD-Dokument mit dieser ID vorhanden", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIpdDocument(
            @Parameter(description = "ID des zu löschenden IPD-Dokuments") @PathVariable String id) {
        ipdDocumentService.deleteIpdDocument(id);
    }

    // GET /api/ipd/{id}/pdf - erzeugt das fertige PDF und liefert es
    // als Datei-Download. Content-Disposition "attachment" sorgt
    // dafür, dass der Browser die Datei speichert, statt sie im Tab zu
    // öffnen.
    @Operation(
            summary = "IPD-Dokument als PDF exportieren",
            description = "Erzeugt aus dem gespeicherten IPD-Dokument ein fertig formatiertes PDF " +
                    "(DahlLab-Design) und liefert es als Datei-Download. Die interne Checkliste wird " +
                    "dabei bewusst nicht im Detail ausgegeben, nur das Gesamtergebnis " +
                    "\"Qualitätssicherung durchgeführt: Ja/Nein\"."
    )
    @ApiResponse(responseCode = "200", description = "PDF wurde erzeugt",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "Kein IPD-Dokument mit dieser ID vorhanden", content = @Content)
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "ID des IPD-Dokuments") @PathVariable String id) {
        byte[] pdf = ipdDocumentService.generatePdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ipd-" + id + ".pdf\"")
                .body(pdf);
    }

    // GET /api/ipd/{id}/checklist-pdf - interne Checkliste für den
    // Techniker als separater Download (Kunden-PDF bleibt unverändert).
    @Operation(
            summary = "Checkliste als PDF herunterladen",
            description = "Erzeugt aus den Checklisten zum Ticket des IPD-Dokuments ein internes PDF mit " +
                    "ausfüllbaren Checkboxen (zum Ausdrucken oder Bearbeiten am Tablet). " +
                    "Nicht für den Kunden bestimmt."
    )
    @ApiResponse(responseCode = "200", description = "PDF wurde erzeugt",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "Kein IPD-Dokument oder keine Checkliste vorhanden", content = @Content)
    @GetMapping("/{id}/checklist-pdf")
    public ResponseEntity<byte[]> downloadChecklistPdf(
            @Parameter(description = "ID des IPD-Dokuments") @PathVariable String id) {
        byte[] pdf = ipdDocumentService.generateChecklistPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"checkliste-" + id + ".pdf\"")
                .body(pdf);
    }
}
