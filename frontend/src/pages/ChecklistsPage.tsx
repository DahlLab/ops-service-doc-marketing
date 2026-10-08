import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Accordion, Alert, Badge, Button, Form, Modal, Spinner, Tab, Tabs, ProgressBar } from 'react-bootstrap';
import HudPanel from '../components/HudPanel';
import { api, ApiError } from '../api/api';
import { formatDate } from '../utils/formatting';
import { DynamicItemList } from '../components/DynamicItemList';
import i18n from '../i18n';
import type {
    ChecklistDto,
    ChecklistFormData,
    ChecklistItemDto,
    ChecklistTemplateDto,
    ChecklistTemplateFormData,
    TicketDto,
} from '../api/types';

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

function parseBlock(description: string): { phase: string; text: string; optional: boolean } {
    const optional = description.endsWith(' (optional)');
    const withoutOptional = optional ? description.slice(0, -' (optional)'.length) : description;
    if (withoutOptional.startsWith('[')) {
        const ende = withoutOptional.indexOf(']');

        if (ende > 1) {
            return { phase: withoutOptional.slice(1, ende), text: withoutOptional.slice(ende + 1).trim(), optional };
        }
    }
    return { phase: i18n.t('checklists:templates.otherPhase'), text: withoutOptional, optional };
}

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

function progressPercent(items: { done: boolean }[]): number {
    if (items.length === 0) return 0;
    return (items.filter((item) => item.done).length / items.length) * 100;
}

