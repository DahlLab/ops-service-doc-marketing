import { useTranslation } from 'react-i18next';
import { Button, Form } from 'react-bootstrap';

interface DynamicItemListProps {
    values: string[];
    onItemChange: (index: number, newText: string) => void;
    onItemRemove: (index: number) => void;
    onItemAdd: () => void;

    placeholderPrefix?: string;
}

export function DynamicItemList({
    values,
    onItemChange,
    onItemRemove,
    onItemAdd,
    placeholderPrefix,
}: Readonly<DynamicItemListProps>) {
    const { t } = useTranslation('checklists');
    const prefix = placeholderPrefix ?? t('itemList.placeholderPrefix');
    return (
        <>
            {values.map((value, index) => (
                <div

                    key={index}
                    className="d-flex gap-2 mb-2"
                >
                    <Form.Control
                        type="text"
                        placeholder={`${prefix} ${index + 1}`}
                        value={value}
                        onChange={(e) => onItemChange(index, e.target.value)}
                    />
                    <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => onItemRemove(index)}

                        disabled={values.length <= 1}
                    >
                        ✕
                    </Button>
                </div>
            ))}
            <Button variant="outline-primary" size="sm" onClick={onItemAdd}>
                {t('itemList.add')}
            </Button>
        </>
    );
}
