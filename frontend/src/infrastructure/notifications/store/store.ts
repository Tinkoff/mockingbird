import type { ReactNode } from 'react';
import { createReducer, createEvent } from '@tramvai/state';

export type Notification =
  | {
      type: 'error';
      icon?: ReactNode;
      timer: number;
      title: string;
      description?: string;
    }
  | {
      type: 'success';
      icon?: ReactNode;
      timer: number;
      title: string;
      description?: string;
    }
  | {
      type: 'info';
      icon?: ReactNode;
      timer: number;
      title: string;
      description?: string;
    };

export type StoreState = {
  notifications: Notification[];
};

const storeName = 'notificationsState' as const;
const initialState: StoreState = {
  notifications: [],
};

export const addToast = createEvent<Notification>('ADD_TOAST_SUCCESS');
export const removeToast = createEvent<Notification>('REMOVE_TOAST_SUCCESS');

export const selector = (state: { notificationsState: StoreState }) =>
  state[storeName];

export default createReducer(storeName, initialState)
  .on(addToast, (state, item) => ({
    ...state,
    notifications: [...state.notifications, item],
  }))
  .on(removeToast, (state, item) => ({
    ...state,
    notifications: state.notifications.filter((n) => n.title !== item.title),
  }));
