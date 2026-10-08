import { useEffect, useState } from 'react';
import { Accordion, Alert, Badge, Button, Form, Modal, Spinner, Tab, Tabs, ProgressBar } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import { formatDate } from '../utils/formatting';
import { DynamicItemList } from '../components/DynamicItemList';
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
function emptyChecklistForm(ticketId: string): ChecklistFormData {
    return {
        ticketId,
        title: '',
        items: [{ id: null, description: '', done: false }],
    };
}

const EMPTY_TEMPLATE_FORM: ChecklistTemplateFormData = {
    name: '',
    itemDescriptions: [''],
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
function parseBlock(description: string): { phase: string; text: string; optional: boolean } {
    const optional = description.endsWith(' (optional)');
    const withoutOptional = optional ? description.slice(0, -' (optional)'.length) : description;
    if (withoutOptional.startsWith('[')) {
        const ende = withoutOptional.indexOf(']');
        // ende > 1 stellt sicher, dass zwischen den Klammern mindestens
        // ein Zeichen steht ("[]" zählt nicht als Phase).
        if (ende > 1) {
            return { phase: withoutOptional.slice(1, ende), text: withoutOptional.slice(ende + 1).trim(), optional };
        }
    }
    return { phase: 'Sonstiges', text: withoutOptional, optional };
}

// Gruppiert die Punkte einer Vorlage nach Phase, in der Reihenfolge,
// in der die Phasen zum ersten Mal auftauchen (nicht alphabetisch) -
// das entspricht dem natürlichen Ablauf (Vorbereitung vor
// Installation vor Abnahme usw.), den ich mir beim Erstellen der
// Vorlagen schon überlegt habe.
function groupByPhase(itemDescriptions: string[]): { phase: string; entries: { text: string; optional: boolean }[] }[] {
    const groups: { phase: string; entries: { text: string; optional: boolean }[] }[] = [];
    for (const description of itemDescriptions) {
        const { phase, text, optional } = parseBlock(description);
        let group = groups.find((g) => g.phase === phase);
        if (!group) {
            group = { phase, entries: [] };
            groups.push(group);
        }
        group.entries.push({ text, optional });
    }
    return groups;
}

// Seite für den Bereich "Checklisten" (entspricht ChecklistController +
// ChecklistTemplateController im Backend). Ich bilde beide Bereiche
// als zwei Tabs EINER Seite ab, weil sie thematisch zusammengehören
// (Vorlagen existieren nur, um daraus Checklisten zu erzeugen), aber
// datentechnisch komplett unabhängig sind (verschiedene Endpunkte,
// verschiedene Services).
// Anteil der erledigten Items in Prozent (0 bei leerer Liste, damit ich nicht durch 0 teile).
function progressPercent(items: { done: boolean }[]): number {
    if (items.length === 0) return 0;
    return (items.filter((item) => item.done).length / items.length) * 100;
}

export function ChecklistsPage() {
    // ---- gemeinsame Daten ----
    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [loadError, setLoadError] = useState<string | null>(null);

    // ---- Tab 1: Checklisten ----
    const [checklists, setChecklists] = useState<ChecklistDto[]>([]);
    const [templates, setTemplates] = useState<ChecklistTemplateDto[]>([]);
    const [checklistsLoading, setChecklistsLoading] = useState(true);
    const [ticketFilter, setTicketFilter] = useState('');

    const [checklistModalOpen, setChecklistModalOpen] = useState(false);
    // 'manuell' = Items werden im Formular selbst eingetragen,
    // 'vorlage' = Items kommen 1:1 aus einer ausgewählten Vorlage,
    // 'baukasten' = ich picke mir einzelne Punkte aus MEHREREN Vorlagen
    // zusammen (statt nur eine komplette Vorlage als Ganzes zu
    // übernehmen) und baue mir daraus meine eigene, gemischte
    // Checkliste. Nur beim NEUANLEGEN relevant - beim Bearbeiten einer
    // bestehenden Checkliste editiere ich immer direkt die konkreten
    // Items.
    const [createMode, setCreateMode] = useState<'manuell' | 'vorlage' | 'baukasten'>('manuell');
    const [editedChecklist, setEditedChecklist] = useState<ChecklistDto | null>(null);
    const [checklistForm, setChecklistForm] = useState<ChecklistFormData>(emptyChecklistForm(''));
    const [selectedTemplateId, setSelectedTemplateId] = useState('');
    // Für den Baukasten-Modus: welche Punkte (aus welcher Vorlage) sind
    // angehakt. Als Key nehme ich "templateId:index" statt nur den Text,
    // damit ich gleichlautende Punkte in verschiedenen Vorlagen (oder
    // sogar doppelte Einträge innerhalb einer Vorlage) sauber
    // auseinanderhalten kann.
    const [selectedBlocks, setSelectedBlocks] = useState<Record<string, boolean>>({});
    // Kurze Erfolgsmeldung nach "Als eigene Vorlage speichern" im
    // Baukasten-Modus - getrennt von checklistFehler, weil beides
    // gleichzeitig sichtbar sein könnte (z.B. Vorlage erfolgreich
    // gespeichert, aber die Checkliste selbst dann doch nicht erzeugt).
    const [builderSaveHint, setBuilderSaveHint] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [checklistError, setChecklistError] = useState<string | null>(null);

    // ---- Tab 2: Vorlagen ----
    const [templatesLoading, setTemplatesLoading] = useState(true);
    const [templateModalOpen, setTemplateModalOpen] = useState(false);
    const [editedTemplate, setEditedTemplate] = useState<ChecklistTemplateDto | null>(null);
    const [templateForm, setTemplateForm] = useState<ChecklistTemplateFormData>(EMPTY_TEMPLATE_FORM);
    const [templateError, setTemplateError] = useState<string | null>(null);

    // Lädt die Checklisten, optional gefiltert nach ticketId - gleiches
    // Muster wie ladeTasks() in der TaskPlannerPage.
    async function loadChecklists(ticketId: string) {
        try {
            const path = ticketId ? `/api/checklists?ticketId=${ticketId}` : '/api/checklists';
            const loadedChecklists = await api.get<ChecklistDto[]>(path);
            setChecklists(loadedChecklists);
        } catch (error) {
            setLoadError('Checklisten konnten nicht geladen werden.');
            console.error(error);
        } finally {
            setChecklistsLoading(false);
        }
    }

    async function loadTemplates() {
        try {
            const loadedTemplates = await api.get<ChecklistTemplateDto[]>('/api/checklist-templates');
            setTemplates(loadedTemplates);
        } catch (error) {
            setLoadError('Checklisten-Vorlagen konnten nicht geladen werden.');
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
        let aborted = false;
        api.get<TicketDto[]>('/api/tickets').then(setTickets).catch(console.error);
        api.get<ChecklistDto[]>('/api/checklists')
            .then((loadedChecklists) => {
                if (!aborted) setChecklists(loadedChecklists);
            })
            .catch((error) => {
                if (aborted) return;
                setLoadError('Checklisten konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setChecklistsLoading(false);
            });
        api.get<ChecklistTemplateDto[]>('/api/checklist-templates')
            .then((loadedTemplates) => {
                if (!aborted) setTemplates(loadedTemplates);
            })
            .catch((error) => {
                if (aborted) return;
                setLoadError('Checklisten-Vorlagen konnten nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setTemplatesLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, []);

    function ticketTitle(ticketId: string): string {
        return tickets.find((t) => t.id === ticketId)?.title ?? '(unbekanntes Ticket)';
    }

    function handleFilterChange(ticketId: string) {
        setTicketFilter(ticketId);
        setChecklistsLoading(true);
        // void markiert explizit, dass ich das Promise bewusst nicht
        // abwarte - ladeChecklisten fängt seine Fehler intern selbst ab.
        void loadChecklists(ticketId);
    }

    // Öffnet das Modal zum Neuanlegen. Ich setze den Modus zurück auf
    // 'manuell' als Standard und übernehme - wie beim TaskPlanner - den
    // aktiven Ticket-Filter als Vorauswahl, falls einer gesetzt ist.
    function handleNewChecklist() {
        setEditedChecklist(null);
        setCreateMode('manuell');
        setChecklistForm(emptyChecklistForm(ticketFilter || tickets[0]?.id || ''));
        setSelectedTemplateId(templates[0]?.id ?? '');
        // Baukasten-Auswahl bei jedem neuen Anlegen zurücksetzen, sonst
        // wären beim nächsten Öffnen noch Häkchen von vorher gesetzt.
        setSelectedBlocks({});
        setBuilderSaveHint(null);
        setChecklistError(null);
        setChecklistModalOpen(true);
    }

    // Öffnet das Modal zum Bearbeiten. Beim Bearbeiten gibt es keinen
    // "aus Vorlage"-Modus mehr - die Checkliste existiert ja schon mit
    // konkreten Items, ich editiere direkt diese Items.
    function handleChecklistEdit(checklist: ChecklistDto) {
        setEditedChecklist(checklist);
        setCreateMode('manuell');
        setChecklistForm({
            ticketId: checklist.ticketId,
            title: checklist.title,
            // Tiefe Kopie der Items, damit ich im Formular tippen kann,
            // ohne den bereits geladenen checklists-State zu verändern
            // (sonst würde React Änderungen im Formular sofort auch in
            // der Liste dahinter anzeigen, bevor gespeichert wurde).
            items: checklist.items.map((item) => ({ ...item })),
        });
        setChecklistError(null);
        setChecklistModalOpen(true);
    }

    // Fügt dem Formular ein weiteres leeres Item hinzu.
    function handleItemAdd() {
        setChecklistForm({
            ...checklistForm,
            items: [...checklistForm.items, { id: null, description: '', done: false }],
        });
    }

    // Entfernt ein Item aus dem Formular anhand seines Index. Ich lasse
    // das letzte verbleibende Item NICHT löschen, weil eine Checkliste
    // laut Backend (@NotEmpty) nie leer sein darf - der Button ist in
    // dem Fall deaktiviert (siehe JSX unten).
    function handleItemRemove(index: number) {
        setChecklistForm({
            ...checklistForm,
            items: checklistForm.items.filter((_, i) => i !== index),
        });
    }

    // Ändert den Beschreibungstext eines Items im Formular.
    function handleItemTextChange(index: number, newText: string) {
        const newItems = [...checklistForm.items];
        newItems[index] = { ...newItems[index], description: newText };
        setChecklistForm({ ...checklistForm, items: newItems });
    }

    // Baukasten-Modus: schaltet einen einzelnen Punkt (identifiziert über
    // seinen "templateId:index"-Key) an/aus.
    function handleBlockToggle(key: string) {
        setSelectedBlocks({
            ...selectedBlocks,
            [key]: !selectedBlocks[key],
        });
    }

    // Baut aus allen aktuell angehakten Bausteinen (über alle Vorlagen
    // hinweg) die fertige Item-Liste für die neue Checkliste. Die
    // Reihenfolge richtet sich nach der Reihenfolge der Vorlagen bzw.
    // der Punkte darin - das reicht hier aus, eine eigene Sortierfunktion
    // würde die Sache nur unnötig verkomplizieren.
    function builderItems(): ChecklistItemDto[] {
        const items: ChecklistItemDto[] = [];
        for (const template of templates) {
            template.itemDescriptions.forEach((description, index) => {
                const key = `${template.id}:${index}`;
                if (selectedBlocks[key]) {
                    items.push({ id: null, description, done: false });
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
    const builderSelectionCount = Object.values(selectedBlocks).filter(Boolean).length;

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
    async function handleSaveBuilderAsTemplate() {
        if (!checklistForm.title.trim() || builderSelectionCount === 0) {
            setChecklistError('Bitte einen Titel und mindestens einen Punkt angeben, bevor du als Vorlage speicherst.');
            return;
        }
        setSaving(true);
        setChecklistError(null);
        setBuilderSaveHint(null);
        try {
            await api.post<ChecklistTemplateDto>('/api/checklist-templates', {
                name: checklistForm.title,
                itemDescriptions: builderItems().map((item) => item.description),
            });
            await loadTemplates();
            setBuilderSaveHint(`Vorlage "${checklistForm.title}" wurde gespeichert.`);
        } catch (error) {
            setChecklistError('Vorlage konnte nicht gespeichert werden.');
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleChecklistSave() {
        setSaving(true);
        setChecklistError(null);
        try {
            if (editedChecklist) {
                await api.put<ChecklistDto>(`/api/checklists/${editedChecklist.id}`, checklistForm);
            } else if (createMode === 'vorlage') {
                await api.post<ChecklistDto>('/api/checklists/from-template', {
                    ticketId: checklistForm.ticketId,
                    templateId: selectedTemplateId,
                });
            } else if (createMode === 'baukasten') {
                // Eigene, aus mehreren Vorlagen zusammengestellte
                // Checkliste - dafür brauche ich keinen eigenen
                // Backend-Endpunkt, der normale POST /api/checklists
                // nimmt ja ohnehin beliebige Items entgegen (genau wie
                // im manuellen Modus), ich befülle sie hier nur anders.
                await api.post<ChecklistDto>('/api/checklists', {
                    ticketId: checklistForm.ticketId,
                    title: checklistForm.title,
                    items: builderItems(),
                });
            } else {
                await api.post<ChecklistDto>('/api/checklists', checklistForm);
            }
            setChecklistModalOpen(false);
            await loadChecklists(ticketFilter);
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setChecklistError('Bitte Ticket, Titel und mindestens ein ausgefülltes Item angeben.');
            } else {
                setChecklistError('Checkliste konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleChecklistDelete(checklist: ChecklistDto) {
        if (!window.confirm(`Checkliste "${checklist.title}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/checklists/${checklist.id}`);
            await loadChecklists(ticketFilter);
        } catch (error) {
            setLoadError('Checkliste konnte nicht gelöscht werden.');
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
    async function handleItemToggle(checklist: ChecklistDto, item: ChecklistItemDto) {
        const updatedItems = checklist.items.map((i) =>
            i.id === item.id ? { ...i, done: !i.done } : i,
        );
        try {
            await api.put<ChecklistDto>(`/api/checklists/${checklist.id}`, {
                ticketId: checklist.ticketId,
                title: checklist.title,
                items: updatedItems,
            });
            await loadChecklists(ticketFilter);
        } catch (error) {
            setLoadError('Item konnte nicht aktualisiert werden.');
            console.error(error);
        }
    }

    // ---- Vorlagen-Tab: analoge Funktionen, aber ohne Ticket-Bezug ----

    function handleNewTemplate() {
        setEditedTemplate(null);
        setTemplateForm(EMPTY_TEMPLATE_FORM);
        setTemplateError(null);
        setTemplateModalOpen(true);
    }

    function handleTemplateEdit(template: ChecklistTemplateDto) {
        setEditedTemplate(template);
        setTemplateForm({ name: template.name, itemDescriptions: [...template.itemDescriptions] });
        setTemplateError(null);
        setTemplateModalOpen(true);
    }

    function handleTemplateItemAdd() {
        setTemplateForm({
            ...templateForm,
            itemDescriptions: [...templateForm.itemDescriptions, ''],
        });
    }

    function handleTemplateItemRemove(index: number) {
        setTemplateForm({
            ...templateForm,
            itemDescriptions: templateForm.itemDescriptions.filter((_, i) => i !== index),
        });
    }

    function handleTemplateItemTextChange(index: number, newText: string) {
        const newDescriptions = [...templateForm.itemDescriptions];
        newDescriptions[index] = newText;
        setTemplateForm({ ...templateForm, itemDescriptions: newDescriptions });
    }

    async function handleTemplateSave() {
        setSaving(true);
        setTemplateError(null);
        try {
            if (editedTemplate) {
                await api.put<ChecklistTemplateDto>(`/api/checklist-templates/${editedTemplate.id}`, templateForm);
            } else {
                await api.post<ChecklistTemplateDto>('/api/checklist-templates', templateForm);
            }
            setTemplateModalOpen(false);
            await loadTemplates();
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setTemplateError('Bitte Namen und mindestens einen ausgefüllten Punkt angeben.');
            } else {
                setTemplateError('Vorlage konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleTemplateDelete(template: ChecklistTemplateDto) {
        if (!window.confirm(`Vorlage "${template.name}" wirklich löschen? Bereits erzeugte Checklisten bleiben erhalten.`)) {
            return;
        }
        try {
            await api.delete(`/api/checklist-templates/${template.id}`);
            await loadTemplates();
        } catch (error) {
            setLoadError('Vorlage konnte nicht gelöscht werden.');
            console.error(error);
        }
    }

    if (checklistsLoading && templatesLoading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        );
    }

    // Welcher Eingabebereich im Modal sichtbar ist - als eigene Variablen statt einer
    // verschachtelten Bedingung im JSX (besser lesbar). Beim Bearbeiten gibt es nur
    // den manuellen Bereich.
    const showTemplateMode = !editedChecklist && createMode === 'vorlage';
    const showBuilderMode = !editedChecklist && createMode === 'baukasten';

    return (
        <div className="py-4">
            <h1 className="mb-4">Checklisten</h1>

            {loadError && <Alert variant="danger">{loadError}</Alert>}

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
                                        {ticket.title}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                        <Button variant="primary" onClick={handleNewChecklist} disabled={tickets.length === 0}>
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
                                ? `Für "${ticketTitle(ticketFilter)}" sind noch keine Checklisten erfasst.`
                                : 'Keine Checklisten vorhanden.'}
                        </Alert>
                    ) : (
                        // Eine Checkliste zeige ich als Card statt als
                        // Tabellenzeile, weil die Items selbst schon eine
                        // kleine Liste sind - das lässt sich in einer
                        // einzelnen Tabellenzelle kaum lesbar darstellen.
                        checklists.map((checklist) => (
                            <HudPanel key={checklist.id} title={checklist.title} className="mb-3">
                                {/* Kopfzeile: zugehöriges Ticket + Status-Badge */}
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <span className="text-muted">{ticketTitle(checklist.ticketId)}</span>
                                    {checklist.completedAt ? (
                                        <Badge bg="success">
                                            Abgeschlossen am {formatDate(checklist.completedAt)}
                                        </Badge>
                                    ) : (
                                        <Badge bg="secondary">Offen</Badge>
                                    )}
                                </div>
                                {/* Fortschrittsbalken: Anteil der erledigten Items */}
                                <ProgressBar
                                    className="hud-progress mb-3"
                                    now={progressPercent(checklist.items)}
                                    aria-label={`Fortschritt ${checklist.title}`}
                                />
                                <Form>
                                    {checklist.items.map((item) => (
                                        <Form.Check
                                            key={item.id}
                                            type="checkbox"
                                            id={`item-${item.id}`}
                                            label={item.description}
                                            checked={item.done}
                                            // Ein Klick aufs Häkchen speichert sofort,
                                            // ohne Umweg über ein Modal - siehe
                                            // handleItemUmschalten oben.
                                            onChange={() => handleItemToggle(checklist, item)}
                                            className={item.done ? 'text-decoration-line-through text-muted' : ''}
                                        />
                                    ))}
                                </Form>
                                <div className="d-flex gap-2 mt-3">
                                    <Button
                                        variant="outline-secondary"
                                        size="sm"
                                        onClick={() => handleChecklistEdit(checklist)}
                                    >
                                        Bearbeiten
                                    </Button>
                                    <Button
                                        variant="outline-danger"
                                        size="sm"
                                        onClick={() => handleChecklistDelete(checklist)}
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
                        <Button variant="primary" onClick={handleNewTemplate}>
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
                                        {template.builtIn && (
                                            <Badge bg="info" className="me-2">
                                                Standard
                                            </Badge>
                                        )}
                                        <Badge bg="secondary" className="me-3">
                                            {template.itemDescriptions.length} Punkte
                                        </Badge>
                                    </Accordion.Header>
                                    <Accordion.Body>
                                        {groupByPhase(template.itemDescriptions).map((group) => (
                                            <div key={group.phase} className="mb-3">
                                                <div className="fw-bold mb-1">{group.phase}</div>
                                                <ul className="mb-0">
                                                    {group.entries.map((entry) => (
                                                        <li key={entry.text}>
                                                            {entry.text}
                                                            {entry.optional && (
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
                                                onClick={() => handleTemplateEdit(template)}
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
                                            {!template.builtIn && (
                                                <Button
                                                    variant="outline-danger"
                                                    size="sm"
                                                    onClick={() => handleTemplateDelete(template)}
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
            <Modal show={checklistModalOpen} onHide={() => setChecklistModalOpen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{editedChecklist ? 'Checkliste bearbeiten' : 'Neue Checkliste'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {checklistError && <Alert variant="danger">{checklistError}</Alert>}
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Ticket</Form.Label>
                            <Form.Select
                                value={checklistForm.ticketId}
                                onChange={(e) =>
                                    setChecklistForm({ ...checklistForm, ticketId: e.target.value })
                                }
                            >
                                {tickets.map((ticket) => (
                                    <option key={ticket.id} value={ticket.id}>
                                        {ticket.title}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>

                        {/* Die Moduswahl zeige ich NUR beim Neuanlegen - eine
                        bestehende Checkliste hat bereits konkrete Items,
                        "aus Vorlage" würde diese ja komplett ersetzen, statt
                        sie zu ergänzen, was hier verwirrend wäre. */}
                        {!editedChecklist && (
                            <Form.Group className="mb-3">
                                <Form.Label>Erstellen</Form.Label>
                                <div>
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-manuell"
                                        label="Manuell"
                                        checked={createMode === 'manuell'}
                                        onChange={() => setCreateMode('manuell')}
                                    />
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-vorlage"
                                        label="Aus Vorlage"
                                        checked={createMode === 'vorlage'}
                                        onChange={() => setCreateMode('vorlage')}
                                        disabled={templates.length === 0}
                                    />
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-baukasten"
                                        label="Baukasten"
                                        checked={createMode === 'baukasten'}
                                        onChange={() => setCreateMode('baukasten')}
                                        disabled={templates.length === 0}
                                    />
                                </div>
                            </Form.Group>
                        )}

                        {showTemplateMode && (
                            // Vorlagen-Modus: ich brauche nur noch die Auswahl
                            // DER Vorlage, Titel und Items übernimmt das Backend
                            // 1:1 aus der Vorlage (ChecklistService.createChecklistFromTemplate).
                            <Form.Group className="mb-3">
                                <Form.Label>Vorlage</Form.Label>
                                <Form.Select
                                    value={selectedTemplateId}
                                    onChange={(e) => setSelectedTemplateId(e.target.value)}
                                >
                                    {templates.map((template) => (
                                        <option key={template.id} value={template.id}>
                                            {template.name} ({template.itemDescriptions.length} Punkte)
                                        </option>
                                    ))}
                                </Form.Select>
                            </Form.Group>
                        )}

                        {showBuilderMode && (
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
                                        value={checklistForm.title}
                                        onChange={(e) =>
                                            setChecklistForm({ ...checklistForm, title: e.target.value })
                                        }
                                        placeholder="z.B. Wartung Kundenserver XY"
                                        required
                                    />
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>
                                        Punkte auswählen{' '}
                                        <span className="text-muted">({builderSelectionCount} ausgewählt)</span>
                                    </Form.Label>
                                    {/* Feste Höhe mit Scrollbalken, weil über alle
                                    zehn Standard-Vorlagen zusammen schnell über
                                    200 einzelne Punkte zusammenkommen - ohne das
                                    würde das Modal unbedienbar lang werden. */}
                                    <div style={{ maxHeight: '45vh', overflowY: 'auto' }} className="border rounded p-2">
                                        {templates.map((template) => (
                                            <div key={template.id} className="mb-3">
                                                <div className="fw-bold mb-1">{template.name}</div>
                                                {template.itemDescriptions.map((description, index) => {
                                                    const key = `${template.id}:${index}`;
                                                    // Hier zeige ich den rohen String bewusst
                                                    // geparst an (Phase + Text statt der
                                                    // eckigen Klammern) - genau wie in der
                                                    // Vorlagen-Übersicht, nur ohne Gruppierung,
                                                    // weil ich hier ohnehin pro Vorlage einen
                                                    // eigenen Block habe.
                                                    const { phase, text, optional } = parseBlock(description);
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
                                                            checked={!!selectedBlocks[key]}
                                                            onChange={() => handleBlockToggle(key)}
                                                        />
                                                    );
                                                })}
                                            </div>
                                        ))}
                                    </div>
                                </Form.Group>

                                {builderSaveHint && (
                                    <Alert variant="success" className="py-2">
                                        {builderSaveHint}
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
                                    onClick={handleSaveBuilderAsTemplate}
                                    disabled={saving || builderSelectionCount === 0}
                                >
                                    Auswahl als eigene Vorlage speichern
                                </Button>
                            </>
                        )}

                        {!showTemplateMode && !showBuilderMode && (
                            // Manueller Modus (oder Bearbeiten): Titel + eine
                            // dynamische Liste von Item-Textfeldern.
                            <>
                                <Form.Group className="mb-3">
                                    <Form.Label>Titel</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={checklistForm.title}
                                        onChange={(e) =>
                                            setChecklistForm({ ...checklistForm, title: e.target.value })
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
                                    <DynamicItemList
                                        values={checklistForm.items.map((item) => item.description)}
                                        onItemChange={handleItemTextChange}
                                        onItemRemove={handleItemRemove}
                                        onItemAdd={handleItemAdd}
                                    />
                                </Form.Group>
                            </>
                        )}
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setChecklistModalOpen(false)}>
                        Abbrechen
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleChecklistSave}
                        disabled={
                            saving ||
                            (!editedChecklist && createMode === 'baukasten' && builderSelectionCount === 0)
                        }
                    >
                        {saving ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* ---- Modal: Vorlage anlegen/bearbeiten ---- */}
            <Modal show={templateModalOpen} onHide={() => setTemplateModalOpen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{editedTemplate ? 'Vorlage bearbeiten' : 'Neue Vorlage'}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {templateError && <Alert variant="danger">{templateError}</Alert>}
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Name</Form.Label>
                            <Form.Control
                                type="text"
                                value={templateForm.name}
                                onChange={(e) => setTemplateForm({ ...templateForm, name: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Punkte</Form.Label>
                            {/* Gleicher Baustein wie bei den Checklisten-Items oben -
                            siehe Kommentar dort. */}
                            <DynamicItemList
                                values={templateForm.itemDescriptions}
                                onItemChange={handleTemplateItemTextChange}
                                onItemRemove={handleTemplateItemRemove}
                                onItemAdd={handleTemplateItemAdd}
                            />
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setTemplateModalOpen(false)}>
                        Abbrechen
                    </Button>
                    <Button variant="primary" onClick={handleTemplateSave} disabled={saving}>
                        {saving ? 'Speichere…' : 'Speichern'}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
