import React from 'react';
import { Text } from '@mantine/core';

interface Props {
  text?: string;
}

export default function ListEmpty(props: Props) {
  const { text = 'Данных нет' } = props;
  return <Text size="sm">{text}</Text>;
}
