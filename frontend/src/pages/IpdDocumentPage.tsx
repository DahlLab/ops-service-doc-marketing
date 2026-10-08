import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Col, Form, Row, Spinner } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { useNavigate, useParams } from 'react-router-dom';
import { api, ApiError } from '../api/api';
import { ladeDateiHerunter } from '../utils/download';
import { formatiereDatum, ipdStatusBadgeVariante } from '../utils/formatierung';
import {
    IPD_DOCUMENT_STATUS_LABELS,
    SZENARIO_TYP_LABELS,
    type IpdDocumentDto,
    type IpdDocumentFormData,
    type IpdDocumentStatus,
} from '../api/types';

// Baut aus einem geladenen IpdDocumentDto genau die Felder, die ich im
// Formular bearbeite - die automatisch verwalteten Felder (ticketId,
// techniker, szenarioTyp, durchgefuehrteSchritte,
// qualitaetssicherungAbgeschlossen, erstelltAm, aktualisiertAm) zeige ich
// separat read-only an, siehe JSX unten.
function formularAusDokument(dokument: IpdDocumentDto): IpdDocumentFormData {
    return {
        titel: dokument.titel,
        kunde: dokument.kunde,
        ansprechpartnerKunde: dokument.ansprechpartnerKunde,
        zeitraum: dokument.zeitraum,
        ausgangslage: dokument.ausgangslage,
        anforderungen: dokument.anforderungen,
        infrastrukturUebersicht: dokument.infrastrukturUebersicht,
        serverUndVms: dokument.serverUndVms,
        netzwerk: dokument.netzwerk,
        rollenUndVerantwortlichkeiten: dokument.rollenUndVerantwortlichkeiten,
        backupKonzept: dokument.backupKonzept,
        securityUeberlegungen: dokument.securityUeberlegungen,
        entscheidungen: dokument.entscheidungen,
        risikenUndAnnahmen: dokument.risikenUndAnnahmen,
        rollbackPlan: dokument.rollbackPlan,
        status: dokument.status,
    };
}

