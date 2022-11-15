import React from 'react';
import { useSelector } from '@tramvai/state';
import { NotificationStack } from '@platform-ui/notification';
import store, { selector } from '../store/store';

export default function Stack() {
  const { notifications } = useSelector(store, selector);
  return <NotificationStack notifications={notifications} />;
}
