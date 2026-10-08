package org.dahllab.opsservicedoc.config;

import lombok.RequiredArgsConstructor;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Dieser CommandLineRunner läuft einmal automatisch beim Start der
// Anwendung (Spring Boot ruft run() auf, nachdem der ApplicationContext
// fertig aufgebaut ist) und legt meine zehn Standard-Checklisten-Vorlagen
// in MongoDB an, falls dort noch keine Vorlagen existieren. So muss ich
// die Vorlagen (Server, Netzwerk, Security, usw.) nicht jedes Mal von
// Hand im Frontend eintippen, sondern sie stehen im "Aus Vorlage"-
// Dropdown auf der Checklisten-Seite sofort zur Auswahl bereit.
//
// Die Inhalte stammen aus meiner eigenen Excel-Datei (OpsDoc_Checklisten.xlsx)
// mit den zehn Arbeitsblättern Server, Hardware, Netzwerk, Security,
// Client, Software_Deployment, VMware, Proxmox, Hyper-V und
// Backup_Restore - das ist die aktuell gepflegte Version (vier Blätter
// davon sind dort als "Überarbeitet" markiert, der Rest als "Original").
//
// Wichtiger Hinweis zum Datenmodell: ChecklistTemplate speichert pro
// Punkt nur einen einzigen String (itemBeschreibungen: List<String>) -
// es gibt kein separates Feld für "Phase" (z.B. "Vorbereitung",
// "Installation", "Security") oder für "Pflicht/Optional" aus meiner
// Excel-Tabelle. Damit diese Information trotzdem nicht verloren geht,
// codiere ich sie direkt mit in den Text hinein:
//   - Die Phase steht in eckigen Klammern vorangestellt, z.B.
//     "[Konfiguration] Domänenbeitritt ... durchführen"
//   - Ist ein Punkt in der Excel-Tabelle als "Optional" markiert (statt
//     "Ja" = Pflicht), hänge ich " (optional)" ans Ende an.
// Das baue ich unten mit der kleinen Hilfsmethode punkt(...) zusammen,
// damit ich beim eigentlichen Anlegen der Vorlagen nur noch Phase, Text
// und ggf. "optional" angeben muss, statt jedes Mal den fertigen String
// von Hand zu verketten.
@Component
@RequiredArgsConstructor
public class ChecklistTemplateSeeder implements CommandLineRunner {

    private final ChecklistTemplateRepository checklistTemplateRepository;

    // Die Phasen-Namen kommen sehr oft vor (in fast jeder Vorlage). Als Konstanten
    // schreibe ich jeden Text nur einmal und vermeide Tippfehler und Kopien.
    private static final String PHASE_SECURITY = "Security";
    private static final String PHASE_ABNAHME = "Abnahme";
    private static final String PHASE_PLANUNG = "Planung";
    private static final String PHASE_BETRIEB = "Betrieb";
    private static final String PHASE_NETZWERK = "Netzwerk";
    private static final String PHASE_INSTALLATION = "Installation";
    private static final String PHASE_STORAGE = "Storage";
    private static final String PHASE_ZUGANG = "Zugang";
    private static final String PHASE_MONITORING = "Monitoring";
    private static final String PHASE_RESSOURCEN = "Ressourcen";
    private static final String PHASE_VORBEREITUNG = "Vorbereitung";
    private static final String PHASE_KONFIGURATION = "Konfiguration";
    private static final String PHASE_UPDATES = "Updates";
    private static final String PHASE_BACKUP = "Backup";
    private static final String PHASE_DEPLOYMENT = "Deployment";
    private static final String PHASE_FIRMWARE = "Firmware";
    private static final String PHASE_ENDGERAET = "Endgerät";
    private static final String PHASE_NOTFALL = "Notfall";
    private static final String PHASE_IDENTITY = "Identity";
    private static final String PHASE_PAKETIERUNG = "Paketierung";
    private static final String PHASE_JOB = "Job";
    private static final String PHASE_RESTORE = "Restore";
    private static final String PHASE_INVENTAR = "Inventar";
    private static final String PHASE_MANAGEMENT = "Management";
    private static final String PHASE_TEST = "Test";
    private static final String PHASE_INTEGRATION = "Integration";
    // "Hardware" ist Phase UND Name einer Vorlage, die VM-Vorlagen teilen sich ausserdem
    // ein paar identische Punkt-Texte - auch diese stehen deshalb nur einmal als Konstante hier.
    private static final String PHASE_HARDWARE = "Hardware";
    private static final String TEXT_IP_GATEWAY_DNS = "IP, Gateway und DNS im Gast konfiguriert";
    private static final String TEXT_GAST_OS_AKTUALISIERT = "Gast-OS aktualisiert";
    private static final String TEXT_VM_MONITORING = "VM/Gast in Monitoring aufgenommen";
    private static final String TEXT_NEUSTART_TEST = "Neustart, Netzwerk und Kernfunktionen getestet";
    private static final String TEXT_VM_DOKUMENTATION = "VM-Dokumentation vollständig";

