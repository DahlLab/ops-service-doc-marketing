import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Col, Form, Row, Spinner } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { useNavigate, useParams } from 'react-router-dom';
import { api, ApiError } from '../api/api';
import { downloadFile } from '../utils/download';
import { formatDate, ipdStatusBadgeVariante } from '../utils/formatting';
import {
    IPD_DOCUMENT_STATUS_LABELS,
    SCENARIO_TYPE_LABELS,
    type IpdDocumentDto,
    type IpdDocumentFormData,
    type IpdDocumentStatus,
} from '../api/types';

// Baut aus einem geladenen IpdDocumentDto genau die Felder, die ich im
// Formular bearbeite - die automatisch verwalteten Felder (ticketId,
// techniker, szenarioTyp, durchgefuehrteSchritte,
// qualitaetssicherungAbgeschlossen, erstelltAm, aktualisiertAm) zeige ich
// separat read-only an, siehe JSX unten.
function formFromDocument(ipdDocument: IpdDocumentDto): IpdDocumentFormData {
    return {
        title: ipdDocument.title,
        customer: ipdDocument.customer,
        customerContact: ipdDocument.customerContact,
        period: ipdDocument.period,
        initialSituation: ipdDocument.initialSituation,
        requirements: ipdDocument.requirements,
        infrastructureOverview: ipdDocument.infrastructureOverview,
        serversAndVms: ipdDocument.serversAndVms,
        network: ipdDocument.network,
        rolesAndResponsibilities: ipdDocument.rolesAndResponsibilities,
        backupPlan: ipdDocument.backupPlan,
        securityConsiderations: ipdDocument.securityConsiderations,
        decisions: ipdDocument.decisions,
        risksAndAssumptions: ipdDocument.risksAndAssumptions,
        rollbackPlan: ipdDocument.rollbackPlan,
        status: ipdDocument.status,
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

    const [ipdDocument, setDocument] = useState<IpdDocumentDto | null>(null);
    const [form, setForm] = useState<IpdDocumentFormData | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [pdfRunning, setPdfRunning] = useState(false);
    const [checklistRunning, setChecklistRunning] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    // Separate Erfolgsmeldung statt nur eines Fehlerfelds, damit ich dem
    // Nutzer nach dem Speichern kurz sichtbares Feedback geben kann,
    // ohne auf eine neue Seite zu springen (anders als bei Erzeugen, wo
    // ich direkt hierher navigiere, bleibe ich nach dem Speichern bewusst
    // auf dieser Seite, weil man idR mehrere Abschnitte nacheinander
    // ergänzt).
    const [success, setSuccess] = useState<string | null>(null);

    // Dokument beim Öffnen (und bei geänderter id) laden: Anfrage direkt im Effect,
    // State nur im Callback. `abgebrochen` verhindert, dass eine späte Antwort eines
    // alten Dokuments das neue überschreibt.
    useEffect(() => {
        if (!id) return;
        let aborted = false;
        api.get<IpdDocumentDto>(`/api/ipd/${id}`)
            .then((loadedDocument) => {
                if (aborted) return;
                setDocument(loadedDocument);
                setForm(formFromDocument(loadedDocument));
                setErrorMessage(null);
            })
            .catch((error) => {
                if (aborted) return;
                setErrorMessage('IPD-Dokument konnte nicht geladen werden.');
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, [id]);

    // Generischer Change-Handler für alle Textarea-Felder: erspart mir
    // 14 fast identische onChange-Funktionen für jedes Formularfeld.
    function handleFieldChange(field: keyof IpdDocumentFormData, value: string) {
        if (!form) return;
        setForm({ ...form, [field]: value || null });
    }

    async function handleSave() {
        if (!ipdDocument || !form) return;
        setSaving(true);
        setErrorMessage(null);
        setSuccess(null);
        try {
            // Das Backend erwartet den VOLLSTÄNDIGEN IpdDocumentDto als
            // Request-Body (auch wenn es einen Großteil der Felder
            // ohnehin ignoriert/neu berechnet, siehe
            // IpdDocumentService.updateIpdDocument) - deshalb baue ich
            // hier aus dem zuletzt geladenen Dokument plus meinen
            // Formular-Änderungen wieder ein komplettes Objekt
            // zusammen, statt nur die editierbaren Felder zu schicken.
            const updatedDocument = await api.put<IpdDocumentDto>(`/api/ipd/${ipdDocument.id}`, {
                ...ipdDocument,
                ...form,
            });
            setDocument(updatedDocument);
            setForm(formFromDocument(updatedDocument));
            setSuccess('Gespeichert.');
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setErrorMessage('Bitte prüfe deine Eingaben - der Titel darf z.B. nicht leer sein.');
            } else {
                setErrorMessage('IPD-Dokument konnte nicht gespeichert werden.');
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    // Lädt das Kunden-PDF herunter (siehe utils/download.ts für das
    // Blob/ObjectURL-Verfahren).
    async function handlePdfDownload() {
        if (!ipdDocument) return;
        setPdfRunning(true);
        setErrorMessage(null);
        try {
            await downloadFile(`/api/ipd/${ipdDocument.id}/pdf`, `ipd-${ipdDocument.id}.pdf`);
        } catch (error) {
            setErrorMessage('PDF konnte nicht erzeugt werden.');
            console.error(error);
        } finally {
            setPdfRunning(false);
        }
    }

    // Lädt die interne Checkliste (Technikerversion mit ausfüllbaren
    // Checkboxen) herunter - getrennt vom Kunden-PDF. 404 heißt: zu diesem
    // Ticket gibt es (noch) keine Checkliste.
    async function handleChecklistDownload() {
        if (!ipdDocument) return;
        setChecklistRunning(true);
        setErrorMessage(null);
        try {
            await downloadFile(`/api/ipd/${ipdDocument.id}/checklist-pdf`, `checkliste-${ipdDocument.id}.pdf`);
        } catch (error) {
            if (error instanceof ApiError && error.status === 404) {
                setErrorMessage('Zu diesem Dokument gibt es noch keine Checkliste.');
            } else {
                setErrorMessage('Checkliste konnte nicht erzeugt werden.');
            }
            console.error(error);
        } finally {
            setChecklistRunning(false);
        }
    }

    async function handleDelete() {
        if (!ipdDocument) return;
        if (!window.confirm(`IPD-Dokument "${ipdDocument.title}" wirklich löschen?`)) {
            return;
        }
        try {
            await api.delete(`/api/ipd/${ipdDocument.id}`);
            void navigate('/ipd');
        } catch (error) {
            setErrorMessage('IPD-Dokument konnte nicht gelöscht werden.');
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

    if (!ipdDocument || !form) {
        return <Alert variant="danger">IPD-Dokument konnte nicht geladen werden.</Alert>;
    }

    return (
        <div className="py-4">
            <Button variant="link" className="ps-0 mb-2" onClick={() => navigate('/ipd')}>
                ← Zurück zur Übersicht
            </Button>

            <div className="d-flex justify-content-between align-items-start mb-4">
                <div>
                    <h1 className="mb-1">{ipdDocument.title}</h1>
                    <Badge bg={ipdStatusBadgeVariante(ipdDocument.status)}>
                        {IPD_DOCUMENT_STATUS_LABELS[ipdDocument.status]}
                    </Badge>
                </div>
                <div className="d-flex gap-2">
                    <Button variant="primary" onClick={handlePdfDownload} disabled={pdfRunning}>
                        {pdfRunning ? 'Erzeuge PDF…' : 'PDF herunterladen'}
                    </Button>
                    <Button
                        variant="outline-secondary"
                        onClick={handleChecklistDownload}
                        disabled={checklistRunning}
                    >
                        {checklistRunning ? 'Erzeuge Checkliste…' : 'Checkliste herunterladen'}
                    </Button>
                    <Button variant="outline-danger" onClick={handleDelete}>
                        Löschen
                    </Button>
                </div>
            </div>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}
            {success && <Alert variant="success">{success}</Alert>}

            {/* Automatisch verwaltete Felder zeige ich read-only in einer
            eigenen Card an - hier zu tippen hätte ohnehin keinen Effekt,
            da das Backend sie bei jedem Speichern aus Ticket/Task/
            Checklist neu berechnet (siehe IpdDocumentService). */}
            <HudPanel title="Übersicht" className="mb-4">
                <div>
                    <Row>
                        <Col md={4}>
                            <strong>Techniker:</strong> {ipdDocument.technician}
                        </Col>
                        <Col md={4}>
                            <strong>Szenario:</strong> {SCENARIO_TYPE_LABELS[ipdDocument.scenarioType]}
                        </Col>
                        <Col md={4}>
                            <strong>Qualitätssicherung:</strong>{' '}
                            {ipdDocument.qualityAssuranceCompleted ? (
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
                            <strong>Erstellt am:</strong> {formatDate(ipdDocument.createdAt)}
                        </Col>
                        <Col md={4}>
                            <strong>Aktualisiert am:</strong> {formatDate(ipdDocument.updatedAt)}
                        </Col>
                    </Row>
                    {ipdDocument.performedSteps && (
                        <Row className="mt-2">
                            <Col>
                                <strong>Durchgeführte Schritte (aus TaskPlanner übernommen):</strong>
                                {/* white-space: pre-line, damit die Zeilenumbrüche aus
                                dem vom Backend gebauten Fließtext (ein Task pro
                                Zeile) auch wirklich als Zeilenumbrüche
                                dargestellt werden. */}
                                <div style={{ whiteSpace: 'pre-line' }}>{ipdDocument.performedSteps}</div>
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
                                        value={form.title}
                                        onChange={(e) => handleFieldChange('title', e.target.value)}
                                        required
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Kunde</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={form.customer ?? ''}
                                        onChange={(e) => handleFieldChange('customer', e.target.value)}
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
                                        value={form.customerContact ?? ''}
                                        onChange={(e) => handleFieldChange('customerContact', e.target.value)}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Zeitraum</Form.Label>
                                    <Form.Control
                                        type="text"
                                        placeholder="z.B. 01.10.2026 - 03.10.2026"
                                        value={form.period ?? ''}
                                        onChange={(e) => handleFieldChange('period', e.target.value)}
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
                                value={form.initialSituation ?? ''}
                                onChange={(e) => handleFieldChange('initialSituation', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Anforderungen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.requirements ?? ''}
                                onChange={(e) => handleFieldChange('requirements', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title="Technische Details" className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>Infrastruktur-Übersicht</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.infrastructureOverview ?? ''}
                                onChange={(e) => handleFieldChange('infrastructureOverview', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Server und VMs</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.serversAndVms ?? ''}
                                onChange={(e) => handleFieldChange('serversAndVms', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Netzwerk</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.network ?? ''}
                                onChange={(e) => handleFieldChange('network', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Rollen und Verantwortlichkeiten</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.rolesAndResponsibilities ?? ''}
                                onChange={(e) => handleFieldChange('rolesAndResponsibilities', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Backup-Konzept</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.backupPlan ?? ''}
                                onChange={(e) => handleFieldChange('backupPlan', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Security-Überlegungen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.securityConsiderations ?? ''}
                                onChange={(e) => handleFieldChange('securityConsiderations', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title="Nachbereitung" className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>Entscheidungen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.decisions ?? ''}
                                onChange={(e) => handleFieldChange('decisions', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Risiken und Annahmen</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.risksAndAssumptions ?? ''}
                                onChange={(e) => handleFieldChange('risksAndAssumptions', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Rollback-Plan</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.rollbackPlan ?? ''}
                                onChange={(e) => handleFieldChange('rollbackPlan', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title="Status" className="mb-4">
                        <Form.Group style={{ maxWidth: 320 }}>
                            <Form.Label>Status</Form.Label>
                            <Form.Select
                                value={form.status}
                                onChange={(e) =>
                                    setForm({ ...form, status: e.target.value as IpdDocumentStatus })
                                }
                            >
                                {Object.entries(IPD_DOCUMENT_STATUS_LABELS).map(([value, label]) => (
                                    <option key={value} value={value}>
                                        {label}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </HudPanel>

                <Button variant="primary" onClick={handleSave} disabled={saving || !form.title}>
                    {saving ? 'Speichere…' : 'Speichern'}
                </Button>
            </Form>
        </div>
    );
}