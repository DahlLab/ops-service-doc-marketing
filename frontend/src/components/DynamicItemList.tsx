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
    placeholderPrefix = 'Punkt',
}: Readonly<DynamicItemListProps>) {
    return (
        <>
            {values.map((value, index) => (
                <div

                    key={index}
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
