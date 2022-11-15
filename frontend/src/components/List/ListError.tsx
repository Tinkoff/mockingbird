import React from 'react';
import Link from '@platform-ui/link';
import Text from '@platform-ui/text';

interface Props {
  text?: string;
  onRetry?: () => void;
}

export default function ListError(props: Props) {
  const { text = 'Ошибка при загрузке данных. ', onRetry } = props;
  return (
    <Text size={15} color="red">
      {text}
      {onRetry && <Link onClick={onRetry}>Попробовать снова</Link>}
    </Text>
  );
}
