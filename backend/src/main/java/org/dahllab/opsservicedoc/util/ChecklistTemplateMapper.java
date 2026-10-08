package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.ChecklistTemplateDto;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;

public class ChecklistTemplateMapper {
    private ChecklistTemplateMapper() {
    }

    public static ChecklistTemplateDto toDto(ChecklistTemplate template) {
        return new ChecklistTemplateDto(
                template.getId(),
                template.getName(),
                template.getItemDescriptions(),
                template.isBuiltIn()
        );
    }
}
