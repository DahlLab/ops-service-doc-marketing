import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Alert, Badge, Button, Col, Form, Row, Spinner } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { useNavigate, useParams } from 'react-router-dom';
import { api, ApiError } from '../api/api';
import { downloadFile } from '../utils/download';
import { formatDate, ipdStatusBadgeVariant } from '../utils/formatting';
import {
    IPD_DOCUMENT_STATUSES,
    type IpdDocumentDto,
    type IpdDocumentFormData,
    type IpdDocumentStatus,
} from '../api/types';

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

export function IpdDocumentPage() {
    const { t } = useTranslation('ipd');
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [ipdDocument, setDocument] = useState<IpdDocumentDto | null>(null);
    const [form, setForm] = useState<IpdDocumentFormData | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [pdfRunning, setPdfRunning] = useState(false);
    const [checklistRunning, setChecklistRunning] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const [success, setSuccess] = useState<string | null>(null);

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
                setErrorMessage(t('document.loadError'));
                console.error(error);
            })
            .finally(() => {
                if (!aborted) setLoading(false);
            });
        return () => {
            aborted = true;
        };
    }, [id, t]);

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
            const updatedDocument = await api.put<IpdDocumentDto>(`/api/ipd/${ipdDocument.id}`, {
                ...ipdDocument,
                ...form,
            });
            setDocument(updatedDocument);
            setForm(formFromDocument(updatedDocument));
            setSuccess(t('document.saved'));
        } catch (error) {
            if (error instanceof ApiError && error.status === 400) {
                setErrorMessage(t('document.validationError'));
            } else {
                setErrorMessage(t('document.saveError'));
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handlePdfDownload() {
        if (!ipdDocument) return;
        setPdfRunning(true);
        setErrorMessage(null);
        try {
            await downloadFile(`/api/ipd/${ipdDocument.id}/pdf`, `ipd-${ipdDocument.id}.pdf`);
        } catch (error) {
            setErrorMessage(t('document.pdfError'));
            console.error(error);
        } finally {
            setPdfRunning(false);
        }
    }

    async function handleChecklistDownload() {
        if (!ipdDocument) return;
        setChecklistRunning(true);
        setErrorMessage(null);
        try {
            await downloadFile(`/api/ipd/${ipdDocument.id}/checklist-pdf`, `checkliste-${ipdDocument.id}.pdf`);
        } catch (error) {
            if (error instanceof ApiError && error.status === 404) {
                setErrorMessage(t('document.checklistMissing'));
            } else {
                setErrorMessage(t('document.checklistError'));
            }
            console.error(error);
        } finally {
            setChecklistRunning(false);
        }
    }

    async function handleDelete() {
        if (!ipdDocument) return;
        if (!window.confirm(t('document.deleteConfirm', { title: ipdDocument.title }))) {
            return;
        }
        try {
            await api.delete(`/api/ipd/${ipdDocument.id}`);
            void navigate('/ipd');
        } catch (error) {
            setErrorMessage(t('document.deleteError'));
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
        return <Alert variant="danger">{t('document.loadError')}</Alert>;
    }

    return (
        <div className="py-4">
            <Button variant="link" className="ps-0 mb-2" onClick={() => navigate('/ipd')}>
                {t('document.back')}
            </Button>

            <div className="d-flex justify-content-between align-items-start mb-4">
                <div>
                    <h1 className="mb-1">{ipdDocument.title}</h1>
                    <Badge bg={ipdStatusBadgeVariant(ipdDocument.status)}>
                        {t(`status.ipd.${ipdDocument.status}`, { ns: 'common' })}
                    </Badge>
                </div>
                <div className="d-flex gap-2">
                    <Button variant="primary" onClick={handlePdfDownload} disabled={pdfRunning}>
                        {pdfRunning ? t('document.pdfRunning') : t('document.pdfDownload')}
                    </Button>
                    <Button
                        variant="outline-secondary"
                        onClick={handleChecklistDownload}
                        disabled={checklistRunning}
                    >
                        {checklistRunning ? t('document.checklistRunning') : t('document.checklistDownload')}
                    </Button>
                    <Button variant="outline-danger" onClick={handleDelete}>
                        {t('document.delete')}
                    </Button>
                </div>
            </div>

            {errorMessage && <Alert variant="danger">{errorMessage}</Alert>}
            {success && <Alert variant="success">{success}</Alert>}

            <HudPanel title={t('document.overview.title')} className="mb-4">
                <div>
                    <Row>
                        <Col md={4}>
                            <strong>{t('document.overview.technician')}</strong> {ipdDocument.technician}
                        </Col>
                        <Col md={4}>
                            <strong>{t('document.overview.scenario')}</strong> {t(`document.scenario.${ipdDocument.scenarioType}`)}
                        </Col>
                        <Col md={4}>
                            <strong>{t('document.overview.qualityAssurance')}</strong>{' '}
                            {ipdDocument.qualityAssuranceCompleted ? (
                                <Badge bg="success">{t('document.overview.qaDone')}</Badge>
                            ) : (
                                <Badge bg="warning">
                                    {t('document.overview.qaOpen')}
                                </Badge>
                            )}
                        </Col>
                    </Row>
                    <Row className="mt-2">
                        <Col md={4}>
                            <strong>{t('document.overview.createdAt')}</strong> {formatDate(ipdDocument.createdAt)}
                        </Col>
                        <Col md={4}>
                            <strong>{t('document.overview.updatedAt')}</strong> {formatDate(ipdDocument.updatedAt)}
                        </Col>
                    </Row>
                    {ipdDocument.performedSteps && (
                        <Row className="mt-2">
                            <Col>
                                <strong>{t('document.overview.performedSteps')}</strong>

                                <div style={{ whiteSpace: 'pre-line' }}>{ipdDocument.performedSteps}</div>
                            </Col>
                        </Row>
                    )}
                </div>
            </HudPanel>

            <Form>
                <HudPanel title={t('document.sections.basics')} className="mb-3">
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>{t('document.fields.title')}</Form.Label>
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
                                    <Form.Label>{t('document.fields.customer')}</Form.Label>
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
                                    <Form.Label>{t('document.fields.customerContact')}</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={form.customerContact ?? ''}
                                        onChange={(e) => handleFieldChange('customerContact', e.target.value)}
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>{t('document.fields.period')}</Form.Label>
                                    <Form.Control
                                        type="text"
                                        placeholder={t('document.fields.periodPlaceholder')}
                                        value={form.period ?? ''}
                                        onChange={(e) => handleFieldChange('period', e.target.value)}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>
                    </HudPanel>

                <HudPanel title={t('document.sections.situation')} className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.initialSituation')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.initialSituation ?? ''}
                                onChange={(e) => handleFieldChange('initialSituation', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.requirements')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.requirements ?? ''}
                                onChange={(e) => handleFieldChange('requirements', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title={t('document.sections.technical')} className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.infrastructureOverview')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.infrastructureOverview ?? ''}
                                onChange={(e) => handleFieldChange('infrastructureOverview', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.serversAndVms')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.serversAndVms ?? ''}
                                onChange={(e) => handleFieldChange('serversAndVms', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.network')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.network ?? ''}
                                onChange={(e) => handleFieldChange('network', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.rolesAndResponsibilities')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.rolesAndResponsibilities ?? ''}
                                onChange={(e) => handleFieldChange('rolesAndResponsibilities', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.backupPlan')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.backupPlan ?? ''}
                                onChange={(e) => handleFieldChange('backupPlan', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.securityConsiderations')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.securityConsiderations ?? ''}
                                onChange={(e) => handleFieldChange('securityConsiderations', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title={t('document.sections.followUp')} className="mb-3">
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.decisions')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.decisions ?? ''}
                                onChange={(e) => handleFieldChange('decisions', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.risksAndAssumptions')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.risksAndAssumptions ?? ''}
                                onChange={(e) => handleFieldChange('risksAndAssumptions', e.target.value)}
                            />
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('document.fields.rollbackPlan')}</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={form.rollbackPlan ?? ''}
                                onChange={(e) => handleFieldChange('rollbackPlan', e.target.value)}
                            />
                        </Form.Group>
                    </HudPanel>

                <HudPanel title={t('document.sections.status')} className="mb-4">
                        <Form.Group style={{ maxWidth: 320 }}>
                            <Form.Label>{t('document.fields.status')}</Form.Label>
                            <Form.Select
                                value={form.status}
                                onChange={(e) =>
                                    setForm({ ...form, status: e.target.value as IpdDocumentStatus })
                                }
                            >
                                {IPD_DOCUMENT_STATUSES.map((value) => (
                                    <option key={value} value={value}>
                                        {t(`status.ipd.${value}`, { ns: 'common' })}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </HudPanel>

                <Button variant="primary" onClick={handleSave} disabled={saving || !form.title}>
                    {saving ? t('document.saving') : t('document.save')}
                </Button>
            </Form>
        </div>
    );
}
