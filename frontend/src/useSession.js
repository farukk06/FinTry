import { useSyncExternalStore } from 'react';
import { getSession, subscribe } from './session.js';
export function useSession() { return useSyncExternalStore(subscribe, getSession, getSession); }
