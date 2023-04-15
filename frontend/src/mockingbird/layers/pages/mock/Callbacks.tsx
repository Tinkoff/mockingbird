import React, { useCallback, useState } from 'react';
import { v4 as generateId } from 'uuid';
import type { BoxProps } from '@mantine/core';
import { Box, Button, Text, Paper, Flex } from '@mantine/core';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconPlus from '@tabler/icons-react/dist/esm/icons/IconPlus';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconTrash from '@tabler/icons-react/dist/esm/icons/IconTrash';
import { parseJSON } from 'src/mockingbird/infrastructure/utils/forms';
import CallbackPopup from './CallbackPopup';
import type { TFormCallback } from './types';
import styles from './Callbacks.css';

type Props = BoxProps & {
  serviceId: string;
  callbacks: TFormCallback[];
  onChange: (callbacks: TFormCallback[]) => void;
};

export default function Callbacks({
  serviceId,
  callbacks,
  onChange,
  ...restProps
}: Props) {
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
    <Box {...restProps}>
      <div className={styles.header}>
        <Text size="md">Коллбэки</Text>
        <Button size="sm" variant="outline" onClick={handleCreate} compact>
          <IconPlus size="1rem" />
        </Button>
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
    </Box>
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
    <Paper shadow="xs" p="md" onClick={handleClick}>
      <Flex align="center" justify="space-between">
        <Text size="md">{formatTitle(callback)}</Text>
        <Button size="xs" variant="outline" onClick={handleDelete} compact>
          <IconTrash size="1rem" />
        </Button>
      </Flex>
    </Paper>
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
