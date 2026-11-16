import type { ChangeEvent } from 'react';
import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useFormFields } from './useFormFields';

const EMPTY_CUSTOMER = { firstName: '', email: '' };

function typed(value: string) {
  return { target: { value } } as ChangeEvent<HTMLInputElement>;
}

describe('useFormFields', () => {
  it('starts with the initial values', () => {
    const { result } = renderHook(() => useFormFields(EMPTY_CUSTOMER));

    expect(result.current.values).toEqual(EMPTY_CUSTOMER);
    expect(result.current.bind('firstName').value).toBe('');
  });

  it('updates only the field that changed', () => {
    const { result } = renderHook(() => useFormFields(EMPTY_CUSTOMER));

    act(() => result.current.bind('firstName').onChange(typed('Aisyah')));

    expect(result.current.values).toEqual({ firstName: 'Aisyah', email: '' });
    expect(result.current.bind('firstName').value).toBe('Aisyah');
  });

  it('reset puts every field back', () => {
    const { result } = renderHook(() => useFormFields(EMPTY_CUSTOMER));
    act(() => result.current.bind('email').onChange(typed('aisyah@example.com')));

    act(() => result.current.reset());

    expect(result.current.values).toEqual(EMPTY_CUSTOMER);
  });
});
