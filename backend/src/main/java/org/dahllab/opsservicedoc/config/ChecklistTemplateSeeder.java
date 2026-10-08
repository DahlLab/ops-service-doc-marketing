package org.dahllab.opsservicedoc.config;

import lombok.RequiredArgsConstructor;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChecklistTemplateSeeder implements CommandLineRunner {
    private final ChecklistTemplateRepository checklistTemplateRepository;

    private static final String PHASE_SECURITY = "Security";
    private static final String PHASE_ACCEPTANCE = "Abnahme";
    private static final String PHASE_PLANNING = "Planung";
    private static final String PHASE_OPERATION = "Betrieb";
    private static final String PHASE_NETWORK = "Netzwerk";
    private static final String PHASE_INSTALLATION = "Installation";
    private static final String PHASE_STORAGE = "Storage";
    private static final String PHASE_ACCESS = "Zugang";
    private static final String PHASE_MONITORING = "Monitoring";
    private static final String PHASE_RESOURCES = "Ressourcen";
    private static final String PHASE_PREPARATION = "Vorbereitung";
    private static final String PHASE_CONFIGURATION = "Konfiguration";
    private static final String PHASE_UPDATES = "Updates";
    private static final String PHASE_BACKUP = "Backup";
    private static final String PHASE_DEPLOYMENT = "Deployment";
    private static final String PHASE_FIRMWARE = "Firmware";
    private static final String PHASE_ENDPOINT = "Endgerät";
    private static final String PHASE_EMERGENCY = "Notfall";
    private static final String PHASE_IDENTITY = "Identity";
    private static final String PHASE_PACKAGING = "Paketierung";
    private static final String PHASE_JOB = "Job";
    private static final String PHASE_RESTORE = "Restore";
    private static final String PHASE_INVENTORY = "Inventar";
    private static final String PHASE_MANAGEMENT = "Management";
    private static final String PHASE_TEST = "Test";
    private static final String PHASE_INTEGRATION = "Integration";

    private static final String PHASE_HARDWARE = "Hardware";
    private static final String TEXT_IP_GATEWAY_DNS = "IP, Gateway und DNS im Gast konfiguriert";
    private static final String TEXT_GUEST_OS_UPDATED = "Gast-OS aktualisiert";
    private static final String TEXT_VM_MONITORING = "VM/Gast in Monitoring aufgenommen";
    private static final String TEXT_RESTART_TEST = "Neustart, Netzwerk und Kernfunktionen getestet";
    private static final String TEXT_VM_DOCUMENTATION = "VM-Dokumentation vollständig";

    @Override
    public void run(String... args) {
        if (checklistTemplateRepository.count() > 0) {
            return;
        }

        List<ChecklistTemplate> templates = new ArrayList<>();

        templates.add(template("Server",
                entry(PHASE_PREPARATION, "Servername nach Namenskonvention festlegen"),
                entry(PHASE_PREPARATION, "IP-Adresse, Subnetz, Gateway und DNS festlegen"),
                entry(PHASE_PREPARATION, "Rolle/Zweck des Servers dokumentieren"),
                entry(PHASE_PREPARATION, "Betriebssystem und Edition festlegen"),
                entry(PHASE_INSTALLATION, "Betriebssystem installieren"),
                entry(PHASE_INSTALLATION, "Aktivierung/Lizenzstatus prüfen"),
                entry(PHASE_CONFIGURATION, "Hostname und Netzwerkkonfiguration setzen"),
                entry(PHASE_CONFIGURATION, "Zeitserver/NTP konfigurieren und Zeit prüfen"),
                entry(PHASE_CONFIGURATION, "Domänenbeitritt bzw. Verzeichnisanbindung durchführen", true),
                entry(PHASE_CONFIGURATION, "Benutzer, Gruppen und Berechtigungen konfigurieren", true),
                entry(PHASE_CONFIGURATION, "Freigaben und Dateisystemberechtigungen konfigurieren", true),
                entry(PHASE_SECURITY, "Firewall aktiv und passend konfiguriert"),
                entry(PHASE_SECURITY, "Patch-/Update-Stand vollständig"),
                entry(PHASE_SECURITY, "Hardening durchgeführt (Remote-Zugriff absichern, veraltete Auth-Protokolle deaktivieren, TLS 1.2+, lokalen Administrator absichern)"),
                entry(PHASE_SECURITY, "Lokale Administratorrechte geprüft, Passwortlösung für lokale Admin-Konten aktiv"),
                entry(PHASE_SECURITY, "Zertifikate nur aus vertrauenswürdigen Domänen zugelassen", true),
                entry(PHASE_OPERATION, "Eventlogs/Systemlogs auf Fehler geprüft"),
                entry(PHASE_OPERATION, "Monitoring angebunden und Funktion geprüft"),
                entry(PHASE_OPERATION, "Backup angebunden und erster Lauf erfolgreich"),
                entry(PHASE_OPERATION, "Restore-/Wiederherstellungstest geplant oder durchgeführt"),
                entry(PHASE_OPERATION, "Server in Configuration Management Database (CMDB) erfasst"),
                entry(PHASE_OPERATION, "Server im IP-Adress-/Inventar-Tool erfasst", true),
                entry(PHASE_SECURITY, "Kritisches System ggf. in Datei-Integritäts-/Audit-Überwachung aufgenommen", true),
                entry(PHASE_SECURITY, "Rollenbasierte Remote-Zugriffsgruppen zugewiesen (nach Zuständigkeit)", true),
                entry(PHASE_ACCEPTANCE, "Remotezugriff/Betriebszugriff geprüft", true),
                entry(PHASE_ACCEPTANCE, "Dokumentation vollständig"),
                entry(PHASE_ACCEPTANCE, "Fachlicher Funktionstest erfolgreich"),
                entry(PHASE_ACCEPTANCE, "Übergabe/Abnahme dokumentiert (inkl. Verantwortlicher und Datum)")
        ));

        templates.add(template(PHASE_HARDWARE,
                entry(PHASE_INVENTORY, "Hersteller, Modell und Seriennummer erfassen"),
                entry(PHASE_INVENTORY, "Asset-Tag/Inventarnummer erfassen", true),
                entry("Standort", "Standort, Raum, Rack und Höheneinheit dokumentieren"),
                entry("Strom", "Redundante Netzteile vorhanden und angeschlossen", true),
                entry("Strom", "USV-/PDU-Anbindung geprüft", true),
                entry(PHASE_HARDWARE, "CPU-Konfiguration geprüft"),
                entry(PHASE_HARDWARE, "RAM-Bestückung und erkannte Kapazität geprüft"),
                entry(PHASE_STORAGE, "Datenträgeranzahl, Typ und Kapazität geprüft"),
                entry(PHASE_STORAGE, "RAID-Level/Storage-Konfiguration festgelegt", true),
                entry(PHASE_STORAGE, "RAID/Controller-Status fehlerfrei"),
                entry(PHASE_FIRMWARE, "BIOS/UEFI-Version geprüft/aktualisiert"),
                entry(PHASE_FIRMWARE, "RAID-, NIC- und Controller-Firmware geprüft"),
                entry(PHASE_MANAGEMENT, "Out-of-Band-Management konfiguriert", true),
                entry(PHASE_MANAGEMENT, "Management-IP, DNS und Zugriff dokumentiert", true),
                entry(PHASE_NETWORK, "NICs und physische Verkabelung geprüft"),
                entry(PHASE_NETWORK, "Switchports/VLANs dokumentiert"),
                entry("Diagnose", "Herstellerdiagnose/Hardwaretest erfolgreich"),
                entry(PHASE_INSTALLATION, "Ziel-OS oder Hypervisor installiert"),
                entry(PHASE_OPERATION, "Monitoring/Hardware-Alerts angebunden", true),
                entry(PHASE_OPERATION, "Garantie-/Supportstatus dokumentiert", true),
                entry(PHASE_ACCEPTANCE, "Burn-in/Funktionstest abgeschlossen"),
                entry(PHASE_ACCEPTANCE, "Hardwaredokumentation vollständig")
        ));

        templates.add(template("Netzwerk",
                entry("Übersicht", "Netzwerkskizze mit allen Komponenten vorhanden (Arbeitsplatzrechner, Server, Switches, Router, Drucker, mobile Geräte)"),
                entry("Übersicht", "Bei jedem Gerät dokumentiert: Zuständiger, Standort, gespeicherte Daten", true),
                entry(PHASE_ACCESS, "Netzwerkanschlüsse und -dosen physisch abgesichert bzw. deaktiviert wenn ungenutzt"),
                entry(PHASE_ACCESS, "Standardzugangsdaten auf allen Netzwerkkomponenten geändert"),
                entry(PHASE_ACCESS, "Server-/Fileserver-Standort zusätzlich abgesichert (abschließbarer Raum, regelmäßige Scans)", true),
                entry(PHASE_SECURITY, "Firewall nur auf notwendige Zugriffe beschränkt, Regeln regelmäßig geprüft"),
                entry(PHASE_SECURITY, "Gäste-WLAN vom internen Netzwerk getrennt"),
                entry(PHASE_SECURITY, "WLAN-Verschlüsselung auf stärkstem verfügbaren Standard, SSID-Broadcast deaktiviert"),
                entry(PHASE_SECURITY, "WLAN-Zugang auf bestimmte Gerätekennungen beschränkt (sofern praktikabel)", true),
                entry(PHASE_OPERATION, "Zugriffe auf Server, Router und Switches protokolliert"),
                entry(PHASE_OPERATION, "Ungenutzte Ports deaktiviert", true),
                entry(PHASE_ACCEPTANCE, "IP-Vergabe und Subnetzstruktur dokumentiert")
        ));

        templates.add(template("Security",
                entry("Organisation", "IT-Sicherheitsrichtlinie vorhanden und Mitarbeitende unterwiesen", true),
                entry("Organisation", "Regelmäßige Security-Awareness-Schulungen (z. B. Phishing, Passwortsicherheit)", true),
                entry(PHASE_ACCESS, "Individuelle Benutzerkonten für alle Nutzer, getrennte Admin-/Benutzerkonten"),
                entry(PHASE_ACCESS, "Starke Passwortrichtlinie erzwungen (Mindestlänge, Komplexität)"),
                entry(PHASE_ACCESS, "Zwei-Faktor-Authentifizierung eingesetzt (mind. für kritische Systeme/VPN)"),
                entry(PHASE_ACCESS, "Verfahren zur Deaktivierung von Konten ausgeschiedener Mitarbeitender vorhanden"),
                entry(PHASE_ENDPOINT, "Antivirus-/Endpoint-Schutz auf allen Geräten aktiv und aktuell"),
                entry(PHASE_ENDPOINT, "Patch-Management für Betriebssystem und Anwendungen eingerichtet"),
                entry(PHASE_ENDPOINT, "Festplattenverschlüsselung auf mobilen Geräten aktiv"),
                entry(PHASE_ENDPOINT, "Externe Geräte/USB-Anschlüsse kontrolliert oder eingeschränkt", true),
                entry("Remote", "Fernzugriff nur über abgesicherte VPN-Verbindung"),
                entry("Kommunikation", "Spam-/Virenfilter für E-Mail aktiv, verdächtige Anhänge werden markiert/blockiert", true),
                entry("Kommunikation", "Meldeweg für Phishing-Versuche vorhanden", true),
                entry("Cloud", "Cloud-Dienste datenschutzkonform genutzt, Verträge mit Auftragsverarbeitern vorhanden", true),
                entry(PHASE_MONITORING, "Sicherheitsrelevante Ereignisse zentral protokolliert (Log-Management/SIEM)", true),
                entry(PHASE_EMERGENCY, "Notfallplan für Sicherheitsvorfälle vorhanden, Zuständigkeiten geklärt", true),
                entry(PHASE_EMERGENCY, "Notfallmanagement regelmäßig getestet", true),
                entry("Audit", "Regelmäßige interne/externe Audits durchgeführt, Ergebnisse dokumentiert", true)
        ));

        templates.add(template("Client",
                entry(PHASE_INVENTORY, "Gerätename und Benutzerzuordnung erfasst"),
                entry(PHASE_INSTALLATION, "Betriebssystem installiert und aktiviert"),
                entry(PHASE_UPDATES, "Betriebssystem vollständig aktualisiert"),
                entry(PHASE_UPDATES, "Automatisches Patchmanagement aktiviert", true),
                entry(PHASE_SECURITY, "Client-Firewall aktiv"),
                entry(PHASE_SECURITY, "Virenschutz/Endpoint Protection aktiv und aktuell"),
                entry(PHASE_SECURITY, "Festplattenverschlüsselung aktiviert", true),
                entry(PHASE_SECURITY, "Bildschirmsperre konfiguriert"),
                entry(PHASE_IDENTITY, "Benutzerkonto eingerichtet"),
                entry(PHASE_IDENTITY, "2FA eingerichtet, falls erforderlich", true),
                entry(PHASE_NETWORK, "LAN/WLAN und DNS funktionieren"),
                entry("Remote", "VPN eingerichtet, falls erforderlich", true),
                entry("Daten", "Netzlaufwerke/Dateiablagen erreichbar", true),
                entry("Software", "Standardsoftware installiert"),
                entry("Software", "Individuelle Anwendungen installiert und getestet", true),
                entry("Peripherie", "Drucker/Scanner/sonstige Geräte geprüft", true),
                entry(PHASE_MANAGEMENT, "Client in Management-/Inventarsystem aufgenommen", true),
                entry(PHASE_BACKUP, "Benutzerdaten-/Profilstrategie geprüft", true),
                entry(PHASE_ACCEPTANCE, "Benutzeranmeldung und Funktionstest erfolgreich"),
                entry(PHASE_ACCEPTANCE, "Übergabe dokumentiert")
        ));

        templates.add(template("Software-Deployment",
                entry(PHASE_PLANNING, "Softwareprodukt, Version und Zielgruppe festgelegt"),
                entry(PHASE_PLANNING, "Systemvoraussetzungen geprüft"),
                entry(PHASE_PLANNING, "Lizenz-/Nutzungsbedingungen geklärt"),
                entry(PHASE_PLANNING, "Abhängigkeiten/Runtime/DB-Anforderungen dokumentiert", true),
                entry(PHASE_PLANNING, "Pilot-/Testgruppe definiert", true),
                entry(PHASE_PACKAGING, "Installationsmedium/Paket geprüft"),
                entry(PHASE_PACKAGING, "Silent-/unattended Installation verfügbar bzw. getestet", true),
                entry(PHASE_PACKAGING, "Installationsparameter dokumentiert"),
                entry(PHASE_PACKAGING, "Konfigurationsdateien/Policies vorbereitet", true),
                entry(PHASE_SECURITY, "Quelle/Signatur/Hash des Installationspakets geprüft", true),
                entry(PHASE_DEPLOYMENT, "Deployment-Methode festgelegt"),
                entry(PHASE_DEPLOYMENT, "Zielgeräte/-systeme ausgewählt"),
                entry(PHASE_DEPLOYMENT, "Wartungsfenster/Termin geplant", true),
                entry(PHASE_DEPLOYMENT, "Backup/Snapshot vor Änderung geprüft", true),
                entry(PHASE_DEPLOYMENT, "Installation erfolgreich"),
                entry(PHASE_TEST, "Dienst/Anwendung startet fehlerfrei"),
                entry(PHASE_TEST, "Kernfunktionen getestet"),
                entry(PHASE_TEST, "Berechtigungen und Zugriff geprüft"),
                entry(PHASE_MONITORING, "Logs/Monitoring nach Deployment geprüft", true),
                entry("Rollback", "Rollback-/Deinstallationsweg dokumentiert"),
                entry(PHASE_ACCEPTANCE, "Pilot-/Fachtest erfolgreich", true),
                entry(PHASE_ACCEPTANCE, "Deployment dokumentiert und abgeschlossen")
        ));

        templates.add(template("VMware",
                entry(PHASE_PREPARATION, "IPD erstellen und Downtime mit Kunde abstimmen"),
                entry(PHASE_PLANNING, "Ziel-vCenter/Datacenter/Cluster/Host festgelegt"),
                entry(PHASE_PLANNING, "VM-Name nach Namenskonvention festgelegt"),
                entry(PHASE_PLANNING, "Gastbetriebssystem und Version festgelegt"),
                entry(PHASE_RESOURCES, "vCPU-Anzahl festgelegt"),
                entry(PHASE_RESOURCES, "RAM-Größe festgelegt"),
                entry(PHASE_STORAGE, "Datastore ausgewählt"),
                entry(PHASE_STORAGE, "Virtuelle Disk(s) und Größe festgelegt"),
                entry(PHASE_STORAGE, "Provisioning-Art festgelegt", true),
                entry(PHASE_NETWORK, "Portgruppe/Netzwerk ausgewählt"),
                entry(PHASE_NETWORK, "VLAN/Zielsegment geprüft", true),
                entry(PHASE_FIRMWARE, "Firmware/UEFI und Secure Boot geprüft", true),
                entry(PHASE_SECURITY, "vTPM/VBS bei Bedarf konfiguriert", true),
                entry(PHASE_INSTALLATION, "ISO oder Template ausgewählt"),
                entry(PHASE_INSTALLATION, "Gastbetriebssystem installiert bzw. Patch eingespielt"),
                entry(PHASE_INTEGRATION, "VMware Tools/Open VM Tools installiert und aktuell"),
                entry(PHASE_NETWORK, TEXT_IP_GATEWAY_DNS),
                entry(PHASE_IDENTITY, "Domänenbeitritt/Verzeichnisanbindung durchgeführt", true),
                entry(PHASE_UPDATES, TEXT_GUEST_OS_UPDATED),
                entry(PHASE_BACKUP, "VM in Backup aufgenommen bzw. aktuelles Backup vor Wartung vorhanden"),
                entry(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                entry(PHASE_OPERATION, "Snapshot vor Wartung erstellt, nach erfolgreichem Test wieder gelöscht", true),
                entry(PHASE_SECURITY, "VM-/vCenter-Berechtigungen geprüft"),
                entry(PHASE_ACCEPTANCE, TEXT_RESTART_TEST),
                entry(PHASE_ACCEPTANCE, TEXT_VM_DOCUMENTATION),
                entry(PHASE_ACCEPTANCE, "Planned Activity Ticket dokumentiert")
        ));

        templates.add(template("Proxmox",
                entry(PHASE_PLANNING, "Ziel-Node/Cluster festgelegt"),
                entry(PHASE_PLANNING, "VM-ID und Name festgelegt"),
                entry(PHASE_PLANNING, "Gastbetriebssystem/OS-Typ ausgewählt"),
                entry(PHASE_RESOURCES, "CPU/Sockets/Cores festgelegt"),
                entry(PHASE_RESOURCES, "RAM/Ballooning festgelegt"),
                entry(PHASE_STORAGE, "Storage-Ziel ausgewählt"),
                entry(PHASE_STORAGE, "Disk-Größe und Bus/Controller festgelegt"),
                entry(PHASE_NETWORK, "Bridge ausgewählt"),
                entry(PHASE_NETWORK, "VLAN-Tag/Segment geprüft", true),
                entry(PHASE_FIRMWARE, "BIOS/UEFI/Machine Type festgelegt", true),
                entry(PHASE_SECURITY, "Secure Boot/TPM bei Bedarf konfiguriert", true),
                entry(PHASE_INSTALLATION, "ISO oder Template ausgewählt"),
                entry(PHASE_INSTALLATION, "Gastbetriebssystem installiert"),
                entry(PHASE_INTEGRATION, "QEMU Guest Agent installiert und aktiviert"),
                entry(PHASE_NETWORK, TEXT_IP_GATEWAY_DNS),
                entry(PHASE_SECURITY, "Proxmox Firewall/VM Firewall geprüft", true),
                entry(PHASE_UPDATES, TEXT_GUEST_OS_UPDATED),
                entry(PHASE_BACKUP, "Backup-Job/Ziel für VM eingerichtet"),
                entry(PHASE_BACKUP, "Erster Backup-Lauf erfolgreich"),
                entry(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                entry(PHASE_SECURITY, "Rollen/Berechtigungen geprüft"),
                entry(PHASE_ACCEPTANCE, TEXT_RESTART_TEST),
                entry(PHASE_ACCEPTANCE, TEXT_VM_DOCUMENTATION)
        ));

        templates.add(template("Hyper-V",
                entry(PHASE_PLANNING, "Ziel-Host/Cluster festgelegt"),
                entry(PHASE_PLANNING, "VM-Name und Speicherort festgelegt"),
                entry(PHASE_PLANNING, "Gastbetriebssystem und Kompatibilität geprüft"),
                entry(PHASE_PLANNING, "VM-Generation gewählt; Gen 2 bevorzugt wenn unterstützt"),
                entry(PHASE_RESOURCES, "vCPU-Anzahl festgelegt"),
                entry(PHASE_RESOURCES, "RAM/Dynamic Memory festgelegt"),
                entry(PHASE_STORAGE, "VHDX-Speicherort und Größe festgelegt"),
                entry(PHASE_NETWORK, "Virtuellen Switch ausgewählt"),
                entry(PHASE_NETWORK, "VLAN/Netzwerksegment geprüft", true),
                entry(PHASE_SECURITY, "Secure Boot geprüft/aktiviert wenn unterstützt", true),
                entry(PHASE_SECURITY, "vTPM/Verschlüsselung bei Bedarf konfiguriert", true),
                entry(PHASE_INSTALLATION, "ISO/VHDX/Deploymentquelle ausgewählt"),
                entry(PHASE_INSTALLATION, "Gastbetriebssystem installiert"),
                entry(PHASE_INTEGRATION, "Hyper-V Integration Services aktiv/aktuell"),
                entry(PHASE_NETWORK, TEXT_IP_GATEWAY_DNS),
                entry(PHASE_IDENTITY, "Domänenbeitritt/Verzeichnisanbindung durchgeführt", true),
                entry(PHASE_UPDATES, TEXT_GUEST_OS_UPDATED),
                entry(PHASE_BACKUP, "VM in Backup aufgenommen"),
                entry(PHASE_OPERATION, "Checkpoints geprüft und Bereinigungsstrategie festgelegt", true),
                entry(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                entry(PHASE_SECURITY, "Host-/VM-Berechtigungen geprüft"),
                entry(PHASE_ACCEPTANCE, TEXT_RESTART_TEST),
                entry(PHASE_ACCEPTANCE, TEXT_VM_DOCUMENTATION)
        ));

        templates.add(template("Backup & Restore",
                entry(PHASE_PLANNING, "Zu sichernde Systeme/Daten festgelegt"),
                entry(PHASE_PLANNING, "RPO festgelegt"),
                entry(PHASE_PLANNING, "RTO festgelegt"),
                entry(PHASE_PLANNING, "Backup-Methode/Typ festgelegt"),
                entry("Ziel", "Backup-Ziel und Kapazität geprüft"),
                entry("Ziel", "Backup-Kopie getrennt vom Produktionsnetz vorhanden"),
                entry(PHASE_SECURITY, "Backup-Zugriffe/Berechtigungen eingeschränkt"),
                entry(PHASE_SECURITY, "Verschlüsselung der Backups geprüft", true),
                entry(PHASE_JOB, "Backup-Job eingerichtet"),
                entry(PHASE_JOB, "Zeitplan und Aufbewahrung konfiguriert"),
                entry(PHASE_JOB, "Applikationskonsistenz/VSS o. ä. geprüft", true),
                entry(PHASE_JOB, "Erster vollständiger Lauf erfolgreich"),
                entry(PHASE_MONITORING, "Fehlerbenachrichtigung/Monitoring eingerichtet"),
                entry(PHASE_RESTORE, "Restore-Verfahren dokumentiert"),
                entry(PHASE_RESTORE, "Datei-/Objekt-Restore getestet"),
                entry(PHASE_RESTORE, "System-/VM-Restore getestet oder geplant", true),
                entry(PHASE_RESTORE, "Restore-Ergebnis fachlich geprüft"),
                entry(PHASE_EMERGENCY, "Offsite/Offline/immutable Kopie geprüft", true),
                entry(PHASE_EMERGENCY, "Notfallkontakte und Eskalation dokumentiert", true),
                entry(PHASE_ACCEPTANCE, "Backup- und Restore-Dokumentation vollständig")
        ));

        checklistTemplateRepository.saveAll(templates);
    }

    private ChecklistTemplate template(String name, String... entries) {
        return new ChecklistTemplate(null, name, List.of(entries), true);
    }

    private String entry(String phase, String text) {
        return entry(phase, text, false);
    }

    private String entry(String phase, String text, boolean optional) {
        String basis = "[" + phase + "] " + text;
        return optional ? basis + " (optional)" : basis;
    }
}
