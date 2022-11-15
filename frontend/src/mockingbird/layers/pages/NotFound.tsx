import React from 'react';
import Button from '@platform-ui/button';
import PageHeader from 'src/components/PageHeader/PageHeader';
import { useNavigate } from '@tramvai/module-router';

export default function NotFound() {
  const navigateTo = useNavigate('/');
  return (
    <div>
      <PageHeader title="Страница не найдена" />
      <Button size="l" onClick={navigateTo}>
        Вернуться на главную
      </Button>
    </div>
  );
}
