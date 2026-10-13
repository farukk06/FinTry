import '@testing-library/jest-dom/vitest';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { clearSession } from '../session.js';
afterEach(() => { cleanup(); clearSession(); vi.restoreAllMocks(); vi.useRealTimers(); });
