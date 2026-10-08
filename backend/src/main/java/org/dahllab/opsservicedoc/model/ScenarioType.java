package org.dahllab.opsservicedoc.model;

// Enum für die Art des Wartungsszenarios, das im Ticket dokumentiert wird.
// Aktuell nur ein Wert, da laut Projektplanung SERVER_WARTUNG das erste
// und einzige Szenario für den Start ist. Weitere Szenario-Typen (z.B.
// NETZWERK_UPDATE) können hier später einfach als weitere Konstanten ergänzt werden.
public enum ScenarioType {
    SERVER_MAINTENANCE
}
