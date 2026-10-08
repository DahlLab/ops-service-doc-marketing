import { Button, Form } from 'react-bootstrap';

interface DynamicItemListProps {
    // Die reinen Texte der Liste - ob das dahinter ChecklistItemDto[]
    // oder ein einfaches string[] ist, interessiert diese Komponente
    // nicht, das übernimmt die aufrufende Seite über die Callbacks.
    values: string[];
    onItemChange: (index: number, newText: string) => void;
    onItemRemove: (index: number) => void;
    onItemAdd: () => void;
    // Text vor der laufenden Nummer im Platzhalter, z.B. "Punkt" ->
    // "Punkt 1", "Punkt 2", ... - Default passt für beide aktuellen
    // Einsatzstellen (Checklisten-Items UND Vorlagen-Punkte).
    placeholderPrefix?: string;
}

// Ich habe diese Komponente herausgezogen, weil ich in ChecklistenPage.tsx
// zwei fast identische Blöcke hatte: einmal die Items einer Checkliste
// (manueller Modus) und einmal die Punkte einer Vorlage - beides "Liste
// von Textfeldern mit ✕-Button zum Entfernen + Button zum Hinzufügen,
// mindestens ein Eintrag muss bleiben". SonarQube hat das zurecht als
// Code-Duplizierung markiert, und inhaltlich ist es ja wirklich derselbe
// UI-Baustein - nur die dahinterliegenden State-Felder unterscheiden
// sich, und die reicht die aufrufende Seite einfach über die Callbacks
// (onAendern/onEntfernen/onHinzufuegen) rein, statt dass diese Komponente
// selbst wissen müsste, ob sie gerade mit items oder itemBeschreibungen
// arbeitet.
export function DynamicItemList({
    values,
    onItemChange,
    onItemRemove,
    onItemAdd,
    placeholderPrefix = 'Punkt',
}: Readonly<DynamicItemListProps>) {
    return (
        <>
            {values.map((value, index) => (
                <div
                    // Index als key ist hier korrekt - die Einträge sind reine Textfelder
                    // ohne eigene ID, werden nie umsortiert und der Text liegt im State der
                    // aufrufenden Seite (controlled input).
                    // Das NOSONAR muss in derselben Zeile wie der key stehen, sonst
                    // greift die Unterdrückung in SonarCloud nicht.
                    key={index} // NOSONAR
                    className="d-flex gap-2 mb-2"
                >
                    <Form.Control
                        type="text"
                        placeholder={`${placeholderPrefix} ${index + 1}`}
                        value={value}
                        onChange={(e) => onItemChange(index, e.target.value)}
                    />
                    <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => onItemRemove(index)}
                        // Mindestens ein Eintrag muss übrig bleiben - das
                        // Backend verlangt bei beiden (Checkliste UND
                        // Vorlage) mindestens einen ausgefüllten Punkt
                        // (@NotEmpty).
                        disabled={values.length <= 1}
                    >
                        ✕
                    </Button>
                </div>
            ))}
            <Button variant="outline-primary" size="sm" onClick={onItemAdd}>
                + Punkt hinzufügen
            </Button>
        </>
    );
}
