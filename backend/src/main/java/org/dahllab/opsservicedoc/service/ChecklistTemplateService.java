package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.ChecklistTemplateDto;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.dahllab.opsservicedoc.util.ChecklistTemplateMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ChecklistTemplateService {

    // Gemeinsamer Teil der "nicht gefunden"-Fehlermeldungen: ein Literal statt vieler Kopien
    private static final String NICHT_GEFUNDEN = " nicht gefunden";
    private static final String VORLAGE_MIT_ID = "Checklisten-Vorlage mit ID ";

    private final ChecklistTemplateRepository checklistTemplateRepository;

    public ChecklistTemplateService(ChecklistTemplateRepository checklistTemplateRepository) {
        this.checklistTemplateRepository = checklistTemplateRepository;
    }

    // GET /api/checklist-templates - liefert alle Vorlagen.
    public List<ChecklistTemplateDto> getAllTemplates() {
        return checklistTemplateRepository.findAll().stream()
                .map(ChecklistTemplateMapper::toDto)
                .toList();
    }

    // GET /api/checklist-templates/{id} - liefert genau eine Vorlage.
    public ChecklistTemplateDto getTemplateById(String id) {
        ChecklistTemplate result = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(VORLAGE_MIT_ID + id + NICHT_GEFUNDEN));
        return ChecklistTemplateMapper.toDto(result);
    }

    // POST /api/checklist-templates - legt eine neue Vorlage an. Eine
    // über die API neu angelegte Vorlage ist NIE eine Standard-Vorlage -
    // standard wird hier hart auf false gesetzt und ein eventuell im
    // Request mitgeschicktes templateDto.standard() bewusst ignoriert,
    // damit niemand sich selbst eine unlöschbare Vorlage anlegen kann.
    // Nur mein ChecklistTemplateSeeder setzt beim Start standard=true.
    public ChecklistTemplateDto createTemplate(ChecklistTemplateDto templateDto) {
        ChecklistTemplate neueTemplate = new ChecklistTemplate(
                null, templateDto.name(), templateDto.itemBeschreibungen(), false);
        ChecklistTemplate result = checklistTemplateRepository.save(neueTemplate);
        return ChecklistTemplateMapper.toDto(result);
    }

    // PUT /api/checklist-templates/{id} - aktualisiert Name und Punkte
    // einer bestehenden Vorlage. Der standard-Status bleibt dabei immer
    // der bisherige (aus der bestehenden Vorlage übernommen, NICHT aus
    // dem Request) - so kann ich z.B. die Standard-Vorlage "Server"
    // inhaltlich anpassen, ohne dass sie dadurch aus Versehen löschbar
    // würde, und umgekehrt kann niemand eine eigene Vorlage per Edit
    // nachträglich zur Standard-Vorlage machen.
    public ChecklistTemplateDto updateTemplate(String id, ChecklistTemplateDto templateDto) {
        ChecklistTemplate bestehendeTemplate = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(VORLAGE_MIT_ID + id + NICHT_GEFUNDEN));

        ChecklistTemplate aktualisierteTemplate = new ChecklistTemplate(
                bestehendeTemplate.getId(),
                templateDto.name(),
                templateDto.itemBeschreibungen(),
                bestehendeTemplate.isStandard()
        );

        ChecklistTemplate result = checklistTemplateRepository.save(aktualisierteTemplate);
        return ChecklistTemplateMapper.toDto(result);
    }

    // DELETE /api/checklist-templates/{id} - löscht eine Vorlage, außer
    // es handelt sich um eine der zehn Standard-Vorlagen (standard=true)
    // - die sollen nicht aus Versehen verschwinden können. Dafür muss
    // ich die Vorlage jetzt per findById statt nur existsById laden,
    // damit ich den standard-Wert überhaupt prüfen kann.
    public void deleteTemplate(String id) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(VORLAGE_MIT_ID + id + NICHT_GEFUNDEN));

        if (template.isStandard()) {
            throw new IllegalStateException(
                    "Die Standard-Vorlage \"" + template.getName() + "\" kann nicht gelöscht werden");
        }

        checklistTemplateRepository.deleteById(id);
    }
}