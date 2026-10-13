import { useEffect, useState } from 'react';
import { api } from './api.js';
export function useApi(path) {
    const [result, setResult] = useState({ data: null, error: '', loading: true });
    useEffect(() => {
        const controller = new AbortController();
        api(path, { signal: controller.signal }).then(data => {
            if (!controller.signal.aborted) setResult({ data, error: '', loading: false });
        }).catch(error => {
            if (!controller.signal.aborted) setResult({ data: null, error: error.message, loading: false });
        });
        return () => controller.abort();
    }, [path]);
    return result;
}
