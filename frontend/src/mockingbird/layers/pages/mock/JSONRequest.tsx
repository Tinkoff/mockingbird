import React, { useCallback, useEffect, useState } from 'react';
import Text from '@platform-ui/text';
import Textarea from '@platform-ui/textarea';
import Toggle from '@platform-ui/toggle';
import Copy from 'src/components/Copy/Copy';
import { stringifyJSON } from 'src/mockingbird/infrastructure/utils/forms';
import styles from './JSONRequest.css';

interface Props {
  getValues: () => string | Promise<string>;
}

export default function JSONRequest({ getValues }: Props) {
  const [show, setShow] = useState(false);
  const handleChange = useCallback(
    (_, { checked }) => {
      setShow(checked);
    },
    [setShow]
  );
  return (
    <div>
      <div className={styles.header}>
        <Text size={17}>Показать как JSON</Text>
        <Toggle checked={show} onChange={handleChange} />
      </div>
      {show && (
        <div className={styles.description}>
          <Text size={15} color="grey">
            Чтобы ваш МОК наверняка работал, рекомендуем предварительно
            сохранить, если вы вносили правки
          </Text>
        </div>
      )}
      {show && <JSONViewer value={getValues()} />}
    </div>
  );
}

function JSONViewer({ value }: { value: string | Promise<string> }) {
  const [v, setValue] = useState('');
  useEffect(() => {
    if (value.then) value.then((val) => setValue(stringifyJSON(val)));
    else setValue(stringifyJSON(value));
  }, [value, setValue]);
  return (
    <div className={styles.json}>
      <Textarea value={v} rightContent={<Copy targetValue={v} />} rows={16} />
    </div>
  );
}
