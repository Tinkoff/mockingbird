import { createReducer, createEvent } from '@tramvai/state';
import type { NotificationStackProps } from '@platform-ui/notification';

type Notification = NotificationStackProps['notifications'];
export type ServiceState = {
  notifications: Notification[];
};

const storeName = 'notificationsState';
const initialState: ServiceState = {
  notifications: [],
};

export const addToast = createEvent<Notification>('ADD_TOAST_SUCCESS');

export const selector = (state) => state[storeName];

export default createReducer(storeName, initialState).on(
  addToast,
  (state, item) => ({
    ...state,
    notifications: [...state.notifications, item],
  })
);
