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

@Tag(name = "IPD documents", description = "Create, maintain and export IPD documents (Infrastructure Planning & Design)")
@RestController
@RequestMapping("/api/ipd")
public class IpdDocumentController {
    private final IpdDocumentService ipdDocumentService;

    public IpdDocumentController(IpdDocumentService ipdDocumentService) {
        this.ipdDocumentService = ipdDocumentService;
    }

    @Operation(
            summary = "Get all IPD documents",
            description = "Returns all IPD documents. If the optional parameter ticketId is provided, " +
                    "only the IPD documents for exactly that ticket are returned."
    )
    @ApiResponse(responseCode = "200", description = "List of IPD documents (may be empty)")
    @GetMapping
    public List<IpdDocumentDto> getAllIpdDocuments(
            @Parameter(description = "Optional ticket ID to filter the results")
            @RequestParam(required = false) String ticketId) {
        if (ticketId != null) {
            return ipdDocumentService.getIpdDocumentsByTicketId(ticketId);
        }
        return ipdDocumentService.getAllIpdDocuments();
    }

    @Operation(summary = "Get an IPD document by its ID")
    @ApiResponse(responseCode = "200", description = "IPD document found",
            content = @Content(schema = @Schema(implementation = IpdDocumentDto.class)))
    @ApiResponse(responseCode = "404", description = "No IPD document with this ID exists", content = @Content)
    @GetMapping("/{id}")
    public IpdDocumentDto getIpdDocumentById(
            @Parameter(description = "ID of the IPD document") @PathVariable String id) {
        return ipdDocumentService.getIpdDocumentById(id);
    }

    @Operation(
            summary = "Create a new IPD draft from a ticket",
            description = "Automatically creates a new IPD draft (status DRAFT) and takes over " +
                    "the title, technician and scenario type from the given ticket. In addition, " +
                    "the performed steps are derived from the completed tasks and the quality assurance " +
                    "status from the checklists of this ticket. All other " +
                    "sections (customer, infrastructure, risks, etc.) must then be added via PUT."
    )
    @ApiResponse(responseCode = "201", description = "IPD draft was created",
            content = @Content(schema = @Schema(implementation = IpdDocumentDto.class)))
    @ApiResponse(responseCode = "404", description = "No ticket with this ID exists", content = @Content)
    @PostMapping("/from-ticket/{ticketId}")
    @ResponseStatus(HttpStatus.CREATED)
    public IpdDocumentDto createIpdDocumentFromTicket(
            @Parameter(description = "ID of the ticket the draft is created from") @PathVariable String ticketId) {
        return ipdDocumentService.createIpdDocumentFromTicket(ticketId);
    }

    @Operation(
            summary = "Update an IPD document",
            description = "Updates the manually maintained sections of an existing IPD document " +
                    "(e.g. customer, contact person, infrastructure, risks) and allows the status change " +
                    "from DRAFT to COMPLETED. The performed steps and the quality assurance " +
                    "status are recalculated on the server and not taken from the request."
    )
    @ApiResponse(responseCode = "200", description = "IPD document was updated",
            content = @Content(schema = @Schema(implementation = IpdDocumentDto.class)))
    @ApiResponse(responseCode = "400", description = "Request body is invalid or incomplete", content = @Content)
    @ApiResponse(responseCode = "404", description = "No IPD document with this ID exists", content = @Content)
    @PutMapping("/{id}")
    public IpdDocumentDto updateIpdDocument(
            @Parameter(description = "ID of the IPD document to update") @PathVariable String id,
            @Valid @RequestBody IpdDocumentDto ipdDocumentDto) {
        return ipdDocumentService.updateIpdDocument(id, ipdDocumentDto);
    }

    @Operation(summary = "Delete an IPD document")
    @ApiResponse(responseCode = "204", description = "IPD document was deleted", content = @Content)
    @ApiResponse(responseCode = "404", description = "No IPD document with this ID exists", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIpdDocument(
            @Parameter(description = "ID of the IPD document to delete") @PathVariable String id) {
        ipdDocumentService.deleteIpdDocument(id);
    }

    @Operation(
            summary = "Export an IPD document as PDF",
            description = "Generates a fully formatted PDF from the stored IPD document " +
                    "(DahlLab design) and returns it as a file download. The internal checklist is " +
                    "deliberately not output in detail, only the overall result " +
                    "\"Quality assurance completed: Yes/No\"."
    )
    @ApiResponse(responseCode = "200", description = "PDF was generated",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "No IPD document with this ID exists", content = @Content)
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "ID of the IPD document") @PathVariable String id) {
        byte[] pdf = ipdDocumentService.generatePdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ipd-" + id + ".pdf\"")
                .body(pdf);
    }

    @Operation(
            summary = "Download a checklist as PDF",
            description = "Generates an internal PDF with " +
                    "fillable checkboxes from the checklists of the IPD document's ticket (for printing or editing on a tablet). " +
                    "Not intended for the customer."
    )
    @ApiResponse(responseCode = "200", description = "PDF was generated",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "No IPD document or no checklist exists", content = @Content)
    @GetMapping("/{id}/checklist-pdf")
    public ResponseEntity<byte[]> downloadChecklistPdf(
            @Parameter(description = "ID of the IPD document") @PathVariable String id) {
        byte[] pdf = ipdDocumentService.generateChecklistPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"checklist-" + id + ".pdf\"")
                .body(pdf);
    }
}
