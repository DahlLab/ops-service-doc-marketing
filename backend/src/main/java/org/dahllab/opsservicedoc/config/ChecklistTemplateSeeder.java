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
    private static final String PHASE_ACCEPTANCE = "Acceptance";
    private static final String PHASE_PLANNING = "Planning";
    private static final String PHASE_OPERATION = "Operation";
    private static final String PHASE_NETWORK = "Network";
    private static final String PHASE_INSTALLATION = "Installation";
    private static final String PHASE_STORAGE = "Storage";
    private static final String PHASE_ACCESS = "Access";
    private static final String PHASE_MONITORING = "Monitoring";
    private static final String PHASE_RESOURCES = "Resources";
    private static final String PHASE_PREPARATION = "Preparation";
    private static final String PHASE_CONFIGURATION = "Configuration";
    private static final String PHASE_UPDATES = "Updates";
    private static final String PHASE_BACKUP = "Backup";
    private static final String PHASE_DEPLOYMENT = "Deployment";
    private static final String PHASE_FIRMWARE = "Firmware";
    private static final String PHASE_ENDPOINT = "Endpoint";
    private static final String PHASE_EMERGENCY = "Emergency";
    private static final String PHASE_IDENTITY = "Identity";
    private static final String PHASE_PACKAGING = "Packaging";
    private static final String PHASE_JOB = "Job";
    private static final String PHASE_RESTORE = "Restore";
    private static final String PHASE_INVENTORY = "Inventory";
    private static final String PHASE_MANAGEMENT = "Management";
    private static final String PHASE_TEST = "Test";
    private static final String PHASE_INTEGRATION = "Integration";

    private static final String PHASE_HARDWARE = "Hardware";
    private static final String TEXT_IP_GATEWAY_DNS = "IP, gateway and DNS configured in guest";
    private static final String TEXT_GUEST_OS_UPDATED = "Guest OS updated";
    private static final String TEXT_VM_MONITORING = "VM/guest added to monitoring";
    private static final String TEXT_RESTART_TEST = "Restart, network and core functions tested";
    private static final String TEXT_VM_DOCUMENTATION = "VM documentation complete";

    @Override
    public void run(String... args) {
        if (checklistTemplateRepository.count() > 0) {
            return;
        }

        List<ChecklistTemplate> templates = new ArrayList<>();

        templates.add(template("Server",
                entry(PHASE_PREPARATION, "Define server name according to naming convention"),
                entry(PHASE_PREPARATION, "Define IP address, subnet, gateway and DNS"),
                entry(PHASE_PREPARATION, "Document server role/purpose"),
                entry(PHASE_PREPARATION, "Define operating system and edition"),
                entry(PHASE_INSTALLATION, "Install operating system"),
                entry(PHASE_INSTALLATION, "Verify activation/license status"),
                entry(PHASE_CONFIGURATION, "Set hostname and network configuration"),
                entry(PHASE_CONFIGURATION, "Configure time server/NTP and verify time"),
                entry(PHASE_CONFIGURATION, "Perform domain join or directory integration", true),
                entry(PHASE_CONFIGURATION, "Configure users, groups and permissions", true),
                entry(PHASE_CONFIGURATION, "Configure shares and file system permissions", true),
                entry(PHASE_SECURITY, "Firewall enabled and configured appropriately"),
                entry(PHASE_SECURITY, "Patch/update level complete"),
                entry(PHASE_SECURITY, "Hardening performed (secure remote access, disable legacy auth protocols, TLS 1.2+, secure local administrator)"),
                entry(PHASE_SECURITY, "Local administrator rights reviewed, password solution for local admin accounts active"),
                entry(PHASE_SECURITY, "Certificates only accepted from trusted domains", true),
                entry(PHASE_OPERATION, "Event logs/system logs checked for errors"),
                entry(PHASE_OPERATION, "Monitoring connected and function verified"),
                entry(PHASE_OPERATION, "Backup connected and first run successful"),
                entry(PHASE_OPERATION, "Restore/recovery test planned or performed"),
                entry(PHASE_OPERATION, "Server recorded in Configuration Management Database (CMDB)"),
                entry(PHASE_OPERATION, "Server recorded in IP address/inventory tool", true),
                entry(PHASE_SECURITY, "Critical system added to file integrity/audit monitoring, if applicable", true),
                entry(PHASE_SECURITY, "Role-based remote access groups assigned (by responsibility)", true),
                entry(PHASE_ACCEPTANCE, "Remote access/operational access verified", true),
                entry(PHASE_ACCEPTANCE, "Documentation complete"),
                entry(PHASE_ACCEPTANCE, "Functional acceptance test successful"),
                entry(PHASE_ACCEPTANCE, "Handover/acceptance documented (incl. responsible person and date)")
        ));

        templates.add(template(PHASE_HARDWARE,
                entry(PHASE_INVENTORY, "Record manufacturer, model and serial number"),
                entry(PHASE_INVENTORY, "Record asset tag/inventory number", true),
                entry("Location", "Document location, room, rack and rack unit"),
                entry("Power", "Redundant power supplies present and connected", true),
                entry("Power", "UPS/PDU connection verified", true),
                entry(PHASE_HARDWARE, "CPU configuration verified"),
                entry(PHASE_HARDWARE, "RAM population and detected capacity verified"),
                entry(PHASE_STORAGE, "Number, type and capacity of drives verified"),
                entry(PHASE_STORAGE, "RAID level/storage configuration defined", true),
                entry(PHASE_STORAGE, "RAID/controller status healthy"),
                entry(PHASE_FIRMWARE, "BIOS/UEFI version verified/updated"),
                entry(PHASE_FIRMWARE, "RAID, NIC and controller firmware verified"),
                entry(PHASE_MANAGEMENT, "Out-of-band management configured", true),
                entry(PHASE_MANAGEMENT, "Management IP, DNS and access documented", true),
                entry(PHASE_NETWORK, "NICs and physical cabling verified"),
                entry(PHASE_NETWORK, "Switch ports/VLANs documented"),
                entry("Diagnostics", "Vendor diagnostics/hardware test successful"),
                entry(PHASE_INSTALLATION, "Target OS or hypervisor installed"),
                entry(PHASE_OPERATION, "Monitoring/hardware alerts connected", true),
                entry(PHASE_OPERATION, "Warranty/support status documented", true),
                entry(PHASE_ACCEPTANCE, "Burn-in/functional test completed"),
                entry(PHASE_ACCEPTANCE, "Hardware documentation complete")
        ));

        templates.add(template("Network",
                entry("Overview", "Network diagram with all components available (workstations, servers, switches, routers, printers, mobile devices)"),
                entry("Overview", "Documented for each device: owner, location, stored data", true),
                entry(PHASE_ACCESS, "Network ports and wall outlets physically secured or disabled when unused"),
                entry(PHASE_ACCESS, "Default credentials changed on all network components"),
                entry(PHASE_ACCESS, "Server/file server location additionally secured (lockable room, regular scans)", true),
                entry(PHASE_SECURITY, "Firewall restricted to necessary access only, rules reviewed regularly"),
                entry(PHASE_SECURITY, "Guest Wi-Fi separated from internal network"),
                entry(PHASE_SECURITY, "Wi-Fi encryption set to strongest available standard, SSID broadcast disabled"),
                entry(PHASE_SECURITY, "Wi-Fi access restricted to specific device identifiers (where practical)", true),
                entry(PHASE_OPERATION, "Access to servers, routers and switches logged"),
                entry(PHASE_OPERATION, "Unused ports disabled", true),
                entry(PHASE_ACCEPTANCE, "IP allocation and subnet structure documented")
        ));

        templates.add(template("Security",
                entry("Organization", "IT security policy in place and employees trained", true),
                entry("Organization", "Regular security awareness training (e.g. phishing, password security)", true),
                entry(PHASE_ACCESS, "Individual user accounts for all users, separate admin/user accounts"),
                entry(PHASE_ACCESS, "Strong password policy enforced (minimum length, complexity)"),
                entry(PHASE_ACCESS, "Two-factor authentication in use (at least for critical systems/VPN)"),
                entry(PHASE_ACCESS, "Procedure for disabling accounts of departed employees in place"),
                entry(PHASE_ENDPOINT, "Antivirus/endpoint protection active and up to date on all devices"),
                entry(PHASE_ENDPOINT, "Patch management for operating system and applications set up"),
                entry(PHASE_ENDPOINT, "Disk encryption active on mobile devices"),
                entry(PHASE_ENDPOINT, "External devices/USB ports controlled or restricted", true),
                entry("Remote", "Remote access only via secured VPN connection"),
                entry("Communication", "Spam/virus filter for email active, suspicious attachments flagged/blocked", true),
                entry("Communication", "Reporting channel for phishing attempts in place", true),
                entry("Cloud", "Cloud services used in compliance with data protection, contracts with data processors in place", true),
                entry(PHASE_MONITORING, "Security-relevant events logged centrally (log management/SIEM)", true),
                entry(PHASE_EMERGENCY, "Emergency plan for security incidents in place, responsibilities defined", true),
                entry(PHASE_EMERGENCY, "Emergency management tested regularly", true),
                entry("Audit", "Regular internal/external audits performed, results documented", true)
        ));

        templates.add(template("Client",
                entry(PHASE_INVENTORY, "Device name and user assignment recorded"),
                entry(PHASE_INSTALLATION, "Operating system installed and activated"),
                entry(PHASE_UPDATES, "Operating system fully updated"),
                entry(PHASE_UPDATES, "Automatic patch management enabled", true),
                entry(PHASE_SECURITY, "Client firewall enabled"),
                entry(PHASE_SECURITY, "Antivirus/endpoint protection active and up to date"),
                entry(PHASE_SECURITY, "Disk encryption enabled", true),
                entry(PHASE_SECURITY, "Screen lock configured"),
                entry(PHASE_IDENTITY, "User account set up"),
                entry(PHASE_IDENTITY, "2FA set up, if required", true),
                entry(PHASE_NETWORK, "LAN/Wi-Fi and DNS working"),
                entry("Remote", "VPN set up, if required", true),
                entry("Data", "Network drives/file shares reachable", true),
                entry("Software", "Standard software installed"),
                entry("Software", "Custom applications installed and tested", true),
                entry("Peripherals", "Printers/scanners/other devices verified", true),
                entry(PHASE_MANAGEMENT, "Client added to management/inventory system", true),
                entry(PHASE_BACKUP, "User data/profile strategy verified", true),
                entry(PHASE_ACCEPTANCE, "User login and functional test successful"),
                entry(PHASE_ACCEPTANCE, "Handover documented")
        ));

        templates.add(template("Software-Deployment",
                entry(PHASE_PLANNING, "Define software product, version and target group"),
                entry(PHASE_PLANNING, "System requirements verified"),
                entry(PHASE_PLANNING, "License/terms of use clarified"),
                entry(PHASE_PLANNING, "Dependencies/runtime/DB requirements documented", true),
                entry(PHASE_PLANNING, "Pilot/test group defined", true),
                entry(PHASE_PACKAGING, "Installation media/package verified"),
                entry(PHASE_PACKAGING, "Silent/unattended installation available or tested", true),
                entry(PHASE_PACKAGING, "Installation parameters documented"),
                entry(PHASE_PACKAGING, "Configuration files/policies prepared", true),
                entry(PHASE_SECURITY, "Source/signature/hash of installation package verified", true),
                entry(PHASE_DEPLOYMENT, "Deployment method defined"),
                entry(PHASE_DEPLOYMENT, "Target devices/systems selected"),
                entry(PHASE_DEPLOYMENT, "Maintenance window/date scheduled", true),
                entry(PHASE_DEPLOYMENT, "Backup/snapshot verified before change", true),
                entry(PHASE_DEPLOYMENT, "Installation successful"),
                entry(PHASE_TEST, "Service/application starts without errors"),
                entry(PHASE_TEST, "Core functions tested"),
                entry(PHASE_TEST, "Permissions and access verified"),
                entry(PHASE_MONITORING, "Logs/monitoring checked after deployment", true),
                entry("Rollback", "Rollback/uninstallation path documented"),
                entry(PHASE_ACCEPTANCE, "Pilot/functional test successful", true),
                entry(PHASE_ACCEPTANCE, "Deployment documented and completed")
        ));

        templates.add(template("VMware",
                entry(PHASE_PREPARATION, "Create IPD and agree downtime with customer"),
                entry(PHASE_PLANNING, "Target vCenter/datacenter/cluster/host defined"),
                entry(PHASE_PLANNING, "VM name defined according to naming convention"),
                entry(PHASE_PLANNING, "Guest operating system and version defined"),
                entry(PHASE_RESOURCES, "Number of vCPUs defined"),
                entry(PHASE_RESOURCES, "RAM size defined"),
                entry(PHASE_STORAGE, "Datastore selected"),
                entry(PHASE_STORAGE, "Virtual disk(s) and size defined"),
                entry(PHASE_STORAGE, "Provisioning type defined", true),
                entry(PHASE_NETWORK, "Port group/network selected"),
                entry(PHASE_NETWORK, "VLAN/target segment verified", true),
                entry(PHASE_FIRMWARE, "Firmware/UEFI and Secure Boot verified", true),
                entry(PHASE_SECURITY, "vTPM/VBS configured if needed", true),
                entry(PHASE_INSTALLATION, "ISO or template selected"),
                entry(PHASE_INSTALLATION, "Guest operating system installed or patch applied"),
                entry(PHASE_INTEGRATION, "VMware Tools/Open VM Tools installed and up to date"),
                entry(PHASE_NETWORK, TEXT_IP_GATEWAY_DNS),
                entry(PHASE_IDENTITY, "Domain join/directory integration performed", true),
                entry(PHASE_UPDATES, TEXT_GUEST_OS_UPDATED),
                entry(PHASE_BACKUP, "VM added to backup or current backup available before maintenance"),
                entry(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                entry(PHASE_OPERATION, "Snapshot created before maintenance, deleted after successful test", true),
                entry(PHASE_SECURITY, "VM/vCenter permissions verified"),
                entry(PHASE_ACCEPTANCE, TEXT_RESTART_TEST),
                entry(PHASE_ACCEPTANCE, TEXT_VM_DOCUMENTATION),
                entry(PHASE_ACCEPTANCE, "Planned activity ticket documented")
        ));

        templates.add(template("Proxmox",
                entry(PHASE_PLANNING, "Target node/cluster defined"),
                entry(PHASE_PLANNING, "VM ID and name defined"),
                entry(PHASE_PLANNING, "Guest operating system/OS type selected"),
                entry(PHASE_RESOURCES, "CPU/sockets/cores defined"),
                entry(PHASE_RESOURCES, "RAM/ballooning defined"),
                entry(PHASE_STORAGE, "Storage target selected"),
                entry(PHASE_STORAGE, "Disk size and bus/controller defined"),
                entry(PHASE_NETWORK, "Bridge selected"),
                entry(PHASE_NETWORK, "VLAN tag/segment verified", true),
                entry(PHASE_FIRMWARE, "BIOS/UEFI/machine type defined", true),
                entry(PHASE_SECURITY, "Secure Boot/TPM configured if needed", true),
                entry(PHASE_INSTALLATION, "ISO or template selected"),
                entry(PHASE_INSTALLATION, "Guest operating system installed"),
                entry(PHASE_INTEGRATION, "QEMU Guest Agent installed and enabled"),
                entry(PHASE_NETWORK, TEXT_IP_GATEWAY_DNS),
                entry(PHASE_SECURITY, "Proxmox firewall/VM firewall verified", true),
                entry(PHASE_UPDATES, TEXT_GUEST_OS_UPDATED),
                entry(PHASE_BACKUP, "Backup job/target for VM set up"),
                entry(PHASE_BACKUP, "First backup run successful"),
                entry(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                entry(PHASE_SECURITY, "Roles/permissions verified"),
                entry(PHASE_ACCEPTANCE, TEXT_RESTART_TEST),
                entry(PHASE_ACCEPTANCE, TEXT_VM_DOCUMENTATION)
        ));

        templates.add(template("Hyper-V",
                entry(PHASE_PLANNING, "Target host/cluster defined"),
                entry(PHASE_PLANNING, "VM name and storage location defined"),
                entry(PHASE_PLANNING, "Guest operating system and compatibility verified"),
                entry(PHASE_PLANNING, "VM generation selected; Gen 2 preferred if supported"),
                entry(PHASE_RESOURCES, "Number of vCPUs defined"),
                entry(PHASE_RESOURCES, "RAM/dynamic memory defined"),
                entry(PHASE_STORAGE, "VHDX location and size defined"),
                entry(PHASE_NETWORK, "Virtual switch selected"),
                entry(PHASE_NETWORK, "VLAN/network segment verified", true),
                entry(PHASE_SECURITY, "Secure Boot verified/enabled if supported", true),
                entry(PHASE_SECURITY, "vTPM/encryption configured if needed", true),
                entry(PHASE_INSTALLATION, "ISO/VHDX/deployment source selected"),
                entry(PHASE_INSTALLATION, "Guest operating system installed"),
                entry(PHASE_INTEGRATION, "Hyper-V Integration Services active/up to date"),
                entry(PHASE_NETWORK, TEXT_IP_GATEWAY_DNS),
                entry(PHASE_IDENTITY, "Domain join/directory integration performed", true),
                entry(PHASE_UPDATES, TEXT_GUEST_OS_UPDATED),
                entry(PHASE_BACKUP, "VM added to backup"),
                entry(PHASE_OPERATION, "Checkpoints reviewed and cleanup strategy defined", true),
                entry(PHASE_MONITORING, TEXT_VM_MONITORING, true),
                entry(PHASE_SECURITY, "Host/VM permissions verified"),
                entry(PHASE_ACCEPTANCE, TEXT_RESTART_TEST),
                entry(PHASE_ACCEPTANCE, TEXT_VM_DOCUMENTATION)
        ));

        templates.add(template("Backup & Restore",
                entry(PHASE_PLANNING, "Systems/data to be backed up defined"),
                entry(PHASE_PLANNING, "RPO defined"),
                entry(PHASE_PLANNING, "RTO defined"),
                entry(PHASE_PLANNING, "Backup method/type defined"),
                entry("Target", "Backup target and capacity verified"),
                entry("Target", "Backup copy separated from production network available"),
                entry(PHASE_SECURITY, "Backup access/permissions restricted"),
                entry(PHASE_SECURITY, "Backup encryption verified", true),
                entry(PHASE_JOB, "Backup job set up"),
                entry(PHASE_JOB, "Schedule and retention configured"),
                entry(PHASE_JOB, "Application consistency/VSS or similar verified", true),
                entry(PHASE_JOB, "First full run successful"),
                entry(PHASE_MONITORING, "Error notification/monitoring set up"),
                entry(PHASE_RESTORE, "Restore procedure documented"),
                entry(PHASE_RESTORE, "File/object restore tested"),
                entry(PHASE_RESTORE, "System/VM restore tested or planned", true),
                entry(PHASE_RESTORE, "Restore result verified functionally"),
                entry(PHASE_EMERGENCY, "Offsite/offline/immutable copy verified", true),
                entry(PHASE_EMERGENCY, "Emergency contacts and escalation documented", true),
                entry(PHASE_ACCEPTANCE, "Backup and restore documentation complete")
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
