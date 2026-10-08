import { useEffect, useState } from 'react';
import { Accordion, Alert, Badge, Button, Form, Modal, Spinner, Tab, Tabs, ProgressBar } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import { formatiereDatum } from '../utils/formatierung';
import { DynamischeItemListe } from '../components/DynamischeItemListe';
import type {
    ChecklistDto,
    ChecklistFormData,
    ChecklistItemDto,
    ChecklistTemplateDto,
    ChecklistTemplateFormData,
    TicketDto,
} from '../api/types';

// Leeres Formular für eine neue Checkliste. Ich starte mit genau EINEM
// leeren Item statt einer leeren Liste, weil mein Backend
// (@NotEmpty auf ChecklistDto.items) mindestens ein Item verlangt -
// so ist das Formular von Anfang an in einem gültigen Grundzustand,
// sobald der Nutzer den Text einträgt.
function leeresChecklistFormular(ticketId: string): ChecklistFormData {
    return {
        ticketId,
        titel: '',
        items: [{ id: null, beschreibung: '', erledigt: false }],
    };
}

const LEERES_TEMPLATE_FORMULAR: ChecklistTemplateFormData = {
    name: '',
    itemBeschreibungen: [''],
};

// Zerlegt einen Vorlagen-Punkt wieder in seine Bestandteile. Mein
// ChecklistTemplateSeeder codiert Phase und "optional" direkt mit in
// den String hinein (z.B. "[Konfiguration] Domänenbeitritt ...
// durchführen (optional)"), weil das Datenmodell selbst kein
// eigenes Feld dafür hat (siehe Kommentar im Seeder). Beim Anzeigen
// hole ich das hier wieder auseinander, damit ich es sauber gruppiert
// und mit einem Badge statt im Fließtext darstellen kann. Punkte ohne
// "[Phase]"-Präfix (z.B. selbst angelegte Vorlagen) fallen einfach
// unter "Sonstiges".
// Ich nehme hier bewusst indexOf statt einer Regex: die vorherige Regex
// hatte laut SonarQube ein super-lineares Laufzeitverhalten (Backtracking)
// - mit indexOf ist die Laufzeit garantiert linear.
function parseBaustein(beschreibung: string): { phase: string; text: string; optional: boolean } {
    const optional = beschreibung.endsWith(' (optional)');
    const ohneOptional = optional ? beschreibung.slice(0, -' (optional)'.length) : beschreibung;
    if (ohneOptional.startsWith('[')) {
        const ende = ohneOptional.indexOf(']');
        // ende > 1 stellt sicher, dass zwischen den Klammern mindestens
        // ein Zeichen steht ("[]" zählt nicht als Phase).
        if (ende > 1) {
            return { phase: ohneOptional.slice(1, ende), text: ohneOptional.slice(ende + 1).trim(), optional };
        }
    }
    return { phase: 'Sonstiges', text: ohneOptional, optional };
}

// Gruppiert die Punkte einer Vorlage nach Phase, in der Reihenfolge,
// in der die Phasen zum ersten Mal auftauchen (nicht alphabetisch) -
// das entspricht dem natürlichen Ablauf (Vorbereitung vor
// Installation vor Abnahme usw.), den ich mir beim Erstellen der
// Vorlagen schon überlegt habe.
function gruppiereNachPhase(itemBeschreibungen: string[]): { phase: string; punkte: { text: string; optional: boolean }[] }[] {
    const gruppen: { phase: string; punkte: { text: string; optional: boolean }[] }[] = [];
    for (const beschreibung of itemBeschreibungen) {
        const { phase, text, optional } = parseBaustein(beschreibung);
        let gruppe = gruppen.find((g) => g.phase === phase);
        if (!gruppe) {
            gruppe = { phase, punkte: [] };
            gruppen.push(gruppe);
        }
        gruppe.punkte.push({ text, optional });
    }
    return gruppen;
}

// Seite für den Bereich "Checklisten" (entspricht ChecklistController +
// ChecklistTemplateController im Backend). Ich bilde beide Bereiche
// als zwei Tabs EINER Seite ab, weil sie thematisch zusammengehören
// (Vorlagen existieren nur, um daraus Checklisten zu erzeugen), aber
// datentechnisch komplett unabhängig sind (verschiedene Endpunkte,
// verschiedene Services).
// Anteil der erledigten Items in Prozent (0 bei leerer Liste, damit ich nicht durch 0 teile).
function fortschrittProzent(items: { erledigt: boolean }[]): number {
    if (items.length === 0) return 0;
    return (items.filter((item) => item.erledigt).length / items.length) * 100;
}

