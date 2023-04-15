import React from 'react';
import { Button } from '@mantine/core';
import { useNavigate } from '@tramvai/module-router';
import PageHeader from 'src/components/PageHeader/PageHeader';

export default function NotFound() {
  const navigateTo = useNavigate('/');
  return (
    <div>
      <PageHeader title="Страница не найдена" />
      <Button variant="outline" size="md" onClick={navigateTo}>
        Вернуться на главную
      </Button>
    </div>
  );
}
