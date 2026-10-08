package org.dahllab.opsservicedoc.config;

import lombok.RequiredArgsConstructor;
import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;
import org.dahllab.opsservicedoc.model.IpdDocument;
import org.dahllab.opsservicedoc.model.IpdDocumentStatus;
import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Task;
import org.dahllab.opsservicedoc.model.TaskStatus;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.repository.ChecklistRepository;
import org.dahllab.opsservicedoc.repository.IpdDocumentRepository;
import org.dahllab.opsservicedoc.repository.TaskRepository;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {
    private static final String TECHNICIAN_SCOTT = "M. Scott";
    private static final String TECHNICIAN_UHURA = "N. Uhura";
    private static final String TECHNICIAN_SULU = "H. Sulu";

    private final TicketRepository ticketRepository;
    private final TaskRepository taskRepository;
    private final ChecklistRepository checklistRepository;
    private final IpdDocumentRepository ipdDocumentRepository;

    @Override
    public void run(String... args) {
        if (ticketRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault()).withNano(0);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Ticket patching = ticketRepository.save(ticket("GLPI-2041", "Quarterly patching of the vSphere cluster",
                "Apply the latest ESXi and vCenter patches to the production cluster outside business hours.",
                TicketStatus.IN_PROGRESS, TECHNICIAN_SCOTT, now.minusDays(9)));
        Ticket backup = ticketRepository.save(ticket("GLPI-2042", "Backup check for the file servers",
                "Weekly verification of the backup jobs and a test restore of a sample share.",
                TicketStatus.NEW, TECHNICIAN_UHURA, now.minusDays(2)));
        Ticket migration = ticketRepository.save(ticket("GLPI-2043", "Migrate the accounting VM to new storage",
                "Move the accounting VM to the new all-flash datastore with a short, announced downtime.",
                TicketStatus.PENDING, TECHNICIAN_SULU, now.minusDays(14)));
        Ticket firmware = ticketRepository.save(ticket("GLPI-2044", "Firmware update for the core switches",
                "Update both core switches one after the other to keep the network available.",
                TicketStatus.SOLVED, TECHNICIAN_SCOTT, now.minusDays(30)));
        Ticket certificates = ticketRepository.save(ticket("GLPI-2045", "Renew expiring TLS certificates",
                "Five internal service certificates expire within the next 30 days.",
                TicketStatus.NEW, TECHNICIAN_UHURA, now.minusDays(1)));
        ticketRepository.save(ticket("GLPI-2046", "Decommission legacy monitoring server",
                "Shut down the old monitoring host after the new platform has been running for a month.",
                TicketStatus.CLOSED, TECHNICIAN_SULU, now.minusDays(45)));

        taskRepository.saveAll(List.of(
                task(patching, "Snapshot all production VMs", "Create snapshots before the maintenance window starts.",
                        now.minusDays(8), today.minusDays(3), null, TaskStatus.OPEN),
                task(patching, "Patch the first ESXi host", "Put the host into maintenance mode, patch, reboot, verify.",
                        now.minusDays(7), today.plusDays(2), null, TaskStatus.IN_PROGRESS),
                task(patching, "Inform stakeholders", "Send the maintenance announcement to all department leads.",
                        now.minusDays(9), today.minusDays(7), now.minusDays(6), TaskStatus.DONE),
                task(backup, "Review last week's backup reports", "Check failed and skipped jobs and note the reasons.",
                        now.minusDays(2), today.plusDays(1), null, TaskStatus.OPEN),
                task(migration, "Agree on the downtime window", "Coordinate a Saturday window with the accounting team.",
                        now.minusDays(13), today.plusDays(5), null, TaskStatus.IN_PROGRESS),
                task(certificates, "Create certificate signing requests", "One CSR per service, with the correct subject alternative names.",
                        now.minusDays(1), today.plusDays(7), null, TaskStatus.OPEN),
                task(firmware, "Verify spanning tree after the update", "Confirm there are no topology changes on any VLAN.",
                        now.minusDays(29), today.minusDays(25), now.minusDays(27), TaskStatus.DONE)
        ));

        checklistRepository.saveAll(List.of(
                checklist(patching, "Server maintenance: vSphere cluster", now.minusDays(8), null,
                        item("[Preparation] Define the maintenance window with the customer", true),
                        item("[Preparation] Create snapshots of all critical VMs", true),
                        item("[Security] Patch level complete on all hosts", false),
                        item("[Operation] Monitoring checked after the update", false),
                        item("[Acceptance] Documentation complete", false)),
                checklist(backup, "Backup & restore verification", now.minusDays(2), null,
                        item("[Planning] Systems and data to verify defined", true),
                        item("[Job] First full run successful", false),
                        item("[Restore] File restore tested", false),
                        item("[Restore] Restore result checked by the owner", false)),
                checklist(firmware, "Network: core switch update", now.minusDays(29), now.minusDays(26),
                        item("[Overview] Network diagram up to date", true),
                        item("[Firmware] Firmware version verified", true),
                        item("[Operation] Unused ports deactivated (optional)", true),
                        item("[Acceptance] IP assignment and subnet structure documented", true))
        ));

        ipdDocumentRepository.saveAll(List.of(
                new IpdDocument(null, patching.getId(), IpdDocumentStatus.DRAFT,
                        patching.getTitle(), TECHNICIAN_SCOTT, ScenarioType.SERVER_MAINTENANCE,
                        "Brightwater Logistics Ltd.", "Dana Whitfield, Head of IT",
                        "Saturday 20:00 - Sunday 02:00",
                        "The production cluster runs on ESXi versions that are two patch levels behind.",
                        "No unplanned downtime; all services available again by Sunday 02:00.",
                        "Three ESXi hosts, one vCenter appliance, shared all-flash storage.",
                        "42 virtual machines, 6 of them business critical.",
                        "Two redundant top-of-rack switches, management VLAN 10, storage VLAN 20.",
                        "Technician: M. Scott. Customer contact: D. Whitfield. Escalation: IT management.",
                        "Nightly image backups; additional snapshots before the maintenance window.",
                        "Hosts are patched one by one; administrative access stays restricted to the jump host.",
                        "Snapshots created, stakeholders informed.",
                        "Patching is done host by host so that vMotion can keep the services running.",
                        "Vendor patches may require a longer reboot than estimated.",
                        "Revert to the snapshots and boot the previous ESXi image.",
                        false, now.minusDays(8), now.minusDays(1)),
                new IpdDocument(null, firmware.getId(), IpdDocumentStatus.COMPLETED,
                        firmware.getTitle(), TECHNICIAN_SCOTT, ScenarioType.SERVER_MAINTENANCE,
                        "Alderon Medical Group", "Priya Raman, Network Lead",
                        "Sunday 01:00 - 04:00",
                        "Both core switches ran a firmware version with known stability issues.",
                        "Update without interrupting patient-facing systems.",
                        "Two core switches in a stacked configuration, 48 access ports each.",
                        "No virtual machines affected; only network infrastructure.",
                        "Redundant uplinks to the distribution layer; spanning tree verified.",
                        "Technician: M. Scott. Customer approval: P. Raman.",
                        "Configuration exported and stored before the update.",
                        "Firmware image checksum verified before installation.",
                        "Updated switch A, verified traffic, then updated switch B.",
                        "The second switch was updated only after the first one was stable for 30 minutes.",
                        "A failing stack member could cause a short loss of connectivity.",
                        "Boot the previous firmware image from the secondary flash partition.",
                        true, now.minusDays(29), now.minusDays(26))
        ));

    }

    private Ticket ticket(String glpiId, String title, String description, TicketStatus status,
                          String technician, LocalDateTime createdAt) {
        return new Ticket(null, glpiId, title, description, status, technician,
                ScenarioType.SERVER_MAINTENANCE, createdAt);
    }

    private Task task(Ticket ticket, String topic, String nextSteps, LocalDateTime recordedAt,
                      LocalDate dueDate, LocalDateTime doneAt, TaskStatus status) {
        return new Task(null, ticket.getId(), topic, nextSteps, recordedAt, dueDate, doneAt, status);
    }

    private Checklist checklist(Ticket ticket, String title, LocalDateTime createdAt,
                                LocalDateTime completedAt, ChecklistItem... items) {
        return new Checklist(null, ticket.getId(), title, List.of(items), createdAt, completedAt);
    }

    private ChecklistItem item(String description, boolean done) {
        return new ChecklistItem(UUID.randomUUID().toString(), description, done);
    }
}