export function ChecklistsPage() {
    const { t } = useTranslation('checklists');
    const [tickets, setTickets] = useState<TicketDto[]>([]);
    const [loadError, setLoadError] = useState<string | null>(null);

    const [checklists, setChecklists] = useState<ChecklistDto[]>([]);
    const [templates, setTemplates] = useState<ChecklistTemplateDto[]>([]);
    const [checklistsLoading, setChecklistsLoading] = useState(true);
    const [ticketFilter, setTicketFilter] = useState('');

    const [checklistModalOpen, setChecklistModalOpen] = useState(false);

    const [createMode, setCreateMode] = useState<'manuell' | 'vorlage' | 'baukasten'>('manuell');
    const [editedChecklist, setEditedChecklist] = useState<ChecklistDto | null>(null);
    const [checklistForm, setChecklistForm] = useState<ChecklistFormData>(emptyChecklistForm(''));
    const [selectedTemplateId, setSelectedTemplateId] = useState('');

    const [selectedBlocks, setSelectedBlocks] = useState<Record<string, boolean>>({});

    const [builderSaveHint, setBuilderSaveHint] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [checklistError, setChecklistError] = useState<string | null>(null);

    const [templatesLoading, setTemplatesLoading] = useState(true);
    const [templateModalOpen, setTemplateModalOpen] = useState(false);
    const [editedTemplate, setEditedTemplate] = useState<ChecklistTemplateDto | null>(null);
    const [templateForm, setTemplateForm] = useState<ChecklistTemplateFormData>(EMPTY_TEMPLATE_FORM);
    const [templateError, setTemplateError] = useState<string | null>(null);

    async function loadChecklists(ticketId: string) {
        try {
            const path = ticketId ? `/api/checklists?ticketId=${ticketId}` : '/api/checklists';
            const loadedChecklists = await api.get<ChecklistDto[]>(path);
            setChecklists(loadedChecklists);
        } catch (error) {
            setLoadError(t('errors.loadChecklists'));
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
            setLoadError(t('errors.loadTemplates'));
            console.error(error);
        } finally {
            setTemplatesLoading(false);
        }
    }

    useEffect(() => {
        let aborted = false;
        api.get<TicketDto[]>('/api/tickets').then(setTickets).catch(console.error);
        api.get<ChecklistDto[]>('/api/checklists')
            .then((loadedChecklists) => {
                if (!aborted) setChecklists(loadedChecklists);
            })
            .catch((error) => {
                if (aborted) return;
                setLoadError(i18n.t('checklists:errors.loadChecklists'));
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
                setLoadError(i18n.t('checklists:errors.loadTemplates'));
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
        return tickets.find((ticket) => ticket.id === ticketId)?.title ?? t('list.unknownTicket');
    }

    function handleFilterChange(ticketId: string) {
        setTicketFilter(ticketId);
        setChecklistsLoading(true);

        void loadChecklists(ticketId);
    }

    function handleNewChecklist() {
        setEditedChecklist(null);
        setCreateMode('manuell');
        setChecklistForm(emptyChecklistForm(ticketFilter || tickets[0]?.id || ''));
        setSelectedTemplateId(templates[0]?.id ?? '');

        setSelectedBlocks({});
        setBuilderSaveHint(null);
        setChecklistError(null);
        setChecklistModalOpen(true);
    }

    function handleChecklistEdit(checklist: ChecklistDto) {
        setEditedChecklist(checklist);
        setCreateMode('manuell');
        setChecklistForm({
            ticketId: checklist.ticketId,
            title: checklist.title,

            items: checklist.items.map((item) => ({ ...item })),
        });
        setChecklistError(null);
        setChecklistModalOpen(true);
    }

    function handleItemAdd() {
        setChecklistForm({
            ...checklistForm,
            items: [...checklistForm.items, { id: null, description: '', done: false }],
        });
    }

    function handleItemRemove(index: number) {
        setChecklistForm({
            ...checklistForm,
            items: checklistForm.items.filter((_, i) => i !== index),
        });
    }

    function handleItemTextChange(index: number, newText: string) {
        const newItems = [...checklistForm.items];
        newItems[index] = { ...newItems[index], description: newText };
        setChecklistForm({ ...checklistForm, items: newItems });
    }

    function handleBlockToggle(key: string) {
        setSelectedBlocks({
            ...selectedBlocks,
            [key]: !selectedBlocks[key],
        });
    }

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

    const builderSelectionCount = Object.values(selectedBlocks).filter(Boolean).length;

    async function handleSaveBuilderAsTemplate() {
        if (!checklistForm.title.trim() || builderSelectionCount === 0) {
            setChecklistError(t('errors.saveTemplateNeedsInput'));
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
            setBuilderSaveHint(t('checklistModal.templateSaved', { title: checklistForm.title }));
        } catch (error) {
            setChecklistError(t('errors.saveTemplate'));
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
                setChecklistError(t('errors.checklistInvalid'));
            } else {
                setChecklistError(t('errors.saveChecklist'));
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleChecklistDelete(checklist: ChecklistDto) {
        if (!window.confirm(t('confirm.deleteChecklist', { title: checklist.title }))) {
            return;
        }
        try {
            await api.delete(`/api/checklists/${checklist.id}`);
            await loadChecklists(ticketFilter);
        } catch (error) {
            setLoadError(t('errors.deleteChecklist'));
            console.error(error);
        }
    }

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
            setLoadError(t('errors.updateItem'));
            console.error(error);
        }
    }

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
                setTemplateError(t('errors.templateInvalid'));
            } else {
                setTemplateError(t('errors.saveTemplate'));
            }
            console.error(error);
        } finally {
            setSaving(false);
        }
    }

    async function handleTemplateDelete(template: ChecklistTemplateDto) {
        if (!window.confirm(t('confirm.deleteTemplate', { name: template.name }))) {
            return;
        }
        try {
            await api.delete(`/api/checklist-templates/${template.id}`);
            await loadTemplates();
        } catch (error) {
            setLoadError(t('errors.deleteTemplate'));
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

    const showTemplateMode = !editedChecklist && createMode === 'vorlage';
    const showBuilderMode = !editedChecklist && createMode === 'baukasten';

    return (
        <div className="py-4">
            <h1 className="mb-4">{t('title')}</h1>

            {loadError && <Alert variant="danger">{loadError}</Alert>}

            <Tabs defaultActiveKey="checklisten" className="mb-3">
                <Tab eventKey="checklisten" title={t('tabs.checklists')}>
                    <div className="d-flex justify-content-between align-items-center my-3">
                        <Form.Group style={{ maxWidth: 320 }}>
                            <Form.Label>{t('filter.label')}</Form.Label>
                            <Form.Select value={ticketFilter} onChange={(e) => handleFilterChange(e.target.value)}>
                                <option value="">{t('filter.all')}</option>
                                {tickets.map((ticket) => (
                                    <option key={ticket.id} value={ticket.id}>
                                        {ticket.title}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                        <Button variant="primary" onClick={handleNewChecklist} disabled={tickets.length === 0}>
                            {t('list.new')}
                        </Button>
                    </div>

                    {tickets.length === 0 && (
                        <Alert variant="info">
                            {t('list.noTickets')}
                        </Alert>
                    )}

                    {checklists.length === 0 ? (
                        <Alert variant="dark" className="text-center">
                            {ticketFilter
                                ? t('list.emptyForTicket', { ticket: ticketTitle(ticketFilter) })
                                : t('list.empty')}
                        </Alert>
                    ) : (
                        checklists.map((checklist) => (
                            <HudPanel key={checklist.id} title={checklist.title} className="mb-3">

                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <span className="text-muted">{ticketTitle(checklist.ticketId)}</span>
                                    {checklist.completedAt ? (
                                        <Badge bg="success">
                                            {t('list.completedOn', { date: formatDate(checklist.completedAt) })}
                                        </Badge>
                                    ) : (
                                        <Badge bg="secondary">{t('status.task.OPEN', { ns: 'common' })}</Badge>
                                    )}
                                </div>

                                <ProgressBar
                                    className="hud-progress mb-3"
                                    now={progressPercent(checklist.items)}
                                    aria-label={t('list.progress', { title: checklist.title })}
                                />
                                <Form>
                                    {checklist.items.map((item) => (
                                        <Form.Check
                                            key={item.id}
                                            type="checkbox"
                                            id={`item-${item.id}`}
                                            label={item.description}
                                            checked={item.done}

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
                                        {t('list.edit')}
                                    </Button>
                                    <Button
                                        variant="outline-danger"
                                        size="sm"
                                        onClick={() => handleChecklistDelete(checklist)}
                                    >
                                        {t('list.delete')}
                                    </Button>
                                </div>
                            </HudPanel>
                        ))
                    )}
                </Tab>

                <Tab eventKey="vorlagen" title={t('tabs.templates')}>
                    <div className="d-flex justify-content-end my-3">
                        <Button variant="primary" onClick={handleNewTemplate}>
                            {t('templates.new')}
                        </Button>
                    </div>

                    {templates.length === 0 ? (
                        <Alert variant="dark" className="text-center">
                            {t('templates.empty')}
                        </Alert>
                    ) : (
                        <Accordion alwaysOpen>
                            {templates.map((template) => (
                                <Accordion.Item eventKey={template.id} key={template.id}>
                                    <Accordion.Header>
                                        <span className="flex-grow-1">{template.name}</span>
                                        {template.builtIn && (
                                            <Badge bg="info" className="me-2">
                                                {t('templates.builtIn')}
                                            </Badge>
                                        )}
                                        <Badge bg="secondary" className="me-3">
                                            {t('templates.pointCount', { count: template.itemDescriptions.length })}
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
                                                                    {t('templates.optional')}
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
                                                {t('templates.edit')}
                                            </Button>

                                            {!template.builtIn && (
                                                <Button
                                                    variant="outline-danger"
                                                    size="sm"
                                                    onClick={() => handleTemplateDelete(template)}
                                                >
                                                    {t('templates.delete')}
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

            <Modal show={checklistModalOpen} onHide={() => setChecklistModalOpen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{editedChecklist ? t('checklistModal.titleEdit') : t('checklistModal.titleNew')}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {checklistError && <Alert variant="danger">{checklistError}</Alert>}
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('checklistModal.ticket')}</Form.Label>
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

                        {!editedChecklist && (
                            <Form.Group className="mb-3">
                                <Form.Label>{t('checklistModal.createMode')}</Form.Label>
                                <div>
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-manuell"
                                        label={t('checklistModal.modeManual')}
                                        checked={createMode === 'manuell'}
                                        onChange={() => setCreateMode('manuell')}
                                    />
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-vorlage"
                                        label={t('checklistModal.modeTemplate')}
                                        checked={createMode === 'vorlage'}
                                        onChange={() => setCreateMode('vorlage')}
                                        disabled={templates.length === 0}
                                    />
                                    <Form.Check
                                        inline
                                        type="radio"
                                        name="erstellModus"
                                        id="modus-baukasten"
                                        label={t('checklistModal.modeBuilder')}
                                        checked={createMode === 'baukasten'}
                                        onChange={() => setCreateMode('baukasten')}
                                        disabled={templates.length === 0}
                                    />
                                </div>
                            </Form.Group>
                        )}

                        {showTemplateMode && (
                            <Form.Group className="mb-3">
                                <Form.Label>{t('checklistModal.template')}</Form.Label>
                                <Form.Select
                                    value={selectedTemplateId}
                                    onChange={(e) => setSelectedTemplateId(e.target.value)}
                                >
                                    {templates.map((template) => (
                                        <option key={template.id} value={template.id}>
                                            {t('checklistModal.templateOption', { name: template.name, count: template.itemDescriptions.length })}
                                        </option>
                                    ))}
                                </Form.Select>
                            </Form.Group>
                        )}

                        {showBuilderMode && (
                            <>
                                <Form.Group className="mb-3">
                                    <Form.Label>{t('checklistModal.title')}</Form.Label>
                                    <Form.Control
                                        type="text"
                                        value={checklistForm.title}
                                        onChange={(e) =>
                                            setChecklistForm({ ...checklistForm, title: e.target.value })
                                        }
                                        placeholder={t('checklistModal.titlePlaceholder')}
                                        required
                                    />
                                </Form.Group>

                                <Form.Group className="mb-3">
                                    <Form.Label>
                                        {t('checklistModal.selectPoints')}{' '}
                                        <span className="text-muted">
                                            {t('checklistModal.selectedCount', { selected: builderSelectionCount })}
                                        </span>
                                    </Form.Label>

                                    <div style={{ maxHeight: '45vh', overflowY: 'auto' }} className="border rounded p-2">
                                        {templates.map((template) => (
                                            <div key={template.id} className="mb-3">
                                                <div className="fw-bold mb-1">{template.name}</div>
                                                {template.itemDescriptions.map((description, index) => {
                                                    const key = `${template.id}:${index}`;

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
                                                                            {t('templates.optional')}
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

                                <Button
                                    variant="outline-primary"
                                    size="sm"
                                    onClick={handleSaveBuilderAsTemplate}
                                    disabled={saving || builderSelectionCount === 0}
                                >
                                    {t('checklistModal.saveAsTemplate')}
                                </Button>
                            </>
                        )}

                        {!showTemplateMode && !showBuilderMode && (
                            <>
                                <Form.Group className="mb-3">
                                    <Form.Label>{t('checklistModal.title')}</Form.Label>
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
                                    <Form.Label>{t('checklistModal.items')}</Form.Label>

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
                        {t('checklistModal.cancel')}
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleChecklistSave}
                        disabled={
                            saving ||
                            (!editedChecklist && createMode === 'baukasten' && builderSelectionCount === 0)
                        }
                    >
                        {saving ? t('checklistModal.saving') : t('checklistModal.save')}
                    </Button>
                </Modal.Footer>
            </Modal>

            <Modal show={templateModalOpen} onHide={() => setTemplateModalOpen(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>{editedTemplate ? t('templateModal.titleEdit') : t('templateModal.titleNew')}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {templateError && <Alert variant="danger">{templateError}</Alert>}
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>{t('templateModal.name')}</Form.Label>
                            <Form.Control
                                type="text"
                                value={templateForm.name}
                                onChange={(e) => setTemplateForm({ ...templateForm, name: e.target.value })}
                                required
                            />
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>{t('templateModal.points')}</Form.Label>

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
                        {t('templateModal.cancel')}
                    </Button>
                    <Button variant="primary" onClick={handleTemplateSave} disabled={saving}>
                        {saving ? t('templateModal.saving') : t('templateModal.save')}
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
}
