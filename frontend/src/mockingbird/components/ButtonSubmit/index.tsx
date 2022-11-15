import React from 'react';
import Button from '@platform-ui/button';
import styles from './ButtonSubmit.css';

interface Props {
  children: React.ReactNode;
  disabled?: boolean;
}

export default function ButtonSubmit({ children, disabled }: Props) {
  return (
    <div className={styles.root}>
      <Button size="l" type="submit" disabled={disabled}>
        {children}
      </Button>
    </div>
  );
}
