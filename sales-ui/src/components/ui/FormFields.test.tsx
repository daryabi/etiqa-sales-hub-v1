import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { Input } from './Input';
import { Select } from './Select';

afterEach(cleanup);

describe('Input and Select labels', () => {
  it('link the label to the field even without an id', () => {
    render(
      <>
        <Input label="Quantity" />
        <Select label="Select Book" options={[{ value: 1, label: 'Effective Java' }]} />
      </>
    );

    const quantity = screen.getByLabelText('Quantity');
    const book = screen.getByLabelText('Select Book');

    expect(quantity.tagName).toBe('INPUT');
    expect(book.tagName).toBe('SELECT');
    expect(quantity.id).not.toBe(book.id);
  });

  it('keep an id that was passed in', () => {
    render(<Input id="email" label="Email" />);

    expect(screen.getByLabelText('Email').id).toBe('email');
  });
});
