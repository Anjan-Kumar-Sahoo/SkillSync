import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderHook } from '@testing-library/react';
import { ActionConfirmProvider, useActionConfirm } from './ActionConfirm';

const Trigger = () => {
  const { requestConfirmation } = useActionConfirm();

  const ask = async () => {
    const result = await requestConfirmation({
      title: 'Delete',
      message: 'Delete this item?',
      confirmLabel: 'Delete now',
      cancelLabel: 'Keep',
    });
    (window as any).__confirmResult = result;
  };

  return <button onClick={ask}>open-confirm</button>;
};

const DefaultTrigger = () => {
  const { requestConfirmation } = useActionConfirm();

  const ask = async () => {
    const result = await requestConfirmation({
      message: 'Proceed with action?',
    });
    (window as any).__defaultConfirmResult = result;
  };

  return <button onClick={ask}>open-default-confirm</button>;
};

describe('ActionConfirm', () => {
  beforeEach(() => {
    (window as any).__confirmResult = undefined;
    (window as any).__defaultConfirmResult = undefined;
  });

  it('resolves true when confirm button is clicked', async () => {
    const user = userEvent.setup();
    render(
      <ActionConfirmProvider>
        <Trigger />
      </ActionConfirmProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'open-confirm' }));
    expect(screen.getByText('Delete this item?')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Delete now' }));
    expect((window as any).__confirmResult).toBe(true);
  });

  it('resolves false on cancel and escape key', async () => {
    const user = userEvent.setup();
    render(
      <ActionConfirmProvider>
        <Trigger />
      </ActionConfirmProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'open-confirm' }));
    await user.keyboard('{Escape}');
    expect((window as any).__confirmResult).toBe(false);

    await user.click(screen.getByRole('button', { name: 'open-confirm' }));
    await user.click(screen.getByRole('button', { name: 'Keep' }));
    expect((window as any).__confirmResult).toBe(false);
  });

  it('throws when hook is used outside provider', () => {
    expect(() => renderHook(() => useActionConfirm())).toThrow(
      'useActionConfirm must be used within ActionConfirmProvider',
    );
  });

  it('uses default labels and ignores non-escape key presses', async () => {
    const user = userEvent.setup();
    render(
      <ActionConfirmProvider>
        <DefaultTrigger />
      </ActionConfirmProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'open-default-confirm' }));

    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
    expect(screen.getByText('Proceed with action?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Yes' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'No' })).toBeInTheDocument();

    await user.keyboard('{Enter}');
    expect(screen.getByText('Proceed with action?')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Yes' }));
    expect((window as any).__defaultConfirmResult).toBe(true);
  });

  it('calls resolverRef.current and sets it to null', async () => {
    const user = userEvent.setup();
    render(
      <ActionConfirmProvider>
        <Trigger />
      </ActionConfirmProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'open-confirm' }));
    // Confirm
    await user.click(screen.getByRole('button', { name: 'Delete now' }));
    // Open again to check resolver is reset
    await user.click(screen.getByRole('button', { name: 'open-confirm' }));
    await user.click(screen.getByRole('button', { name: 'Keep' }));
    // If resolverRef was not reset, this would fail
    expect((window as any).__confirmResult).toBe(false);
  });
});