// Die "Ergebnis"-Seite aus meiner geplanten Reihenfolge: hier fülle ich
// die vom Backend beim Erzeugen leer gelassenen Abschnitte eines
// IPD-Dokuments aus, wechsle den Status auf ABGESCHLOSSEN und lade am
// Ende das fertige PDF herunter. Erreichbar über /ipd/:id, z.B. direkt
// nach dem Erzeugen in IpdGeneratorPage oder über "Öffnen" in dessen
// Tabelle.
export function IpdDocumentPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [dokument, setDokument] = useState<IpdDocumentDto | null>(null);
    const [formular, setFormular] = useState<IpdDocumentFormData | null>(null);
    const [loading, setLoading] = useState(true);
    const [speichernLaeuft, setSpeichernLaeuft] = useState(false);
    const [pdfLaeuft, setPdfLaeuft] = useState(false);
    const [checklisteLaeuft, setChecklisteLaeuft] = useState(false);
    const [fehler, setFehler] = useState<string | null>(null);
    // Separate Erfolgsmeldung statt nur eines Fehlerfelds, damit ich dem
    // Nutzer nach dem Speichern kurz sichtbares Feedback geben kann,
    // ohne auf eine neue Seite zu springen (anders als bei Erzeugen, wo
    // ich direkt hierher navigiere, bleibe ich nach dem Speichern bewusst
    // auf dieser Seite, weil man idR mehrere Abschnitte nacheinander
    // ergänzt).
    const [erfolg, setErfolg] = useState<string | null>(null);

    // Dokument beim Öffnen (und bei geänderter id) laden: Anfrage direkt im Effect,
    // State nur im Callback. `abgebrochen` verhindert, dass eine späte Antwort eines
    // alten Dokuments das neue überschreibt.
    useEffect(() => {
        if (!id) return;
        let abgebrochen = false;
        api.get<IpdDocumentDto>(`/api/ipd/${id}`)
            .then((geladenesDokument) => {
                if (abgebrochen) return;
                setDokument(geladenesDokument);
                setFormular(formularAusDokument(geladenesDokument));
                setFehler(null);
            })
            .catch((error) => {
                if (abgebrochen) return;
                setFehler('IPD-Dokument konnte nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!abgebrochen) setLoading(false);
            });
        return () => {
            abgebrochen = true;
        };
    }, [id]);

    // Generischer Change-Handler für alle Textarea-Felder: erspart mir
    // 14 fast identische onChange-Funktionen für jedes Formularfeld.
    function handleFeldAendern(feld: keyof IpdDocumentFormData, wert: string) {
        if (!formular) return;
        setFormular({ ...formular, [feld]: wert || null });
    }

    async function handleSpeichern() {
        if (!dokument || !formular) return;
        setSpeichernLaeuft(true);
        setFehler(null);
        setErfolg(null);
        try {
            // Das Backend erwartet den VOLLSTÄNDIGEN IpdDocumentDto als
            // Request-Body (auch wenn es einen Großteil der Felder
            // ohnehin ignoriert/neu berechnet, siehe
            // IpdDocumentService.updateIpdDocument) - deshalb baue ich
            // hier aus dem zuletzt geladenen Dokument plus meinen
            // Formular-Änderungen wieder ein komplettes Objekt
            // zusammen, statt nur die editierbaren Felder zu schicken.
            const aktualisiertesDokument = await api.put<IpdDocumentDto>(`/api/ipd/${dokument.id}`, {
                ...dokument,
                ...formular,
            });
            setDokument(aktualisiertesDokument);
            setFormular(formularAusDokument(aktualisiertesDokument));
            setErfolg('Gespeichert.');
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setFehler('Bitte prüfe deine Eingaben - der Titel darf z.B. nicht leer sein.');
            } else {
                setFehler('IPD-Dokument konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSpeichernLaeuft(false);
        }
    }

    // Lädt das Kunden-PDF herunter (siehe utils/download.ts für das
    // Blob/ObjectURL-Verfahren).
    async function handlePdfHerunterladen() {
        if (!dokument) return;
        setPdfLaeuft(true);
        setFehler(null);
        try {
            await ladeDateiHerunter(`/api/ipd/${dokument.id}/pdf`, `ipd-${dokument.id}.pdf`);
        } catch (error) {
            setFehler('PDF konnte nicht erzeugt werden.');
            console.error(error);
        } finally {
            setPdfLaeuft(false);
        }
    }

    // Lädt die interne Checkliste (Technikerversion mit ausfüllbaren
    // Checkboxen) herunter - getrennt vom Kunden-PDF. 404 heißt: zu diesem
    // Ticket gibt es (noch) keine Checkliste.
    async function handleChecklisteHerunterladen() {
        if (!dokument) return;
        setChecklisteLaeuft(true);
        setFehler(null);
        try {
            await ladeDateiHerunter(`/api/ipd/${dokument.id}/checklist-pdf`, `checkliste-${dokument.id}.pdf`);
        } catch (error) {
            if (error instanceof ApiError && error.status === 404) {
                setFehler('Zu diesem Dokument gibt es noch keine Checkliste.');
            } else {
                setFehler('Checkliste konnte nicht erzeugt werden.');
            }
            console.error(error);
        } finally {
            setChecklisteLaeuft(false);
        }
    }

    async function handleLoeschen() {
        if (!dokument) return;
        if (!window.confirm(`IPD-Dokument "${dokument.titel}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/ipd/${dokument.id}`);
            void navigate('/ipd');
        } catch (error) {
            setFehler('IPD-Dokument konnte nicht gelöscht werden.');
            console.error(error);
        }
    }

    if (loading) {
        return (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
                <Spinner animation="border" role="status" />
            </div>
        );
    }

    if (!dokument || !formular) {
        return <Alert variant="danger">IPD-Dokument konnte nicht geladen werden.</Alert>;
    }

    return (
        <div className="py-4">
            <Button variant="link" className="ps-0 mb-2" onClick={() => navigate('/ipd')}>
                ← Zurück zur Übersicht
            </Button>

            <div className="d-flex justify-content-between align-items-start mb-4">
                <div>
                    <h1 className="mb-1">{dokument.titel}</h1>
                    <Badge bg={ipdStatusBadgeVariante(dokument.status)}>
                        {IPD_DOCUMENT_STATUS_LABELS[dokument.status]}
                    </Badge>
                </div>
                <div className="d-flex gap-2">
                    <Button variant="primary" onClick={handlePdfHerunterladen} disabled={pdfLaeuft}>
                        {pdfLaeuft ? 'Erzeuge PDF…' : 'PDF herunterladen'}
                    </Button>
                    <Button
                        variant="outline-secondary"
                        onClick={handleChecklisteHerunterladen}
                        disabled={checklisteLaeuft}
                    >
                        {checklisteLaeuft ? 'Erzeuge Checkliste…' : 'Checkliste herunterladen'}
                    </Button>
                    <Button variant="outline-danger" onClick={handleLoeschen}>
                        Löschen
                    </Button>
                </div>
            </div>

            {fehler && <Alert variant="danger">{fehler}</Alert>}
            {erfolg && <Alert variant="success">{erfolg}</Alert>}

            {/* Automatisch verwaltete Felder zeige ich read-only in einer
            eigenen Card an - hier zu tippen hätte ohnehin keinen Effekt,
            da das Backend sie bei jedem Speichern aus Ticket/Task/
            Checklist neu berechnet (siehe IpdDocumentService). */}
            <HudPanel title="Übersicht" className="mb-4">
                <div>
                    <Row>
                        <Col md={4}>
                            <strong>Techniker:</strong> {dokument.techniker}
                        </Col>
                        <Col md={4}>
                            <strong>Szenario:</strong> {SZENARIO_TYP_LABELS[dokument.szenarioTyp]}
                        </Col>
                        <Col md={4}>
                            <strong>Qualitätssicherung:</strong>{' '}
                            {dokument.qualitaetssicherungAbgeschlossen ? (
                                <Badge bg="success">Durchgeführt</Badge>
                            ) : (
                                <Badge bg="warning">
                                    Noch offen
                                </Badge>
                            )}
                        </Col>
                    </Row>
                    <Row className="mt-2">
                        <Col md={4}>
                            <strong>Erstellt am:</strong> {formatiereDatum(dokument.erstelltAm)}
                        </Col>
                        <Col md={4}>
                            <strong>Aktualisiert am:</strong> {formatiereDatum(dokument.aktualisiertAm)}
                        </Col>
                    </Row>
                    {dokument.durchgefuehrteSchritte && (
                        <Row className="mt-2">
                            <Col>
                                <strong>Durchgeführte Schritte (aus TaskPlanner übernommen):</strong>
                                {/* white-space: pre-line, damit die Zeilenumbrüche aus
                                dem vom Backend gebauten Fließtext (ein Task pro
                                Zeile) auch wirklich als Zeilenumbrüche
                                dargestellt werden. */}
                                <div style={{ whiteSpace: 'pre-line' }}>{dokument.durchgefuehrteSchritte}</div>
                            </Col>
                        </Row>
                    )}
                </div>
            </HudPanel>

            {/* Manuell zu pflegende Abschnitte - ich gruppiere sie in
            Cards passend zur Gliederung eines echten IPD-Dokuments
            (Kunde/Rahmendaten, Ausgangslage, technische Details,
            Nachbereitung), statt 15 Textfelder untereinander ohne
            Struktur zu zeigen. */}
            <Form>
                <HudPanel title="Rahmendaten" className="mb-3">
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Titel</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={formular.titel}
                                        onChange={(e) => handleFeldAendern('titel', e.target.value)}
                                        required
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Kunde</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={formular.kunde ?? ''}
                                        onChange={(e) => handleFeldAendern('kunde', e.target.value)}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Ansprechpartner Kunde</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={formular.ansprechpartnerKunde ?? ''}
                                        onChange={(e) => handleFeldAendern('ansprechpartnerKunde', e.target.value)}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Zeitraum</Form.Label>
                                    <Form.Control
                                        type="text"
                                        placeholder="z.B. 01.10.2026 - 03.10.2026"
                                        value={formular.zeitraum ?? ''}
                                        onChange={(e) => handleFeldAendern('zeitraum', e.target.value)}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                    </HudPanel>

                <HudPanel title="Ausgangslage &amp; Anforderungen" className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>Ausgangslage</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.ausgangslage ?? ''}
                                onChange={(e) => handleFeldAendern('ausgangslage', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Anforderungen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.anforderungen ?? ''}
                                onChange={(e) => handleFeldAendern('anforderungen', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title="Technische Details" className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>Infrastruktur-Übersicht</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.infrastrukturUebersicht ?? ''}
                                onChange={(e) => handleFeldAendern('infrastrukturUebersicht', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Server und VMs</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.serverUndVms ?? ''}
                                onChange={(e) => handleFeldAendern('serverUndVms', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Netzwerk</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.netzwerk ?? ''}
                                onChange={(e) => handleFeldAendern('netzwerk', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Rollen und Verantwortlichkeiten</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.rollenUndVerantwortlichkeiten ?? ''}
                                onChange={(e) => handleFeldAendern('rollenUndVerantwortlichkeiten', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Backup-Konzept</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.backupKonzept ?? ''}
                                onChange={(e) => handleFeldAendern('backupKonzept', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Security-Überlegungen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.securityUeberlegungen ?? ''}
                                onChange={(e) => handleFeldAendern('securityUeberlegungen', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title="Nachbereitung" className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>Entscheidungen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.entscheidungen ?? ''}
                                onChange={(e) => handleFeldAendern('entscheidungen', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Risiken und Annahmen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.risikenUndAnnahmen ?? ''}
                                onChange={(e) => handleFeldAendern('risikenUndAnnahmen', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Rollback-Plan</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={formular.rollbackPlan ?? ''}
                                onChange={(e) => handleFeldAendern('rollbackPlan', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title="Status" className="mb-4">
                        <Form.Group style={{ maxWidth: 320 }}>
                            <Form.Label>Status</Form.Label>
                            <Form.Select
                                value={formular.status}
                                onChange={(e) =>
                                    setFormular({ ...formular, status: e.target.value as IpdDocumentStatus })
                                }
                            >
                                {Object.entries(IPD_DOCUMENT_STATUS_LABELS).map(([wert, label]) => (
                                    <option key={wert} value={wert}>
                                        {label}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </HudPanel>

                <Button variant="primary" onClick={handleSpeichern} disabled={speichernLaeuft || !formular.titel}>
                    {speichernLaeuft ? 'Speichere…' : 'Speichern'}
                </Button>
            </Form>
        </div>
    );
}