export function ChecklistenPage() {
    // ---- gemeinsame Daten ----
    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [ladeFehler, setLadeFehler] = useState<string | null>(null);

    // ---- Tab 1: Checklisten ----
    const [checklists, setChecklists] = useState<ChecklistDto[]>([]);
    const [templates, setTemplates] = useState<ChecklistTemplateDto[]>([]);
    const [checklistenLoading, setChecklistenLoading] = useState(true);
    const [ticketFilter, setTicketFilter] = useState('');

    const [checklistModalOffen, setChecklistModalOffen] = useState(false);
    // 'manuell' = Items werden im Formular selbst eingetragen,
    // 'vorlage' = Items kommen 1:1 aus einer ausgewählten Vorlage,
    // 'baukasten' = ich picke mir einzelne Punkte aus MEHREREN Vorlagen
    // zusammen (statt nur eine komplette Vorlage als Ganzes zu
    // übernehmen) und baue mir daraus meine eigene, gemischte
    // Checkliste. Nur beim NEUANLEGEN relevant - beim Bearbeiten einer
    // bestehenden Checkliste editiere ich immer direkt die konkreten
    // Items.
    const [erstellModus, setErstellModus] = useState<'manuell' | 'vorlage' | 'baukasten'>('manuell');
    const [bearbeiteteChecklist, setBearbeiteteChecklist] = useState<ChecklistDto | null>(null);
    const [checklistFormular, setChecklistFormular] = useState<ChecklistFormData>(leeresChecklistFormular(''));
    const [ausgewaehlteTemplateId, setAusgewaehlteTemplateId] = useState('');
    // Für den Baukasten-Modus: welche Punkte (aus welcher Vorlage) sind
    // angehakt. Als Key nehme ich "templateId:index" statt nur den Text,
    // damit ich gleichlautende Punkte in verschiedenen Vorlagen (oder
    // sogar doppelte Einträge innerhalb einer Vorlage) sauber
    // auseinanderhalten kann.
    const [ausgewaehlteBausteine, setAusgewaehlteBausteine] = useState<Record<string, boolean>>({});
    // Kurze Erfolgsmeldung nach "Als eigene Vorlage speichern" im
    // Baukasten-Modus - getrennt von checklistFehler, weil beides
    // gleichzeitig sichtbar sein könnte (z.B. Vorlage erfolgreich
    // gespeichert, aber die Checkliste selbst dann doch nicht erzeugt).
    const [baukastenSpeicherHinweis, setBaukastenSpeicherHinweis] = useState<string | null>(null);
    const [speichernLaeuft, setSpeichernLaeuft] = useState(false);
    const [checklistFehler, setChecklistFehler] = useState<string | null>(null);

    // ---- Tab 2: Vorlagen ----
    const [templatesLoading, setTemplatesLoading] = useState(true);
    const [templateModalOffen, setTemplateModalOffen] = useState(false);
    const [bearbeitetesTemplate, setBearbeitetesTemplate] = useState<ChecklistTemplateDto | null>(null);
    const [templateFormular, setTemplateFormular] = useState<ChecklistTemplateFormData>(LEERES_TEMPLATE_FORMULAR);
    const [templateFehler, setTemplateFehler] = useState<string | null>(null);

    // Lädt die Checklisten, optional gefiltert nach ticketId - gleiches
    // Muster wie ladeTasks() in der TaskPlannerPage.
    async function ladeChecklisten(ticketId: string) {
        try {
            const pfad = ticketId ? `/api/checklists?ticketId=${ticketId}` : '/api/checklists';
            const geladeneChecklisten = await api.get<ChecklistDto[]>(pfad);
            setChecklists(geladeneChecklisten);
        } catch (error) {
            setLadeFehler('Checklisten konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setChecklistenLoading(false);
        }
    }

    async function ladeTemplates() {
        try {
            const geladeneTemplates = await api.get<ChecklistTemplateDto[]>('/api/checklist-templates');
            setTemplates(geladeneTemplates);
        } catch (error) {
            setLadeFehler('Checklisten-Vorlagen konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setTemplatesLoading(false);
        }
    }

    // Beim ersten Rendern lade ich Tickets, Checklisten UND Vorlagen auf
    // einmal - Vorlagen brauche ich schon im Checklisten-Tab für die
    // Dropdown-Auswahl "aus Vorlage erzeugen", nicht erst im Vorlagen-Tab.
    // Die Anfragen stehen direkt im Effect: State wird nur im Callback gesetzt,
    // wenn die Daten ankommen, und `abgebrochen` schützt davor, State nach dem
    // Verlassen der Seite zu setzen.
    useEffect(() => {
        let abgebrochen = false;
        api.get<TicketDto[]>('/api/tickets').then(setTickets).catch(console.error);
        api.get<ChecklistDto[]>('/api/checklists')
            .then((geladeneChecklisten) => {
                if (!abgebrochen) setChecklists(geladeneChecklisten);
            })
            .catch((error) => {
                if (abgebrochen) return;
                setLadeFehler('Checklisten konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!abgebrochen) setChecklistenLoading(false);
            });
        api.get<ChecklistTemplateDto[]>('/api/checklist-templates')
            .then((geladeneTemplates) => {
                if (!abgebrochen) setTemplates(geladeneTemplates);
            })
            .catch((error) => {
                if (abgebrochen) return;
                setLadeFehler('Checklisten-Vorlagen konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!abgebrochen) setTemplatesLoading(false);
            });
        return () => {
            abgebrochen = true;
        };
    }, []);

    function ticketTitel(ticketId: string): string {
        return tickets.find((t) => t.id === ticketId)?.titel ?? '(unbekanntes Ticket)';
    }

    function handleFilterChange(ticketId: string) {
        setTicketFilter(ticketId);
        setChecklistenLoading(true);
        // void markiert explizit, dass ich das Promise bewusst nicht
        // abwarte - ladeChecklisten fängt seine Fehler intern selbst ab.
        void ladeChecklisten(ticketId);
    }

    // Öffnet das Modal zum Neuanlegen. Ich setze den Modus zurück auf
    // 'manuell' als Standard und übernehme - wie beim TaskPlanner - den
    // aktiven Ticket-Filter als Vorauswahl, falls einer gesetzt ist.
    function handleNeueChecklist() {
        setBearbeiteteChecklist(null);
        setErstellModus('manuell');
        setChecklistFormular(leeresChecklistFormular(ticketFilter || tickets[0]?.id || ''));
        setAusgewaehlteTemplateId(templates[0]?.id ?? '');
        // Baukasten-Auswahl bei jedem neuen Anlegen zurücksetzen, sonst
        // wären beim nächsten Öffnen noch Häkchen von vorher gesetzt.
        setAusgewaehlteBausteine({});
        setBaukastenSpeicherHinweis(null);
        setChecklistFehler(null);
        setChecklistModalOffen(true);
    }

    // Öffnet das Modal zum Bearbeiten. Beim Bearbeiten gibt es keinen
    // "aus Vorlage"-Modus mehr - die Checkliste existiert ja schon mit
    // konkreten Items, ich editiere direkt diese Items.
    function handleChecklistBearbeiten(checklist: ChecklistDto) {
        setBearbeiteteChecklist(checklist);
        setErstellModus('manuell');
        setChecklistFormular({
            ticketId: checklist.ticketId,
            titel: checklist.titel,
            // Tiefe Kopie der Items, damit ich im Formular tippen kann,
            // ohne den bereits geladenen checklists-State zu verändern
            // (sonst würde React Änderungen im Formular sofort auch in
            // der Liste dahinter anzeigen, bevor gespeichert wurde).
            items: checklist.items.map((item) => ({ ...item })),
        });
        setChecklistFehler(null);
        setChecklistModalOffen(true);
    }

    // Fügt dem Formular ein weiteres leeres Item hinzu.
    function handleItemHinzufuegen() {
        setChecklistFormular({
            ...checklistFormular,
            items: [...checklistFormular.items, { id: null, beschreibung: '', erledigt: false }],
        });
    }

    // Entfernt ein Item aus dem Formular anhand seines Index. Ich lasse
    // das letzte verbleibende Item NICHT löschen, weil eine Checkliste
    // laut Backend (@NotEmpty) nie leer sein darf - der Button ist in
    // dem Fall deaktiviert (siehe JSX unten).
    function handleItemEntfernen(index: number) {
        setChecklistFormular({
            ...checklistFormular,
            items: checklistFormular.items.filter((_, i) => i !== index),
        });
    }

    // Ändert den Beschreibungstext eines Items im Formular.
    function handleItemTextAendern(index: number, neuerText: string) {
        const neueItems = [...checklistFormular.items];
        neueItems[index] = { ...neueItems[index], beschreibung: neuerText };
        setChecklistFormular({ ...checklistFormular, items: neueItems });
    }

    // Baukasten-Modus: schaltet einen einzelnen Punkt (identifiziert über
    // seinen "templateId:index"-Key) an/aus.
    function handleBausteinUmschalten(key: string) {
        setAusgewaehlteBausteine({
            ...ausgewaehlteBausteine,
            [key]: !ausgewaehlteBausteine[key],
        });
    }

    // Baut aus allen aktuell angehakten Bausteinen (über alle Vorlagen
    // hinweg) die fertige Item-Liste für die neue Checkliste. Die
    // Reihenfolge richtet sich nach der Reihenfolge der Vorlagen bzw.
    // der Punkte darin - das reicht hier aus, eine eigene Sortierfunktion
    // würde die Sache nur unnötig verkomplizieren.
    function baukastenItems(): ChecklistItemDto[] {
        const items: ChecklistItemDto[] = [];
        for (const template of templates) {
            template.itemBeschreibungen.forEach((beschreibung, index) => {
                const key = `${template.id}:${index}`;
                if (ausgewaehlteBausteine[key]) {
                    items.push({ id: null, beschreibung, erledigt: false });
                }
            });
        }
        return items;
    }

    // Anzahl der aktuell angehakten Bausteine - brauche ich, um den
    // Speichern-Button zu deaktivieren, solange noch nichts ausgewählt
    // ist (eine leere Checkliste lehnt das Backend ohnehin per
    // @NotEmpty ab, aber so bekommt die Person schon vorher eine klare
    // Rückmeldung statt erst nach einem Fehler vom Server).
    const baukastenAuswahlAnzahl = Object.values(ausgewaehlteBausteine).filter(Boolean).length;

    // Speichert die aktuell im Baukasten angehakten Punkte als EIGENE,
    // neue, benennbare Vorlage ab (über den normalen
    // POST /api/checklist-templates-Endpunkt) - unabhängig davon, ob ich
    // daraus gerade auch eine Checkliste erzeuge oder nicht. Der Grund:
    // Die Standard-Vorlagen sind jetzt vor dem Löschen geschützt, aber
    // eine eigene Zusammenstellung quer durch mehrere Vorlagen will ich
    // nicht jedes Mal neu zusammenklicken müssen - also sichere ich sie
    // mir hier unter dem eingetragenen Titel als wiederverwendbare,
    // eigene (also NICHT standard, also jederzeit wieder löschbare)
    // Vorlage. Den Namen nehme ich einfach aus dem Titel-Feld, das ich
    // für die Checkliste ohnehin schon ausfülle.
    async function handleBaukastenAlsVorlageSpeichern() {
        if (!checklistFormular.titel.trim() || baukastenAuswahlAnzahl === 0) {
            setChecklistFehler('Bitte einen Titel und mindestens einen Punkt angeben, bevor du als Vorlage speicherst.');
            return;
        }
        setSpeichernLaeuft(true);
        setChecklistFehler(null);
        setBaukastenSpeicherHinweis(null);
        try {
            await api.post<ChecklistTemplateDto>('/api/checklist-templates', {
                name: checklistFormular.titel,
                itemBeschreibungen: baukastenItems().map((item) => item.beschreibung),
            });
            await ladeTemplates();
            setBaukastenSpeicherHinweis(`Vorlage "${checklistFormular.titel}" wurde gespeichert.`);
        } catch (error) {
            setChecklistFehler('Vorlage konnte nicht gespeichert werden.');
            console.error(error);
        } finally {
            setSpeichernLaeuft(false);
        }
    }

    async function handleChecklistSpeichern() {
        setSpeichernLaeuft(true);
        setChecklistFehler(null);
        try {
            if (bearbeiteteChecklist) {
                await api.put<ChecklistDto>(`/api/checklists/${bearbeiteteChecklist.id}`, checklistFormular);
            } else if (erstellModus === 'vorlage') {
                await api.post<ChecklistDto>('/api/checklists/from-template', {
                    ticketId: checklistFormular.ticketId,
                    templateId: ausgewaehlteTemplateId,
                });
            } else if (erstellModus === 'baukasten') {
                // Eigene, aus mehreren Vorlagen zusammengestellte
                // Checkliste - dafür brauche ich keinen eigenen
                // Backend-Endpunkt, der normale POST /api/checklists
                // nimmt ja ohnehin beliebige Items entgegen (genau wie
                // im manuellen Modus), ich befülle sie hier nur anders.
                await api.post<ChecklistDto>('/api/checklists', {
                    ticketId: checklistFormular.ticketId,
                    titel: checklistFormular.titel,
                    items: baukastenItems(),
                });
            } else {
                await api.post<ChecklistDto>('/api/checklists', checklistFormular);
            }
            setChecklistModalOffen(false);
            await ladeChecklisten(ticketFilter);
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setChecklistFehler('Bitte Ticket, Titel und mindestens ein ausgefülltes Item angeben.');
            } else {
                setChecklistFehler('Checkliste konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSpeichernLaeuft(false);
        }
    }

    async function handleChecklistLoeschen(checklist: ChecklistDto) {
        if (!window.confirm(`Checkliste "${checklist.titel}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/checklists/${checklist.id}`);
            await ladeChecklisten(ticketFilter);
        } catch (error) {
            setLadeFehler('Checkliste konnte nicht gelöscht werden.');
            console.error(error);
        }
    }

    // Hakt ein einzelnes Item direkt in der Kartenansicht ab/aus, ohne
    // dass ich dafür das Bearbeiten-Modal öffnen muss - das ist der
    // häufigste Vorgang beim Abarbeiten einer Checkliste, der soll so
    // schnell wie möglich gehen. Ich baue die komplette Checkliste mit
    // dem umgeschalteten Item neu zusammen und schicke sie per PUT ans
    // Backend, das dabei automatisch abgeschlossenAm neu bewertet
    // (siehe ChecklistService.setzeAbschlussdatumWennAlleErledigt).
    async function handleItemUmschalten(checklist: ChecklistDto, item: ChecklistItemDto) {
        const aktualisierteItems = checklist.items.map((i) =>
            i.id === item.id ? { ...i, erledigt: !i.erledigt } : i,
        );
        try {
            await api.put<ChecklistDto>(`/api/checklists/${checklist.id}`, {
                ticketId: checklist.ticketId,
                titel: checklist.titel,
                items: aktualisierteItems,
            });
            await ladeChecklisten(ticketFilter);
        } catch (error) {
            setLadeFehler('Item konnte nicht aktualisiert werden.');
            console.error(error);
        }
    }

    // ---- Vorlagen-Tab: analoge Funktionen, aber ohne Ticket-Bezug ----

    function handleNeuesTemplate() {
        setBearbeitetesTemplate(null);
        setTemplateFormular(LEERES_TEMPLATE_FORMULAR);
        setTemplateFehler(null);
        setTemplateModalOffen(true);
    }

    function handleTemplateBearbeiten(template: ChecklistTemplateDto) {
        setBearbeitetesTemplate(template);
        setTemplateFormular({ name: template.name, itemBeschreibungen: [...template.itemBeschreibungen] });
        setTemplateFehler(null);
        setTemplateModalOffen(true);
    }

    function handleTemplateItemHinzufuegen() {
        setTemplateFormular({
            ...templateFormular,
            itemBeschreibungen: [...templateFormular.itemBeschreibungen, ''],
        });
    }

    function handleTemplateItemEntfernen(index: number) {
        setTemplateFormular({
            ...templateFormular,
            itemBeschreibungen: templateFormular.itemBeschreibungen.filter((_, i) => i !== index),
        });
    }

    function handleTemplateItemTextAendern(index: number, neuerText: string) {
        const neueBeschreibungen = [...templateFormular.itemBeschreibungen];
        neueBeschreibungen[index] = neuerText;
        setTemplateFormular({ ...templateFormular, itemBeschreibungen: neueBeschreibungen });
    }

    async function handleTemplateSpeichern() {
        setSpeichernLaeuft(true);
        setTemplateFehler(null);
        try {
            if (bearbeitetesTemplate) {
                await api.put<ChecklistTemplateDto>(`/api/checklist-templates/${bearbeitetesTemplate.id}`, templateFormular);
            } else {
                await api.post<ChecklistTemplateDto>('/api/checklist-templates', templateFormular);
            }
            setTemplateModalOffen(false);
            await ladeTemplates();
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setTemplateFehler('Bitte Namen und mindestens einen ausgefüllten Punkt angeben.');
            } else {
                setTemplateFehler('Vorlage konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSpeichernLaeuft(false);
        }
    }

    async function handleTemplateLoeschen(template: ChecklistTemplateDto) {
        if (!window.confirm(`Vorlage "${template.name}" wirklich löschen? Bereits erzeugte Checklisten bleiben erhalten.`)) {
            return;
        }
        try {
            await api.delete(`/api/checklist-templates/${template.id}`);
            await ladeTemplates();
        } catch (error) {
            setLadeFehler('Vorlage konnte nicht gelöscht werden.');
            console.error(error);
        }
    }

    if (checklistenLoading && templatesLoading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        );
    }

    // Welcher Eingabebereich im Modal sichtbar ist - als eigene Variablen statt einer
    // verschachtelten Bedingung im JSX (besser lesbar). Beim Bearbeiten gibt es nur
    // den manuellen Bereich.
    const zeigeVorlageModus = !bearbeiteteChecklist && erstellModus === 'vorlage';
    const zeigeBaukastenModus = !bearbeiteteChecklist && erstellModus === 'baukasten';

    return (
        <div className="py-4">
            <h1 className="mb-4">Checklisten</h1>

            {ladeFehler && <Alert variant="danger">{ladeFehler}</Alert>}

            {/* Ich verwende Tabs statt zweier separater Seiten, weil beide
            Bereiche eng zusammengehören (Vorlagen dienen nur dazu,
            Checklisten zu erzeugen) und ich so nicht extra zwischen
            Routen wechseln muss, um z.B. schnell eine neue Vorlage
            anzulegen, während ich gerade eine Checkliste erstelle. */}
            <Tabs defaultActiveKey="checklisten" className="mb-3">
                <Tab eventKey="checklisten" title="Checklisten">
                    <div className="d-flex justify-content-between align-items-center my-3">
                        <Form.Group style={{ maxWidth: 320 }}>
                            <Form.Label>Nach Ticket filtern</Form.Label>
                            <Form.Select value={ticketFilter} onChange={(e) => handleFilterChange(e.target.value)}>
                                <option value="">Alle Tickets</option>
                                {tickets.map((ticket) => (
                                    <option key={ticket.id} value={ticket.id}>
                                        {ticket.titel}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                        <Button variant="primary" onClick={handleNeueChecklist} disabled={tickets.length === 0}>
                            Neue Checkliste
                        </Button>
                    </div>

                    {tickets.length === 0 && (
                        <Alert variant="info">
                            Es existiert noch kein Ticket - lege zuerst ein Ticket an, bevor du Checklisten erfassen kannst.
                        </Alert>
                    )}

                    {checklists.length === 0 ? (
                        <Alert variant="dark" className="text-center">
                            {ticketFilter
                                ? `Für "${ticketTitel(ticketFilter)}" sind noch keine Checklisten erfasst.`
                                : 'Keine Checklisten vorhanden.'}
                        </Alert>
                    ) : (
                        // Eine Checkliste zeige ich als Card statt als
                        // Tabellenzeile, weil die Items selbst schon eine
                        // kleine Liste sind - das lässt sich in einer
                        // einzelnen Tabellenzelle kaum lesbar darstellen.
                        checklists.map((checklist) => (
                            <HudPanel key={checklist.id} title={checklist.titel} className="mb-3">
                                {/* Kopfzeile: zugehöriges Ticket + Status-Badge */}
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <span className="text-muted">{ticketTitel(checklist.ticketId)}</span>
                                    {checklist.abgeschlossenAm ? (
                                        <Badge bg="success">
                                            Abgeschlossen am {formatiereDatum(checklist.abgeschlossenAm)}
                                        </Badge>
                                    ) : (
                                        <Badge bg="secondary">Offen</Badge>
                                    )}
                                </div>
                                {/* Fortschrittsbalken: Anteil der erledigten Items */}
                                <ProgressBar
                                    className="hud-progress mb-3"
                                    now={fortschrittProzent(checklist.items)}
                                    aria-label={`Fortschritt ${checklist.titel}`}
                                />
                                <Form>
                                    {checklist.items.map((item) => (
                                        <Form.Check
                                            key={item.id}
                                            type="checkbox"
                                            id={`item-${item.id}`}
                                            label={item.beschreibung}
                                            checked={item.erledigt}
                                            // Ein Klick aufs Häkchen speichert sofort,
                                            // ohne Umweg über ein Modal - siehe
                                            // handleItemUmschalten oben.
                                            onChange={() => handleItemUmschalten(checklist, item)}
                                            className={item.erledigt ? 'text-decoration-line-through text-muted' : ''}
                                        />
                                    ))}
                                </Form>
                                <div className="d-flex gap-2 mt-3">
                                    <Button
                                        variant="outline-secondary"
                                        size="sm"
                                        onClick={() => handleChecklistBearbeiten(checklist)}
                                    >
                                        Bearbeiten
                                    </Button>
                                    <Button
                                        variant="outline-danger"
                                        size="sm"
                                        onClick={() => handleChecklistLoeschen(checklist)}
                                    >
                                        Löschen
                                    </Button>
                                </div>
                            </HudPanel>
                        ))
                    )}
                </Tab>

                <Tab eventKey="vorlagen" title="Vorlagen">
                    <div className="d-flex justify-content-end my-3">
                        <Button variant="primary" onClick={handleNeuesTemplate}>
                            Neue Vorlage
                        </Button>
                    </div>

                    {templates.length === 0 ? (
                        <Alert variant="dark" className="text-center">
                            Noch keine Vorlagen vorhanden.
                        </Alert>
                    ) : (
                        // Statt einer Tabelle mit einer riesigen Komma-Liste
                        // in einer einzigen Zelle (bei 20-28 Punkten pro
                        // Vorlage unlesbar) nehme ich ein Accordion: pro
                        // Vorlage eingeklappt nur Name + Anzahl Punkte,
                        // aufgeklappt die Punkte sauber nach Phase gruppiert.
                        <Accordion alwaysOpen>
                            {templates.map((template) => (
                                <Accordion.Item eventKey={template.id} key={template.id}>
                                    <Accordion.Header>
                                        <span className="flex-grow-1">{template.name}</span>
                                        {template.standard && (
                                            <Badge bg="info" className="me-2">
                                                Standard
                                            </Badge>
                                        )}
                                        <Badge bg="secondary" className="me-3">
                                            {template.itemBeschreibungen.length} Punkte
                                        </Badge>
                                    </Accordion.Header>
                                    <Accordion.Body>
                                        {gruppiereNachPhase(template.itemBeschreibungen).map((gruppe) => (
                                            <div key={gruppe.phase} className="mb-3">
                                                <div className="fw-bold mb-1">{gruppe.phase}</div>
                                                <ul className="mb-0">
                                                    {gruppe.punkte.map((punkt) => (
                                                        <li key={punkt.text}>
                                                            {punkt.text}
                                                            {punkt.optional && (
                                                                <Badge bg="secondary" className="ms-2">
                                                                    optional
                                                                </Badge>
                                                            )}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        ))}
                                        <div className="d-flex gap-2 align-items-center">
                                            <Button
                                                variant="outline-secondary"
                                                size="sm"
                                                onClick={() => handleTemplateBearbeiten(template)}
                                            >
                                                Bearbeiten
                                            </Button>
                                            {/* Standard-Vorlagen (vom Seeder angelegt)
                                            kann man inhaltlich noch bearbeiten, aber
                                            NICHT löschen - siehe
                                            ChecklistTemplateService.deleteTemplate im
                                            Backend, das lehnt das ohnehin ab. Der
                                            Löschen-Button ist hier deshalb konsequent
                                            gar nicht erst vorhanden, statt nur
                                            deaktiviert zu sein. */}
                                            {!template.standard && (
                                                <Button
                                                    variant="outline-danger"
                                                    size="sm"
                                                    onClick={() => handleTemplateLoeschen(template)}
                                                >
                                                    Löschen
                                                </Button>
                                            )}
                                        </div>
                                    </Accordion.Body>
                                </Accordion.Item>
                            ))}
                        </Accordion>
                    )}
                </Tab>
            </Tabs>

            {/* ---- Modal: Checkliste anlegen/bearbeiten ---- */}
            <Modal show={checklistModalOffen} onHide={() => setChecklistModalOffen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{bearbeiteteChecklist ? 'Checkliste bearbeiten' : 'Neue Checkliste'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {checklistFehler && <Alert variant="danger">{checklistFehler}</Alert>}
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Ticket</Form.Label>
                            <Form.Select
                                value={checklistFormular.ticketId}
                                onChange={(e) =>
                                    setChecklistFormular({ ...checklistFormular, ticketId: e.target.value })
                                }
                            >
                                {tickets.map((ticket) => (
                                    <option key={ticket.id} value={ticket.id}>
                                        {ticket.titel}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>

                        {/* Die Moduswahl zeige ich NUR beim Neuanlegen - eine
                        bestehende Checkliste hat bereits konkrete Items,
                        "aus Vorlage" würde diese ja komplett ersetzen, statt
                        sie zu ergänzen, was hier verwirrend wäre. */}
                        {!bearbeiteteChecklist && (
                            <Form.Group className="mb-3">
                                <Form.Label>Erstellen</Form.Label>
                                <div>
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-manuell"
                                        label="Manuell"
                                        checked={erstellModus === 'manuell'}
                                        onChange={() => setErstellModus('manuell')}
                                    />
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-vorlage"
                                        label="Aus Vorlage"
                                        checked={erstellModus === 'vorlage'}
                                        onChange={() => setErstellModus('vorlage')}
                                        disabled={templates.length === 0}
                                    />
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-baukasten"
                                        label="Baukasten"
                                        checked={erstellModus === 'baukasten'}
                                        onChange={() => setErstellModus('baukasten')}
                                        disabled={templates.length === 0}
                                    />
                                </div>
                            </Form.Group>
                        )}

                        {zeigeVorlageModus && (
                            // Vorlagen-Modus: ich brauche nur noch die Auswahl
                            // DER Vorlage, Titel und Items übernimmt das Backend
                            // 1:1 aus der Vorlage (ChecklistService.createChecklistFromTemplate).
                            <Form.Group className="mb-3">
                                <Form.Label>Vorlage</Form.Label>
                                <Form.Select
                                    value={ausgewaehlteTemplateId}
                                    onChange={(e) => setAusgewaehlteTemplateId(e.target.value)}
                                >
                                    {templates.map((template) => (
                                        <option key={template.id} value={template.id}>
                                            {template.name} ({template.itemBeschreibungen.length} Punkte)
                                        </option>
                                    ))}
                                </Form.Select>
                            </Form.Group>
                        )}

                        {zeigeBaukastenModus && (
                            // Baukasten-Modus: Titel tippe ich selbst ein (es
                            // gibt ja keine einzelne Vorlage mehr, deren Namen
                            // ich übernehmen könnte), darunter liste ich ALLE
                            // Vorlagen mit ihren Punkten als Checkboxen auf -
                            // so kann ich mir z.B. ein paar Security-Punkte
                            // mit ein paar Netzwerk-Punkten zusammen zu einer
                            // eigenen Checkliste zusammenklicken.
                            <>
                                <Form.Group className="mb-3">
                                    <Form.Label>Titel</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={checklistFormular.titel}
                                        onChange={(e) =>
                                            setChecklistFormular({ ...checklistFormular, titel: e.target.value })
                                        }
                                        placeholder="z.B. Wartung Kundenserver XY"
                                        required
                                    />
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>
                                        Punkte auswählen{' '}
                                        <span className="text-muted">({baukastenAuswahlAnzahl} ausgewählt)</span>
                                    </Form.Label>
                                    {/* Feste Höhe mit Scrollbalken, weil über alle
                                    zehn Standard-Vorlagen zusammen schnell über
                                    200 einzelne Punkte zusammenkommen - ohne das
                                    würde das Modal unbedienbar lang werden. */}
                                    <div style={{ maxHeight: '45vh', overflowY: 'auto' }} className="border rounded p-2">
                                        {templates.map((template) => (
                                            <div key={template.id} className="mb-3">
                                                <div className="fw-bold mb-1">{template.name}</div>
                                                {template.itemBeschreibungen.map((beschreibung, index) => {
                                                    const key = `${template.id}:${index}`;
                                                    // Hier zeige ich den rohen String bewusst
                                                    // geparst an (Phase + Text statt der
                                                    // eckigen Klammern) - genau wie in der
                                                    // Vorlagen-Übersicht, nur ohne Gruppierung,
                                                    // weil ich hier ohnehin pro Vorlage einen
                                                    // eigenen Block habe.
                                                    const { phase, text, optional } = parseBaustein(beschreibung);
                                                    return (
                                                        <Form.Check
                                                            key={key}
                                                            type="checkbox"
                                                            id={`baustein-${key}`}
                                                            label={
                                                                <>
                                                                    <span className="text-muted">[{phase}]</span> {text}
                                                                    {optional && (
                                                                        <Badge bg="secondary" className="ms-2">
                                                                            optional
                                                                        </Badge>
                                                                    )}
                                                                </>
                                                            }
                                                            checked={!!ausgewaehlteBausteine[key]}
                                                            onChange={() => handleBausteinUmschalten(key)}
                                                        />
                                                    );
                                                })}
                                            </div>
                                        ))}
                                    </div>
                                </Form.Group>

                                {baukastenSpeicherHinweis && (
                                    <Alert variant="success" className="py-2">
                                        {baukastenSpeicherHinweis}
                                    </Alert>
                                )}

                                {/* Eigenständige Aktion, getrennt vom
                                "Speichern"-Button im Footer: hiermit lege ich
                                NUR eine neue Vorlage aus der aktuellen Auswahl
                                an, ohne schon eine Checkliste zu erzeugen -
                                beides zusammen ("Vorlage speichern UND
                                Checkliste erzeugen") kann ich danach immer
                                noch über den normalen Speichern-Button machen. */}
                                <Button
                                    variant="outline-primary"
                                    size="sm"
                                    onClick={handleBaukastenAlsVorlageSpeichern}
                                    disabled={speichernLaeuft || baukastenAuswahlAnzahl === 0}
                                >
                                    Auswahl als eigene Vorlage speichern
                                </Button>
                            </>
                        )}

                        {!zeigeVorlageModus && !zeigeBaukastenModus && (
                            // Manueller Modus (oder Bearbeiten): Titel + eine
                            // dynamische Liste von Item-Textfeldern.
                            <>
                                <Form.Group className="mb-3">
                                    <Form.Label>Titel</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={checklistFormular.titel}
                                        onChange={(e) =>
                                            setChecklistFormular({ ...checklistFormular, titel: e.target.value })
                                        }
                                        required
                                    />
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>Items</Form.Label>
                                    {/* Ausgelagert in DynamischeItemListe, weil dieser
                                    Block (Text-Input je Eintrag + ✕ zum Entfernen +
                                    Button zum Hinzufügen) inhaltlich identisch zum
                                    Vorlagen-Punkte-Block unten im Vorlagen-Modal war -
                                    SonarQube hat das als Duplizierung markiert, und zu
                                    Recht: es ist derselbe UI-Baustein. Ich reiche hier
                                    nur die reinen Beschreibungstexte rein, die
                                    id/erledigt-Felder der Items bleiben unverändert,
                                    weil handleItemTextAendern/-Entfernen/-Hinzufuegen
                                    die vollständigen Items weiterhin selbst verwalten. */}
                                    <DynamischeItemListe
                                        werte={checklistFormular.items.map((item) => item.beschreibung)}
                                        onAendern={handleItemTextAendern}
                                        onEntfernen={handleItemEntfernen}
                                        onHinzufuegen={handleItemHinzufuegen}
                                    />
                                </Form.Group>
                            </>
                        )}
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setChecklistModalOffen(false)}>
                        Abbrechen
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleChecklistSpeichern}
                        disabled={
                            speichernLaeuft ||
                            (!bearbeiteteChecklist && erstellModus === 'baukasten' && baukastenAuswahlAnzahl === 0)
                        }
                    >
                        {speichernLaeuft ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* ---- Modal: Vorlage anlegen/bearbeiten ---- */}
            <Modal show={templateModalOffen} onHide={() => setTemplateModalOffen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{bearbeitetesTemplate ? 'Vorlage bearbeiten' : 'Neue Vorlage'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {templateFehler && <Alert variant="danger">{templateFehler}</Alert>}
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Name</Form.Label>
                            <Form.Control
                                type="text"
                                value={templateFormular.name}
                                onChange={(e) => setTemplateFormular({ ...templateFormular, name: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Punkte</Form.Label>
                            {/* Gleicher Baustein wie bei den Checklisten-Items oben -
                            siehe Kommentar dort. */}
                            <DynamischeItemListe
                                werte={templateFormular.itemBeschreibungen}
                                onAendern={handleTemplateItemTextAendern}
                                onEntfernen={handleTemplateItemEntfernen}
                                onHinzufuegen={handleTemplateItemHinzufuegen}
                            />
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setTemplateModalOffen(false)}>
                        Abbrechen
                    </Button>
                    <Button variant="primary" onClick={handleTemplateSpeichern} disabled={speichernLaeuft}>
                        {speichernLaeuft ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
