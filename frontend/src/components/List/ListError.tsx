import React from 'react';
import { Text, Button } from '@mantine/core';

interface Props {
  text?: string;
  onRetry?: () => void;
}

export default function ListError(props: Props) {
  const { text = 'Ошибка при загрузке данных ', onRetry } = props;
  return (
    <Text size="sm" color="red">
      {text}
      {onRetry && (
        <Button variant="subtle" compact onClick={onRetry}>
          Попробовать снова
        </Button>
      )}
    </Text>
  );
}
