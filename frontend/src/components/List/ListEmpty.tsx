import React from 'react';
import Text from '@platform-ui/text';

interface Props {
  text?: string;
}

export default function ListEmpty(props: Props) {
  const { text = 'Данных нет' } = props;
  return <Text size={15}>{text}</Text>;
}
