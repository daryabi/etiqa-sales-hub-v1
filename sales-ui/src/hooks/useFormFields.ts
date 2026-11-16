import { useCallback, useState, type ChangeEvent } from 'react';

export function useFormFields<TFields extends Record<string, string>>(initialValues: TFields) {
  const [values, setValues] = useState(initialValues);

  const bind = useCallback(
    (field: keyof TFields) => ({
      value: values[field],
      onChange: (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
        setValues((current) => ({ ...current, [field]: event.target.value })),
    }),
    [values]
  );

  const reset = useCallback(() => setValues(initialValues), [initialValues]);

  return { values, bind, reset };
}
