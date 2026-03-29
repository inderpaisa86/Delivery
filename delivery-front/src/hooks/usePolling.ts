import { useQuery } from '@tanstack/react-query';

/**
 * Hook genérico de polling. Refresca cada `interval` ms.
 */
export function usePolling<T>(
  key: string[],
  fetcher: () => Promise<T>,
  interval = 5000,
  enabled = true,
) {
  return useQuery({
    queryKey: key,
    queryFn: fetcher,
    refetchInterval: interval,
    enabled,
  });
}
