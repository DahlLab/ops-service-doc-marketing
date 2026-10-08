package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "checklist_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplate {
    private String id;
    private String name;
    private List<String> itemDescriptions;

    private boolean builtIn;
}
