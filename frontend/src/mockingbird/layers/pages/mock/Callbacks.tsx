import React, { useCallback, useState } from 'react';
import Button from '@platform-ui/button';
import { Int16Plus, Int16Close } from '@platform-ui/iconsPack';
import Island from '@platform-ui/island';
import generateId from '@platform-ui/generateId';
import Text from '@platform-ui/text';
import { parseJSON } from 'src/mockingbird/infrastructure/utils/forms';
import CallbackPopup from './CallbackPopup';
import type { TFormCallback } from './types';
import styles from './Callbacks.css';

interface Props {
  serviceId: string;
  callbacks: TFormCallback[];
  onChange: (callbacks: TFormCallback[]) => void;
}

export default function Callbacks({ serviceId, callbacks, onChange }: Props) {
  const [index, setIndex] = useState(-1);
  const handleCreate = useCallback(() => {
    setIndex(callbacks.length);
  }, [callbacks.length]);
  const handleEdit = useCallback((i) => setIndex(i), []);
  const handleDelete = useCallback(
    (i: number) => {
      const result = callbacks.filter((_, ind) => ind !== i);
      onChange(result);
    },
    [callbacks, onChange]
  );
  const handleSave = useCallback(
    (callback: TFormCallback) => {
      const result = [...callbacks];
      result[index] = callback;
      if (!result[index].id) result[index].id = generateId();
      onChange(result);
      setIndex(-1);
    },
    [index, callbacks, onChange]
  );
  const handleClose = useCallback(() => {
    setIndex(-1);
  }, []);
  return (
    <div>
      <div className={styles.header}>
        <Text size={17}>Коллбэки</Text>
        <Button
          size="s"
          theme="outlineDark"
          icon={Int16Plus}
          onClick={handleCreate}
          round
        />
      </div>
      {callbacks.length > 0 && (
        <div className={styles.items}>
          {callbacks.map((callback, i) => (
            <CallbackItem
              key={callback.id}
              index={i}
              callback={callback}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
      {index > -1 && (
        <CallbackPopup
          serviceId={serviceId}
          callback={callbacks[index]}
          onSave={handleSave}
          onClose={handleClose}
        />
      )}
    </div>
  );
}

interface ItemProps {
  callback: TFormCallback;
  index: number;
  onEdit: (index: number) => void;
  onDelete: (index: number) => void;
}

function CallbackItem({ callback, index, onEdit, onDelete }: ItemProps) {
  const handleClick = useCallback(() => {
    onEdit(index);
  }, [index, onEdit]);
  const handleDelete = useCallback(
    (e) => {
      e.stopPropagation();
      onDelete(index);
    },
    [index, onDelete]
  );
  return (
    <Island
      title={formatTitle(callback)}
      clickable
      onClick={handleClick}
      side={
        <Button
          size="xs"
          theme="outlineDark"
          icon={Int16Close}
          onClick={handleDelete}
          round
        />
      }
    />
  );
}

function formatTitle(callback: TFormCallback) {
  const delay = formatDelay(callback.delay);
  switch (callback.type) {
    case 'http': {
      const { url, method } = parseJSON(callback.request);
      return ['HTTP', method, url, delay].filter(Boolean).join(' ');
    }
    case 'message':
      return ['MESSAGE', callback.destination, delay].filter(Boolean).join(' ');
    default:
      return 'UNKNOWN';
  }
}

function formatDelay(delay?: string) {
  return delay ? `(delay ${delay})` : null;
}
