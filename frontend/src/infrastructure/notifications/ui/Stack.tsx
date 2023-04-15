import React, { useEffect } from 'react';
import { useSelector, useActions } from '@tramvai/state';
import type { NotificationProps } from '@mantine/core';
import { Notification, Box } from '@mantine/core';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconCheck from '@tabler/icons-react/dist/esm/icons/IconCheck';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconX from '@tabler/icons-react/dist/esm/icons/IconX';
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore for tree shaking purposes
import IconInfoCircle from '@tabler/icons-react/dist/esm/icons/IconInfoCircle';
import store, { selector } from '../store/store';
import type { Notification as Notify } from '../store/store';
import { removeToastAction } from '../store/actions';

export default function Stack() {
  const { notifications } = useSelector(store, selector);
  return <NotificationsStack notifications={notifications} />;
}

function NotificationsStack({ notifications }: { notifications: Notify[] }) {
  const removeNotification = useActions(removeToastAction);
  if (!notifications.length) return null;
  return (
    <Box
      sx={{
        width: '20rem',
        position: 'fixed',
        top: '6rem',
        right: '1.625rem',
        // eslint-disable-next-line @typescript-eslint/naming-convention
        '& > * + *': {
          marginTop: '1rem',
        },
      }}
    >
      {notifications.map((n) => (
        <NotificationItem
          key={n.title}
          title={n.title}
          color={getColor(n)}
          icon={<IconSwitcher type={n.type} />}
          timer={n.timer}
          onRemove={() => removeNotification(n)}
          onClose={() => removeNotification(n)}
        >
          {n.description}
        </NotificationItem>
      ))}
    </Box>
  );
}

function NotificationItem(
  props: NotificationProps & { timer: number; onRemove: () => void }
) {
  const { children, timer, onRemove, ...restProps } = props;
  useEffect(() => {
    setTimeout(onRemove, timer);
  }, [timer, onRemove]);
  return <Notification {...restProps}>{children}</Notification>;
}

function getColor(n: Notify) {
  switch (n.type) {
    case 'error':
      return 'red';
    case 'success':
      return 'green';
    default:
      return 'cyan';
  }
}

function IconSwitcher({ type }: { type: Notify['type'] }) {
  switch (type) {
    case 'error':
      return <IconX size="1.1rem" />;
    case 'success':
      return <IconCheck size="1.1rem" />;
    default:
      return <IconInfoCircle size="1.1rem" />;
  }
}