    @Override
    public void run(String... args) {
        // Guard gegen doppeltes Einfügen: Ohne diese Prüfung würde bei
        // jedem Neustart der Anwendung ein weiterer Satz der zehn
        // Vorlagen angelegt werden. Da ich hier nur beim allerersten
        // Start (leere Collection) etwas tun will, reicht eine einfache
        // count()-Prüfung - für eigene, zusätzlich von Hand angelegte
        // Vorlagen greift das nicht mehr ein, sobald schon mindestens
        // eine Vorlage existiert.
        if (checklistTemplateRepository.count() > 0) {
            return;
        }

        List<ChecklistTemplate> vorlagen = new ArrayList<>();

        vorlagen.add(vorlage("Server",
                punkt(PHASE_VORBEREITUNG, "Servername nach Namenskonvention festlegen"),
                punkt(PHASE_VORBEREITUNG, "IP-Adresse, Subnetz, Gateway und DNS festlegen"),
                punkt(PHASE_VORBEREITUNG, "Rolle/Zweck des Servers dokumentieren"),
                punkt(PHASE_VORBEREITUNG, "Betriebssystem und Edition festlegen"),
                punkt(PHASE_INSTALLATION, "Betriebssystem installieren"),
                punkt(PHASE_INSTALLATION, "Aktivierung/Lizenzstatus prüfen"),
                punkt(PHASE_KONFIGURATION, "Hostname und Netzwerkkonfiguration setzen"),
                punkt(PHASE_KONFIGURATION, "Zeitserver/NTP konfigurieren und Zeit prüfen"),
                punkt(PHASE_KONFIGURATION, "Domänenbeitritt bzw. Verzeichnisanbindung durchführen", true),
                punkt(PHASE_KONFIGURATION, "Benutzer, Gruppen und Berechtigungen konfigurieren", true),
                punkt(PHASE_KONFIGURATION, "Freigaben und Dateisystemberechtigungen konfigurieren", true),
                punkt(PHASE_SECURITY, "Firewall aktiv und passend konfiguriert"),
                punkt(PHASE_SECURITY, "Patch-/Update-Stand vollständig"),
                punkt(PHASE_SECURITY, "Hardening durchgeführt (Remote-Zugriff absichern, veraltete Auth-Protokolle deaktivieren, TLS 1.2+, lokalen Administrator absichern)"),
                punkt(PHASE_SECURITY, "Lokale Administratorrechte geprüft, Passwortlösung für lokale Admin-Konten aktiv"),
                punkt(PHASE_SECURITY, "Zertifikate nur aus vertrauenswürdigen Domänen zugelassen", true),
                punkt(PHASE_BETRIEB, "Eventlogs/Systemlogs auf Fehler geprüft"),
                punkt(PHASE_BETRIEB, "Monitoring angebunden und Funktion geprüft"),
                punkt(PHASE_BETRIEB, "Backup angebunden und erster Lauf erfolgreich"),
                punkt(PHASE_BETRIEB, "Restore-/Wiederherstellungstest geplant oder durchgeführt"),
                punkt(PHASE_BETRIEB, "Server in Configuration Management Database (CMDB) erfasst"),
                punkt(PHASE_BETRIEB, "Server im IP-Adress-/Inventar-Tool erfasst", true),
                punkt(PHASE_SECURITY, "Kritisches System ggf. in Datei-Integritäts-/Audit-Überwachung aufgenommen", true),
                punkt(PHASE_SECURITY, "Rollenbasierte Remote-Zugriffsgruppen zugewiesen (nach Zuständigkeit)", true),
                punkt(PHASE_ABNAHME, "Remotezugriff/Betriebszugriff geprüft", true),
                punkt(PHASE_ABNAHME, "Dokumentation vollständig"),
                punkt(PHASE_ABNAHME, "Fachlicher Funktionstest erfolgreich"),
                punkt(PHASE_ABNAHME, "Übergabe/Abnahme dokumentiert (inkl. Verantwortlicher und Datum)")
        ));

        vorlagen.add(vorlage(PHASE_HARDWARE,
                punkt(PHASE_INVENTAR, "Hersteller, Modell und Seriennummer erfassen"),
                punkt(PHASE_INVENTAR, "Asset-Tag/Inventarnummer erfassen", true),
                punkt("Standort", "Standort, Raum, Rack und Höheneinheit dokumentieren"),
                punkt("Strom", "Redundante Netzteile vorhanden und angeschlossen", true),
                punkt("Strom", "USV-/PDU-Anbindung geprüft", true),
                punkt(PHASE_HARDWARE, "CPU-Konfiguration geprüft"),
                punkt(PHASE_HARDWARE, "RAM-Bestückung und erkannte Kapazität geprüft"),
                punkt(PHASE_STORAGE, "Datenträgeranzahl, Typ und Kapazität geprüft"),
                punkt(PHASE_STORAGE, "RAID-Level/Storage-Konfiguration festgelegt", true),
                punkt(PHASE_STORAGE, "RAID/Controller-Status fehlerfrei"),
                punkt(PHASE_FIRMWARE, "BIOS/UEFI-Version geprüft/aktualisiert"),
                punkt(PHASE_FIRMWARE, "RAID-, NIC- und Controller-Firmware geprüft"),
                punkt(PHASE_MANAGEMENT, "Out-of-Band-Management konfiguriert", true),
                punkt(PHASE_MANAGEMENT, "Management-IP, DNS und Zugriff dokumentiert", true),
                punkt(PHASE_NETZWERK, "NICs und physische Verkabelung geprüft"),
                punkt(PHASE_NETZWERK, "Switchports/VLANs dokumentiert"),
                punkt("Diagnose", "Herstellerdiagnose/Hardwaretest erfolgreich"),
                punkt(PHASE_INSTALLATION, "Ziel-OS oder Hypervisor installiert"),
                punkt(PHASE_BETRIEB, "Monitoring/Hardware-Alerts angebunden", true),
                punkt(PHASE_BETRIEB, "Garantie-/Supportstatus dokumentiert", true),
                punkt(PHASE_ABNAHME, "Burn-in/Funktionstest abgeschlossen"),
                punkt(PHASE_ABNAHME, "Hardwaredokumentation vollständig")
        ));

        vorlagen.add(vorlage("Netzwerk",
                punkt("Übersicht", "Netzwerkskizze mit allen Komponenten vorhanden (Arbeitsplatzrechner, Server, Switches, Router, Drucker, mobile Geräte)"),
                punkt("Übersicht", "Bei jedem Gerät dokumentiert: Zuständiger, Standort, gespeicherte Daten", true),
                punkt(PHASE_ZUGANG, "Netzwerkanschlüsse und -dosen physisch abgesichert bzw. deaktiviert wenn ungenutzt"),
                punkt(PHASE_ZUGANG, "Standardzugangsdaten auf allen Netzwerkkomponenten geändert"),
                punkt(PHASE_ZUGANG, "Server-/Fileserver-Standort zusätzlich abgesichert (abschließbarer Raum, regelmäßige Scans)", true),
                punkt(PHASE_SECURITY, "Firewall nur auf notwendige Zugriffe beschränkt, Regeln regelmäßig geprüft"),
                punkt(PHASE_SECURITY, "Gäste-WLAN vom internen Netzwerk getrennt"),
                punkt(PHASE_SECURITY, "WLAN-Verschlüsselung auf stärkstem verfügbaren Standard, SSID-Broadcast deaktiviert"),
                punkt(PHASE_SECURITY, "WLAN-Zugang auf bestimmte Gerätekennungen beschränkt (sofern praktikabel)", true),
                punkt(PHASE_BETRIEB, "Zugriffe auf Server, Router und Switches protokolliert"),
                punkt(PHASE_BETRIEB, "Ungenutzte Ports deaktiviert", true),
                punkt(PHASE_ABNAHME, "IP-Vergabe und Subnetzstruktur dokumentiert")
        ));

        vorlagen.add(vorlage("Security",
                punkt("Organisation", "IT-Sicherheitsrichtlinie vorhanden und Mitarbeitende unterwiesen", true),
                punkt("Organisation", "Regelmäßige Security-Awareness-Schulungen (z. B. Phishing, Passwortsicherheit)", true),
                punkt(PHASE_ZUGANG, "Individuelle Benutzerkonten für alle Nutzer, getrennte Admin-/Benutzerkonten"),
                punkt(PHASE_ZUGANG, "Starke Passwortrichtlinie erzwungen (Mindestlänge, Komplexität)"),
                punkt(PHASE_ZUGANG, "Zwei-Faktor-Authentifizierung eingesetzt (mind. für kritische Systeme/VPN)"),
                punkt(PHASE_ZUGANG, "Verfahren zur Deaktivierung von Konten ausgeschiedener Mitarbeitender vorhanden"),
                punkt(PHASE_ENDGERAET, "Antivirus-/Endpoint-Schutz auf allen Geräten aktiv und aktuell"),
                punkt(PHASE_ENDGERAET, "Patch-Management für Betriebssystem und Anwendungen eingerichtet"),
                punkt(PHASE_ENDGERAET, "Festplattenverschlüsselung auf mobilen Geräten aktiv"),
                punkt(PHASE_ENDGERAET, "Externe Geräte/USB-Anschlüsse kontrolliert oder eingeschränkt", true),
                punkt("Remote", "Fernzugriff nur über abgesicherte VPN-Verbindung"),
                punkt("Kommunikation", "Spam-/Virenfilter für E-Mail aktiv, verdächtige Anhänge werden markiert/blockiert", true),
                punkt("Kommunikation", "Meldeweg für Phishing-Versuche vorhanden", true),
                punkt("Cloud", "Cloud-Dienste datenschutzkonform genutzt, Verträge mit Auftragsverarbeitern vorhanden", true),
                punkt(PHASE_MONITORING, "Sicherheitsrelevante Ereignisse zentral protokolliert (Log-Management/SIEM)", true),
                punkt(PHASE_NOTFALL, "Notfallplan für Sicherheitsvorfälle vorhanden, Zuständigkeiten geklärt", true),
                punkt(PHASE_NOTFALL, "Notfallmanagement regelmäßig getestet", true),
                punkt("Audit", "Regelmäßige interne/externe Audits durchgeführt, Ergebnisse dokumentiert", true)
        ));

        vorlagen.add(vorlage("Client",
                punkt(PHASE_INVENTAR, "Gerätename und Benutzerzuordnung erfasst"),
                punkt(PHASE_INSTALLATION, "Betriebssystem installiert und aktiviert"),
                punkt(PHASE_UPDATES, "Betriebssystem vollständig aktualisiert"),
                punkt(PHASE_UPDATES, "Automatisches Patchmanagement aktiviert", true),
                punkt(PHASE_SECURITY, "Client-Firewall aktiv"),
                punkt(PHASE_SECURITY, "Virenschutz/Endpoint Protection aktiv und aktuell"),
                punkt(PHASE_SECURITY, "Festplattenverschlüsselung aktiviert", true),
                punkt(PHASE_SECURITY, "Bildschirmsperre konfiguriert"),
                punkt(PHASE_IDENTITY, "Benutzerkonto eingerichtet"),
                punkt(PHASE_IDENTITY, "2FA eingerichtet, falls erforderlich", true),
                punkt(PHASE_NETZWERK, "LAN/WLAN und DNS funktionieren"),
                punkt("Remote", "VPN eingerichtet, falls erforderlich", true),
                punkt("Daten", "Netzlaufwerke/Dateiablagen erreichbar", true),
                punkt("Software", "Standardsoftware installiert"),
                punkt("Software", "Individuelle Anwendungen installiert und getestet", true),
                punkt("Peripherie", "Drucker/Scanner/sonstige Geräte geprüft", true),
                punkt(PHASE_MANAGEMENT, "Client in Management-/Inventarsystem aufgenommen", true),
                punkt(PHASE_BACKUP, "Benutzerdaten-/Profilstrategie geprüft", true),
                punkt(PHASE_ABNAHME, "Benutzeranmeldung und Funktionstest erfolgreich"),
                punkt(PHASE_ABNAHME, "Übergabe dokumentiert")
        ));

        vorlagen.add(vorlage("Software-Deployment",
                punkt(PHASE_PLANUNG, "Softwareprodukt, Version und Zielgruppe festgelegt"),
                punkt(PHASE_PLANUNG, "Systemvoraussetzungen geprüft"),
                punkt(PHASE_PLANUNG, "Lizenz-/Nutzungsbedingungen geklärt"),
                punkt(PHASE_PLANUNG, "Abhängigkeiten/Runtime/DB-Anforderungen dokumentiert", true),
                punkt(PHASE_PLANUNG, "Pilot-/Testgruppe definiert", true),
                punkt(PHASE_PAKETIERUNG, "Installationsmedium/Paket geprüft"),
                punkt(PHASE_PAKETIERUNG, "Silent-/unattended Installation verfügbar bzw. getestet", true),
                punkt(PHASE_PAKETIERUNG, "Installationsparameter dokumentiert"),
                punkt(PHASE_PAKETIERUNG, "Konfigurationsdateien/Policies vorbereitet", true),
                punkt(PHASE_SECURITY, "Quelle/Signatur/Hash des Installationspakets geprüft", true),
                punkt(PHASE_DEPLOYMENT, "Deployment-Methode festgelegt"),
                punkt(PHASE_DEPLOYMENT, "Zielgeräte/-systeme ausgewählt"),
                punkt(PHASE_DEPLOYMENT, "Wartungsfenster/Termin geplant", true),
                punkt(PHASE_DEPLOYMENT, "Backup/Snapshot vor Änderung geprüft", true),
                punkt(PHASE_DEPLOYMENT, "Installation erfolgreich"),
                punkt(PHASE_TEST, "Dienst/Anwendung startet fehlerfrei"),
                punkt(PHASE_TEST, "Kernfunktionen getestet"),
                punkt(PHASE_TEST, "Berechtigungen und Zugriff geprüft"),
                punkt(PHASE_MONITORING, "Logs/Monitoring nach Deployment geprüft", true),
                punkt("Rollback", "Rollback-/Deinstallationsweg dokumentiert"),
                punkt(PHASE_ABNAHME, "Pilot-/Fachtest erfolgreich", true),
                punkt(PHASE_ABNAHME, "Deployment dokumentiert und abgeschlossen")
        ));

        vorlagen.add(vorlage("VMware",
                punkt(PHASE_VORBEREITUNG, "IPD erstellen und Downtime mit Kunde abstimmen"),
                punkt(PHASE_PLANUNG, "Ziel-vCenter/Datacenter/Cluster/Host festgelegt"),
                punkt(PHASE_PLANUNG, "VM-Name nach Namenskonvention festgelegt"),
                punkt(PHASE_PLANUNG, "Gastbetriebssystem und Version festgelegt"),
                punkt(PHASE_RESSOURCEN, "vCPU-Anzahl festgelegt"),
                punkt(PHASE_RESSOURCEN, "RAM-Größe festgelegt"),
                punkt(PHASE_STORAGE, "Datastore ausgewählt"),
                punkt(PHASE_STORAGE, "Virtuelle Disk(s) und Größe festgelegt"),
                punkt(PHASE_STORAGE, "Provisioning-Art festgelegt", true),
                punkt(PHASE_NETZWERK, "Portgruppe/Netzwerk ausgewählt"),
                punkt(PHASE_NETZWERK, "VLAN/Zielsegment geprüft", true),
                punkt(PHASE_FIRMWARE, "Firmware/UEFI und Secure Boot geprüft", true),
                punkt(PHASE_SECURITY, "vTPM/VBS bei Bedarf konfiguriert", true),
                punkt(PHASE_INSTALLATION, "ISO oder Template ausgewählt"),
                punkt(PHASE_INSTALLATION, "Gastbetriebssystem installiert bzw. Patch eingespielt"),
                punkt(PHASE_INTEGRATION, "VMware Tools/Open VM Tools installiert und aktuell"),
                punkt(PHASE_NETZWERK, TEXT_IP_GATEWAY_DNS),
                punkt(PHASE_IDENTITY, "Domänenbeitritt/Verzeichnisanbindung durchgeführt", true),
                punkt(PHASE_UPDATES, TEXT_GAST_OS_AKTUALISIERT),
                punkt(PHASE_BACKUP, "VM in Backup aufgenommen bzw. aktuelles Backup vor Wartung vorhanden"),
                punkt(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                punkt(PHASE_BETRIEB, "Snapshot vor Wartung erstellt, nach erfolgreichem Test wieder gelöscht", true),
                punkt(PHASE_SECURITY, "VM-/vCenter-Berechtigungen geprüft"),
                punkt(PHASE_ABNAHME, TEXT_NEUSTART_TEST),
                punkt(PHASE_ABNAHME, TEXT_VM_DOKUMENTATION),
                punkt(PHASE_ABNAHME, "Planned Activity Ticket dokumentiert")
        ));

        vorlagen.add(vorlage("Proxmox",
                punkt(PHASE_PLANUNG, "Ziel-Node/Cluster festgelegt"),
                punkt(PHASE_PLANUNG, "VM-ID und Name festgelegt"),
                punkt(PHASE_PLANUNG, "Gastbetriebssystem/OS-Typ ausgewählt"),
                punkt(PHASE_RESSOURCEN, "CPU/Sockets/Cores festgelegt"),
                punkt(PHASE_RESSOURCEN, "RAM/Ballooning festgelegt"),
                punkt(PHASE_STORAGE, "Storage-Ziel ausgewählt"),
                punkt(PHASE_STORAGE, "Disk-Größe und Bus/Controller festgelegt"),
                punkt(PHASE_NETZWERK, "Bridge ausgewählt"),
                punkt(PHASE_NETZWERK, "VLAN-Tag/Segment geprüft", true),
                punkt(PHASE_FIRMWARE, "BIOS/UEFI/Machine Type festgelegt", true),
                punkt(PHASE_SECURITY, "Secure Boot/TPM bei Bedarf konfiguriert", true),
                punkt(PHASE_INSTALLATION, "ISO oder Template ausgewählt"),
                punkt(PHASE_INSTALLATION, "Gastbetriebssystem installiert"),
                punkt(PHASE_INTEGRATION, "QEMU Guest Agent installiert und aktiviert"),
                punkt(PHASE_NETZWERK, TEXT_IP_GATEWAY_DNS),
                punkt(PHASE_SECURITY, "Proxmox Firewall/VM Firewall geprüft", true),
                punkt(PHASE_UPDATES, TEXT_GAST_OS_AKTUALISIERT),
                punkt(PHASE_BACKUP, "Backup-Job/Ziel für VM eingerichtet"),
                punkt(PHASE_BACKUP, "Erster Backup-Lauf erfolgreich"),
                punkt(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                punkt(PHASE_SECURITY, "Rollen/Berechtigungen geprüft"),
                punkt(PHASE_ABNAHME, TEXT_NEUSTART_TEST),
                punkt(PHASE_ABNAHME, TEXT_VM_DOKUMENTATION)
        ));

        vorlagen.add(vorlage("Hyper-V",
                punkt(PHASE_PLANUNG, "Ziel-Host/Cluster festgelegt"),
                punkt(PHASE_PLANUNG, "VM-Name und Speicherort festgelegt"),
                punkt(PHASE_PLANUNG, "Gastbetriebssystem und Kompatibilität geprüft"),
                punkt(PHASE_PLANUNG, "VM-Generation gewählt; Gen 2 bevorzugt wenn unterstützt"),
                punkt(PHASE_RESSOURCEN, "vCPU-Anzahl festgelegt"),
                punkt(PHASE_RESSOURCEN, "RAM/Dynamic Memory festgelegt"),
                punkt(PHASE_STORAGE, "VHDX-Speicherort und Größe festgelegt"),
                punkt(PHASE_NETZWERK, "Virtuellen Switch ausgewählt"),
                punkt(PHASE_NETZWERK, "VLAN/Netzwerksegment geprüft", true),
                punkt(PHASE_SECURITY, "Secure Boot geprüft/aktiviert wenn unterstützt", true),
                punkt(PHASE_SECURITY, "vTPM/Verschlüsselung bei Bedarf konfiguriert", true),
                punkt(PHASE_INSTALLATION, "ISO/VHDX/Deploymentquelle ausgewählt"),
                punkt(PHASE_INSTALLATION, "Gastbetriebssystem installiert"),
                punkt(PHASE_INTEGRATION, "Hyper-V Integration Services aktiv/aktuell"),
                punkt(PHASE_NETZWERK, TEXT_IP_GATEWAY_DNS),
                punkt(PHASE_IDENTITY, "Domänenbeitritt/Verzeichnisanbindung durchgeführt", true),
                punkt(PHASE_UPDATES, TEXT_GAST_OS_AKTUALISIERT),
                punkt(PHASE_BACKUP, "VM in Backup aufgenommen"),
                punkt(PHASE_BETRIEB, "Checkpoints geprüft und Bereinigungsstrategie festgelegt", true),
                punkt(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                punkt(PHASE_SECURITY, "Host-/VM-Berechtigungen geprüft"),
                punkt(PHASE_ABNAHME, TEXT_NEUSTART_TEST),
                punkt(PHASE_ABNAHME, TEXT_VM_DOKUMENTATION)
        ));

        vorlagen.add(vorlage("Backup & Restore",
                punkt(PHASE_PLANUNG, "Zu sichernde Systeme/Daten festgelegt"),
                punkt(PHASE_PLANUNG, "RPO festgelegt"),
                punkt(PHASE_PLANUNG, "RTO festgelegt"),
                punkt(PHASE_PLANUNG, "Backup-Methode/Typ festgelegt"),
                punkt("Ziel", "Backup-Ziel und Kapazität geprüft"),
                punkt("Ziel", "Backup-Kopie getrennt vom Produktionsnetz vorhanden"),
                punkt(PHASE_SECURITY, "Backup-Zugriffe/Berechtigungen eingeschränkt"),
                punkt(PHASE_SECURITY, "Verschlüsselung der Backups geprüft", true),
                punkt(PHASE_JOB, "Backup-Job eingerichtet"),
                punkt(PHASE_JOB, "Zeitplan und Aufbewahrung konfiguriert"),
                punkt(PHASE_JOB, "Applikationskonsistenz/VSS o. ä. geprüft", true),
                punkt(PHASE_JOB, "Erster vollständiger Lauf erfolgreich"),
                punkt(PHASE_MONITORING, "Fehlerbenachrichtigung/Monitoring eingerichtet"),
                punkt(PHASE_RESTORE, "Restore-Verfahren dokumentiert"),
                punkt(PHASE_RESTORE, "Datei-/Objekt-Restore getestet"),
                punkt(PHASE_RESTORE, "System-/VM-Restore getestet oder geplant", true),
                punkt(PHASE_RESTORE, "Restore-Ergebnis fachlich geprüft"),
                punkt(PHASE_NOTFALL, "Offsite/Offline/immutable Kopie geprüft", true),
                punkt(PHASE_NOTFALL, "Notfallkontakte und Eskalation dokumentiert", true),
                punkt(PHASE_ABNAHME, "Backup- und Restore-Dokumentation vollständig")
        ));

        checklistTemplateRepository.saveAll(vorlagen);
    }

    // Baut eine einzelne Vorlage aus Namen + einer beliebigen Anzahl an
    // bereits fertig formatierten Punkt-Strings (siehe punkt(...) unten).
    // Die id lasse ich bewusst null - die vergibt MongoDB automatisch
    // beim Speichern, genauso wie es ChecklistTemplateService.createTemplate
    // beim manuellen Anlegen über die Oberfläche auch macht.
    // Das letzte Argument (true) markiert alle zehn hier erzeugten
    // Vorlagen als "standard" - dadurch verweigert
    // ChecklistTemplateService.deleteTemplate das Löschen, siehe
    // Kommentar dort und am standard-Feld in ChecklistTemplate selbst.
    private ChecklistTemplate vorlage(String name, String... punkte) {
        return new ChecklistTemplate(null, name, List.of(punkte), true);
    }

    // Pflicht-Punkt: Phase wird in eckigen Klammern vorangestellt.
    private String punkt(String phase, String text) {
        return punkt(phase, text, false);
    }

    // Überladung für optionale Punkte (in meiner Excel-Tabelle als
    // "Optional" statt "Ja" markiert) - hängt zusätzlich " (optional)"
    // an, damit das beim Abhaken der Checkliste im Frontend sofort
    // erkennbar bleibt.
    private String punkt(String phase, String text, boolean optional) {
        String basis = "[" + phase + "] " + text;
        return optional ? basis + " (optional)" : basis;
    }
}